
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/shipments")
public class AdminShipmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String[] STATUSES = {
        "PENDING",
        "PICKED_UP",
        "IN_TRANSIT",
        "OUT_FOR_DELIVERY",
        "DELIVERED",
        "FAILED"
    };

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        return session != null
            && "admin".equalsIgnoreCase(
                String.valueOf(session.getAttribute("role"))
            );
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendRedirect(
                request.getContextPath() + "/login.jsp"
            );
            return;
        }

        List<Map<String, Object>> shipments = new ArrayList<>();
        List<Map<String, Object>> vehicles = new ArrayList<>();

        String shipmentSql =
            "SELECT s.shipment_id, s.booking_id, s.tracking_number, " +
            "s.current_location, s.shipment_status, s.shipped_date, " +
            "s.delivered_date, s.vehicle_id, s.driver_id, " +
            "v.vehicle_number, d.driver_name " +
            "FROM shipments s " +
            "LEFT JOIN vehicles v ON s.vehicle_id = v.vehicle_id " +
            "LEFT JOIN drivers d ON s.driver_id = d.driver_id " +
            "ORDER BY s.shipment_id DESC";

        String vehicleSql =
            "SELECT v.vehicle_id, v.vehicle_number, v.vehicle_type, " +
            "v.status, v.driver_id, d.driver_name " +
            "FROM vehicles v " +
            "LEFT JOIN drivers d ON v.driver_id = d.driver_id " +
            "ORDER BY v.vehicle_number";

        try (Connection con = DBConnection.getConnection()) {

            try (PreparedStatement ps =
                    con.prepareStatement(shipmentSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("shipmentId", rs.getInt("shipment_id"));
                    row.put("bookingId", rs.getInt("booking_id"));
                    row.put("trackingNumber",
                            rs.getString("tracking_number"));
                    row.put("currentLocation",
                            rs.getString("current_location"));
                    row.put("status",
                            rs.getString("shipment_status"));
                    row.put("shippedDate",
                            rs.getTimestamp("shipped_date"));
                    row.put("deliveredDate",
                            rs.getTimestamp("delivered_date"));
                    row.put("vehicleId",
                            rs.getObject("vehicle_id"));
                    row.put("driverId",
                            rs.getObject("driver_id"));
                    row.put("vehicleNumber",
                            rs.getString("vehicle_number"));
                    row.put("driverName",
                            rs.getString("driver_name"));

                    shipments.add(row);
                }
            }

            try (PreparedStatement ps =
                    con.prepareStatement(vehicleSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("vehicleId",
                            rs.getInt("vehicle_id"));
                    row.put("vehicleNumber",
                            rs.getString("vehicle_number"));
                    row.put("vehicleType",
                            rs.getString("vehicle_type"));
                    row.put("status",
                            rs.getString("status"));
                    row.put("driverId",
                            rs.getObject("driver_id"));
                    row.put("driverName",
                            rs.getString("driver_name"));

                    vehicles.add(row);
                }
            }

        } catch (SQLException e) {
            throw new ServletException(
                "Unable to load shipment management data.", e
            );
        }

        request.setAttribute("shipments", shipments);
        request.setAttribute("vehicles", vehicles);

        request.getRequestDispatcher("/admin/shipments.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendRedirect(
                request.getContextPath() + "/login.jsp"
            );
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("assign".equals(action)) {
                assignVehicleAndDriver(request);
            } else if ("updateStatus".equals(action)) {
                updateShipmentStatus(request);
            } else {
                throw new IllegalArgumentException(
                    "Invalid shipment action."
                );
            }

            response.sendRedirect(
                request.getContextPath() + "/admin/shipments?success=1"
            );

        } catch (IllegalArgumentException e) {
            response.sendRedirect(
                request.getContextPath()
                + "/admin/shipments?error="
                + java.net.URLEncoder.encode(
                    e.getMessage(), "UTF-8"
                )
            );

        } catch (SQLException e) {
            throw new ServletException(
                "Unable to update shipment.", e
            );
        }
    }

    private void assignVehicleAndDriver(
            HttpServletRequest request) throws SQLException {

        int shipmentId = parseId(
            request.getParameter("shipmentId"), "Shipment"
        );

        int vehicleId = parseId(
            request.getParameter("vehicleId"), "Vehicle"
        );

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int driverId;

                // Lock the selected vehicle while assigning it.
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT driver_id, status FROM vehicles " +
                    "WHERE vehicle_id = ? FOR UPDATE")) {

                    ps.setInt(1, vehicleId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                "Vehicle not found."
                            );
                        }

                        if (!"AVAILABLE".equals(
                                rs.getString("status"))) {
                            throw new IllegalArgumentException(
                                "Selected vehicle is not available."
                            );
                        }

                        Object value = rs.getObject("driver_id");

                        if (value == null) {
                            throw new IllegalArgumentException(
                                "Assign an available driver to this " +
                                "vehicle first."
                            );
                        }

                        driverId = ((Number) value).intValue();
                    }
                }

                // Lock and verify the assigned driver.
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT status FROM drivers " +
                    "WHERE driver_id = ? FOR UPDATE")) {

                    ps.setInt(1, driverId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()
                                || !"AVAILABLE".equals(
                                    rs.getString("status"))) {
                            throw new IllegalArgumentException(
                                "The vehicle's driver is not available."
                            );
                        }
                    }
                }

                // Lock and verify the shipment.
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT shipment_status, vehicle_id " +
                    "FROM shipments WHERE shipment_id = ? FOR UPDATE")) {

                    ps.setInt(1, shipmentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                "Shipment not found."
                            );
                        }

                        String status =
                            rs.getString("shipment_status");

                        if ("DELIVERED".equals(status)
                                || "FAILED".equals(status)) {
                            throw new IllegalArgumentException(
                                "A completed shipment cannot be reassigned."
                            );
                        }

                        if (rs.getObject("vehicle_id") != null) {
                            throw new IllegalArgumentException(
                                "This shipment already has a vehicle. " +
                                "Finish or cancel its current assignment " +
                                "before assigning another vehicle."
                            );
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE shipments SET vehicle_id = ?, driver_id = ? " +
                    "WHERE shipment_id = ?")) {

                    ps.setInt(1, vehicleId);
                    ps.setInt(2, driverId);
                    ps.setInt(3, shipmentId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "Shipment assignment failed."
                        );
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE vehicles SET status = 'IN_USE' " +
                    "WHERE vehicle_id = ? AND status = 'AVAILABLE'")) {

                    ps.setInt(1, vehicleId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "Vehicle availability changed. Try again."
                        );
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE drivers SET status = 'ASSIGNED' " +
                    "WHERE driver_id = ? AND status = 'AVAILABLE'")) {

                    ps.setInt(1, driverId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "Driver availability changed. Try again."
                        );
                    }
                }

                con.commit();

            } catch (Exception e) {
                con.rollback();

                if (e instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e;
                }

                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }

                throw new SQLException(
                    "Assignment failed.", e
                );

            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private void updateShipmentStatus(
            HttpServletRequest request) throws SQLException {

        int shipmentId = parseId(
            request.getParameter("shipmentId"), "Shipment"
        );

        String newStatus = request.getParameter("status");
        String location = request.getParameter("currentLocation");

        boolean validStatus = false;

        if (newStatus != null) {
            for (String status : STATUSES) {
                if (status.equals(newStatus)) {
                    validStatus = true;
                    break;
                }
            }
        }

        if (!validStatus) {
            throw new IllegalArgumentException(
                "Invalid shipment status."
            );
        }

        if (location != null) {
            location = location.trim();
            if (location.length() > 255) {
                throw new IllegalArgumentException(
                    "Location must be 255 characters or fewer."
                );
            }
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                String oldStatus;
                Object vehicleValue;
                Object driverValue;

                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT shipment_status, vehicle_id, driver_id " +
                    "FROM shipments WHERE shipment_id = ? FOR UPDATE")) {

                    ps.setInt(1, shipmentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                "Shipment not found."
                            );
                        }

                        oldStatus =
                            rs.getString("shipment_status");
                        vehicleValue = rs.getObject("vehicle_id");
                        driverValue = rs.getObject("driver_id");
                    }
                }

                if ("DELIVERED".equals(oldStatus)
                        || "FAILED".equals(oldStatus)) {
                    throw new IllegalArgumentException(
                        "This shipment is already closed."
                    );
                }

                if (("PICKED_UP".equals(newStatus)
                        || "IN_TRANSIT".equals(newStatus)
                        || "OUT_FOR_DELIVERY".equals(newStatus)
                        || "DELIVERED".equals(newStatus))
                        && (vehicleValue == null
                            || driverValue == null)) {
                    throw new IllegalArgumentException(
                        "Assign a vehicle and driver before " +
                        "moving the shipment forward."
                    );
                }

                String sql =
                    "UPDATE shipments SET shipment_status = ?, " +
                    "current_location = ?, " +
                    "shipped_date = CASE WHEN ? = 'PICKED_UP' " +
                    "AND shipped_date IS NULL THEN CURRENT_TIMESTAMP " +
                    "ELSE shipped_date END, " +
                    "delivered_date = CASE WHEN ? = 'DELIVERED' " +
                    "THEN CURRENT_TIMESTAMP ELSE delivered_date END " +
                    "WHERE shipment_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(sql)) {

                    ps.setString(1, newStatus);
                    ps.setString(2,
                        location == null || location.isEmpty()
                        ? null : location);
                    ps.setString(3, newStatus);
                    ps.setString(4, newStatus);
                    ps.setInt(5, shipmentId);

                    ps.executeUpdate();
                }

                if ("DELIVERED".equals(newStatus)) {
                    int vehicleId =
                        ((Number) vehicleValue).intValue();
                    int driverId =
                        ((Number) driverValue).intValue();

                    try (PreparedStatement ps =
                            con.prepareStatement(
                                "UPDATE vehicles SET status = 'AVAILABLE' " +
                                "WHERE vehicle_id = ?")) {
                        ps.setInt(1, vehicleId);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps =
                            con.prepareStatement(
                                "UPDATE drivers SET status = 'AVAILABLE' " +
                                "WHERE driver_id = ?")) {
                        ps.setInt(1, driverId);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps =
                            con.prepareStatement(
                                "UPDATE bookings SET booking_status = " +
                                "'COMPLETED' WHERE booking_id = " +
                                "(SELECT booking_id FROM shipments " +
                                "WHERE shipment_id = ?)")) {
                        ps.setInt(1, shipmentId);
                        ps.executeUpdate();
                    }
                }

                con.commit();

            } catch (Exception e) {
                con.rollback();

                if (e instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e;
                }

                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }

                throw new SQLException(
                    "Status update failed.", e
                );

            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private int parseId(String value, String label) {
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid " + label.toLowerCase() + " ID."
            );
        }
    }
}
