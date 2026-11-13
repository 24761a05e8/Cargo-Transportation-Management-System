package com.cargo.servlet;

import com.cargo.dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/register", "/RegisterServlet"})
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String role = request.getParameter("role");
        if (role == null || role.trim().isEmpty()) {
            role = "CUSTOMER";
        } else {
            role = role.trim().toUpperCase();
        }

        // Security check: public registration must NEVER create an ADMIN account
        if ("ADMIN".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/register.jsp?error=forbidden_role");
            return;
        }

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String fullName = request.getParameter("fullName");

        // Basic validation
        if (username == null || email == null || password == null || fullName == null
                || username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty() || fullName.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/register.jsp?error=required&type=" + role.toLowerCase());
            return;
        }

        username = username.trim();
        email = email.trim().toLowerCase();
        password = password.trim();
        fullName = fullName.trim();

        if (confirmPassword != null && !password.equals(confirmPassword.trim())) {
            response.sendRedirect(request.getContextPath() + "/register.jsp?error=password_mismatch&type=" + role.toLowerCase());
            return;
        }

        if (username.length() < 3 || username.length() > 50) {
            response.sendRedirect(request.getContextPath() + "/register.jsp?error=invalid_username&type=" + role.toLowerCase());
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            response.sendRedirect(request.getContextPath() + "/register.jsp?error=invalid_email&type=" + role.toLowerCase());
            return;
        }

        if (password.length() < 6) {
            response.sendRedirect(request.getContextPath() + "/register.jsp?error=short_password&type=" + role.toLowerCase());
            return;
        }

        try {
            if (userDAO.usernameExists(username)) {
                response.sendRedirect(request.getContextPath() + "/register.jsp?error=username_exists&type=" + role.toLowerCase());
                return;
            }

            if (userDAO.emailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/register.jsp?error=email_exists&type=" + role.toLowerCase());
                return;
            }

            if ("DRIVER".equals(role)) {
                String phone = request.getParameter("phone");
                String licenseNumber = request.getParameter("licenseNumber");
                String expStr = request.getParameter("experienceYears");

                if (phone == null || phone.trim().isEmpty() || licenseNumber == null || licenseNumber.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/register.jsp?error=driver_fields_required&type=driver");
                    return;
                }

                phone = phone.trim();
                licenseNumber = licenseNumber.trim().toUpperCase();
                int experienceYears = 0;
                if (expStr != null && !expStr.trim().isEmpty()) {
                    try {
                        experienceYears = Integer.parseInt(expStr.trim());
                    } catch (NumberFormatException ignored) {}
                }

                if (userDAO.licenseExists(licenseNumber)) {
                    response.sendRedirect(request.getContextPath() + "/register.jsp?error=license_exists&type=driver");
                    return;
                }

                // Register driver in transaction with initial status INACTIVE (Pending Admin Approval)
                userDAO.registerDriver(username, email, password, fullName, phone, licenseNumber, experienceYears, "INACTIVE");

                response.sendRedirect(request.getContextPath() + "/login.jsp?registered=driver_pending");
                return;

            } else {
                // Customer registration
                userDAO.registerCustomer(username, email, password, fullName);
                response.sendRedirect(request.getContextPath() + "/login.jsp?registered=success");
                return;
            }

        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/register.jsp?error=server&type=" + role.toLowerCase());
        }
    }
}
