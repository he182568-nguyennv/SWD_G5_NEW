package Controller;

import Dao.TransactionDAO;
import Model.Transaction;
import Utils.GsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/transactions")
public class TransactionServlet extends HttpServlet {

    private final TransactionDAO dao = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int userId = (int) uid;
        int roleId = (int) rid;

        try {
            List<Transaction> list;

            if (roleId == 1) {
                String lotParam  = req.getParameter("lotId");
                String fromParam = req.getParameter("from");
                String toParam   = req.getParameter("to");
                if (fromParam != null && toParam != null) {
                    int lotId = (lotParam != null) ? Integer.parseInt(lotParam) : 0;
                    list = dao.findByRange(lotId, fromParam, toParam);
                } else {
                    list = dao.findAll();
                }
            } else if (roleId == 2) {
                list = dao.findByStaff(userId);
            } else if (roleId == 3) {
                list = dao.findByUser(userId);
            } else {
                GsonUtil.error(resp, 403, "Forbidden");
                return;
            }

            // Gson convert thẳng List<Transaction> → JSON array
            GsonUtil.ok(resp, list);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
