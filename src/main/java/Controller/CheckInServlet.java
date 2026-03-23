package Controller;

import Dao.ParkingSessionDAO;
import Model.ParkingSession;
import Utils.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/staff/checkin")
public class CheckInServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        int roleId = (int) req.getAttribute("jwtRoleId");
        int staffId = (int) req.getAttribute("jwtUserId");
        if (roleId != 2) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ staff mới được check-in\"}");
            return;
        }

        try {
            String body        = JsonUtil.readBody(req);
            String plateNumber = JsonUtil.getString(body, "plateNumber");
            int    lotId       = JsonUtil.getInt(body, "lotId", -1);
            int    cardId      = JsonUtil.getInt(body, "cardId", -1);
            String imgIn       = JsonUtil.getString(body, "vehicleImgIn");

            if (plateNumber == null || lotId < 0 || cardId < 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu plateNumber, lotId hoặc cardId\"}");
                return;
            }

            ParkingSession existing = sessionDAO.findActiveByPlate(plateNumber);
            if (existing != null) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"success\":false,\"message\":\"Xe " + JsonUtil.escape(plateNumber) + " đang trong bãi rồi\"}");
                return;
            }

            ParkingSession s = new ParkingSession();
            s.setPlateNumber(plateNumber);
            s.setLotId(lotId);
            s.setCardId(cardId);
            s.setStaffCheckinId(staffId);
            s.setVehicleImgIn(imgIn);

            int sid = sessionDAO.checkIn(s);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(
                "{\"success\":true," +
                "\"sessionId\":" + sid + "," +
                "\"plateNumber\":\"" + JsonUtil.escape(plateNumber) + "\"," +
                "\"lotId\":" + lotId + "," +
                "\"cardId\":" + cardId + "," +
                "\"staffCheckinId\":" + staffId + "," +
                "\"checkinTime\":\"" + java.time.LocalDateTime.now() + "\"," +
                "\"status\":\"active\"}"
            );
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Lỗi server: " + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
