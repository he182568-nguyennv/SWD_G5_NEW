package Controller;


import Dao.ParkingLotDAO;
import Model.ParkingLot;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/manager/lots")
public class ParkingLotServlet extends HttpServlet {

    private final ParkingLotDAO lotDAO = new ParkingLotDAO();

    private String lotToJson(ParkingLot l) {
        return "{" +
            "\"lotId\":" + l.getLotId() + "," +
            "\"lotName\":\"" + JsonUtil.escape(l.getLotName()) + "\"," +
            "\"address\":\"" + JsonUtil.escape(l.getAddress()) + "\"," +
            "\"capacity\":" + l.getCapacity() + "," +
            "\"currentCount\":" + l.getCurrentCount() +
            "}";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int role = (int) req.getAttribute("jwtRoleId");
        if (role != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }
        try {
            List<ParkingLot> lots = lotDAO.findAll();
            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < lots.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(lotToJson(lots.get(i)));
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
        int role = (int) req.getAttribute("jwtRoleId");
        if (role != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }
        try {
            String body    = JsonUtil.readBody(req);
            String action  = JsonUtil.getString(body, "action");
            int    lotId   = JsonUtil.getInt(body, "lotId", -1);
            String lotName = JsonUtil.getString(body, "lotName");
            String address = JsonUtil.getString(body, "address");
            int    capacity= JsonUtil.getInt(body, "capacity", 0);

            if ("update".equals(action) && lotId > 0) {
                ParkingLot l = lotDAO.findById(lotId);
                if (l == null) { resp.setStatus(404); resp.getWriter().write("{\"success\":false,\"message\":\"Lot not found\"}"); return; }
                if (lotName != null) l.setLotName(lotName);
                if (address != null) l.setAddress(address);
                if (capacity > 0) l.setCapacity(capacity);
                lotDAO.update(l);
                resp.setStatus(200); resp.getWriter().write("{\"success\":true,\"lot\":" + lotToJson(l) + "}");
            } else {
                if (lotName == null || address == null || capacity <= 0) {
                    resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing fields\"}"); return;
                }
                ParkingLot l = new ParkingLot();
                l.setLotName(lotName); l.setAddress(address); l.setCapacity(capacity);
                int newId = lotDAO.insert(l);
                l.setLotId(newId);
                resp.setStatus(201); resp.getWriter().write("{\"success\":true,\"lot\":" + lotToJson(l) + "}");
            }
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
