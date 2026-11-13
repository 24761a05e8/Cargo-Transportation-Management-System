package com.cargo.servlet;

import com.cargo.dao.UserDAO;
import com.cargo.model.Driver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/driver/profile", "/driver-profile", "/DriverProfileServlet"})
public class DriverProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null
                || !"DRIVER".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
            return;
        }

        // Strictly load authenticated session user's ID (prevent IDOR)
        int userId = (Integer) session.getAttribute("userId");

        try {
            Driver driver = userDAO.findDriverByUserId(userId);

            if (driver == null) {
                response.sendRedirect(request.getContextPath() + "/driver-approval-pending.jsp?error=no_profile");
                return;
            }

            request.setAttribute("driver", driver);
            request.getRequestDispatcher("/driver/profile.jsp").forward(request, response);

        } catch (SQLException e) {
            System.err.println("DriverProfileServlet error: " + e.getMessage());
            throw new ServletException("Unable to load driver profile.", e);
        }
    }
}
