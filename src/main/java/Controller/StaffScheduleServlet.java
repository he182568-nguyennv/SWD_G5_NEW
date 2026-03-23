package Controller;


import Dao.StaffScheduleDAO;
import Dao.UserDAO;
import Model.StaffSchedule;
import Model.User;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/schedule")
public class StaffScheduleServlet extends HttpServlet {

    private final StaffScheduleDAO scheduleDAO = new StaffScheduleDAO();
    private final UserDAO userDAO = new UserDAO();

    private String toJson(StaffSchedule s, String staffName) {
        return "{" +
            "\"scheduleId\":"   + s.getScheduleId() + "," +
            "\"staffId\":"      + s.getStaffId()    + "," +
            "\"staffName\":\""  + JsonUtil.escape(staffName != null ? staffName : "") + "\"," +
            "\"lotId\":"        + s.getLotId()      + "," +
            "\"workDate\":\""   + JsonUtil.escape(s.getWorkDate())    + "\"," +
            "\"shiftStart\":\"" + JsonUtil.escape(s.getShiftStart())  + "\"," +
            "\"shiftEnd\":\""   + JsonUtil.escape(s.getShiftEnd())    + "\"," +
            "\"status\":\""     + JsonUtil.escape(s.getStatus())      + "\"" +
            "}";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int roleId  = (int) req.getAttribute("jwtRoleId");
        int userId  = (int) req.getAttribute("jwtUserId");

        try {
            List<StaffSchedule> schedules;
            if (roleId == 1) {
                // Manager: get all or by staff
                String staffParam = req.getParameter("staffId");
                schedules = staffParam != null
                    ? scheduleDAO.findByStaff(Integer.parseInt(staffParam))
                    : scheduleDAO.findAll();
            } else if (roleId == 2) {
                schedules = scheduleDAO.findByStaff(userId);
            } else {
                resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return;
            }

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < schedules.size(); i++) {
                if (i > 0) sb.append(",");
                StaffSchedule s = schedules.get(i);
                User staff = userDAO.findById(s.getStaffId());
                sb.append(toJson(s, staff != null ? staff.getFullName() : ""));
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
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }

        try {
            String body   = JsonUtil.readBody(req);
            String action = JsonUtil.getString(body, "action");

            if ("updateStatus".equals(action)) {
                int scheduleId = JsonUtil.getInt(body, "scheduleId", -1);
                String status  = JsonUtil.getString(body, "status");
                scheduleDAO.updateStatus(scheduleId, status);
                resp.setStatus(200); resp.getWriter().write("{\"success\":true}");
                return;
            }

            // Create new schedule
            StaffSchedule s = new StaffSchedule();
            s.setStaffId(JsonUtil.getInt(body, "staffId", -1));
            s.setLotId(JsonUtil.getInt(body, "lotId", 1));
            s.setWorkDate(JsonUtil.getString(body, "workDate"));
            s.setShiftStart(JsonUtil.getString(body, "shiftStart"));
            s.setShiftEnd(JsonUtil.getString(body, "shiftEnd"));

            if (s.getStaffId() < 0 || s.getWorkDate() == null) {
                resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing required fields\"}"); return;
            }
            int newId = scheduleDAO.insert(s);
            resp.setStatus(201); resp.getWriter().write("{\"success\":true,\"scheduleId\":" + newId + "}");
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
