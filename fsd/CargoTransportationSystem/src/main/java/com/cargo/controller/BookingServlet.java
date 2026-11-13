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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/customer/bookings")
public class BookingServlet extends HttpServlet {

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

        try (Connection con = DBConnection.getConnection()) {

            int customerId = getCustomerId(con, userId);

            if (customerId == -1) {
                response.sendRedirect(
                    request.getContextPath() + "/customer/profile");
                return;
            }

            List<CargoOption> cargoList = new ArrayList<>();

            String cargoSql =
                "SELECT cargo_id, cargo_type, weight_kg, quantity " +
                "FROM cargo WHERE customer_id = ? ORDER BY cargo_id DESC";

            try (PreparedStatement ps = con.prepareStatement(cargoSql)) {
                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CargoOption cargo = new CargoOption();
                        cargo.id = rs.getInt("cargo_id");
                        cargo.type = rs.getString("cargo_type");
                        cargo.weight = rs.getBigDecimal("weight_kg");
                        cargo.quantity = rs.getInt("quantity");
                        cargoList.add(cargo);
                    }
                }
            }

            List<AddressOption> addressList = new ArrayList<>();

            String addressSql =
                "SELECT address_id, address_type, address_line, " +
                "city, state, pincode FROM addresses " +
                "WHERE customer_id = ? ORDER BY address_id DESC";

            try (PreparedStatement ps = con.prepareStatement(addressSql)) {
                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AddressOption address = new AddressOption();
                        address.id = rs.getInt("address_id");
                        address.type = rs.getString("address_type");
                        address.line = rs.getString("address_line");
                        address.city = rs.getString("city");
                        address.state = rs.getString("state");
                        address.pincode = rs.getString("pincode");
                        addressList.add(address);
                    }
                }
            }

            List<Booking> bookingList = new ArrayList<>();

            String bookingSql =
                "SELECT b.booking_id, b.cargo_id, b.pickup_address_id, " +
                "b.delivery_address_id, b.booking_date, b.pickup_date, " +
                "b.expected_delivery_date, b.booking_status, b.total_amount, " +
                "c.cargo_type, pa.city AS pickup_city, " +
                "da.city AS delivery_city " +
                "FROM bookings b " +
                "JOIN cargo c ON b.cargo_id = c.cargo_id " +
                "JOIN addresses pa ON b.pickup_address_id = pa.address_id " +
                "JOIN addresses da ON b.delivery_address_id = da.address_id " +
                "WHERE b.customer_id = ? " +
                "ORDER BY b.booking_id DESC";

            try (PreparedStatement ps = con.prepareStatement(bookingSql)) {
                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Booking booking = new Booking();

                        booking.id = rs.getInt("booking_id");
                        booking.cargoId = rs.getInt("cargo_id");
                        booking.pickupAddressId =
                            rs.getInt("pickup_address_id");
                        booking.deliveryAddressId =
                            rs.getInt("delivery_address_id");
                        booking.cargoType = rs.getString("cargo_type");
                        booking.pickupCity = rs.getString("pickup_city");
                        booking.deliveryCity = rs.getString("delivery_city");
                        booking.bookingDate =
                            rs.getTimestamp("booking_date");
                        booking.pickupDate = rs.getDate("pickup_date");
                        booking.expectedDate =
                            rs.getDate("expected_delivery_date");
                        booking.status = rs.getString("booking_status");
                        booking.amount = rs.getBigDecimal("total_amount");

                        bookingList.add(booking);
                    }
                }
            }

            request.setAttribute("cargoList", cargoList);
            request.setAttribute("addressList", addressList);
            request.setAttribute("bookingList", bookingList);

            request.getRequestDispatcher("/customer/bookings.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException("Unable to load bookings.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("userId") == null
                || !"CUSTOMER".equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");
        String contextPath = request.getContextPath();

        try (Connection con = DBConnection.getConnection()) {

            int customerId = getCustomerId(con, userId);

            if (customerId == -1) {
                response.sendRedirect(contextPath + "/customer/profile");
                return;
            }

            if ("cancel".equals(action)) {
                int bookingId = Integer.parseInt(
                    request.getParameter("bookingId"));

                String cancelSql =
                    "UPDATE bookings SET booking_status = 'CANCELLED' " +
                    "WHERE booking_id = ? AND customer_id = ? " +
                    "AND booking_status = 'PENDING'";

                try (PreparedStatement ps = con.prepareStatement(cancelSql)) {
                    ps.setInt(1, bookingId);
                    ps.setInt(2, customerId);
                    ps.executeUpdate();
                }

                response.sendRedirect(contextPath + "/customer/bookings");
                return;
            }

            int cargoId = Integer.parseInt(request.getParameter("cargoId"));
            int pickupAddressId = Integer.parseInt(
                request.getParameter("pickupAddressId"));
            int deliveryAddressId = Integer.parseInt(
                request.getParameter("deliveryAddressId"));

            String pickupDateText = request.getParameter("pickupDate");
            String expectedDateText =
                request.getParameter("expectedDeliveryDate");
            String amountText = request.getParameter("totalAmount");

            Date pickupDate = Date.valueOf(pickupDateText);
            Date expectedDate = Date.valueOf(expectedDateText);
            BigDecimal amount = new BigDecimal(amountText);

            if (amount.compareTo(BigDecimal.ZERO) < 0
                    || expectedDate.before(pickupDate)
                    || pickupAddressId == deliveryAddressId) {
                response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Check the dates, amount, and different pickup/delivery addresses.");
                return;
            }

            // Ensure the selected cargo belongs to this customer.
            String cargoCheck =
                "SELECT cargo_id FROM cargo " +
                "WHERE cargo_id = ? AND customer_id = ?";

            try (PreparedStatement ps = con.prepareStatement(cargoCheck)) {
                ps.setInt(1, cargoId);
                ps.setInt(2, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Invalid cargo selection.");
                        return;
                    }
                }
            }

            // Ensure both selected addresses belong to this customer.
            String addressCheck =
                "SELECT COUNT(*) FROM addresses " +
                "WHERE customer_id = ? " +
                "AND address_id IN (?, ?)";

            try (PreparedStatement ps = con.prepareStatement(addressCheck)) {
                ps.setInt(1, customerId);
                ps.setInt(2, pickupAddressId);
                ps.setInt(3, deliveryAddressId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 2) {
                        response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Select two addresses belonging to your account.");
                        return;
                    }
                }
            }

            String insertSql =
                "INSERT INTO bookings " +
                "(customer_id, cargo_id, pickup_address_id, " +
                "delivery_address_id, pickup_date, " +
                "expected_delivery_date, booking_status, total_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?)";

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, customerId);
                ps.setInt(2, cargoId);
                ps.setInt(3, pickupAddressId);
                ps.setInt(4, deliveryAddressId);
                ps.setDate(5, pickupDate);
                ps.setDate(6, expectedDate);
                ps.setBigDecimal(7, amount);
                ps.executeUpdate();
            }

            response.sendRedirect(contextPath + "/customer/bookings");

        } catch (IllegalArgumentException e) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid booking details. Check the dates, IDs, and amount.");
        } catch (SQLException e) {
            throw new ServletException("Unable to process booking.", e);
        }
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

    public static class CargoOption {
        public int id;
        public String type;
        public BigDecimal weight;
        public int quantity;
    }

    public static class AddressOption {
        public int id;
        public String type;
        public String line;
        public String city;
        public String state;
        public String pincode;
    }

    public static class Booking {
        public int id;
        public int cargoId;
        public int pickupAddressId;
        public int deliveryAddressId;
        public String cargoType;
        public String pickupCity;
        public String deliveryCity;
        public java.sql.Timestamp bookingDate;
        public Date pickupDate;
        public Date expectedDate;
        public String status;
        public BigDecimal amount;
    }
}

