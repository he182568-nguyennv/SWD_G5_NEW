package Controller;

import Dao.*;
import Model.*;
import Utils.GsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

@WebServlet("/staff/checkout")
public class CheckOutServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO     = new ParkingSessionDAO();
    private final TransactionDAO    transactionDAO = new TransactionDAO();
    private final PricingRuleDAO    pricingDAO     = new PricingRuleDAO();
    private final MembershipDAO     membershipDAO  = new MembershipDAO();
    private final VehicleDAO        vehicleDAO     = new VehicleDAO();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** GET /staff/checkout?plate=30A-12345 — xem trước phí */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object roleAttr = req.getAttribute("jwtRoleId");
        if (roleAttr == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        int roleId = (int) roleAttr;
        if (roleId != 1 && roleId != 2) { GsonUtil.error(resp, 403, "Forbidden"); return; }

        String plate = req.getParameter("plate");
        if (plate == null || plate.isBlank()) { GsonUtil.error(resp, 400, "Thiếu biển số xe"); return; }

        try {
            ParkingSession session = sessionDAO.findActiveByPlate(plate.trim().toUpperCase());
            if (session == null) {
                GsonUtil.error(resp, 404, "Không tìm thấy xe " + plate + " đang gửi trong bãi");
                return;
            }
            FeeResult fee = calcFee(session);
            GsonUtil.ok(resp, feeJson(session, fee));
        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }

    /** POST /staff/checkout — xác nhận checkout + tạo transaction */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        Object roleAttr = req.getAttribute("jwtRoleId");
        Object userAttr = req.getAttribute("jwtUserId");
        if (roleAttr == null || userAttr == null) { GsonUtil.error(resp, 401, "Unauthorized"); return; }

        int roleId  = (int) roleAttr;
        int staffId = (int) userAttr;
        if (roleId != 2) { GsonUtil.error(resp, 403, "Chỉ nhân viên mới được check-out"); return; }

        try {
            JsonObject body      = GsonUtil.parseBody(req);
            String plateNumber   = GsonUtil.getString(body, "plateNumber");
            String paymentMethod = GsonUtil.getString(body, "paymentMethod");
            String imgOut        = GsonUtil.getString(body, "vehicleImgOut");

            if (plateNumber == null || plateNumber.isBlank() || paymentMethod == null) {
                GsonUtil.error(resp, 400, "Thiếu plateNumber hoặc paymentMethod");
                return;
            }

            plateNumber = plateNumber.trim().toUpperCase();

            ParkingSession session = sessionDAO.findActiveByPlate(plateNumber);
            if (session == null) {
                GsonUtil.error(resp, 404, "Không tìm thấy xe " + plateNumber + " đang gửi trong bãi");
                return;
            }

            FeeResult fee = calcFee(session);

            boolean updated = sessionDAO.checkOut(session.getSessionId(), staffId, imgOut != null ? imgOut : "");
            if (!updated) { GsonUtil.error(resp, 409, "Session đã được checkout rồi"); return; }

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

            JsonObject res = new JsonObject();
            res.addProperty("transId",         transId);
            res.addProperty("sessionId",        session.getSessionId());
            res.addProperty("plateNumber",      plateNumber);
            res.addProperty("durationMinutes",  fee.durationMinutes);
            res.addProperty("baseFee",          fee.baseFee);
            res.addProperty("discountAmount",   fee.discountAmount);
            res.addProperty("finalFee",         fee.finalFee);
            res.addProperty("paymentMethod",    paymentMethod);
            res.addProperty("feeType",          fee.feeType);
            GsonUtil.ok(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }

    private FeeResult calcFee(ParkingSession session) throws Exception {
        FeeResult r = new FeeResult();

        LocalDateTime checkin = LocalDateTime.parse(session.getCheckinTime(), FMT);
        LocalDateTime now     = LocalDateTime.now();
        r.durationMinutes     = (int) Math.max(1, Duration.between(checkin, now).toMinutes());

        LocalTime t = now.toLocalTime();
        if (t.isAfter(LocalTime.of(22, 0)) || t.isBefore(LocalTime.of(6, 0)))
            r.feeType = "overnight";
        else if (t.isAfter(LocalTime.of(17, 0)))
            r.feeType = "peak";
        else
            r.feeType = "normal";

        int typeId = 1;
        if (session.getVehicleId() > 0) {
            RegisteredVehicle v = vehicleDAO.findByPlate(session.getPlateNumber());
            if (v != null) typeId = v.getTypeId();
        }

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

        int blocks = (int) Math.ceil((double) r.durationMinutes / blockMinutes);
        r.baseFee  = Math.min(blocks * pricePerBlock, maxDailyFee);

        if (session.getMembershipId() > 0) {
            Membership m = membershipDAO.findById(session.getMembershipId());
            if (m != null) {
                double discPct   = membershipDAO.getPlanDiscountPct(m.getPlanId());
                r.discountPct    = discPct;
                r.discountAmount = Math.round(r.baseFee * discPct / 100.0);
                r.hasMembership  = true;
            }
        }
        r.finalFee = Math.round(r.baseFee - r.discountAmount);
        return r;
    }

    private JsonObject feeJson(ParkingSession s, FeeResult fee) {
        JsonObject o = new JsonObject();
        o.addProperty("sessionId",       s.getSessionId());
        o.addProperty("plateNumber",     s.getPlateNumber());
        o.addProperty("lotId",           s.getLotId());
        o.addProperty("checkinTime",     s.getCheckinTime());
        o.addProperty("durationMinutes", fee.durationMinutes);
        o.addProperty("feeType",         fee.feeType);
        o.addProperty("baseFee",         fee.baseFee);
        o.addProperty("discountPct",     fee.discountPct);
        o.addProperty("discountAmount",  fee.discountAmount);
        o.addProperty("hasMembership",   fee.hasMembership);
        o.addProperty("finalFee",        fee.finalFee);
        return o;
    }

    static class FeeResult {
        int     durationMinutes, ruleId;
        double  baseFee, discountAmount, discountPct, finalFee;
        boolean hasMembership;
        String  feeType = "normal";
    }
}
