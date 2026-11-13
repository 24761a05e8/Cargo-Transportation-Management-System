
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.cargo.controller.PaymentServlet.Payment" %>

<%
    if (session.getAttribute("userId") == null
            || !"CUSTOMER".equals(session.getAttribute("role"))) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    if (request.getAttribute("paymentList") == null) {
        response.sendRedirect(request.getContextPath() + "/customer/payments");
        return;
    }

    List<Payment> paymentList =
        (List<Payment>) request.getAttribute("paymentList");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Payment Management</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background: #f2f5fa;
            margin: 0;
            padding: 30px;
            color: #243247;
        }

        .container {
            max-width: 1100px;
            margin: auto;
        }

        h1 { color: #174ea6; }

        .back {
            display: inline-block;
            margin-bottom: 20px;
            color: #174ea6;
            text-decoration: none;
        }

        .table-wrap {
            overflow-x: auto;
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 3px 12px #00000010;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            padding: 13px;
            text-align: left;
            border-bottom: 1px solid #e5eaf2;
        }

        th {
            background: #eaf1ff;
            color: #174ea6;
        }

        .status {
            display: inline-block;
            padding: 6px 10px;
            border-radius: 20px;
            background: #eaf1ff;
            color: #174ea6;
            font-size: 12px;
            font-weight: bold;
        }

        .empty {
            padding: 25px;
            text-align: center;
            color: #64748b;
        }
    </style>
</head>
<body>
<div class="container">
    <a class="back"
       href="<%= request.getContextPath() %>/customer/dashboard.jsp">
       &larr; Back to Dashboard
    </a>

    <h1>Payment Management</h1>
    <p>View your booking payments and transaction details.</p>

    <div class="table-wrap">
        <% if (paymentList.isEmpty()) { %>
            <div class="empty">
                <h3>No payments found</h3>
                <p>Payments linked to your bookings will appear here.</p>
            </div>
        <% } else { %>
            <table>
                <thead>
                    <tr>
                        <th>Payment ID</th>
                        <th>Booking ID</th>
                        <th>Amount (₹)</th>
                        <th>Method</th>
                        <th>Payment Date</th>
                        <th>Status</th>
                        <th>Transaction ID</th>
                    </tr>
                </thead>
                <tbody>
                <% for (Payment p : paymentList) { %>
                    <tr>
                        <td><%= p.id %></td>
                        <td><%= p.bookingId %></td>
                        <td>₹<%= p.amount %></td>
                        <td><%= p.method %></td>
                        <td><%= p.paymentDate == null ? "—" : p.paymentDate %></td>
                        <td>
                            <span class="status">
                                <%= p.status == null ? "PENDING" : p.status %>
                            </span>
                        </td>
                        <td><%= p.transactionId == null ? "—" : p.transactionId %></td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        <% } %>
    </div>
</div>
</body>
</html>
