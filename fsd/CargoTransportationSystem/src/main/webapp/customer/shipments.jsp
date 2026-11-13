<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.cargo.controller.ShipmentServlet.Shipment" %>

<%
    if (session.getAttribute("userId") == null
            || !"CUSTOMER".equals(session.getAttribute("role"))) {
        response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
        return;
    }

    List<Shipment> shipmentList = (List<Shipment>) request.getAttribute("shipmentList");
    if (shipmentList == null) {
        shipmentList = new ArrayList<>();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Track Shipments | Cargo Transportation System</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        body {
            background: #f1f5f9;
            color: #1e293b;
            padding: 30px 20px;
        }

        .container {
            max-width: 1200px;
            margin: auto;
        }

        .top-nav {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 25px;
        }

        .back-link {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            color: #0284c7;
            text-decoration: none;
            font-weight: 600;
            font-size: 14px;
        }

        .back-link:hover {
            text-decoration: underline;
        }

        h1 {
            color: #0f172a;
            font-size: 24px;
            margin-bottom: 6px;
        }

        p.subtitle {
            color: #64748b;
            font-size: 14px;
            margin-bottom: 25px;
        }

        .table-wrap {
            overflow-x: auto;
            background: white;
            border-radius: 12px;
            border: 1px solid #e2e8f0;
            box-shadow: 0 2px 8px rgba(0,0,0,0.04);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 14px;
        }

        th {
            background: #f8fafc;
            color: #475569;
            font-weight: 600;
            padding: 14px 16px;
            text-align: left;
            border-bottom: 1px solid #e2e8f0;
        }

        td {
            padding: 14px 16px;
            border-bottom: 1px solid #f1f5f9;
            vertical-align: middle;
        }

        tr:hover {
            background: #f8fafc;
        }

        .status-badge {
            display: inline-block;
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
        }

        .badge-pending { background: #fef3c7; color: #b45309; }
        .badge-picked { background: #e0f2fe; color: #0369a1; }
        .badge-transit { background: #f3e8ff; color: #7e22ce; }
        .badge-out { background: #fed7aa; color: #c2410c; }
        .badge-delivered { background: #dcfce7; color: #15803d; }
        .badge-failed { background: #fee2e2; color: #b91c1c; }

        .empty {
            padding: 40px 20px;
            text-align: center;
            color: #64748b;
        }
        .empty h3 {
            color: #0f172a;
            margin-bottom: 8px;
        }
    </style>
</head>
<body>

<div class="container">
    <div class="top-nav">
        <a class="back-link" href="<%= request.getContextPath() %>/customer/dashboard.jsp">
            ← Back to Dashboard
        </a>
    </div>

    <h1>🚚 Live Shipment Tracking</h1>
    <p class="subtitle">Real-time status, assigned fleet vehicle, and driver contact for your cargo orders.</p>

    <div class="table-wrap">
        <% if (shipmentList.isEmpty()) { %>
            <div class="empty">
                <h3>No shipments found</h3>
                <p>When the administrator confirms your bookings and dispatches them, they will appear here.</p>
            </div>
        <% } else { %>
            <table>
                <thead>
                    <tr>
                        <th>Tracking Number</th>
                        <th>Booking ID</th>
                        <th>Current Location</th>
                        <th>Assigned Vehicle</th>
                        <th>Assigned Driver</th>
                        <th>Status</th>
                        <th>Dispatched Date</th>
                        <th>Delivered Date</th>
                    </tr>
                </thead>
                <tbody>
                <% for (Shipment s : shipmentList) { 
                    String st = s.status != null ? s.status : "PENDING";
                    String badgeClass = "badge-pending";
                    if ("PICKED_UP".equals(st)) badgeClass = "badge-picked";
                    else if ("IN_TRANSIT".equals(st)) badgeClass = "badge-transit";
                    else if ("OUT_FOR_DELIVERY".equals(st)) badgeClass = "badge-out";
                    else if ("DELIVERED".equals(st)) badgeClass = "badge-delivered";
                    else if ("FAILED".equals(st)) badgeClass = "badge-failed";
                %>
                    <tr>
                        <td>
                            <strong><%= s.trackingNumber %></strong><br>
                            <small style="color:#64748b;">ID #<%= s.id %></small>
                        </td>
                        <td>Booking #<%= s.bookingId %></td>
                        <td>📍 <%= s.location == null ? "Processing Center" : s.location %></td>
                        <td>
                            <%= s.vehicleNumber != null ? "🚛 " + s.vehicleNumber : "<span style='color:#94a3b8;'>Pending Assignment</span>" %>
                        </td>
                        <td>
                            <% if (s.driverName != null) { %>
                                <strong><%= s.driverName %></strong><br>
                                <small>📞 <%= s.driverPhone != null ? s.driverPhone : "N/A" %></small>
                            <% } else { %>
                                <span style="color:#94a3b8;">Pending Assignment</span>
                            <% } %>
                        </td>
                        <td><span class="status-badge <%= badgeClass %>"><%= st %></span></td>
                        <td><%= s.shippedDate == null ? "—" : s.shippedDate %></td>
                        <td><%= s.deliveredDate == null ? "—" : s.deliveredDate %></td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        <% } %>
    </div>
</div>

</body>
</html>
