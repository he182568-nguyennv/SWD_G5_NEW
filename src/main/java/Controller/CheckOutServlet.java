package Controller;

import Dao.*;
import Model.*;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

@WebServlet("/staff/checkout")
public class CheckOutServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO     = new ParkingSessionDAO();
    private final TransactionDAO    transactionDAO = new TransactionDAO();
    private final PricingRuleDAO pricingDAO     = new PricingRuleDAO();
    private final MembershipDAO membershipDAO  = new MembershipDAO();
    private final VehicleDAO vehicleDAO     = new VehicleDAO();

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** GET /staff/checkout?plate=30A-12345 — xem trước phí */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 1 && roleId != 2) {
            resp.setStatus(403);
            resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
            return;
        }
        String plate = req.getParameter("plate");
        if (plate == null || plate.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu biển số xe\"}");
            return;
        }
        try {
            ParkingSession session = sessionDAO.findActiveByPlate(plate.trim().toUpperCase());
            if (session == null) {
                resp.setStatus(404);
                resp.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy xe " +
                    JsonUtil.escape(plate) + " đang gửi trong bãi\"}");
                return;
            }
            FeeResult fee = calcFee(session);
            resp.setStatus(200);
            resp.getWriter().write(feeJson(session, fee));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" +
                JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    /** POST /staff/checkout — xác nhận checkout + tạo transaction */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        int roleId  = (int) req.getAttribute("jwtRoleId");
        int staffId = (int) req.getAttribute("jwtUserId");
        if (roleId != 2) {
            resp.setStatus(403);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ nhân viên mới được check-out\"}");
            return;
        }
        try {
            String body          = JsonUtil.readBody(req);
            String plateNumber   = JsonUtil.getString(body, "plateNumber");
            String paymentMethod = JsonUtil.getString(body, "paymentMethod");
            String imgOut        = JsonUtil.getString(body, "vehicleImgOut");

            if (plateNumber == null || paymentMethod == null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu plateNumber hoặc paymentMethod\"}");
                return;
            }

            ParkingSession session = sessionDAO.findActiveByPlate(plateNumber.trim().toUpperCase());
            if (session == null) {
                resp.setStatus(404);
                resp.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy xe " +
                    JsonUtil.escape(plateNumber) + " đang gửi trong bãi\"}");
                return;
            }

            FeeResult fee = calcFee(session);

            // 1. Cập nhật session → completed
            sessionDAO.checkOut(session.getSessionId(), staffId, imgOut != null ? imgOut : "");

            // 2. Tạo transaction
            Transaction t = new Transaction();
            t.setSessionId(session.getSessionId());
            t.setUserId(staffId);
            t.setMembershipId(session.getMembershipId());
            t.setRuleId(fee.ruleId);
            t.setAmount(fee.finalFee);
            t.setDiscountAmount(fee.discountAmount);
            t.setPaymentMethod(paymentMethod);
            t.setFeeType(fee.feeType);
            int transId = transactionDAO.insert(t);
            transactionDAO.markPaid(transId, paymentMethod);

            resp.setStatus(200);
            resp.getWriter().write(
                "{\"success\":true," +
                "\"transId\":"         + transId                                      + "," +
                "\"sessionId\":"       + session.getSessionId()                       + "," +
                "\"plateNumber\":\""   + JsonUtil.escape(plateNumber)                 + "\"," +
                "\"durationMinutes\":" + fee.durationMinutes                          + "," +
                "\"baseFee\":"         + fee.baseFee                                  + "," +
                "\"discountAmount\":"  + fee.discountAmount                           + "," +
                "\"finalFee\":"        + fee.finalFee                                 + "," +
                "\"paymentMethod\":\"" + JsonUtil.escape(paymentMethod)               + "\"," +
                "\"feeType\":\""       + JsonUtil.escape(fee.feeType)                 + "\"}"
            );
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" +
                JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    // ── Tính phí ──────────────────────────────────────────────
    private FeeResult calcFee(ParkingSession session) throws Exception {
        FeeResult r = new FeeResult();

        // Duration
        LocalDateTime checkin = LocalDateTime.parse(session.getCheckinTime(), FMT);
        LocalDateTime now     = LocalDateTime.now();
        r.durationMinutes     = (int) Math.max(1, Duration.between(checkin, now).toMinutes());

        // Fee type
        LocalTime t = now.toLocalTime();
        if (t.isAfter(LocalTime.of(22, 0)) || t.isBefore(LocalTime.of(6, 0)))
            r.feeType = "overnight";
        else if (t.isAfter(LocalTime.of(17, 0)))
            r.feeType = "peak";
        else
            r.feeType = "normal";

        // Vehicle type
        int typeId = 1;
        if (session.getVehicleId() > 0) {
            RegisteredVehicle v = vehicleDAO.findByPlate(session.getPlateNumber());
            if (v != null) typeId = v.getTypeId();
        }

        // Pricing rule (fallback chain: feeType → normal → default 5000/60min)
        PricingRule rule = pricingDAO.findApplicable(session.getLotId(), typeId, r.feeType);
        if (rule == null) rule = pricingDAO.findApplicable(session.getLotId(), typeId, "normal");

        double pricePerBlock = 5000, maxDailyFee = 50000;
        int    blockMinutes  = 60;
        if (rule != null) {
            pricePerBlock = rule.getPricePerBlock();
            blockMinutes  = rule.getBlockMinutes();
            maxDailyFee   = rule.getMaxDailyFee();
            r.ruleId      = rule.getRuleId();
        }

        // Base fee
        int blocks  = (int) Math.ceil((double) r.durationMinutes / blockMinutes);
        r.baseFee   = Math.min(blocks * pricePerBlock, maxDailyFee);

        // Membership discount (session.membershipId links to memberships.membership_id)
        if (session.getMembershipId() > 0) {
            Membership m = membershipDAO.findActiveByUser(session.getMembershipId());
            if (m != null) {
                double discPct    = membershipDAO.getPlanDiscountPct(m.getPlanId());
                r.discountPct     = discPct;
                r.discountAmount  = Math.round(r.baseFee * discPct / 100.0);
                r.hasMembership   = true;
            }
        }
        r.finalFee = Math.round(r.baseFee - r.discountAmount);
        return r;
    }

    private String feeJson(ParkingSession s, FeeResult fee) {
        return "{\"success\":true," +
            "\"sessionId\":"       + s.getSessionId()                       + "," +
            "\"plateNumber\":\""   + JsonUtil.escape(s.getPlateNumber())    + "\"," +
            "\"lotId\":"           + s.getLotId()                           + "," +
            "\"checkinTime\":\""   + JsonUtil.escape(s.getCheckinTime())    + "\"," +
            "\"durationMinutes\":" + fee.durationMinutes                    + "," +
            "\"feeType\":\""       + JsonUtil.escape(fee.feeType)           + "\"," +
            "\"baseFee\":"         + fee.baseFee                            + "," +
            "\"discountPct\":"     + fee.discountPct                        + "," +
            "\"discountAmount\":"  + fee.discountAmount                     + "," +
            "\"hasMembership\":"   + fee.hasMembership                      + "," +
            "\"finalFee\":"        + fee.finalFee                           + "}";
    }

    static class FeeResult {
        int     durationMinutes, ruleId;
        double  baseFee, discountAmount, discountPct, finalFee;
        boolean hasMembership;
        String  feeType = "normal";
    }
}
