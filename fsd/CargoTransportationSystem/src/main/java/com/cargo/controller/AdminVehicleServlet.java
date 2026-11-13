
package com.cargo.controller;

import com.cargo.util.DBConnection;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/admin/vehicles")
public class AdminVehicleServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        List<Map<String, Object>> vehicles = new ArrayList<>();
        List<Map<String, Object>> drivers = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT v.vehicle_id, v.vehicle_number, " +
                    "v.vehicle_type, v.model, v.capacity_kg, v.driver_id, " +
                    "v.status, d.driver_name " +
                    "FROM vehicles v LEFT JOIN drivers d " +
                    "ON v.driver_id = d.driver_id " +
                    "ORDER BY v.vehicle_id DESC";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> v = new HashMap<>();
                    v.put("vehicleId", rs.getInt("vehicle_id"));
                    v.put("vehicleNumber", rs.getString("vehicle_number"));
                    v.put("vehicleType", rs.getString("vehicle_type"));
                    v.put("model", rs.getString("model"));
                    v.put("capacity", rs.getBigDecimal("capacity_kg"));
                    v.put("driverId", (Integer) rs.getObject("driver_id"));
                    v.put("driverName", rs.getString("driver_name"));
                    v.put("status", rs.getString("status"));
                    vehicles.add(v);
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT driver_id, driver_name, status FROM drivers " +
                    "ORDER BY driver_name");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> d = new HashMap<>();
                    d.put("driverId", rs.getInt("driver_id"));
                    d.put("name", rs.getString("driver_name"));
                    d.put("status", rs.getString("status"));
                    drivers.add(d);
                }
            }

            request.setAttribute("vehicles", vehicles);
            request.setAttribute("drivers", drivers);

            String action = request.getParameter("action");

            if ("edit".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT * FROM vehicles WHERE vehicle_id=?")) {
                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            response.sendRedirect(request.getContextPath()
                                    + "/admin/vehicles?error=notfound");
                            return;
                        }

                        request.setAttribute("vehicleId",
                                rs.getInt("vehicle_id"));
                        request.setAttribute("vehicleNumber",
                                rs.getString("vehicle_number"));
                        request.setAttribute("vehicleType",
                                rs.getString("vehicle_type"));
                        request.setAttribute("model", rs.getString("model"));
                        request.setAttribute("capacity",
                                rs.getBigDecimal("capacity_kg"));
                        request.setAttribute("driverId",
                                (Integer) rs.getObject("driver_id"));
                        request.setAttribute("status",
                                rs.getString("status"));
                    }
                }

                request.getRequestDispatcher("/admin/vehicle-form.jsp")
                        .forward(request, response);
            } else if ("add".equals(action)) {
                request.getRequestDispatcher("/admin/vehicle-form.jsp")
                        .forward(request, response);
            } else {
                request.getRequestDispatcher("/admin/vehicles.jsp")
                        .forward(request, response);
            }

        } catch (Exception e) {
            throw new ServletException("Unable to load vehicle management.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("vehicleId"));

                // Do not delete a vehicle that has shipment records.
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT COUNT(*) FROM shipments WHERE vehicle_id=?")) {
                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();

                        if (rs.getInt(1) > 0) {
                            response.sendRedirect(request.getContextPath()
                                    + "/admin/vehicles?error=shipment");
                            return;
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM vehicles WHERE vehicle_id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

            } else if ("add".equals(action) || "update".equals(action)) {

                String number = request.getParameter("vehicleNumber");
                String type = request.getParameter("vehicleType");
                String model = request.getParameter("model");
                String capacityText = request.getParameter("capacity");
                String driverText = request.getParameter("driverId");
                String status = request.getParameter("status");

                if (number == null || number.trim().isEmpty()
                        || type == null || type.trim().isEmpty()
                        || capacityText == null || capacityText.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath()
                            + "/admin/vehicles?error=required");
                    return;
                }

                BigDecimal capacity = new BigDecimal(capacityText);

                if (capacity.compareTo(BigDecimal.ZERO) <= 0) {
                    response.sendRedirect(request.getContextPath()
                            + "/admin/vehicles?error=capacity");
                    return;
                }

                if (!"AVAILABLE".equals(status)
                        && !"IN_USE".equals(status)
                        && !"MAINTENANCE".equals(status)) {
                    status = "AVAILABLE";
                }

                Integer driverId = null;
                if (driverText != null && !driverText.trim().isEmpty()) {
                    driverId = Integer.parseInt(driverText);
                }

                if (driverId != null) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT status FROM drivers WHERE driver_id=?")) {
                        ps.setInt(1, driverId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                response.sendRedirect(request.getContextPath()
                                        + "/admin/vehicles?error=driver");
                                return;
                            }

                            if ("INACTIVE".equals(rs.getString("status"))) {
                                response.sendRedirect(request.getContextPath()
                                        + "/admin/vehicles?error=driver");
                                return;
                            }
                        }
                    }
                }

                if ("add".equals(action)) {
                    String sql = "INSERT INTO vehicles " +
                            "(vehicle_number, vehicle_type, model, capacity_kg, " +
                            "driver_id, status) VALUES (?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, number.trim());
                        ps.setString(2, type.trim());
                        ps.setString(3,
                                model == null || model.trim().isEmpty()
                                        ? null : model.trim());
                        ps.setBigDecimal(4, capacity);

                        if (driverId == null) {
                            ps.setNull(5, Types.INTEGER);
                        } else {
                            ps.setInt(5, driverId);
                        }

                        ps.setString(6, status);
                        ps.executeUpdate();
                    }

                } else {
                    int id = Integer.parseInt(request.getParameter("vehicleId"));

                    String sql = "UPDATE vehicles SET vehicle_number=?, " +
                            "vehicle_type=?, model=?, capacity_kg=?, " +
                            "driver_id=?, status=? WHERE vehicle_id=?";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, number.trim());
                        ps.setString(2, type.trim());
                        ps.setString(3,
                                model == null || model.trim().isEmpty()
                                        ? null : model.trim());
                        ps.setBigDecimal(4, capacity);

                        if (driverId == null) {
                            ps.setNull(5, Types.INTEGER);
                        } else {
                            ps.setInt(5, driverId);
                        }

                        ps.setString(6, status);
                        ps.setInt(7, id);
                        ps.executeUpdate();
                    }
                }
            }

            response.sendRedirect(request.getContextPath() + "/admin/vehicles");

        } catch (SQLIntegrityConstraintViolationException e) {
            response.sendRedirect(request.getContextPath()
                    + "/admin/vehicles?error=duplicate");
        } catch (Exception e) {
            throw new ServletException("Unable to save vehicle details.", e);
        }
    }
}
