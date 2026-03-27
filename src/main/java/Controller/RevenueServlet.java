package Controller;

import Dao.ParkingSessionDAO;
import Dao.TransactionDAO;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/manager/revenue")
public class RevenueServlet extends HttpServlet {

    private final TransactionDAO    transDAO   = new TransactionDAO();
    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)    { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1) { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            String rangeParam = req.getParameter("range");
            int days = "30d".equals(rangeParam) ? 30 : "90d".equals(rangeParam) ? 90 : 7;

            JsonArray dailyData    = new JsonArray();
            double    totalRevenue = 0;
            int       totalSessions= 0;

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                String from    = date + " 00:00:00";
                String to      = date + " 23:59:59";
                double rev     = transDAO.revenueByLot(0, from, to);
                int    sess    = sessionDAO.countByDate(date.toString());

                JsonObject day = new JsonObject();
                day.addProperty("date",     date.getDayOfMonth() + "/" + date.getMonthValue());
                day.addProperty("revenue",  rev);
                day.addProperty("sessions", sess);
                dailyData.add(day);

                totalRevenue  += rev;
                totalSessions += sess;
            }

            // Revenue by lot — hardcoded ở đây, nên refactor sau
            int[]    lotIds   = {1, 2};
            String[] lotNames = {"Bãi xe A", "Bãi xe B"};
            JsonArray byLot   = new JsonArray();
            String fromRange  = LocalDate.now().minusDays(days - 1) + " 00:00:00";
            String toRange    = LocalDate.now() + " 23:59:59";
            for (int i = 0; i < lotIds.length; i++) {
                double rev = transDAO.revenueByLot(lotIds[i], fromRange, toRange);
                JsonObject o = new JsonObject();
                o.addProperty("lotName", lotNames[i]);
                o.addProperty("revenue", rev);
                byLot.add(o);
            }

            JsonObject res = new JsonObject();
            res.add("dailyData",      dailyData);
            res.add("byLot",          byLot);
            res.addProperty("totalRevenue",  totalRevenue);
            res.addProperty("totalSessions", totalSessions);
            GsonUtil.ok(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
