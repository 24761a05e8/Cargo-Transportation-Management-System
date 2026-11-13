
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.cargo.controller.AdminBookingServlet.Booking" %>

<%!
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
%>

<%
    if (session.getAttribute("userId") == null
            || !"ADMIN".equals(session.getAttribute("role"))) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    List<Booking> bookings =
        (List<Booking>) request.getAttribute("bookings");

    if (bookings == null) bookings = new ArrayList<>();
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Booking Management</title>
<style>
    body {
        margin: 0;
        padding: 28px;
        font-family: Arial, sans-serif;
        background: #f3f6fb;
        color: #243247;
    }

    .container { max-width: 1250px; margin: auto; }

    a { color: #1769aa; text-decoration: none; }

    h1 { color: #174a7e; }

    .panel {
        background: white;
        padding: 20px;
        border-radius: 12px;
        box-shadow: 0 3px 12px #0001;
        overflow-x: auto;
    }

    table {
        width: 100%;
        border-collapse: collapse;
        min-width: 950px;
    }

    th, td {
        text-align: left;
        padding: 13px 10px;
        border-bottom: 1px solid #e6edf5;
        font-size: 13px;
    }

    th { background: #eaf2fc; color: #174a7e; }

    .status {
        font-weight: bold;
        font-size: 12px;
    }

    button {
        border: 0;
        padding: 8px 10px;
        border-radius: 6px;
        color: white;
        cursor: pointer;
        margin: 2px;
    }

    .confirm { background: #17804a; }
    .cancel { background: #c0392b; }
    .empty { padding: 30px; text-align: center; color: #64748b; }

    .notice {
        background: #e8f5ed;
        padding: 12px;
        border-radius: 7px;
        margin-bottom: 18px;
    }
</style>
</head>
<body>
<div class="container">
    <a href="<%= request.getContextPath() %>/admin/dashboard.jsp">
        &larr; Back to Admin Dashboard
    </a>

    <h1>Booking Management</h1>
    <p>Review bookings and approve or cancel pending requests.</p>

    <% if ("updated".equals(request.getParameter("message"))) { %>
        <div class="notice">Booking updated successfully.</div>
    <% } else if ("unchanged".equals(request.getParameter("message"))) { %>
        <div class="notice">This booking is no longer pending or does not exist.</div>
    <% } %>

    <div class="panel">
    <% if (bookings.isEmpty()) { %>
        <div class="empty">
            <h3>No bookings found</h3>
            <p>Bookings created by customers will appear here.</p>
        </div>
    <% } else { %>
        <table>
            <thead>
            <tr>
                <th>Booking ID</th>
                <th>Customer ID</th>
                <th>Cargo</th>
                <th>Pickup City</th>
                <th>Delivery City</th>
                <th>Pickup Date</th>
                <th>Expected Delivery</th>
                <th>Amount (₹)</th>
                <th>Status</th>
                <th>Action</th>
            </tr>
            </thead>
            <tbody>
            <% for (Booking b : bookings) { %>
                <tr>
                    <td><%= b.id %></td>
                    <td><%= b.customerId %></td>
                    <td><%= escapeHtml(b.cargoType) %></td>
                    <td><%= escapeHtml(b.pickupCity) %></td>
                    <td><%= escapeHtml(b.deliveryCity) %></td>
                    <td><%= b.pickupDate == null ? "—" : b.pickupDate %></td>
                    <td><%= b.expectedDate == null ? "—" : b.expectedDate %></td>
                    <td>₹<%= b.amount %></td>
                    <td class="status"><%= escapeHtml(b.status) %></td>
                    <td>
                    <% if ("PENDING".equals(b.status)) { %>
                        <form method="post"
                              action="<%= request.getContextPath() %>/admin/bookings"
                              style="display:inline">
                            <input type="hidden" name="bookingId"
                                   value="<%= b.id %>">
                            <input type="hidden" name="action" value="confirm">
                            <button class="confirm" type="submit">Confirm</button>
                        </form>

                        <form method="post"
                              action="<%= request.getContextPath() %>/admin/bookings"
                              style="display:inline"
                              onsubmit="return confirm('Cancel this booking?');">
                            <input type="hidden" name="bookingId"
                                   value="<%= b.id %>">
                            <input type="hidden" name="action" value="cancel">
                            <button class="cancel" type="submit">Cancel</button>
                        </form>
                    <% } else { %>
                        —
                    <% } %>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    <% } %>
    </div>
</div>
</body>
</html>
