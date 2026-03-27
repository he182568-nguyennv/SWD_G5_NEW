package Controller;

import Dao.VehicleDAO;
import Model.RegisteredVehicle;
import Utils.GsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/customer/vehicles")
public class VehicleServlet extends HttpServlet {

    private final VehicleDAO vehicleDAO = new VehicleDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int userId = (int) uid;
        int roleId = (int) rid;
        int targetId = userId;

        if (roleId != 3) {
            String param = req.getParameter("userId");
            if (param != null && !param.isBlank()) {
                try { targetId = Integer.parseInt(param); }
                catch (NumberFormatException e) { GsonUtil.error(resp, 400, "userId không hợp lệ"); return; }
            }
        }

        try {
            List<RegisteredVehicle> list = vehicleDAO.findByUser(targetId);
            // Gson tự convert List<RegisteredVehicle> → JSON array
            GsonUtil.ok(resp, list);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 3) { GsonUtil.error(resp, 403, "Chỉ customer mới được quản lý xe"); return; }

        int userId = (int) uid;

        try {
            JsonObject body   = GsonUtil.parseBody(req);
            String action     = GsonUtil.getString(body, "action");

            if ("toggle".equals(action)) {
                int vehicleId = GsonUtil.getInt(body, "vehicleId", -1);
                if (vehicleId < 0) { GsonUtil.error(resp, 400, "Missing vehicleId"); return; }
                boolean ok = vehicleDAO.toggleActive(vehicleId, userId);
                if (ok) GsonUtil.ok(resp); else GsonUtil.error(resp, 404, "Không tìm thấy xe");
                return;
            }

            if ("delete".equals(action)) {
                int vehicleId = GsonUtil.getInt(body, "vehicleId", -1);
                if (vehicleId < 0) { GsonUtil.error(resp, 400, "Missing vehicleId"); return; }
                boolean ok = vehicleDAO.delete(vehicleId, userId);
                if (ok) GsonUtil.ok(resp); else GsonUtil.error(resp, 404, "Không tìm thấy xe");
                return;
            }

            // Thêm xe mới
            String plate  = GsonUtil.getString(body, "plateNumber");
            int    typeId = GsonUtil.getInt(body, "typeId", 1);

            if (plate == null || plate.isBlank()) { GsonUtil.error(resp, 400, "Thiếu plateNumber"); return; }
            if (vehicleDAO.findByPlate(plate) != null) { GsonUtil.error(resp, 409, "Biển số đã tồn tại"); return; }

            RegisteredVehicle v = new RegisteredVehicle();
            v.setUserId(userId);
            v.setTypeId(typeId);
            v.setPlateNumber(plate.toUpperCase().trim());
            int newId = vehicleDAO.insert(v);
            v.setVehicleId(newId);
            v.setActive(true);

            JsonObject res = new JsonObject();
            res.add("vehicle", GsonUtil.GSON.toJsonTree(v));
            GsonUtil.created(resp, res);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
