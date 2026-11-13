package com.cargo.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.cargo.util.DBConnection;

@WebServlet("/customer/shipments")
public class ShipmentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("userId") == null
                || !"CUSTOMER".equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        List<Shipment> shipmentList = new ArrayList<>();

        String sql =
            "SELECT s.shipment_id, s.booking_id, s.tracking_number, " +
            "s.current_location, s.shipment_status, s.shipped_date, s.delivered_date, " +
            "v.vehicle_number, d.driver_name, d.phone AS driver_phone " +
            "FROM shipments s " +
            "JOIN bookings b ON s.booking_id = b.booking_id " +
            "JOIN customers c ON b.customer_id = c.customer_id " +
            "LEFT JOIN vehicles v ON s.vehicle_id = v.vehicle_id " +
            "LEFT JOIN drivers d ON s.driver_id = d.driver_id " +
            "WHERE c.user_id = ? " +
            "ORDER BY s.shipment_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Shipment s = new Shipment();

                    s.id = rs.getInt("shipment_id");
                    s.bookingId = rs.getInt("booking_id");
                    s.trackingNumber = rs.getString("tracking_number");
                    s.location = rs.getString("current_location");
                    s.status = rs.getString("shipment_status");
                    s.shippedDate = rs.getTimestamp("shipped_date");
                    s.deliveredDate = rs.getTimestamp("delivered_date");
                    s.vehicleNumber = rs.getString("vehicle_number");
                    s.driverName = rs.getString("driver_name");
                    s.driverPhone = rs.getString("driver_phone");

                    shipmentList.add(s);
                }
            }

        } catch (Exception e) {
            throw new ServletException("Unable to load shipments.", e);
        }

        request.setAttribute("shipmentList", shipmentList);
        request.getRequestDispatcher("/customer/shipments.jsp")
               .forward(request, response);
    }

    public static class Shipment {
        public int id;
        public int bookingId;
        public String trackingNumber;
        public String location;
        public String status;
        public java.sql.Timestamp shippedDate;
        public java.sql.Timestamp deliveredDate;
        public String vehicleNumber;
        public String driverName;
        public String driverPhone;
    }
}
