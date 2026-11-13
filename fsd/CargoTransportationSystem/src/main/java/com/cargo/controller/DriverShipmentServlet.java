package com.cargo.controller;

import com.cargo.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/driver/update-status")
public class DriverShipmentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null
                || !"DRIVER".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String shipmentIdStr = request.getParameter("shipmentId");
        String newStatus = request.getParameter("newStatus");
        String location = request.getParameter("location");
        String remarks = request.getParameter("remarks");

        if (shipmentIdStr == null || newStatus == null || shipmentIdStr.trim().isEmpty() || newStatus.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/driver/dashboard?error=invalid_input");
            return;
        }

        int shipmentId = Integer.parseInt(shipmentIdStr.trim());
        newStatus = newStatus.trim().toUpperCase();

        try (Connection con = DBConnection.getConnection()) {
            // 1. Resolve Driver ID
            int driverId = -1;
            String driverSql = "SELECT driver_id FROM drivers WHERE user_id = ?";
            try (PreparedStatement ps = con.prepareStatement(driverSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        driverId = rs.getInt("driver_id");
                    }
                }
            }

            if (driverId == -1) {
                response.sendRedirect(request.getContextPath() + "/driver/dashboard?error=driver_not_found");
                return;
            }

            // 2. Verify Shipment belongs to this driver
            int bookingId = -1;
            Integer vehicleId = null;
            String currentStatus = null;

            String checkSql = "SELECT booking_id, vehicle_id, shipment_status FROM shipments WHERE shipment_id = ? AND driver_id = ?";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, shipmentId);
                ps.setInt(2, driverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bookingId = rs.getInt("booking_id");
                        vehicleId = (Integer) rs.getObject("vehicle_id");
                        currentStatus = rs.getString("shipment_status");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/driver/dashboard?error=unauthorized_shipment");
                        return;
                    }
                }
            }

            // 3. Begin transaction for atomic status update
            con.setAutoCommit(false);
            try {
                // Insert into delivery_status log
                String logSql = "INSERT INTO delivery_status (shipment_id, status, location, remarks, updated_at) VALUES (?, ?, ?, ?, NOW())";
                try (PreparedStatement psLog = con.prepareStatement(logSql)) {
                    psLog.setInt(1, shipmentId);
                    psLog.setString(2, newStatus);
                    psLog.setString(3, (location != null && !location.trim().isEmpty()) ? location.trim() : "En Route");
                    psLog.setString(4, (remarks != null && !remarks.trim().isEmpty()) ? remarks.trim() : "Status updated by driver");
                    psLog.executeUpdate();
                }

                // Update shipment table
                StringBuilder updateShipment = new StringBuilder("UPDATE shipments SET shipment_status = ?, current_location = ?");
                if ("PICKED_UP".equals(newStatus)) {
                    updateShipment.append(", shipped_date = NOW()");
                } else if ("DELIVERED".equals(newStatus)) {
                    updateShipment.append(", delivered_date = NOW()");
                }
                updateShipment.append(" WHERE shipment_id = ?");

                try (PreparedStatement psShip = con.prepareStatement(updateShipment.toString())) {
                    psShip.setString(1, newStatus);
                    psShip.setString(2, (location != null && !location.trim().isEmpty()) ? location.trim() : "In Transit");
                    psShip.setInt(3, shipmentId);
                    psShip.executeUpdate();
                }

                // If Delivered, mark booking as COMPLETED and release vehicle and driver to AVAILABLE
                if ("DELIVERED".equals(newStatus)) {
                    // Update booking status
                    if (bookingId > 0) {
                        try (PreparedStatement psBook = con.prepareStatement("UPDATE bookings SET booking_status = 'COMPLETED' WHERE booking_id = ?")) {
                            psBook.setInt(1, bookingId);
                            psBook.executeUpdate();
                        }
                    }

                    // Release vehicle to AVAILABLE
                    if (vehicleId != null) {
                        try (PreparedStatement psVeh = con.prepareStatement("UPDATE vehicles SET status = 'AVAILABLE' WHERE vehicle_id = ?")) {
                            psVeh.setInt(1, vehicleId);
                            psVeh.executeUpdate();
                        }
                    }

                    // Release driver to AVAILABLE
                    try (PreparedStatement psDrv = con.prepareStatement("UPDATE drivers SET status = 'AVAILABLE' WHERE driver_id = ?")) {
                        psDrv.setInt(1, driverId);
                        psDrv.executeUpdate();
                    }
                }

                con.commit();
                response.sendRedirect(request.getContextPath() + "/driver/dashboard?updated=success");

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Driver shipment update error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/driver/dashboard?error=server");
        }
    }
}
