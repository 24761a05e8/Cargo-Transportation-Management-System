package com.cargo.servlet;

import com.cargo.dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/driver/update-profile", "/driver-update-profile", "/DriverUpdateProfileServlet"})
public class DriverUpdateProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null
                || !"DRIVER".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        // Strictly load authenticated session's userId (cannot be forged via URL or form parameters)
        int userId = (Integer) session.getAttribute("userId");

        String phone = request.getParameter("phone");
        String email = request.getParameter("email");

        if (phone == null || email == null || phone.trim().isEmpty() || email.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/driver/profile?error=required");
            return;
        }

        phone = phone.trim();
        email = email.trim().toLowerCase();

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            response.sendRedirect(request.getContextPath() + "/driver/profile?error=invalid_email");
            return;
        }

        try {
            // Check if email already used by another user
            if (userDAO.emailExistsExcludingUser(email, userId)) {
                response.sendRedirect(request.getContextPath() + "/driver/profile?error=email_exists");
                return;
            }

            // Update only permitted fields (phone & email)
            userDAO.updateDriverSelfProfile(userId, phone, email);

            response.sendRedirect(request.getContextPath() + "/driver/profile?success=updated");

        } catch (SQLException e) {
            System.err.println("DriverUpdateProfileServlet error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/driver/profile?error=server");
        }
    }
}
