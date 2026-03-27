package Controller;

import Dao.ParkingSessionDAO;
import Dao.ReportDAO;
import Dao.TransactionDAO;
import Dao.UserDAO;
import Model.Report;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/manager/dashboard")
public class ManagerDashboardServlet extends HttpServlet {

    private final TransactionDAO    transDAO   = new TransactionDAO();
    private final UserDAO           userDAO    = new UserDAO();
    private final ReportDAO         reportDAO  = new ReportDAO();
    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)       { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1)    { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            String today = LocalDate.now().toString();
            String from  = today + " 00:00:00";
            String to    = today + " 23:59:59";

            double todayRevenue   = transDAO.revenueByLot(0, from, to);
            int    pendingReports = reportDAO.countByStatus("pending");
            int    totalStaff     = userDAO.countByRole(2);
            int    totalCustomers = userDAO.countByRole(3);
            int    activeSessions = sessionDAO.countByStatus("active");
            int    todaySessions  = sessionDAO.countByDate(today);

            // Revenue last 7 days
            JsonArray revTrend = new JsonArray();
            double prevRevenue = 0;
            for (int i = 6; i >= 0; i--) {
                LocalDate d  = LocalDate.now().minusDays(i);
                String df    = d + " 00:00:00";
                String dt    = d + " 23:59:59";
                double rev   = transDAO.revenueByLot(0, df, dt);
                int    sess  = sessionDAO.countByDate(d.toString());

                JsonObject day = new JsonObject();
                day.addProperty("date",     d.getDayOfMonth() + "/" + d.getMonthValue());
                day.addProperty("revenue",  rev);
                day.addProperty("sessions", sess);
                revTrend.add(day);

                if (i == 1) prevRevenue = rev;
            }

            double growthPct = prevRevenue > 0
                ? Math.round(((todayRevenue - prevRevenue) / prevRevenue) * 100.0)
                : 0;

            // Recent reports
            List<Report> recentReports = reportDAO.findRecent(5);
            JsonArray reportsArr = new JsonArray();
            for (Report r : recentReports) {
                JsonObject o = new JsonObject();
                o.addProperty("reportId",   r.getReportId());
                o.addProperty("reportType", r.getReportType());
                o.addProperty("status",     r.getStatus());
                o.addProperty("createdAt",  r.getCreatedAt());
                reportsArr.add(o);
            }

            JsonObject res = new JsonObject();
            res.addProperty("todayRevenue",   todayRevenue);
            res.addProperty("pendingReports", pendingReports);
            res.addProperty("totalStaff",     totalStaff);
            res.addProperty("totalCustomers", totalCustomers);
            res.addProperty("activeSessions", activeSessions);
            res.addProperty("todaySessions",  todaySessions);
            res.addProperty("growthPct",      growthPct);
            res.add("revenueTrend",           revTrend);
            res.add("recentReports",          reportsArr);
            GsonUtil.ok(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
