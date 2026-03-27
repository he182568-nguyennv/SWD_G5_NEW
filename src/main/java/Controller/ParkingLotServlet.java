package Controller;

import Dao.ParkingLotDAO;
import Model.ParkingLot;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/lots")
public class ParkingLotServlet extends HttpServlet {

    private final ParkingLotDAO lotDAO = new ParkingLotDAO();

    private JsonObject lotToJson(ParkingLot l) {
        JsonObject o = new JsonObject();
        o.addProperty("lotId",        l.getLotId());
        o.addProperty("lotName",      l.getLotName());
        o.addProperty("address",      l.getAddress());
        o.addProperty("capacity",     l.getCapacity());
        o.addProperty("currentCount", l.getCurrentCount());
        return o;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)                        { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1 && (int) rid != 2)  { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            List<ParkingLot> lots = lotDAO.findAll();
            JsonArray arr = new JsonArray();
            for (ParkingLot l : lots) arr.add(lotToJson(l));

            JsonObject res = new JsonObject();
            res.add("data", arr);
            GsonUtil.ok(resp, res);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)       { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1)    { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            JsonObject body = GsonUtil.parseBody(req);
            String action   = GsonUtil.getString(body, "action");
            int    lotId    = GsonUtil.getInt(body, "lotId", -1);
            String lotName  = GsonUtil.getString(body, "lotName");
            String address  = GsonUtil.getString(body, "address");
            int    capacity = GsonUtil.getInt(body, "capacity", 0);

            if ("update".equals(action) && lotId > 0) {
                ParkingLot l = lotDAO.findById(lotId);
                if (l == null) { GsonUtil.error(resp, 404, "Lot not found"); return; }
                if (lotName != null) l.setLotName(lotName);
                if (address != null) l.setAddress(address);
                if (capacity > 0)   l.setCapacity(capacity);
                lotDAO.update(l);

                JsonObject res = new JsonObject();
                res.add("lot", lotToJson(l));
                GsonUtil.ok(resp, res);
            } else {
                if (lotName == null || address == null || capacity <= 0) {
                    GsonUtil.error(resp, 400, "Missing fields"); return;
                }
                ParkingLot l = new ParkingLot();
                l.setLotName(lotName); l.setAddress(address); l.setCapacity(capacity);
                int newId = lotDAO.insert(l);
                l.setLotId(newId);

                JsonObject res = new JsonObject();
                res.add("lot", lotToJson(l));
                GsonUtil.created(resp, res);
            }
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
