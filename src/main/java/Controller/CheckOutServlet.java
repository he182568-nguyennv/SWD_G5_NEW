package Controller;

import Dao.ParkingSessionDAO;
import Dao.TransactionDAO;
import Model.ParkingSession;
import Model.Transaction;
import Model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/staff/checkout")
public class CheckOutServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        User staff = (User) req.getSession().getAttribute("currentUser");
        if (staff == null || staff.getRoleId() != 2) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Không có quyền truy cập\"}");
            return;
        }

        String plateNumber   = req.getParameter("plateNumber");
        String imgOut        = req.getParameter("vehicleImgOut");
        String paymentMethod = req.getParameter("paymentMethod");

        try {
            ParkingSession session = sessionDAO.findActiveByPlate(plateNumber);
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy xe đang gửi: " + plateNumber + "\"}");
                return;
            }

            double fee = calculateFee(session);
            sessionDAO.checkOut(session.getSessionId(), staff.getId(), imgOut);

            Transaction t = new Transaction();
            t.setSessionId(session.getSessionId());
            t.setUserId(staff.getId());
            t.setAmount(fee);
            t.setDiscountAmount(0);
            t.setPaymentMethod(paymentMethod);
            t.setFeeType("normal");
            int transId = transactionDAO.insert(t);
            transactionDAO.markPaid(transId, paymentMethod);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(
                    "{\"success\":true," +
                            "\"sessionId\":" + session.getSessionId() + "," +
                            "\"transId\":" + transId + "," +
                            "\"plateNumber\":\"" + plateNumber + "\"," +
                            "\"fee\":" + fee + "," +
                            "\"paymentMethod\":\"" + paymentMethod + "\"," +
                            "\"checkoutTime\":\"" + java.time.LocalDateTime.now() + "\"," +
                            "\"status\":\"completed\"}"
            );
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Lỗi server: " + e.getMessage() + "\"}");
        }
    }

    private double calculateFee(ParkingSession session) {
        try {
            long checkin = java.sql.Timestamp.valueOf(session.getCheckinTime()).getTime();
            double hours = Math.max(1, (System.currentTimeMillis() - checkin) / 3_600_000.0);
            return Math.round(hours) * 5000;
        } catch (Exception e) { return 5000; }
    }
}