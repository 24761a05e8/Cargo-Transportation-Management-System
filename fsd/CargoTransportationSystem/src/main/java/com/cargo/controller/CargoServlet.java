
package com.cargo.controller;

import com.cargo.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/customer/cargo")
public class CargoServlet extends HttpServlet {

    private boolean isCustomer(HttpSession session) {
        return session != null
                && "CUSTOMER".equals(session.getAttribute("role"))
                && session.getAttribute("userId") instanceof Integer;
    }

    private int getCustomerId(Connection con, int userId)
            throws SQLException {

        String sql = "SELECT customer_id FROM customers WHERE user_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("customer_id");
                }
            }
        }

        return -1;
    }

    // Display the customer's cargo records
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (!isCustomer(session)) {
            response.sendRedirect(
                    request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        List<Cargo> cargoList = new ArrayList<>();

        String sql =
                "SELECT cargo_id, cargo_type, description, weight_kg, "
              + "quantity, fragile, created_at "
              + "FROM cargo WHERE customer_id = ? "
              + "ORDER BY cargo_id DESC";

        try (Connection con = DBConnection.getConnection()) {

            int customerId = getCustomerId(con, userId);

            if (customerId == -1) {
                response.sendRedirect(
                        request.getContextPath() + "/customer/profile");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Cargo item = new Cargo();

                        item.id = rs.getInt("cargo_id");
                        item.type = rs.getString("cargo_type");
                        item.description = rs.getString("description");
                        item.weight = rs.getBigDecimal("weight_kg");
                        item.quantity = rs.getInt("quantity");
                        item.fragile = rs.getBoolean("fragile");
                        item.createdAt = rs.getTimestamp("created_at");

                        cargoList.add(item);
                    }
                }
            }

            request.setAttribute("cargoList", cargoList);

            request.getRequestDispatcher(
                    "/customer/cargo.jsp").forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load cargo records.", e);
        }
    }

    // Add or delete cargo
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (!isCustomer(session)) {
            response.sendRedirect(
                    request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            int customerId = getCustomerId(con, userId);

            if (customerId == -1) {
                response.sendRedirect(
                        request.getContextPath() + "/customer/profile");
                return;
            }

            // Delete only cargo owned by the logged-in customer
            if ("delete".equals(action)) {

                int cargoId;

                try {
                    cargoId = Integer.parseInt(
                            request.getParameter("cargoId"));
                } catch (Exception e) {
                    response.sendRedirect(
                            request.getContextPath()
                            + "/customer/cargo?error=invalid");
                    return;
                }

                String sql =
                        "DELETE FROM cargo "
                      + "WHERE cargo_id = ? AND customer_id = ?";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, cargoId);
                    ps.setInt(2, customerId);
                    ps.executeUpdate();
                }

                response.sendRedirect(
                        request.getContextPath()
                        + "/customer/cargo?deleted=success");
                return;
            }

            // Add new cargo
            if ("add".equals(action)) {

                String type = clean(request.getParameter("cargoType"));
                String description =
                        clean(request.getParameter("description"));
                String weightText =
                        clean(request.getParameter("weightKg"));
                String quantityText =
                        clean(request.getParameter("quantity"));
                String fragileText = request.getParameter("fragile");

                BigDecimal weight;
                int quantity;

                try {
                    weight = new BigDecimal(weightText);
                    quantity = Integer.parseInt(quantityText);

                    if (weight.compareTo(BigDecimal.ZERO) <= 0
                            || weight.compareTo(
                                new BigDecimal("99999999.99")) > 0
                            || weight.scale() > 2
                            || quantity <= 0) {
                        throw new NumberFormatException();
                    }

                } catch (Exception e) {
                    response.sendRedirect(
                            request.getContextPath()
                            + "/customer/cargo?error=invalid");
                    return;
                }

                if (type == null || type.length() > 100) {
                    response.sendRedirect(
                            request.getContextPath()
                            + "/customer/cargo?error=invalid");
                    return;
                }

                if (description != null && description.length() > 5000) {
                    response.sendRedirect(
                            request.getContextPath()
                            + "/customer/cargo?error=invalid");
                    return;
                }

                boolean fragile = "on".equals(fragileText)
                        || "true".equals(fragileText);

                String sql =
                        "INSERT INTO cargo "
                      + "(customer_id, cargo_type, description, "
                      + "weight_kg, quantity, fragile) "
                      + "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, customerId);
                    ps.setString(2, type);

                    if (description == null) {
                        ps.setNull(3, java.sql.Types.VARCHAR);
                    } else {
                        ps.setString(3, description);
                    }

                    ps.setBigDecimal(4, weight);
                    ps.setInt(5, quantity);
                    ps.setBoolean(6, fragile);

                    ps.executeUpdate();
                }

                response.sendRedirect(
                        request.getContextPath()
                        + "/customer/cargo?saved=success");
                return;
            }

            response.sendRedirect(
                    request.getContextPath()
                    + "/customer/cargo?error=invalid");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to save or delete cargo.", e);
        }
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    // Data object used by cargo.jsp
    public static class Cargo {
        public int id;
        public String type;
        public String description;
        public BigDecimal weight;
        public int quantity;
        public boolean fragile;
        public java.sql.Timestamp createdAt;
    }
}
