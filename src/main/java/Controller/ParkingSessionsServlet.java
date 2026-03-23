package Controller;


import Dao.ParkingSessionDAO;
import Model.ParkingSession;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/sessions")
public class ParkingSessionsServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 2 && roleId != 1) {
            resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return;
        }

        try {
            String status  = req.getParameter("status");   // active | completed | all
            String lotParam= req.getParameter("lotId");
            String plate   = req.getParameter("plate");

            List<ParkingSession> sessions;
            if (plate != null && !plate.isEmpty()) {
                ParkingSession s = sessionDAO.findActiveByPlate(plate);
                sessions = s != null ? List.of(s) : List.of();
            } else if (lotParam != null) {
                sessions = sessionDAO.findByLot(Integer.parseInt(lotParam));
            } else {
                sessions = sessionDAO.findRecent(50); // last 50
            }

            // Filter by status if provided
            if (status != null && !status.equals("all")) {
                final String f = status;
                sessions = sessions.stream().filter(s -> f.equals(s.getStatus())).toList();
            }

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < sessions.size(); i++) {
                ParkingSession s = sessions.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"sessionId\":")      .append(s.getSessionId())                          .append(",")
                  .append("\"vehicleId\":")       .append(s.getVehicleId())                         .append(",")
                  .append("\"lotId\":")           .append(s.getLotId())                             .append(",")
                  .append("\"plateNumber\":\"")   .append(JsonUtil.escape(s.getPlateNumber()))      .append("\",")
                  .append("\"checkinTime\":\"")   .append(JsonUtil.escape(s.getCheckinTime()))      .append("\",")
                  .append("\"checkoutTime\":")    .append(s.getCheckoutTime() != null ? "\"" + JsonUtil.escape(s.getCheckoutTime()) + "\"" : "null").append(",")
                  .append("\"status\":\"")        .append(JsonUtil.escape(s.getStatus()))           .append("\"")
                  .append("}");
            }
            sb.append("]}");
            resp.setStatus(200); resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
