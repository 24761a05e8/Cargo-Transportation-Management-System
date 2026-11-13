
package com.cargo.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.cargo.util.DBConnection;

@WebServlet("/admin/bookings")
public class AdminBookingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("userId") == null
                || !"ADMIN".equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        List<Booking> bookings = new ArrayList<>();

        String sql =
            "SELECT b.booking_id, b.customer_id, b.cargo_id, " +
            "b.pickup_date, b.expected_delivery_date, " +
            "b.booking_status, b.total_amount, " +
            "c.cargo_type, pa.city AS pickup_city, da.city AS delivery_city " +
            "FROM bookings b " +
            "JOIN cargo c ON b.cargo_id = c.cargo_id " +
            "JOIN addresses pa ON b.pickup_address_id = pa.address_id " +
            "JOIN addresses da ON b.delivery_address_id = da.address_id " +
            "ORDER BY b.booking_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Booking b = new Booking();
                b.id = rs.getInt("booking_id");
                b.customerId = rs.getInt("customer_id");
                b.cargoId = rs.getInt("cargo_id");
                b.cargoType = rs.getString("cargo_type");
                b.pickupCity = rs.getString("pickup_city");
                b.deliveryCity = rs.getString("delivery_city");
                b.pickupDate = rs.getDate("pickup_date");
                b.expectedDate = rs.getDate("expected_delivery_date");
                b.status = rs.getString("booking_status");
                b.amount = rs.getBigDecimal("total_amount");
                bookings.add(b);
            }

        } catch (Exception e) {
            throw new ServletException("Unable to load bookings.", e);
        }

        request.setAttribute("bookings", bookings);
        request.getRequestDispatcher("/admin/bookings.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("userId") == null
                || !"ADMIN".equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        try {
            int bookingId = Integer.parseInt(
                request.getParameter("bookingId"));
            String action = request.getParameter("action");

            if (!"confirm".equals(action) && !"cancel".equals(action)) {
                response.sendError(400, "Invalid booking action.");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                con.setAutoCommit(false);

                try {
                    String statusSql =
                        "UPDATE bookings SET booking_status = ? " +
                        "WHERE booking_id = ? AND booking_status = 'PENDING'";

                    int updated;

                    try (PreparedStatement ps =
                             con.prepareStatement(statusSql)) {
                        ps.setString(1,
                            "confirm".equals(action) ? "CONFIRMED" : "CANCELLED");
                        ps.setInt(2, bookingId);
                        updated = ps.executeUpdate();
                    }

                    if (updated == 0) {
                        con.rollback();
                        response.sendRedirect(
                            request.getContextPath() +
                            "/admin/bookings?message=unchanged");
                        return;
                    }

                    if ("confirm".equals(action)) {
                        String trackingNumber =
                            "CTMS" + UUID.randomUUID()
                                .toString().replace("-", "")
                                .substring(0, 12).toUpperCase();

                        String shipmentSql =
                            "INSERT INTO shipments " +
                            "(booking_id, tracking_number, current_location, shipment_status) " +
                            "SELECT ?, ?, ?, 'PENDING' " +
                            "WHERE NOT EXISTS " +
                            "(SELECT 1 FROM shipments WHERE booking_id = ?)";

                        try (PreparedStatement ps =
                                 con.prepareStatement(shipmentSql)) {
                            ps.setInt(1, bookingId);
                            ps.setString(2, trackingNumber);
                            ps.setString(3, "Booking Processing Center");
                            ps.setInt(4, bookingId);
                            ps.executeUpdate();
                        }
                    }

                    con.commit();

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }

            response.sendRedirect(
                request.getContextPath() + "/admin/bookings?message=updated");

        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid booking ID.");
        } catch (Exception e) {
            throw new ServletException("Unable to update booking.", e);
        }
    }

    public static class Booking {
        public int id;
        public int customerId;
        public int cargoId;
        public String cargoType;
        public String pickupCity;
        public String deliveryCity;
        public java.sql.Date pickupDate;
        public java.sql.Date expectedDate;
        public String status;
        public java.math.BigDecimal amount;
    }
}
