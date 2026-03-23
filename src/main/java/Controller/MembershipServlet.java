package Controller;


import Dao.MembershipDAO;
import Model.Membership;
import Model.MembershipPlan;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/customer/membership")
public class MembershipServlet extends HttpServlet {

    private final MembershipDAO membershipDAO = new MembershipDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = (int) req.getAttribute("jwtUserId");
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 3) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }

        try {
            List<MembershipPlan> plans   = membershipDAO.findAllPlans();
            Membership activeMembership  = membershipDAO.findActiveByUser(userId);
            List<Membership> history     = membershipDAO.findByUser(userId);

            // Plans JSON
            StringBuilder plansJson = new StringBuilder("[");
            for (int i = 0; i < plans.size(); i++) {
                MembershipPlan p = plans.get(i);
                if (i > 0) plansJson.append(",");
                plansJson.append("{")
                    .append("\"planId\":").append(p.getPlanId()).append(",")
                    .append("\"name\":\"").append(JsonUtil.escape(p.getName())).append("\",")
                    .append("\"durationDays\":").append(p.getDurationDays()).append(",")
                    .append("\"price\":").append(p.getPrice()).append(",")
                    .append("\"discountPct\":").append(p.getDiscountPct())
                    .append("}");
            }
            plansJson.append("]");

            // Active membership JSON
            String activeJson = "null";
            if (activeMembership != null) {
                activeJson = "{" +
                    "\"membershipId\":" + activeMembership.getMembershipId() + "," +
                    "\"planId\":"       + activeMembership.getPlanId()       + "," +
                    "\"startDate\":\""  + activeMembership.getStartDate()    + "\"," +
                    "\"endDate\":\""    + activeMembership.getEndDate()      + "\"," +
                    "\"status\":\""     + activeMembership.getStatus()       + "\"" +
                    "}";
            }

            resp.setStatus(200);
            resp.getWriter().write(
                "{\"success\":true," +
                "\"plans\":"  + plansJson + "," +
                "\"active\":" + activeJson + "}"
            );
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        int userId = (int) req.getAttribute("jwtUserId");
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 3) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }

        try {
            String body  = JsonUtil.readBody(req);
            int planId   = JsonUtil.getInt(body, "planId", -1);
            if (planId < 0) {
                resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing planId\"}"); return;
            }
            // Check no active membership
            Membership existing = membershipDAO.findActiveByUser(userId);
            if (existing != null) {
                resp.setStatus(409); resp.getWriter().write("{\"success\":false,\"message\":\"Bạn đang có gói thành viên active\"}"); return;
            }
            int newId = membershipDAO.register(userId, planId);
            resp.setStatus(201); resp.getWriter().write("{\"success\":true,\"membershipId\":" + newId + "}");
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
