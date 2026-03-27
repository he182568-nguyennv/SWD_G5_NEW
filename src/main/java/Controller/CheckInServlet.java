package Controller;

import Dao.ParkingSessionDAO;
import Model.ParkingSession;
import Utils.GsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/staff/checkin")
public class CheckInServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        Object roleAttr = req.getAttribute("jwtRoleId");
        Object userAttr = req.getAttribute("jwtUserId");
        if (roleAttr == null || userAttr == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int roleId  = (int) roleAttr;
        int staffId = (int) userAttr;
        if (roleId != 2) { GsonUtil.error(resp, 403, "Chỉ staff mới được check-in"); return; }

        try {
            JsonObject body     = GsonUtil.parseBody(req);
            String plateNumber  = GsonUtil.getString(body, "plateNumber");
            int    lotId        = GsonUtil.getInt(body, "lotId", -1);
            int    cardId       = GsonUtil.getInt(body, "cardId", -1);
            String imgIn        = GsonUtil.getString(body, "vehicleImgIn");
            int    vehicleId    = GsonUtil.getInt(body, "vehicleId", 0);
            int    membershipId = GsonUtil.getInt(body, "membershipId", 0);

            if (plateNumber == null || plateNumber.isBlank() || lotId < 0 || cardId < 0) {
                GsonUtil.error(resp, 400, "Thiếu plateNumber, lotId hoặc cardId");
                return;
            }

            plateNumber = plateNumber.trim().toUpperCase();

            ParkingSession existing = sessionDAO.findActiveByPlate(plateNumber);
            if (existing != null) {
                GsonUtil.error(resp, 409, "Xe " + plateNumber + " đang trong bãi rồi");
                return;
            }

            ParkingSession s = new ParkingSession();
            s.setPlateNumber(plateNumber);
            s.setLotId(lotId);
            s.setCardId(cardId);
            s.setStaffCheckinId(staffId);
            s.setVehicleImgIn(imgIn != null ? imgIn : "");
            s.setVehicleId(vehicleId);
            s.setMembershipId(membershipId);

            int sid = sessionDAO.checkIn(s);
            if (sid < 0) { GsonUtil.error(resp, 500, "Check-in thất bại, không lấy được session ID"); return; }

            JsonObject res = new JsonObject();
            res.addProperty("sessionId",      sid);
            res.addProperty("plateNumber",    plateNumber);
            res.addProperty("lotId",          lotId);
            res.addProperty("cardId",         cardId);
            res.addProperty("vehicleId",      vehicleId);
            res.addProperty("membershipId",   membershipId);
            res.addProperty("staffCheckinId", staffId);
            res.addProperty("checkinTime",    LocalDateTime.now().toString());
            res.addProperty("status",         "active");
            GsonUtil.ok(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, "Lỗi server: " + e.getMessage());
        }
    }
}
