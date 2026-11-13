
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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/admin/customers")
public class AdminCustomerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static class Customer {
        private int customerId;
        private int userId;
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private String gender;
        private Date dateOfBirth;

        public int getCustomerId() {
            return customerId;
        }

        public void setCustomerId(int customerId) {
            this.customerId = customerId;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public Date getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(Date dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
    }

    private boolean isAdmin(HttpServletRequest request,
                            HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
            !"ADMIN".equalsIgnoreCase(
                String.valueOf(session.getAttribute("role")))) {

            response.sendRedirect(
                request.getContextPath() + "/login.jsp"
            );
            return false;
        }

        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        List<Customer> customers = new ArrayList<>();

        String sql =
            "SELECT c.customer_id, c.user_id, c.full_name, " +
            "c.phone, c.gender, c.date_of_birth, " +
            "u.username, u.email " +
            "FROM customers c " +
            "INNER JOIN users u ON c.user_id = u.user_id " +
            "ORDER BY c.customer_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Customer customer = new Customer();

                customer.setCustomerId(rs.getInt("customer_id"));
                customer.setUserId(rs.getInt("user_id"));
                customer.setFullName(rs.getString("full_name"));
                customer.setPhone(rs.getString("phone"));
                customer.setGender(rs.getString("gender"));
                customer.setDateOfBirth(
                    rs.getDate("date_of_birth")
                );
                customer.setUsername(rs.getString("username"));
                customer.setEmail(rs.getString("email"));

                customers.add(customer);
            }

        } catch (SQLException e) {
            throw new ServletException(
                "Unable to load customers.", e
            );
        }

        request.setAttribute("customers", customers);

        request.getRequestDispatcher(
            "/admin/customers.jsp"
        ).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        try {
            if ("update".equals(action)) {
                updateCustomer(request);
                request.getSession().setAttribute(
                    "customerMessage",
                    "Customer details updated successfully."
                );

            } else if ("delete".equals(action)) {
                deleteCustomer(request);
                request.getSession().setAttribute(
                    "customerMessage",
                    "Customer profile deleted successfully."
                );

            } else {
                request.getSession().setAttribute(
                    "customerError",
                    "Invalid customer action."
                );
            }

        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute(
                "customerError", e.getMessage()
            );

        } catch (SQLException e) {
            request.getSession().setAttribute(
                "customerError",
                "Unable to complete the operation. " +
                "Check whether related records depend on this customer."
            );
            getServletContext().log(
                "Customer management database error", e
            );
        }

        response.sendRedirect(
            request.getContextPath() + "/admin/customers"
        );
    }

    private void updateCustomer(HttpServletRequest request)
            throws SQLException {

        int customerId = parseCustomerId(
            request.getParameter("customerId")
        );

        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String gender = request.getParameter("gender");
        String dob = request.getParameter("dateOfBirth");

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Full name is required."
            );
        }

        fullName = fullName.trim();

        if (fullName.length() > 100) {
            throw new IllegalArgumentException(
                "Full name must not exceed 100 characters."
            );
        }

        if (phone != null) {
            phone = phone.trim();
            if (phone.isEmpty()) {
                phone = null;
            }
            if (phone != null && phone.length() > 15) {
                throw new IllegalArgumentException(
                    "Phone must not exceed 15 characters."
                );
            }
        }

        if (gender != null) {
            gender = gender.trim();
            if (gender.isEmpty()) {
                gender = null;
            }
            if (gender != null && gender.length() > 10) {
                throw new IllegalArgumentException(
                    "Gender must not exceed 10 characters."
                );
            }
        }

        Date dateOfBirth = null;

        if (dob != null && !dob.trim().isEmpty()) {
            try {
                dateOfBirth = Date.valueOf(dob.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Please enter a valid date of birth."
                );
            }
        }

        String sql =
            "UPDATE customers SET full_name = ?, phone = ?, " +
            "gender = ?, date_of_birth = ? " +
            "WHERE customer_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, gender);
            ps.setDate(4, dateOfBirth);
            ps.setInt(5, customerId);

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException(
                    "Customer not found."
                );
            }
        }
    }

    private void deleteCustomer(HttpServletRequest request)
            throws SQLException {

        int customerId = parseCustomerId(
            request.getParameter("customerId")
        );

        String sql =
            "DELETE FROM customers WHERE customer_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException(
                    "Customer not found."
                );
            }
        }
    }

    private int parseCustomerId(String value) {
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid customer ID."
            );
        }
    }
}
