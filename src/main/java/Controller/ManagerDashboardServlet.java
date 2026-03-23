package Controller;


import Dao.ParkingSessionDAO;
import Dao.ReportDAO;
import Dao.TransactionDAO;
import Dao.UserDAO;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet("/manager/dashboard")
public class ManagerDashboardServlet extends HttpServlet {

    private final TransactionDAO    transDAO   = new TransactionDAO();
    private final UserDAO           userDAO    = new UserDAO();
    private final ReportDAO         reportDAO  = new ReportDAO();
    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 1) {
            resp.setStatus(403);
            resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
            return;
        }

        try {
            String today = LocalDate.now().toString();
            String from  = today + " 00:00:00";
            String to    = today + " 23:59:59";

            // ── KPIs ────────────────────────────────────────────
            double todayRevenue    = transDAO.revenueByLot(0, from, to);   // 0 = all lots
            int    pendingReports  = reportDAO.countByStatus("pending");
            int    totalStaff      = userDAO.countByRole(2);
            int    totalCustomers  = userDAO.countByRole(3);
            int    activeSessions  = sessionDAO.countByStatus("active");
            int    todaySessions   = sessionDAO.countByDate(today);

            // ── Revenue last 7 days ──────────────────────────────
            StringBuilder revTrend = new StringBuilder("[");
            double prevRevenue = 0;
            for (int i = 6; i >= 0; i--) {
                LocalDate d   = LocalDate.now().minusDays(i);
                String df     = d.toString() + " 00:00:00";
                String dt     = d.toString() + " 23:59:59";
                double rev    = transDAO.revenueByLot(0, df, dt);
                int    sess   = sessionDAO.countByDate(d.toString());
                if (i < 6) revTrend.append(",");
                revTrend.append("{")
                    .append("\"date\":\"").append(d.getDayOfMonth()).append("/").append(d.getMonthValue()).append("\",")
                    .append("\"revenue\":").append(rev).append(",")
                    .append("\"sessions\":").append(sess)
                    .append("}");
                if (i == 1) prevRevenue = rev;
            }
            revTrend.append("]");

            // Growth % vs yesterday
            double growthPct = prevRevenue > 0
                ? Math.round(((todayRevenue - prevRevenue) / prevRevenue) * 100.0)
                : 0;

            // ── Recent reports (5 latest) ────────────────────────
            List<Model.Report> recentReports = reportDAO.findRecent(5);
            StringBuilder reportsJson = new StringBuilder("[");
            for (int i = 0; i < recentReports.size(); i++) {
                Model.Report r = recentReports.get(i);
                if (i > 0) reportsJson.append(",");
                reportsJson.append("{")
                    .append("\"reportId\":").append(r.getReportId()).append(",")
                    .append("\"reportType\":\"").append(JsonUtil.escape(r.getReportType())).append("\",")
                    .append("\"status\":\"").append(JsonUtil.escape(r.getStatus())).append("\",")
                    .append("\"createdAt\":\"").append(JsonUtil.escape(r.getCreatedAt())).append("\"")
                    .append("}");
            }
            reportsJson.append("]");

            // ── Compose response ─────────────────────────────────
            resp.setStatus(200);
            resp.getWriter().write(
                "{\"success\":true," +
                "\"todayRevenue\":"   + todayRevenue   + "," +
                "\"pendingReports\":" + pendingReports + "," +
                "\"totalStaff\":"     + totalStaff     + "," +
                "\"totalCustomers\":" + totalCustomers + "," +
                "\"activeSessions\":" + activeSessions + "," +
                "\"todaySessions\":"  + todaySessions  + "," +
                "\"growthPct\":"      + growthPct      + "," +
                "\"revenueTrend\":"   + revTrend       + "," +
                "\"recentReports\":"  + reportsJson    + "}"
            );
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" +
                JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
