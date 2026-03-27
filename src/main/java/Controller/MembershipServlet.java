package Controller;

import Dao.MembershipDAO;
import Model.Membership;
import Model.MembershipPlan;
import Utils.GsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@WebServlet("/customer/membership")
public class MembershipServlet extends HttpServlet {

    private final MembershipDAO dao = new MembershipDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int userId = (int) uid;
        int roleId = (int) rid;
        if (roleId != 3 && roleId != 1) { GsonUtil.error(resp, 403, "Forbidden"); return; }
        if (roleId == 1) {
            String p = req.getParameter("userId");
            if (p != null && !p.isBlank()) try { userId = Integer.parseInt(p); } catch (Exception ignored) {}
        }

        try {
            List<MembershipPlan> plans = dao.findAllPlans();
            Membership active          = dao.findActiveByUser(userId);

            // plans: Gson convert thẳng từ List
            JsonObject res = new JsonObject();
            res.add("plans", GsonUtil.GSON.toJsonTree(plans));

            // active: thêm trường daysLeft và planName (không có trong model)
            if (active != null) {
                long daysLeft = 0;
                try {
                    daysLeft = Math.max(0, ChronoUnit.DAYS.between(
                        LocalDate.now(), LocalDate.parse(active.getEndDate())));
                } catch (Exception ignored) {}

                MembershipPlan plan = dao.getPlanById(active.getPlanId());

                // Convert model sang JsonObject rồi thêm field bổ sung
                JsonObject activeObj = GsonUtil.GSON.toJsonTree(active).getAsJsonObject();
                activeObj.addProperty("daysLeft",  daysLeft);
                activeObj.addProperty("planName",  plan != null ? plan.getName() : "");
                res.add("active", activeObj);
            } else {
                res.add("active", com.google.gson.JsonNull.INSTANCE);
            }

            GsonUtil.ok(resp, res);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 3) { GsonUtil.error(resp, 403, "Chỉ customer mới được đăng ký"); return; }

        int userId = (int) uid;

        try {
            JsonObject body = GsonUtil.parseBody(req);
            int planId      = GsonUtil.getInt(body, "planId", -1);
            if (planId < 0) { GsonUtil.error(resp, 400, "Thiếu planId"); return; }

            Membership existing = dao.findActiveByUser(userId);
            if (existing != null) {
                GsonUtil.error(resp, 409, "Đang có gói active đến " + existing.getEndDate());
                return;
            }

            int newId = dao.register(userId, planId);
            if (newId < 0) { GsonUtil.error(resp, 400, "Gói không tồn tại"); return; }

            JsonObject res = new JsonObject();
            res.addProperty("membershipId", newId);
            GsonUtil.created(resp, res);
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }
}
