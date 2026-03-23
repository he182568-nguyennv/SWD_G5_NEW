package Controller;


import Dao.VehicleDAO;
import Model.RegisteredVehicle;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/customer/vehicles")
public class VehicleServlet extends HttpServlet {

    private final VehicleDAO vehicleDAO = new VehicleDAO();

    private String toJson(RegisteredVehicle v) {
        return "{" +
            "\"vehicleId\":"    + v.getVehicleId()                      + "," +
            "\"userId\":"       + v.getUserId()                         + "," +
            "\"typeId\":"       + v.getTypeId()                         + "," +
            "\"plateNumber\":\"" + JsonUtil.escape(v.getPlateNumber())  + "\"," +
            "\"isActive\":"     + v.isActive()                         +
            "}";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = (int) req.getAttribute("jwtUserId");
        int roleId = (int) req.getAttribute("jwtRoleId");
        // Customer sees own vehicles; manager/staff can query by userId param
        int targetId = (roleId == 3) ? userId : JsonUtil.getInt(req.getParameter("userId") != null ? "{\"userId\":" + req.getParameter("userId") + "}" : "{}", "userId", userId);

        try {
            List<RegisteredVehicle> list = vehicleDAO.findByUser(targetId);
            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]}");
            resp.setStatus(200); resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        int userId = (int) req.getAttribute("jwtUserId");
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 3) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Only customers can manage vehicles\"}"); return; }

        try {
            String body       = JsonUtil.readBody(req);
            String action     = JsonUtil.getString(body, "action");

            if ("toggle".equals(action)) {
                int vehicleId = JsonUtil.getInt(body, "vehicleId", -1);
                vehicleDAO.toggleActive(vehicleId, userId);
                resp.setStatus(200); resp.getWriter().write("{\"success\":true}");
                return;
            }

            if ("delete".equals(action)) {
                int vehicleId = JsonUtil.getInt(body, "vehicleId", -1);
                vehicleDAO.delete(vehicleId, userId);
                resp.setStatus(200); resp.getWriter().write("{\"success\":true}");
                return;
            }

            // Add vehicle
            String plate = JsonUtil.getString(body, "plateNumber");
            int typeId   = JsonUtil.getInt(body, "typeId", 1);
            if (plate == null || plate.isEmpty()) {
                resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing plateNumber\"}"); return;
            }
            // Check duplicate
            if (vehicleDAO.findByPlate(plate) != null) {
                resp.setStatus(409); resp.getWriter().write("{\"success\":false,\"message\":\"Biển số đã tồn tại\"}"); return;
            }
            RegisteredVehicle v = new RegisteredVehicle();
            v.setUserId(userId); v.setTypeId(typeId); v.setPlateNumber(plate);
            int newId = vehicleDAO.insert(v);
            v.setVehicleId(newId); v.setActive(true);
            resp.setStatus(201); resp.getWriter().write("{\"success\":true,\"vehicle\":" + toJson(v) + "}");
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
