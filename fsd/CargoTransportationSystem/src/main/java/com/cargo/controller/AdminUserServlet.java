
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

@WebServlet("/admin/users")
public class AdminUserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Public model class: JSP can access it without reflection.
    public static class User {
        private int userId;
        private String username;
        private String email;
        private String role;

        public User(int userId, String username,
                    String email, String role) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.role = role;
        }

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
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

        List<User> users = new ArrayList<>();

        String sql =
            "SELECT user_id, username, email, role " +
            "FROM users ORDER BY user_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User user = new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("role")
                );

                users.add(user);
            }

        } catch (SQLException e) {
            getServletContext().log(
                "Unable to load users", e
            );
            throw new ServletException(
                "Unable to load users from the database.", e
            );
        }

        request.setAttribute("users", users);

        request.getRequestDispatcher(
            "/admin/users.jsp"
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

        HttpSession session = request.getSession(false);
        String action = request.getParameter("action");

        try {
            if ("updateRole".equals(action)) {
                updateRole(request, session);
                session.setAttribute(
                    "userMessage",
                    "User role updated successfully."
                );

            } else if ("delete".equals(action)) {
                deleteUser(request, session);
                session.setAttribute(
                    "userMessage",
                    "User deleted successfully."
                );

            } else {
                throw new IllegalArgumentException(
                    "Invalid action."
                );
            }

        } catch (IllegalArgumentException e) {
            session.setAttribute(
                "userError", e.getMessage()
            );

        } catch (SQLException e) {
            getServletContext().log(
                "User management database error", e
            );

            session.setAttribute(
                "userError",
                "Database operation failed. The user may have " +
                "related records that prevent deletion."
            );
        }

        response.sendRedirect(
            request.getContextPath() + "/admin/users"
        );
    }

    private void updateRole(HttpServletRequest request,
                            HttpSession session)
            throws SQLException {

        int userId = parseUserId(
            request.getParameter("userId")
        );

        String newRole = request.getParameter("role");

        if (newRole == null) {
            throw new IllegalArgumentException(
                "Please select a role."
            );
        }

        newRole = newRole.trim().toUpperCase();

        if (!"ADMIN".equals(newRole) &&
            !"CUSTOMER".equals(newRole) &&
            !"DRIVER".equals(newRole)) {

            throw new IllegalArgumentException(
                "Invalid role selected."
            );
        }

        int loggedInUserId = parseUserId(
            String.valueOf(session.getAttribute("userId"))
        );

        if (userId == loggedInUserId) {
            throw new IllegalArgumentException(
                "You cannot change your own role."
            );
        }

        String sql =
            "UPDATE users SET role = ? WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newRole);
            ps.setInt(2, userId);

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException(
                    "User not found."
                );
            }
        }
    }

    private void deleteUser(HttpServletRequest request,
                            HttpSession session)
            throws SQLException {

        int userId = parseUserId(
            request.getParameter("userId")
        );

        int loggedInUserId = parseUserId(
            String.valueOf(session.getAttribute("userId"))
        );

        if (userId == loggedInUserId) {
            throw new IllegalArgumentException(
                "You cannot delete your own account."
            );
        }

        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException(
                    "User not found."
                );
            }
        }
    }

    private int parseUserId(String value) {
        try {
            int id = Integer.parseInt(value);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid user ID."
            );
        }
    }
}
