
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.sql.Timestamp" %>

<%
    String ctx = request.getContextPath();

    List<Map<String, Object>> shipments =
        (List<Map<String, Object>>) request.getAttribute("shipments");

    List<Map<String, Object>> vehicles =
        (List<Map<String, Object>>) request.getAttribute("vehicles");

    String success = request.getParameter("success");
    String error = request.getParameter("error");
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shipment Management | CTMS</title>

    <style>
        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f3f6fb;
            color: #1e293b;
        }

        header {
            background: #12325b;
            color: white;
            padding: 20px 28px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
        }

        header a {
            color: white;
            text-decoration: none;
            margin-left: 16px;
        }

        main {
            padding: 24px;
            max-width: 1500px;
            margin: auto;
        }

        .message {
            padding: 12px;
            border-radius: 6px;
            margin-bottom: 16px;
        }

        .success {
            background: #dcfce7;
            color: #166534;
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
        }

        .table-container {
            overflow-x: auto;
            background: white;
            border-radius: 10px;
            padding: 12px;
            box-shadow: 0 2px 8px #0000000d;
        }

        table {
            width: 100%;
            min-width: 1150px;
            border-collapse: collapse;
        }

        th, td {
            padding: 12px;
            border-bottom: 1px solid #e2e8f0;
            text-align: left;
            vertical-align: top;
        }

        th {
            background: #eaf0f8;
        }

        input, select, button {
            width: 100%;
            padding: 9px;
            margin: 4px 0;
            border: 1px solid #cbd5e1;
            border-radius: 5px;
        }

        button {
            background: #1d4ed8;
            color: white;
            border: none;
            cursor: pointer;
        }

        button:hover {
            background: #1e40af;
        }

        .muted {
            color: #64748b;
            font-size: 12px;
        }

        .status {
            font-weight: bold;
        }
    </style>
</head>

<body>

<header>
    <div>
        <h2>Cargo Transportation Management</h2>
        <div>Admin — Shipment Management</div>
    </div>

    <nav>
        <a href="<%= ctx %>/admin/dashboard.jsp">Dashboard</a>
        <a href="<%= ctx %>/logout">Logout</a>
    </nav>
</header>

<main>

    <h2>Shipment Management</h2>

    <% if ("1".equals(success)) { %>
        <div class="message success">
            Operation completed successfully.
        </div>
    <% } %>

   <% if (error != null && !error.isEmpty()) { %>
    <div class="message error">
        <%= error.replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;")
                 .replace("\"", "&quot;")
                 .replace("'", "&#39;") %>
    </div>
<% } %>

    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <th>Shipment</th>
                    <th>Booking</th>
                    <th>Tracking</th>
                    <th>Assignment</th>
                    <th>Update Status</th>
                    <th>Dates</th>
                </tr>
            </thead>

            <tbody>
            <% if (shipments != null && !shipments.isEmpty()) {
                for (Map<String, Object> s : shipments) {

                    Object vehicleId = s.get("vehicleId");
                    Object driverId = s.get("driverId");

                    String status = String.valueOf(s.get("status"));
                    boolean closed =
                        "DELIVERED".equals(status)
                        || "FAILED".equals(status);
            %>

                <tr>
                    <td>
                        <strong>#<%= s.get("shipmentId") %></strong>
                        <div class="muted">
                            Status:
                            <span class="status"><%= status %></span>
                        </div>
                    </td>

                    <td>#<%= s.get("bookingId") %></td>

                    <td>
                        <strong><%= s.get("trackingNumber") %></strong>
                        <div><%= s.get("currentLocation") == null
                            ? "Location not updated"
                            : s.get("currentLocation") %></div>
                    </td>

                    <td>
                    <% if (vehicleId == null || driverId == null) { %>

                        <form method="post" action="<%= ctx %>/admin/shipments">
                            <input type="hidden" name="action" value="assign">
                            <input type="hidden" name="shipmentId"
                                value="<%= s.get("shipmentId") %>">

                            <label>Available vehicle</label>
                            <select name="vehicleId" required>
                                <option value="">Select vehicle</option>

                                <% if (vehicles != null) {
                                    for (Map<String, Object> v : vehicles) {
                                        if ("AVAILABLE".equals(
                                                String.valueOf(v.get("status")))
                                                && v.get("driverId") != null
                                                && "AVAILABLE".equals(
                                                    String.valueOf(
                                                        getDriverStatusPlaceholder(
                                                            v.get("driverId")
                                                        )
                                                    ))) {
                                            // Availability is validated by the servlet.
                                        }
                                        if ("AVAILABLE".equals(
                                                String.valueOf(v.get("status")))
                                                && v.get("driverId") != null) {
                                %>
                                    <option value="<%= v.get("vehicleId") %>">
                                        <%= v.get("vehicleNumber") %>
                                        — <%= v.get("vehicleType") %>
                                        — Driver: <%= v.get("driverName") %>
                                    </option>
                                <%      }
                                    }
                                } %>
                            </select>

                            <button type="submit">Assign Vehicle &amp; Driver</button>
                            <div class="muted">
                                The servlet checks driver availability before saving.
                            </div>
                        </form>

                    <% } else { %>

                        <strong><%= s.get("vehicleNumber") %></strong>
                        <div>Driver: <%= s.get("driverName") %></div>

                    <% } %>
                    </td>

                    <td>
                    <% if (!closed) { %>
                        <form method="post" action="<%= ctx %>/admin/shipments">
                            <input type="hidden" name="action"
                                value="updateStatus">
                            <input type="hidden" name="shipmentId"
                                value="<%= s.get("shipmentId") %>">

                            <select name="status" required>
                                <option value="PENDING"
                                    <%= "PENDING".equals(status)
                                        ? "selected" : "" %>>PENDING</option>
                                <option value="PICKED_UP"
                                    <%= "PICKED_UP".equals(status)
                                        ? "selected" : "" %>>PICKED_UP</option>
                                <option value="IN_TRANSIT"
                                    <%= "IN_TRANSIT".equals(status)
                                        ? "selected" : "" %>>IN_TRANSIT</option>
                                <option value="OUT_FOR_DELIVERY"
                                    <%= "OUT_FOR_DELIVERY".equals(status)
                                        ? "selected" : "" %>>OUT_FOR_DELIVERY</option>
                                <option value="DELIVERED"
                                    <%= "DELIVERED".equals(status)
                                        ? "selected" : "" %>>DELIVERED</option>
                                <option value="FAILED"
                                    <%= "FAILED".equals(status)
                                        ? "selected" : "" %>>FAILED</option>
                            </select>

                            <input type="text" name="currentLocation"
                                maxlength="255"
                                placeholder="Current location"
                                value="<%= s.get("currentLocation") == null
                                    ? "" : s.get("currentLocation") %>">

                            <button type="submit">Update Status</button>
                        </form>
                    <% } else { %>
                        Shipment closed.
                    <% } %>
                    </td>

                    <td>
                        <div>Shipped:</div>
                        <%= s.get("shippedDate") == null
                            ? "—" : s.get("shippedDate") %>
                        <div>Delivered:</div>
                        <%= s.get("deliveredDate") == null
                            ? "—" : s.get("deliveredDate") %>
                    </td>
                </tr>

            <%  }
               } else { %>
                <tr>
                    <td colspan="6">No shipments found.</td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>

</main>
</body>
</html>

<%!
    // Placeholder only; the servlet performs the authoritative DB check.
    private String getDriverStatusPlaceholder(Object driverId) {
        return "CHECKED_BY_SERVLET";
    }
%>
