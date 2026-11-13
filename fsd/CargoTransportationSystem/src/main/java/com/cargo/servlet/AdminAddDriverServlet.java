package com.cargo.servlet;

import com.cargo.dao.UserDAO;
import com.cargo.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/admin/add-driver", "/AdminAddDriverServlet"})
public class AdminAddDriverServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

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

        // Provide generated temporary credentials suggestion
        String genUser = PasswordUtil.generateUsername("driver");
        String genPass = PasswordUtil.generateTemporaryPassword();
        request.setAttribute("genUsername", genUser);
        request.setAttribute("genPassword", genPass);

        request.getRequestDispatcher("/admin/driver-form.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");

        String fullName = request.getParameter("driverName");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String licenseNumber = request.getParameter("licenseNumber");
        String expStr = request.getParameter("experienceYears");
        String password = request.getParameter("password");
        String status = request.getParameter("status");

        if (fullName == null || phone == null || licenseNumber == null
                || fullName.trim().isEmpty() || phone.trim().isEmpty() || licenseNumber.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/add-driver?error=required");
            return;
        }

        fullName = fullName.trim();
        phone = phone.trim();
        licenseNumber = licenseNumber.trim().toUpperCase();

        // Auto-generate username or password if left blank
        if (username == null || username.trim().isEmpty()) {
            username = PasswordUtil.generateUsername(fullName);
        } else {
            username = username.trim();
        }

        if (password == null || password.trim().isEmpty()) {
            password = PasswordUtil.generateTemporaryPassword();
        } else {
            password = password.trim();
        }

        if (email == null || email.trim().isEmpty()) {
            email = username + "@cargo-fleet.local";
        } else {
            email = email.trim().toLowerCase();
        }

        int experienceYears = 0;
        if (expStr != null && !expStr.trim().isEmpty()) {
            try {
                experienceYears = Integer.parseInt(expStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (status == null || (!status.equals("AVAILABLE") && !status.equals("ASSIGNED") && !status.equals("INACTIVE"))) {
            status = "AVAILABLE";
        }

        try {
            if (userDAO.usernameExists(username)) {
                response.sendRedirect(request.getContextPath() + "/admin/add-driver?error=username_exists");
                return;
            }

            if (userDAO.emailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/admin/add-driver?error=email_exists");
                return;
            }

            if (userDAO.licenseExists(licenseNumber)) {
                response.sendRedirect(request.getContextPath() + "/admin/add-driver?error=license_exists");
                return;
            }

            // Register driver inside transaction
            userDAO.registerDriver(username, email, password, fullName, phone, licenseNumber, experienceYears, status);

            // Redirect with success and generated credentials info
            response.sendRedirect(request.getContextPath() + "/admin/drivers?success=driver_created&createdUser="
                    + java.net.URLEncoder.encode(username, "UTF-8")
                    + "&createdPass=" + java.net.URLEncoder.encode(password, "UTF-8"));

        } catch (SQLException e) {
            System.err.println("AdminAddDriverServlet error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/add-driver?error=server");
        }
    }
}
