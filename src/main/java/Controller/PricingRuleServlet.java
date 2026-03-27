package Controller;

import Dao.PricingRuleDAO;
import Model.PricingRule;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/manager/pricing")
public class PricingRuleServlet extends HttpServlet {

    private final PricingRuleDAO dao = new PricingRuleDAO();

    private JsonObject ruleToJson(PricingRule r) {
        JsonObject o = new JsonObject();
        o.addProperty("ruleId",        r.getRuleId());
        o.addProperty("lotId",         r.getLotId());
        o.addProperty("typeId",        r.getTypeId());
        o.addProperty("feeType",       r.getFeeType());
        o.addProperty("pricePerBlock", r.getPricePerBlock());
        o.addProperty("blockMinutes",  r.getBlockMinutes());
        o.addProperty("maxDailyFee",   r.getMaxDailyFee());
        o.addProperty("isNightFee",    r.isNightFee());
        o.addProperty("isActive",      r.isActive());
        return o;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)    { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1) { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            List<PricingRule> rules = dao.findAll();
            JsonArray arr = new JsonArray();
            for (PricingRule r : rules) arr.add(ruleToJson(r));

            JsonObject res = new JsonObject();
            res.add("data", arr);
            GsonUtil.ok(resp, res);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)    { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1) { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            JsonObject body = GsonUtil.parseBody(req);
            String action   = GsonUtil.getString(body, "action");
            int    ruleId   = GsonUtil.getInt(body, "ruleId", -1);

            if ("toggle".equals(action)) {
                dao.toggleActive(ruleId);
                GsonUtil.ok(resp);
                return;
            }

            PricingRule r = new PricingRule();
            r.setLotId(GsonUtil.getInt(body, "lotId", 1));
            r.setTypeId(GsonUtil.getInt(body, "typeId", 1));
            r.setFeeType(GsonUtil.getString(body, "feeType"));
            r.setBlockMinutes(GsonUtil.getInt(body, "blockMinutes", 60));
            r.setNightFee("true".equals(GsonUtil.getString(body, "isNightFee")));

            // pricePerBlock và maxDailyFee có thể là number hoặc string trong JSON
            try {
                r.setPricePerBlock(body.has("pricePerBlock") && !body.get("pricePerBlock").isJsonNull()
                    ? body.get("pricePerBlock").getAsDouble() : 5000);
                r.setMaxDailyFee(body.has("maxDailyFee") && !body.get("maxDailyFee").isJsonNull()
                    ? body.get("maxDailyFee").getAsDouble() : 50000);
            } catch (Exception ex) {
                r.setPricePerBlock(5000);
                r.setMaxDailyFee(50000);
            }

            if ("update".equals(action) && ruleId > 0) {
                r.setRuleId(ruleId);
                dao.update(r);
                JsonObject res = new JsonObject();
                res.addProperty("ruleId", ruleId);
                GsonUtil.ok(resp, res);
            } else {
                int newId = dao.insert(r);
                JsonObject res = new JsonObject();
                res.addProperty("ruleId", newId);
                GsonUtil.created(resp, res);
            }
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
