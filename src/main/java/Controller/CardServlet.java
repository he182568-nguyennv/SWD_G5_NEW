package Controller;

import Utils.DBConnection;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/staff/cards")
public class CardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)                        { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1 && (int) rid != 2)  { GsonUtil.error(resp, 403, "Forbidden");    return; }

        String lotParam = req.getParameter("lotId");

        // Bảng cards dùng cột "status" (INTEGER 1=active), KHÔNG phải "is_active"
        String sql = lotParam != null && !lotParam.isEmpty()
            ? "SELECT card_id, card_code, lot_id, card_type FROM cards " +
              "WHERE lot_id = ? AND status = 1 " +
              "AND card_id NOT IN (SELECT card_id FROM parking_sessions WHERE status = 'active') " +
              "ORDER BY card_code"
            : "SELECT card_id, card_code, lot_id, card_type FROM cards " +
              "WHERE status = 1 " +
              "AND card_id NOT IN (SELECT card_id FROM parking_sessions WHERE status = 'active') " +
              "ORDER BY lot_id, card_code";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (lotParam != null && !lotParam.isEmpty()) {
                try { ps.setInt(1, Integer.parseInt(lotParam)); }
                catch (NumberFormatException e) { GsonUtil.error(resp, 400, "lotId không hợp lệ"); return; }
            }

            JsonArray arr = new JsonArray();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject card = new JsonObject();
                    card.addProperty("cardId",   rs.getInt("card_id"));
                    card.addProperty("cardCode", rs.getString("card_code"));
                    card.addProperty("lotId",    rs.getInt("lot_id"));
                    card.addProperty("cardType", rs.getString("card_type"));
                    arr.add(card);
                }
            }

            JsonObject res = new JsonObject();
            res.add("data", arr);
            GsonUtil.ok(resp, res);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
