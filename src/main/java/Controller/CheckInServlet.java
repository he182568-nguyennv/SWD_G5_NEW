package Controller;

import Dao.ParkingSessionDAO;
import Model.ParkingSession;
import Model.User;
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

        User staff = (User) req.getSession().getAttribute("currentUser");
        if (staff == null || staff.getRoleId() != 2) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Không có quyền truy cập\"}");
            return;
        }

        String plateNumber = req.getParameter("plateNumber");
        int lotId   = Integer.parseInt(req.getParameter("lotId"));
        int cardId  = Integer.parseInt(req.getParameter("cardId"));
        String imgIn = req.getParameter("vehicleImgIn");

        try {
            ParkingSession existing = sessionDAO.findActiveByPlate(plateNumber);
            if (existing != null) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"success\":false,\"message\":\"Xe " + plateNumber + " đang trong bãi rồi\"}");
                return;
            }

            ParkingSession s = new ParkingSession();
            s.setPlateNumber(plateNumber);
            s.setLotId(lotId);
            s.setCardId(cardId);
            s.setStaffCheckinId(staff.getId());
            s.setVehicleImgIn(imgIn);

            int sid = sessionDAO.checkIn(s);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(
                    "{\"success\":true," +
                            "\"sessionId\":" + sid + "," +
                            "\"plateNumber\":\"" + plateNumber + "\"," +
                            "\"lotId\":" + lotId + "," +
                            "\"cardId\":" + cardId + "," +
                            "\"staffCheckinId\":" + staff.getId() + "," +
                            "\"checkinTime\":\"" + java.time.LocalDateTime.now() + "\"," +
                            "\"status\":\"active\"}"
            );
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Lỗi server: " + e.getMessage() + "\"}");
        }
    }
}