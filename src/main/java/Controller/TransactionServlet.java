package Controller;


import Dao.TransactionDAO;
import Model.Transaction;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/transactions")
public class TransactionServlet extends HttpServlet {

    private final TransactionDAO dao = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int roleId = (int) req.getAttribute("jwtRoleId");
        int userId = (int) req.getAttribute("jwtUserId");

        try {
            List<Transaction> list;

            if (roleId == 1) {
                // Manager: tất cả hoặc filter theo lotId/ngày
                String lotParam  = req.getParameter("lotId");
                String fromParam = req.getParameter("from");
                String toParam   = req.getParameter("to");
                if (fromParam != null && toParam != null) {
                    int lotId = lotParam != null ? Integer.parseInt(lotParam) : 0;
                    list = dao.findByRange(lotId, fromParam, toParam);
                } else {
                    list = dao.findAll();
                }
            } else if (roleId == 2) {
                // Staff: giao dịch trong ca hôm nay
                list = dao.findByStaff(userId);
            } else if (roleId == 3) {
                // Customer: lịch sử của chính mình qua sessionId
                list = dao.findByUser(userId);
            } else {
                resp.setStatus(403);
                resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
                return;
            }

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]}");
            resp.setStatus(200);
            resp.getWriter().write(sb.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" +
                JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private String toJson(Transaction t) {
        return "{" +
            "\"transId\":"         + t.getTransId()                              + "," +
            "\"sessionId\":"       + t.getSessionId()                            + "," +
            "\"amount\":"          + t.getAmount()                               + "," +
            "\"discountAmount\":"  + t.getDiscountAmount()                       + "," +
            "\"paymentMethod\":\"" + JsonUtil.escape(t.getPaymentMethod())       + "\"," +
            "\"feeType\":\""       + JsonUtil.escape(t.getFeeType())             + "\"," +
            "\"paymentStatus\":\"" + JsonUtil.escape(t.getPaymentStatus())       + "\"," +
            "\"createdAt\":\""     + JsonUtil.escape(t.getCreatedAt())           + "\"" +
            "}";
    }
}
