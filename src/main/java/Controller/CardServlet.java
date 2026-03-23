package Controller;

import Utils.DBConnection;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

/**
 * GET /staff/cards?lotId=1
 * Trả danh sách thẻ còn khả dụng (không đang được dùng trong session active).
 */
@WebServlet("/staff/cards")
public class CardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 1 && roleId != 2) {
            resp.setStatus(403);
            resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
            return;
        }

        String lotParam = req.getParameter("lotId");

        try (Connection c = DBConnection.getConnection()) {
            String sql;
            PreparedStatement ps;

            if (lotParam != null && !lotParam.isEmpty()) {
                sql = "SELECT c.card_id, c.card_code, c.lot_id, c.card_type " +
                      "FROM cards c " +
                      "WHERE c.lot_id = ? AND c.is_active = 1 " +
                      "AND c.card_id NOT IN (" +
                      "  SELECT card_id FROM parking_sessions WHERE status='active'" +
                      ") ORDER BY c.card_code";
                ps = c.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(lotParam));
            } else {
                sql = "SELECT c.card_id, c.card_code, c.lot_id, c.card_type " +
                      "FROM cards c " +
                      "WHERE c.is_active = 1 " +
                      "AND c.card_id NOT IN (" +
                      "  SELECT card_id FROM parking_sessions WHERE status='active'" +
                      ") ORDER BY c.lot_id, c.card_code";
                ps = c.prepareStatement(sql);
            }

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{")
                      .append("\"cardId\":"     ).append(rs.getInt("card_id"))                   .append(",")
                      .append("\"cardCode\":\"" ).append(JsonUtil.escape(rs.getString("card_code"))).append("\",")
                      .append("\"lotId\":"      ).append(rs.getInt("lot_id"))                     .append(",")
                      .append("\"cardType\":\"" ).append(JsonUtil.escape(rs.getString("card_type"))).append("\"")
                      .append("}");
                    first = false;
                }
            }
            sb.append("]}");
            ps.close();
            resp.setStatus(200);
            resp.getWriter().write(sb.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" +
                JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
