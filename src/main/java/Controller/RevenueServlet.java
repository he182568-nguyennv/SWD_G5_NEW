package Controller;


import Dao.ParkingSessionDAO;
import Dao.TransactionDAO;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/manager/revenue")
public class RevenueServlet extends HttpServlet {

    private final TransactionDAO transDAO = new TransactionDAO();
    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int role = (int) req.getAttribute("jwtRoleId");
        if (role != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }

        try {
            String rangeParam = req.getParameter("range");
            int days = "30d".equals(rangeParam) ? 30 : "90d".equals(rangeParam) ? 90 : 7;

            // Build daily data for last N days
            StringBuilder dailyData = new StringBuilder("[");
            double totalRevenue = 0;
            int totalSessions = 0;

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date  = LocalDate.now().minusDays(i);
                String from     = date + " 00:00:00";
                String to       = date + " 23:59:59";
                double rev      = transDAO.revenueByLot(0, from, to); // 0 = all lots
                int sessCount   = sessionDAO.countByDate(date.toString());

                if (i < days - 1) dailyData.append(",");
                dailyData.append("{")
                    .append("\"date\":\"").append(date.getDayOfMonth()).append("/").append(date.getMonthValue()).append("\",")
                    .append("\"revenue\":").append(rev).append(",")
                    .append("\"sessions\":").append(sessCount)
                    .append("}");
                totalRevenue += rev;
                totalSessions += sessCount;
            }
            dailyData.append("]");

            // Revenue by lot
            int[] lotIds = {1, 2};
            String[] lotNames = {"Bãi xe A", "Bãi xe B"};
            StringBuilder byLot = new StringBuilder("[");
            for (int i = 0; i < lotIds.length; i++) {
                String from = LocalDate.now().minusDays(days - 1) + " 00:00:00";
                String to   = LocalDate.now() + " 23:59:59";
                double rev  = transDAO.revenueByLot(lotIds[i], from, to);
                if (i > 0) byLot.append(",");
                byLot.append("{")
                    .append("\"lotName\":\"").append(lotNames[i]).append("\",")
                    .append("\"revenue\":").append(rev)
                    .append("}");
            }
            byLot.append("]");

            resp.setStatus(200);
            resp.getWriter().write(
                "{\"success\":true," +
                "\"dailyData\":" + dailyData + "," +
                "\"byLot\":" + byLot + "," +
                "\"totalRevenue\":" + totalRevenue + "," +
                "\"totalSessions\":" + totalSessions + "}"
            );
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
