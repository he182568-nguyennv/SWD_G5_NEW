package Controller;


import Dao.PricingRuleDAO;
import Model.PricingRule;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/manager/pricing")
public class PricingRuleServlet extends HttpServlet {

    private final PricingRuleDAO dao = new PricingRuleDAO();

    private String toJson(PricingRule r) {
        return "{" +
            "\"ruleId\":"         + r.getRuleId()       + "," +
            "\"lotId\":"          + r.getLotId()         + "," +
            "\"typeId\":"         + r.getTypeId()        + "," +
            "\"feeType\":\""      + JsonUtil.escape(r.getFeeType()) + "\"," +
            "\"pricePerBlock\":"  + r.getPricePerBlock() + "," +
            "\"blockMinutes\":"   + r.getBlockMinutes()  + "," +
            "\"maxDailyFee\":"    + r.getMaxDailyFee()   + "," +
            "\"isNightFee\":"     + r.isNightFee()       + "," +
            "\"isActive\":"       + r.isActive()         +
            "}";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int role = (int) req.getAttribute("jwtRoleId");
        if (role != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }
        try {
            List<PricingRule> rules = dao.findAll();
            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < rules.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(rules.get(i)));
            }
            sb.append("]}");
            resp.setStatus(200); resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        int role = (int) req.getAttribute("jwtRoleId");
        if (role != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }
        try {
            String body   = JsonUtil.readBody(req);
            String action = JsonUtil.getString(body, "action");
            int ruleId    = JsonUtil.getInt(body, "ruleId", -1);

            if ("toggle".equals(action)) {
                dao.toggleActive(ruleId);
                resp.setStatus(200); resp.getWriter().write("{\"success\":true}");
                return;
            }

            PricingRule r = new PricingRule();
            r.setLotId(JsonUtil.getInt(body, "lotId", 1));
            r.setTypeId(JsonUtil.getInt(body, "typeId", 1));
            r.setFeeType(JsonUtil.getString(body, "feeType"));
            r.setPricePerBlock(Double.parseDouble(JsonUtil.getString(body, "pricePerBlock") != null ? JsonUtil.getString(body, "pricePerBlock") : "5000"));
            r.setBlockMinutes(JsonUtil.getInt(body, "blockMinutes", 60));
            r.setMaxDailyFee(Double.parseDouble(JsonUtil.getString(body, "maxDailyFee") != null ? JsonUtil.getString(body, "maxDailyFee") : "50000"));
            r.setNightFee("true".equals(JsonUtil.getString(body, "isNightFee")));

            if ("update".equals(action) && ruleId > 0) {
                r.setRuleId(ruleId);
                dao.update(r);
                resp.setStatus(200); resp.getWriter().write("{\"success\":true,\"ruleId\":" + ruleId + "}");
            } else {
                int newId = dao.insert(r);
                resp.setStatus(201); resp.getWriter().write("{\"success\":true,\"ruleId\":" + newId + "}");
            }
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
