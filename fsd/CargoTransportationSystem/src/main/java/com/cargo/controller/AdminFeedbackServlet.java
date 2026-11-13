package com.cargo.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.cargo.model.Feedback;
import com.cargo.util.DBConnection;

public class AdminFeedbackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static class Feedback extends com.cargo.model.Feedback {
        public Feedback() { super(); }
        public Feedback(int feedbackId, int customerId, int bookingId, int rating, String comments, java.sql.Timestamp feedbackDate) {
            super(feedbackId, customerId, bookingId, rating, comments, feedbackDate);
        }
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null
                || !"ADMIN".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        List<Feedback> feedbackList = new ArrayList<>();
        String ratingFilter = request.getParameter("rating");
        String search = request.getParameter("q");

        // KPI accumulators
        int totalCount = 0;
        int ratingSum = 0;
        int fiveStarCount = 0;
        int lowRatingCount = 0;

        StringBuilder sql = new StringBuilder(
            "SELECT f.feedback_id, f.customer_id, f.booking_id, f.rating, f.comments, f.feedback_date, " +
            "c.full_name AS customer_name, c.phone AS customer_phone, u.username AS customer_username, " +
            "cg.cargo_type, s.tracking_number " +
            "FROM feedback f " +
            "JOIN customers c ON f.customer_id = c.customer_id " +
            "JOIN users u ON c.user_id = u.user_id " +
            "JOIN bookings b ON f.booking_id = b.booking_id " +
            "LEFT JOIN cargo cg ON b.cargo_id = cg.cargo_id " +
            "LEFT JOIN shipments s ON b.booking_id = s.booking_id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (ratingFilter != null && !ratingFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(ratingFilter)) {
            try {
                int r = Integer.parseInt(ratingFilter.trim());
                sql.append("AND f.rating = ? ");
                params.add(r);
            } catch (NumberFormatException ignored) {}
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR u.username LIKE ? OR CAST(f.booking_id AS CHAR) LIKE ? OR s.tracking_number LIKE ? OR f.comments LIKE ?) ");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("ORDER BY f.feedback_date DESC, f.feedback_id DESC");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Feedback f = new Feedback();
                    f.setFeedbackId(rs.getInt("feedback_id"));
                    f.setCustomerId(rs.getInt("customer_id"));
                    f.setBookingId(rs.getInt("booking_id"));
                    f.setRating(rs.getInt("rating"));
                    f.setComments(rs.getString("comments"));
                    f.setFeedbackDate(rs.getTimestamp("feedback_date"));
                    f.setCustomerName(rs.getString("customer_name"));
                    f.setCustomerPhone(rs.getString("customer_phone"));
                    f.setCustomerUsername(rs.getString("customer_username"));
                    f.setCargoType(rs.getString("cargo_type"));
                    f.setTrackingNumber(rs.getString("tracking_number"));

                    feedbackList.add(f);

                    totalCount++;
                    ratingSum += f.getRating();
                    if (f.getRating() == 5) {
                        fiveStarCount++;
                    } else if (f.getRating() <= 2) {
                        lowRatingCount++;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("AdminFeedbackServlet doGet error: " + e.getMessage());
            throw new ServletException("Unable to load feedback records from database.", e);
        }

        double avgRating = totalCount > 0
                ? BigDecimal.valueOf((double) ratingSum / totalCount).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        request.setAttribute("feedbackList", feedbackList);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("avgRating", avgRating);
        request.setAttribute("fiveStarCount", fiveStarCount);
        request.setAttribute("lowRatingCount", lowRatingCount);
        request.setAttribute("selectedRating", ratingFilter);
        request.setAttribute("searchTerm", search);

        request.getRequestDispatcher("/admin/feedback.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String idValue = request.getParameter("feedbackId");

        if (!"delete".equalsIgnoreCase(action) || idValue == null || idValue.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/feedback?error=invalid_action");
            return;
        }

        try {
            int feedbackId = Integer.parseInt(idValue.trim());

            String sql = "DELETE FROM feedback WHERE feedback_id = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, feedbackId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    response.sendRedirect(request.getContextPath() + "/admin/feedback?success=deleted");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/feedback?error=not_found");
                }
            }

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/feedback?error=invalid_id");
        } catch (SQLException e) {
            System.err.println("AdminFeedbackServlet doPost error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/feedback?error=server_error");
        }
    }
}
