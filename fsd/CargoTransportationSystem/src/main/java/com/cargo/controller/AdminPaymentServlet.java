package com.cargo.controller;

import java.io.IOException;
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

import com.cargo.model.Payment;
import com.cargo.util.DBConnection;

public class AdminPaymentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static class Payment extends com.cargo.model.Payment {
        public Payment() { super(); }
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

        List<Payment> payments = new ArrayList<>();
        String statusFilter = request.getParameter("status");
        String search = request.getParameter("q");

        StringBuilder sql = new StringBuilder(
            "SELECT p.payment_id, p.booking_id, p.amount, "
            + "p.payment_method, p.payment_date, p.payment_status, p.transaction_id, "
            + "c.full_name AS customer_name, c.phone AS customer_phone, cg.cargo_type "
            + "FROM payments p "
            + "JOIN bookings b ON p.booking_id = b.booking_id "
            + "JOIN customers c ON b.customer_id = c.customer_id "
            + "JOIN cargo cg ON b.cargo_id = cg.cargo_id "
            + "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append("AND p.payment_status = ? ");
            params.add(statusFilter.trim().toUpperCase());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (p.transaction_id LIKE ? OR CAST(p.booking_id AS CHAR) LIKE ? OR c.full_name LIKE ?) ");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("ORDER BY p.payment_id DESC");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setPaymentId(rs.getInt("payment_id"));
                    p.setBookingId(rs.getInt("booking_id"));
                    p.setAmount(rs.getBigDecimal("amount"));
                    p.setPaymentMethod(rs.getString("payment_method"));
                    p.setPaymentDate(rs.getTimestamp("payment_date"));
                    p.setPaymentStatus(rs.getString("payment_status"));
                    p.setTransactionId(rs.getString("transaction_id"));
                    p.setCustomerName(rs.getString("customer_name"));
                    p.setCustomerPhone(rs.getString("customer_phone"));
                    p.setCargoType(rs.getString("cargo_type"));
                    payments.add(p);
                }
            }

        } catch (SQLException e) {
            System.err.println("AdminPaymentServlet error: " + e.getMessage());
            throw new ServletException("Unable to load payment records from database.", e);
        }

        request.setAttribute("payments", payments);
        request.setAttribute("selectedStatus", statusFilter);
        request.setAttribute("searchTerm", search);

        request.getRequestDispatcher("/admin/payments.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String idValue = request.getParameter("paymentId");

        if (idValue == null || idValue.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/payments?error=missing_id");
            return;
        }

        int paymentId;
        try {
            paymentId = Integer.parseInt(idValue.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/payments?error=invalid_id");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            if ("updateStatus".equals(action)) {
                String newStatus = request.getParameter("status");
                if (newStatus == null || (!newStatus.equals("PENDING") && !newStatus.equals("SUCCESS")
                        && !newStatus.equals("FAILED") && !newStatus.equals("REFUNDED"))) {
                    response.sendRedirect(request.getContextPath() + "/admin/payments?error=invalid_status");
                    return;
                }

                String sql = "UPDATE payments SET payment_status = ? WHERE payment_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, paymentId);
                    ps.executeUpdate();
                }
                response.sendRedirect(request.getContextPath() + "/admin/payments?success=updated");
                return;

            } else if ("delete".equals(action)) {
                String sql = "DELETE FROM payments WHERE payment_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, paymentId);
                    ps.executeUpdate();
                }
                response.sendRedirect(request.getContextPath() + "/admin/payments?success=deleted");
                return;

            } else {
                response.sendRedirect(request.getContextPath() + "/admin/payments?error=invalid_action");
            }

        } catch (SQLException e) {
            System.err.println("AdminPaymentServlet doPost error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/payments?error=server");
        }
    }
}
