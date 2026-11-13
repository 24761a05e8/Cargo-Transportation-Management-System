
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map" %>
<%
    Object vehicleId = request.getAttribute("vehicleId");
    boolean editing = vehicleId != null;

    List<Map<String, Object>> drivers =
        (List<Map<String, Object>>) request.getAttribute("drivers");

    Object selectedDriver = request.getAttribute("driverId");
    String selectedStatus = editing
            ? String.valueOf(request.getAttribute("status"))
            : "AVAILABLE";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Vehicle Form</title>
    <style>
        body { font-family: Arial; background: #f1f5f9; padding: 30px; }
        .box { max-width: 550px; margin: auto; background: white;
               padding: 25px; border-radius: 12px; }
        label { display: block; margin-top: 15px; }
        input, select { width: 100%; padding: 10px; margin-top: 6px;
                        box-sizing: border-box; }
        button, a { display: inline-block; margin-top: 20px; padding: 10px 15px;
                    border: 0; border-radius: 5px; text-decoration: none; }
        button { background: #2563eb; color: white; cursor: pointer; }
        a { background: #64748b; color: white; }
    </style>
</head>
<body>
<div class="box">
    <h2><%= editing ? "Edit Vehicle" : "Add Vehicle" %></h2>

    <form method="post"
          action="<%= request.getContextPath() %>/admin/vehicles">
        <input type="hidden" name="action"
               value="<%= editing ? "update" : "add" %>">

        <% if (editing) { %>
            <input type="hidden" name="vehicleId" value="<%= vehicleId %>">
        <% } %>

        <label>Vehicle Number</label>
        <input name="vehicleNumber" maxlength="20" required
               value="<%= editing ? request.getAttribute("vehicleNumber") : "" %>">

        <label>Vehicle Type</label>
        <input name="vehicleType" maxlength="50" required
               value="<%= editing ? request.getAttribute("vehicleType") : "" %>">

        <label>Model</label>
        <input name="model" maxlength="100"
               value="<%= editing ? request.getAttribute("model") : "" %>">

        <label>Capacity (kg)</label>
        <input type="number" name="capacity" min="0.01" step="0.01" required
               value="<%= editing ? request.getAttribute("capacity") : "" %>">

        <label>Assign Driver (optional)</label>
        <select name="driverId">
            <option value="">No driver assigned</option>
            <% if (drivers != null) {
                for (Map<String, Object> d : drivers) {
                    Object id = d.get("driverId");
                    boolean selected = selectedDriver != null
                            && selectedDriver.equals(id);
                    boolean inactive = "INACTIVE".equals(d.get("status"));
            %>
                <option value="<%= id %>"
                    <%= selected ? "selected" : "" %>
                    <%= inactive ? "disabled" : "" %>>
                    <%= d.get("name") %> (ID: <%= id %>)
                    <%= inactive ? "- Inactive" : "" %>
                </option>
            <% } } %>
        </select>

        <label>Status</label>
        <select name="status">
            <option value="AVAILABLE"
                <%= "AVAILABLE".equals(selectedStatus) ? "selected" : "" %>>
                Available
            </option>
            <option value="IN_USE"
                <%= "IN_USE".equals(selectedStatus) ? "selected" : "" %>>
                In Use
            </option>
            <option value="MAINTENANCE"
                <%= "MAINTENANCE".equals(selectedStatus) ? "selected" : "" %>>
                Maintenance
            </option>
        </select>

        <button type="submit">
            <%= editing ? "Update Vehicle" : "Save Vehicle" %>
        </button>

        <a href="<%= request.getContextPath() %>/admin/vehicles">Cancel</a>
    </form>
</div>
</body>
</html>
