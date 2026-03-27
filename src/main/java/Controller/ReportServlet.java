package Controller;

import Dao.ParkingSessionDAO;
import Dao.ReportDAO;
import Model.ParkingSession;
import Model.Report;
import Utils.GsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {

    private final ReportDAO         reportDAO  = new ReportDAO();
    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object uid = req.getAttribute("jwtUserId");
        if (uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int userId = (int) uid;
        int roleId = (int) req.getAttribute("jwtRoleId");

        try {
            List<Report> list;
            if (roleId == 1)      list = reportDAO.findAll();
            else if (roleId == 2) list = reportDAO.findPending();
            else                  list = reportDAO.findByReporter(userId);

            // Enrich từng report thêm plateNumber + checkinTime từ session
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (Report r : list) {
                JsonObject obj = GsonUtil.GSON.toJsonTree(r).getAsJsonObject();

                // Lấy thêm thông tin từ parking_sessions
                try {
                    ParkingSession s = sessionDAO.findById(r.getSessionId());
                    obj.addProperty("plateNumber", s != null && s.getPlateNumber() != null ? s.getPlateNumber() : "");
                    obj.addProperty("checkinTime", s != null && s.getCheckinTime() != null ? s.getCheckinTime() : "");
                } catch (Exception ignored) {
                    obj.addProperty("plateNumber", "");
                    obj.addProperty("checkinTime", "");
                }
                arr.add(obj);
            }

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
        Object uid = req.getAttribute("jwtUserId");
        if (uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int userId     = (int) uid;
        int roleId     = (int) req.getAttribute("jwtRoleId");
        String jwtUser = (String) req.getAttribute("jwtUsername");

        try {
            JsonObject body   = GsonUtil.parseBody(req);
            String action     = GsonUtil.getString(body, "action");

            // Duyệt / từ chối report (manager only)
            if ("approve".equals(action)) {
                if (roleId != 1) { GsonUtil.error(resp, 403, "Forbidden"); return; }

                int    reportId = GsonUtil.getInt(body, "reportId", -1);
                String decision = GsonUtil.getString(body, "decision");
                String note     = GsonUtil.getString(body, "note");

                if (reportId < 0 || decision == null) { GsonUtil.error(resp, 400, "Missing reportId or decision"); return; }

                reportDAO.approve(reportId, userId, decision, note != null ? note : "");

                JsonObject res = new JsonObject();
                res.addProperty("reportId", reportId);
                GsonUtil.ok(resp, res);
                return;
            }

            // Tạo report mới
            int    vehicleId    = GsonUtil.getInt(body, "vehicleId", 0);
            int    sessionId    = GsonUtil.getInt(body, "sessionId", -1);
            String reportType   = GsonUtil.getString(body, "reportType");
            String notes        = GsonUtil.getString(body, "notes");
            String reporterName = GsonUtil.getString(body, "reporterName");
            String reporterPhone= GsonUtil.getString(body, "reporterPhone");

            if (sessionId < 0 || reportType == null) { GsonUtil.error(resp, 400, "Thiếu sessionId hoặc reportType"); return; }
            if (reporterName == null || reporterName.isBlank()) reporterName = jwtUser != null ? jwtUser : "Unknown";

            Report r = new Report();
            r.setReporterId(userId);
            r.setVehicleId(vehicleId);
            r.setSessionId(sessionId);
            r.setReporterName(reporterName);
            r.setReporterPhone(reporterPhone != null ? reporterPhone : "");
            r.setReportType(reportType);
            r.setNotes(notes != null ? notes : "");

            int newId = reportDAO.insert(r);

            JsonObject res = new JsonObject();
            res.addProperty("reportId", newId);
            GsonUtil.created(resp, res);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
