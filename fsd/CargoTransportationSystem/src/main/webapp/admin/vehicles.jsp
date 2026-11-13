
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map" %>
<%
    List<Map<String, Object>> vehicles =
        (List<Map<String, Object>>) request.getAttribute("vehicles");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Vehicle Management</title>
    <style>
        body { font-family: Arial; background: #f1f5f9; padding: 25px; }
        .box { max-width: 1200px; margin: auto; background: white;
               padding: 25px; border-radius: 12px; overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { padding: 12px; border-bottom: 1px solid #e2e8f0;
                 text-align: left; }
        th { background: #dbeafe; }
        a, button { padding: 8px 12px; border: 0; border-radius: 5px;
                    text-decoration: none; cursor: pointer; }
        .add, .edit { background: #2563eb; color: white; }
        .delete { background: #dc2626; color: white; }
        .message { padding: 12px; background: #fef3c7; }
    </style>
</head>
<body>
<div class="box">
    <h2>Vehicle Management</h2>

    <a class="add"
       href="<%= request.getContextPath() %>/admin/vehicles?action=add">
       + Add Vehicle
    </a>

    <%
        String error = request.getParameter("error");
        if ("duplicate".equals(error)) {
    %>
        <p class="message">That vehicle number is already registered.</p>
    <% } else if ("shipment".equals(error)) { %>
        <p class="message">This vehicle has shipment records and cannot be deleted.</p>
    <% } else if ("required".equals(error)) { %>
        <p class="message">Please fill in all required fields.</p>
    <% } else if ("capacity".equals(error)) { %>
        <p class="message">Capacity must be greater than zero.</p>
    <% } else if ("driver".equals(error)) { %>
        <p class="message">The selected driver does not exist or is inactive.</p>
    <% } %>

    <table>
        <thead>
        <tr>
            <th>ID</th><th>Number</th><th>Type</th><th>Model</th>
            <th>Capacity (kg)</th><th>Driver</th><th>Status</th><th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <% if (vehicles != null && !vehicles.isEmpty()) {
            for (Map<String, Object> v : vehicles) { %>
            <tr>
                <td><%= v.get("vehicleId") %></td>
                <td><%= v.get("vehicleNumber") %></td>
                <td><%= v.get("vehicleType") %></td>
                <td><%= v.get("model") == null ? "-" : v.get("model") %></td>
                <td><%= v.get("capacity") %></td>
                <td><%= v.get("driverName") == null
                        ? "Not assigned" : v.get("driverName") %></td>
                <td><%= v.get("status") %></td>
                <td>
                    <a class="edit"
                       href="<%= request.getContextPath() %>/admin/vehicles?action=edit&id=<%= v.get("vehicleId") %>">
                       Edit
                    </a>

                    <form method="post"
                          action="<%= request.getContextPath() %>/admin/vehicles"
                          style="display:inline"
                          onsubmit="return confirm('Delete this vehicle?');">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="vehicleId"
                               value="<%= v.get("vehicleId") %>">
                        <button class="delete" type="submit">Delete</button>
                    </form>
                </td>
            </tr>
        <% } } else { %>
            <tr><td colspan="8">No vehicles found.</td></tr>
        <% } %>
        </tbody>
    </table>

    <p><a href="<%= request.getContextPath() %>/admin/dashboard.jsp">Back to Admin Dashboard</a></p>
</div>
</body>
</html>
