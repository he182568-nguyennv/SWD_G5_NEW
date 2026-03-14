package Controller;

import Dao.ReportDAO;
import Model.Report;
import Model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {

    private final ReportDAO reportDAO = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        User user = (User) req.getSession().getAttribute("currentUser");
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chưa đăng nhập\"}");
            return;
        }

        try {
            List<Report> reports = (user.getRoleId() == 1)
                    ? reportDAO.findPending()
                    : reportDAO.findByReporter(user.getId());

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < reports.size(); i++) {
                Report r = reports.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"reportId\":").append(r.getReportId()).append(",")
                        .append("\"reporterId\":").append(r.getReporterId()).append(",")
                        .append("\"vehicleId\":").append(r.getVehicleId()).append(",")
                        .append("\"sessionId\":").append(r.getSessionId()).append(",")
                        .append("\"reporterName\":\"").append(safe(r.getReporterName())).append("\",")
                        .append("\"reporterPhone\":\"").append(safe(r.getReporterPhone())).append("\",")
                        .append("\"reportType\":\"").append(safe(r.getReportType())).append("\",")
                        .append("\"notes\":\"").append(safe(r.getNotes())).append("\",")
                        .append("\"status\":\"").append(safe(r.getStatus())).append("\",")
                        .append("\"createdAt\":\"").append(safe(r.getCreatedAt())).append("\"")
                        .append("}");
            }
            sb.append("]}");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        User user = (User) req.getSession().getAttribute("currentUser");
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chưa đăng nhập\"}");
            return;
        }

        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                Report r = new Report();
                r.setReporterId(user.getId());
                r.setVehicleId(Integer.parseInt(req.getParameter("vehicleId")));
                r.setSessionId(Integer.parseInt(req.getParameter("sessionId")));
                r.setReportType(req.getParameter("reportType"));
                r.setReporterName(user.getFullName());
                r.setReporterPhone(user.getPhone());
                r.setNotes(req.getParameter("notes"));
                int id = reportDAO.insert(r);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true,\"reportId\":" + id + ",\"status\":\"pending\"}");

            } else if ("approve".equals(action)) {
                if (user.getRoleId() != 1) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ manager mới được duyệt\"}");
                    return;
                }
                int reportId  = Integer.parseInt(req.getParameter("reportId"));
                String decision = req.getParameter("decision");
                String note     = req.getParameter("note");
                reportDAO.approve(reportId, user.getId(), decision, note);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true,\"reportId\":" + reportId + ",\"decision\":\"" + decision + "\"}");

            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Action không hợp lệ\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private String safe(String s) { return s == null ? "" : s.replace("\"", "\\\""); }
}