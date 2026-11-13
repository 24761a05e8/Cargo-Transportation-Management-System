
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

@WebServlet("/customer/payments")
public class PaymentServlet extends HttpServlet {
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
        List<Payment> paymentList = new ArrayList<>();

        String sql =
            "SELECT p.payment_id, p.booking_id, p.amount, " +
            "p.payment_method, p.payment_date, p.payment_status, " +
            "p.transaction_id " +
            "FROM payments p " +
            "JOIN bookings b ON p.booking_id = b.booking_id " +
            "JOIN customers c ON b.customer_id = c.customer_id " +
            "WHERE c.user_id = ? " +
            "ORDER BY p.payment_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.id = rs.getInt("payment_id");
                    p.bookingId = rs.getInt("booking_id");
                    p.amount = rs.getBigDecimal("amount");
                    p.method = rs.getString("payment_method");
                    p.paymentDate = rs.getTimestamp("payment_date");
                    p.status = rs.getString("payment_status");
                    p.transactionId = rs.getString("transaction_id");

                    paymentList.add(p);
                }
            }

        } catch (Exception e) {
            throw new ServletException("Unable to load payments.", e);
        }

        request.setAttribute("paymentList", paymentList);
        request.getRequestDispatcher("/customer/payments.jsp")
               .forward(request, response);
    }

    public static class Payment {
        public int id;
        public int bookingId;
        public java.math.BigDecimal amount;
        public String method;
        public java.sql.Timestamp paymentDate;
        public String status;
        public String transactionId;
    }
}
