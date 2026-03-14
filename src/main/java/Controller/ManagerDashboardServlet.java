package Controller;


import Dao.ReportDAO;
import Dao.TransactionDAO;
import Dao.UserDAO;
import Model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/manager/dashboard")
public class ManagerDashboardServlet extends HttpServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final UserDAO userDAO        = new UserDAO();
    private final ReportDAO      reportDAO      = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        User user = (User) req.getSession().getAttribute("currentUser");
        if (user == null || user.getRoleId() != 1) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ manager mới được truy cập\"}");
            return;
        }

        try {
            String today = java.time.LocalDate.now().toString();
            double todayRevenue  = transactionDAO.revenueByLot(1, today + " 00:00:00", today + " 23:59:59");
            int pendingReports   = reportDAO.findPending().size();
            int totalStaff       = userDAO.findByRole(2).size();
            int totalCustomers   = userDAO.findByRole(3).size();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(
                    "{\"success\":true," +
                            "\"todayRevenue\":" + todayRevenue + "," +
                            "\"pendingReports\":" + pendingReports + "," +
                            "\"totalStaff\":" + totalStaff + "," +
                            "\"totalCustomers\":" + totalCustomers + "}"
            );
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}