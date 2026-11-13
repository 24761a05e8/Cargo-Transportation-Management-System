
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
import java.util.List;

@WebServlet("/customer/addresses")
public class AddressServlet extends HttpServlet {

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

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    // DISPLAY CUSTOMER ADDRESSES
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
        List<Address> addresses = new ArrayList<>();

        String sql =
                "SELECT address_id, address_type, address_line, "
              + "city, state, pincode "
              + "FROM addresses WHERE customer_id = ? "
              + "ORDER BY address_id DESC";

        try (Connection con = DBConnection.getConnection()) {

            int customerId = getCustomerId(con, userId);

            if (customerId == -1) {
                response.sendRedirect(
                        request.getContextPath()
                        + "/customer/profile");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Address address = new Address();

                        address.id = rs.getInt("address_id");
                        address.type = rs.getString("address_type");
                        address.addressLine =
                                rs.getString("address_line");
                        address.city = rs.getString("city");
                        address.state = rs.getString("state");
                        address.pincode = rs.getString("pincode");

                        addresses.add(address);
                    }
                }
            }

            request.setAttribute("addresses", addresses);

            request.getRequestDispatcher(
                    "/customer/addresses.jsp").forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load customer addresses.", e);
        }
    }

    // ADD OR DELETE ADDRESS
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
                        request.getContextPath()
                        + "/customer/profile");
                return;
            }

            // DELETE ADDRESS
            if ("delete".equals(action)) {

                int addressId;

                try {
                    addressId = Integer.parseInt(
                            request.getParameter("addressId"));
                } catch (Exception e) {
                    response.sendRedirect(
                            request.getContextPath()
                            + "/customer/addresses?error=invalid");
                    return;
                }

                String sql =
                        "DELETE FROM addresses "
                      + "WHERE address_id = ? AND customer_id = ?";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, addressId);
                    ps.setInt(2, customerId);
                    ps.executeUpdate();
                }

                response.sendRedirect(
                        request.getContextPath()
                        + "/customer/addresses?deleted=success");
                return;
            }

            // ADD ADDRESS
            if ("add".equals(action)) {

                String type = clean(
                        request.getParameter("addressType"));
                String addressLine = clean(
                        request.getParameter("addressLine"));
                String city = clean(request.getParameter("city"));
                String state = clean(request.getParameter("state"));
                String pincode = clean(request.getParameter("pincode"));

                if (type == null
                        || !(type.equals("HOME")
                        || type.equals("OFFICE")
                        || type.equals("OTHER"))
                        || addressLine == null
                        || addressLine.length() > 255
                        || city == null || city.length() > 100
                        || state == null || state.length() > 100
                        || pincode == null || pincode.length() > 10) {

                    response.sendRedirect(
                            request.getContextPath()
                            + "/customer/addresses?error=invalid");
                    return;
                }

                String sql =
                        "INSERT INTO addresses "
                      + "(customer_id, address_type, address_line, "
                      + "city, state, pincode) "
                      + "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, customerId);
                    ps.setString(2, type);
                    ps.setString(3, addressLine);
                    ps.setString(4, city);
                    ps.setString(5, state);
                    ps.setString(6, pincode);

                    ps.executeUpdate();
                }

                response.sendRedirect(
                        request.getContextPath()
                        + "/customer/addresses?saved=success");
                return;
            }

            response.sendRedirect(
                    request.getContextPath()
                    + "/customer/addresses?error=invalid");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to save or delete address.", e);
        }
    }

    // ADDRESS DATA OBJECT
    public static class Address {
        public int id;
        public String type;
        public String addressLine;
        public String city;
        public String state;
        public String pincode;
    }
}
