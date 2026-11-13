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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/driver/dashboard")
public class DriverDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null
                || !"DRIVER".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String ctx = request.getContextPath();

        Map<String, Object> driverInfo = new HashMap<>();
        Map<String, Object> vehicleInfo = new HashMap<>();
        List<Map<String, Object>> activeDeliveries = new ArrayList<>();
        List<Map<String, Object>> completedDeliveries = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {

            // 1. Fetch Driver Details
            int driverId = -1;
            String driverSql = "SELECT driver_id, driver_name, phone, license_number, experience_years, status FROM drivers WHERE user_id = ?";
            try (PreparedStatement ps = con.prepareStatement(driverSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        driverId = rs.getInt("driver_id");
                        driverInfo.put("driverId", driverId);
                        driverInfo.put("name", rs.getString("driver_name"));
                        driverInfo.put("phone", rs.getString("phone"));
                        driverInfo.put("license", rs.getString("license_number"));
                        driverInfo.put("experience", rs.getInt("experience_years"));
                        driverInfo.put("status", rs.getString("status"));
                        session.setAttribute("driverId", driverId);
                    }
                }
            }

            if (driverId != -1) {
                // 2. Fetch Assigned Vehicle
                String vehicleSql = "SELECT vehicle_id, vehicle_number, vehicle_type, model, capacity_kg, status FROM vehicles WHERE driver_id = ?";
                try (PreparedStatement ps = con.prepareStatement(vehicleSql)) {
                    ps.setInt(1, driverId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            vehicleInfo.put("vehicleId", rs.getInt("vehicle_id"));
                            vehicleInfo.put("number", rs.getString("vehicle_number"));
                            vehicleInfo.put("type", rs.getString("vehicle_type"));
                            vehicleInfo.put("model", rs.getString("model"));
                            vehicleInfo.put("capacity", rs.getBigDecimal("capacity_kg"));
                            vehicleInfo.put("status", rs.getString("status"));
                        }
                    }
                }

                // 3. Fetch Active Assigned Deliveries
                String activeSql = "SELECT s.shipment_id, s.tracking_number, s.shipment_status, s.current_location, s.shipped_date, "
                        + "b.booking_id, b.pickup_date, b.expected_delivery_date, "
                        + "c.full_name AS customer_name, c.phone AS customer_phone, "
                        + "cg.cargo_type, cg.weight_kg, cg.quantity, cg.fragile, "
                        + "pa.address_line AS pickup_line, pa.city AS pickup_city, pa.pincode AS pickup_pincode, "
                        + "da.address_line AS delivery_line, da.city AS delivery_city, da.pincode AS delivery_pincode "
                        + "FROM shipments s "
                        + "JOIN bookings b ON s.booking_id = b.booking_id "
                        + "JOIN customers c ON b.customer_id = c.customer_id "
                        + "JOIN cargo cg ON b.cargo_id = cg.cargo_id "
                        + "JOIN addresses pa ON b.pickup_address_id = pa.address_id "
                        + "JOIN addresses da ON b.delivery_address_id = da.address_id "
                        + "WHERE s.driver_id = ? AND s.shipment_status != 'DELIVERED' "
                        + "ORDER BY s.shipment_id DESC";

                try (PreparedStatement ps = con.prepareStatement(activeSql)) {
                    ps.setInt(1, driverId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            activeDeliveries.add(mapDeliveryRow(rs));
                        }
                    }
                }

                // 4. Fetch Completed Delivery History
                String completedSql = "SELECT s.shipment_id, s.tracking_number, s.shipment_status, s.current_location, s.shipped_date, s.delivered_date, "
                        + "b.booking_id, b.pickup_date, b.expected_delivery_date, "
                        + "c.full_name AS customer_name, c.phone AS customer_phone, "
                        + "cg.cargo_type, cg.weight_kg, cg.quantity, cg.fragile, "
                        + "pa.address_line AS pickup_line, pa.city AS pickup_city, pa.pincode AS pickup_pincode, "
                        + "da.address_line AS delivery_line, da.city AS delivery_city, da.pincode AS delivery_pincode "
                        + "FROM shipments s "
                        + "JOIN bookings b ON s.booking_id = b.booking_id "
                        + "JOIN customers c ON b.customer_id = c.customer_id "
                        + "JOIN cargo cg ON b.cargo_id = cg.cargo_id "
                        + "JOIN addresses pa ON b.pickup_address_id = pa.address_id "
                        + "JOIN addresses da ON b.delivery_address_id = da.address_id "
                        + "WHERE s.driver_id = ? AND s.shipment_status = 'DELIVERED' "
                        + "ORDER BY s.delivered_date DESC LIMIT 20";

                try (PreparedStatement ps = con.prepareStatement(completedSql)) {
                    ps.setInt(1, driverId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> row = mapDeliveryRow(rs);
                            row.put("deliveredDate", rs.getTimestamp("delivered_date"));
                            completedDeliveries.add(row);
                        }
                    }
                }
                // 5. Fetch Customer Ratings & Reviews on Deliveries Completed by this Driver
                List<Map<String, Object>> driverReviews = new ArrayList<>();
                int driverReviewCount = 0;
                int driverRatingSum = 0;

                String reviewsSql =
                    "SELECT f.feedback_id, f.rating, f.comments, f.feedback_date, " +
                    "b.booking_id, s.tracking_number, cg.cargo_type, c.full_name AS customer_name " +
                    "FROM feedback f " +
                    "JOIN shipments s ON f.booking_id = s.booking_id " +
                    "JOIN bookings b ON s.booking_id = b.booking_id " +
                    "JOIN customers c ON f.customer_id = c.customer_id " +
                    "LEFT JOIN cargo cg ON b.cargo_id = cg.cargo_id " +
                    "WHERE s.driver_id = ? " +
                    "ORDER BY f.feedback_date DESC LIMIT 15";

                try (PreparedStatement ps = con.prepareStatement(reviewsSql)) {
                    ps.setInt(1, driverId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> rev = new HashMap<>();
                            int rating = rs.getInt("rating");
                            rev.put("feedbackId", rs.getInt("feedback_id"));
                            rev.put("rating", rating);
                            rev.put("comments", rs.getString("comments"));
                            rev.put("feedbackDate", rs.getTimestamp("feedback_date"));
                            rev.put("bookingId", rs.getInt("booking_id"));
                            rev.put("trackingNumber", rs.getString("tracking_number"));
                            rev.put("cargoType", rs.getString("cargo_type"));
                            rev.put("customerName", rs.getString("customer_name"));
                            driverReviews.add(rev);

                            driverReviewCount++;
                            driverRatingSum += rating;
                        }
                    }
                }

                double driverAvgRating = driverReviewCount > 0
                        ? Math.round(((double) driverRatingSum / driverReviewCount) * 10.0) / 10.0
                        : 5.0;

                request.setAttribute("driverReviews", driverReviews);
                request.setAttribute("driverReviewCount", driverReviewCount);
                request.setAttribute("driverAvgRating", driverAvgRating);
            }

            request.setAttribute("driverInfo", driverInfo);
            request.setAttribute("vehicleInfo", vehicleInfo);
            request.setAttribute("activeDeliveries", activeDeliveries);
            request.setAttribute("completedDeliveries", completedDeliveries);

            request.getRequestDispatcher("/driver/dashboard.jsp").forward(request, response);

        } catch (SQLException e) {
            throw new ServletException("Unable to load driver dashboard data.", e);
        }
    }

    private Map<String, Object> mapDeliveryRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("shipmentId", rs.getInt("shipment_id"));
        row.put("trackingNumber", rs.getString("tracking_number"));
        row.put("status", rs.getString("shipment_status"));
        row.put("currentLocation", rs.getString("current_location"));
        row.put("shippedDate", rs.getTimestamp("shipped_date"));
        row.put("bookingId", rs.getInt("booking_id"));
        row.put("pickupDate", rs.getDate("pickup_date"));
        row.put("expectedDate", rs.getDate("expected_delivery_date"));
        row.put("customerName", rs.getString("customer_name"));
        row.put("customerPhone", rs.getString("customer_phone"));
        row.put("cargoType", rs.getString("cargo_type"));
        row.put("weightKg", rs.getBigDecimal("weight_kg"));
        row.put("quantity", rs.getInt("quantity"));
        row.put("fragile", rs.getBoolean("fragile"));
        row.put("pickupAddress", rs.getString("pickup_line") + ", " + rs.getString("pickup_city") + " - " + rs.getString("pickup_pincode"));
        row.put("deliveryAddress", rs.getString("delivery_line") + ", " + rs.getString("delivery_city") + " - " + rs.getString("delivery_pincode"));
        return row;
    }
}
