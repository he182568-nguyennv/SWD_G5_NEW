package Controller;

import Dao.ParkingSessionDAO;
import Model.ParkingSession;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/sessions")
public class ParkingSessionsServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    private JsonObject sessionToJson(ParkingSession s) {
        JsonObject o = new JsonObject();
        o.addProperty("sessionId",    s.getSessionId());
        o.addProperty("vehicleId",    s.getVehicleId());
        o.addProperty("lotId",        s.getLotId());
        o.addProperty("plateNumber",  s.getPlateNumber());
        o.addProperty("checkinTime",  s.getCheckinTime());
        o.addProperty("status",       s.getStatus());
        // checkoutTime có thể null
        JsonElement checkoutTime = s.getCheckoutTime() != null
            ? GsonUtil.GSON.toJsonTree(s.getCheckoutTime())
            : JsonNull.INSTANCE;
        o.add("checkoutTime", checkoutTime);
        return o;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)                        { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1 && (int) rid != 2)  { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            String status   = req.getParameter("status");
            String lotParam = req.getParameter("lotId");
            String plate    = req.getParameter("plate");

            List<ParkingSession> sessions;
            if (plate != null && !plate.isEmpty()) {
                ParkingSession s = sessionDAO.findActiveByPlate(plate);
                sessions = s != null ? List.of(s) : List.of();
            } else if (lotParam != null) {
                sessions = sessionDAO.findByLot(Integer.parseInt(lotParam));
            } else {
                sessions = sessionDAO.findRecent(50);
            }

            if (status != null && !status.equals("all")) {
                final String f = status;
                sessions = sessions.stream().filter(s -> f.equals(s.getStatus())).toList();
            }

            JsonArray arr = new JsonArray();
            for (ParkingSession s : sessions) arr.add(sessionToJson(s));

            JsonObject res = new JsonObject();
            res.add("data", arr);
            GsonUtil.ok(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
