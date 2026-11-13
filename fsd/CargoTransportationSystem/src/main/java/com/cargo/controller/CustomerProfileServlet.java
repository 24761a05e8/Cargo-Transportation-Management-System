
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
import java.sql.Types;
import java.sql.Date;

@WebServlet("/customer/profile")
public class CustomerProfileServlet extends HttpServlet {

    private boolean isCustomer(HttpSession session) {
        return session != null
                && "CUSTOMER".equals(session.getAttribute("role"))
                && session.getAttribute("userId") instanceof Integer;
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void setNullableString(
            PreparedStatement ps, int index, String value)
            throws SQLException {

        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private void setNullableDate(
            PreparedStatement ps, int index, Date value)
            throws SQLException {

        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, value);
        }
    }

    // VIEW PROFILE
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

        String sql =
                "SELECT customer_id, full_name, phone, gender, date_of_birth "
              + "FROM customers WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    request.setAttribute(
                            "customerId", rs.getInt("customer_id"));
                    request.setAttribute(
                            "fullName", rs.getString("full_name"));
                    request.setAttribute(
                            "phone", rs.getString("phone"));
                    request.setAttribute(
                            "gender", rs.getString("gender"));
                    request.setAttribute(
                            "dateOfBirth", rs.getDate("date_of_birth"));
                }
            }

            request.getRequestDispatcher(
                    "/customer/profile.jsp").forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load customer profile.", e);
        }
    }

    // CREATE OR UPDATE PROFILE
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

        String fullName = clean(request.getParameter("fullName"));
        String phone = clean(request.getParameter("phone"));
        String gender = clean(request.getParameter("gender"));
        String dob = clean(request.getParameter("dateOfBirth"));

        if (fullName == null || fullName.length() > 100) {
            response.sendRedirect(
                    request.getContextPath()
                    + "/customer/profile?error=name");
            return;
        }

        if (phone != null && phone.length() > 15) {
            response.sendRedirect(
                    request.getContextPath()
                    + "/customer/profile?error=phone");
            return;
        }

        if (gender != null
                && !gender.equals("Male")
                && !gender.equals("Female")
                && !gender.equals("Other")) {
            response.sendRedirect(
                    request.getContextPath()
                    + "/customer/profile?error=gender");
            return;
        }

        Date dateOfBirth = null;

        try {
            if (dob != null) {
                dateOfBirth = Date.valueOf(dob);
            }
        } catch (IllegalArgumentException e) {
            response.sendRedirect(
                    request.getContextPath()
                    + "/customer/profile?error=date");
            return;
        }

        String checkSql =
                "SELECT customer_id FROM customers WHERE user_id = ?";

        String insertSql =
                "INSERT INTO customers "
              + "(user_id, full_name, phone, gender, date_of_birth) "
              + "VALUES (?, ?, ?, ?, ?)";

        String updateSql =
                "UPDATE customers SET full_name = ?, phone = ?, "
              + "gender = ?, date_of_birth = ? WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            boolean exists;

            try (PreparedStatement check =
                         con.prepareStatement(checkSql)) {

                check.setInt(1, userId);

                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }

            String sql = exists ? updateSql : insertSql;

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                if (exists) {
                    ps.setString(1, fullName);
                    setNullableString(ps, 2, phone);
                    setNullableString(ps, 3, gender);
                    setNullableDate(ps, 4, dateOfBirth);
                    ps.setInt(5, userId);
                } else {
                    ps.setInt(1, userId);
                    ps.setString(2, fullName);
                    setNullableString(ps, 3, phone);
                    setNullableString(ps, 4, gender);
                    setNullableDate(ps, 5, dateOfBirth);
                }

                ps.executeUpdate();
            }

            response.sendRedirect(
                    request.getContextPath()
                    + "/customer/profile?saved=success");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to save customer profile.", e);
        }
    }
}
