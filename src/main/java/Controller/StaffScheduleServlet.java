package Controller;

import Dao.StaffScheduleDAO;
import Dao.UserDAO;
import Model.StaffSchedule;
import Model.User;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/schedule")
public class StaffScheduleServlet extends HttpServlet {

    private final StaffScheduleDAO scheduleDAO = new StaffScheduleDAO();
    private final UserDAO          userDAO     = new UserDAO();

    private JsonObject scheduleToJson(StaffSchedule s, String staffName) {
        JsonObject o = new JsonObject();
        o.addProperty("scheduleId",  s.getScheduleId());
        o.addProperty("staffId",     s.getStaffId());
        o.addProperty("staffName",   staffName != null ? staffName : "");
        o.addProperty("lotId",       s.getLotId());
        o.addProperty("workDate",    s.getWorkDate());
        o.addProperty("shiftStart",  s.getShiftStart());
        o.addProperty("shiftEnd",    s.getShiftEnd());
        o.addProperty("status",      s.getStatus());
        return o;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        Object uid = req.getAttribute("jwtUserId");
        if (rid == null || uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int roleId = (int) rid;
        int userId = (int) uid;

        try {
            List<StaffSchedule> schedules;
            if (roleId == 1) {
                String staffParam = req.getParameter("staffId");
                schedules = staffParam != null
                    ? scheduleDAO.findByStaff(Integer.parseInt(staffParam))
                    : scheduleDAO.findAll();
            } else if (roleId == 2) {
                schedules = scheduleDAO.findByStaff(userId);
            } else {
                GsonUtil.error(resp, 403, "Forbidden"); return;
            }

            JsonArray arr = new JsonArray();
            for (StaffSchedule s : schedules) {
                User staff = userDAO.findById(s.getStaffId());
                arr.add(scheduleToJson(s, staff != null ? staff.getFullName() : ""));
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
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)    { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1) { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            JsonObject body = GsonUtil.parseBody(req);
            String action   = GsonUtil.getString(body, "action");

            if ("updateStatus".equals(action)) {
                int scheduleId = GsonUtil.getInt(body, "scheduleId", -1);
                String status  = GsonUtil.getString(body, "status");
                scheduleDAO.updateStatus(scheduleId, status);
                GsonUtil.ok(resp);
                return;
            }

            StaffSchedule s = new StaffSchedule();
            s.setStaffId(GsonUtil.getInt(body, "staffId", -1));
            s.setLotId(GsonUtil.getInt(body, "lotId", 1));
            s.setWorkDate(GsonUtil.getString(body, "workDate"));
            s.setShiftStart(GsonUtil.getString(body, "shiftStart"));
            s.setShiftEnd(GsonUtil.getString(body, "shiftEnd"));

            if (s.getStaffId() < 0 || s.getWorkDate() == null) {
                GsonUtil.error(resp, 400, "Missing required fields"); return;
            }

            int newId = scheduleDAO.insert(s);
            JsonObject res = new JsonObject();
            res.addProperty("scheduleId", newId);
            GsonUtil.created(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
