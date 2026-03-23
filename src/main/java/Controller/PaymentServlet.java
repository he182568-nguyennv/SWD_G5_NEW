package Controller;


import Dao.ParkingSessionDAO;
import Dao.TransactionDAO;
import Model.ParkingSession;
import Model.Transaction;
import Utils.JsonUtil;
import Utils.VNPayUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * POST /payment/create   — tạo URL thanh toán VNPay
 * GET  /payment/vnpay-return — VNPay redirect về sau khi customer TT
 * POST /payment/vnpay-ipn    — VNPay gọi IPN (server-to-server xác nhận)
 */
@WebServlet({"/payment/create", "/payment/vnpay-return", "/payment/vnpay-ipn"})
public class PaymentServlet extends HttpServlet {

    private final TransactionDAO transDAO   = new TransactionDAO();
    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    // ── POST /payment/create ─────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getServletPath();

        if ("/payment/create".equals(path)) {
            handleCreate(req, resp);
        } else if ("/payment/vnpay-ipn".equals(path)) {
            handleIpn(req, resp);
        } else {
            resp.setStatus(404);
            resp.getWriter().write("{\"success\":false,\"message\":\"Not found\"}");
        }
    }

    // ── GET /payment/vnpay-return ────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getServletPath();
        if ("/payment/vnpay-return".equals(path)) {
            handleReturn(req, resp);
        } else {
            resp.setStatus(404);
            resp.getWriter().write("{\"success\":false,\"message\":\"Not found\"}");
        }
    }

    // ─────────────────────────────────────────────────────────
    // 1. TẠO LINK VNPAY
    // Body: { "transId": 42, "returnUrl": "http://localhost:5173/payment/result" }
    // ─────────────────────────────────────────────────────────
    private void handleCreate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // AuthFilter đã verify JWT — customer (role 3) hoặc staff (role 2) đều được
        int roleId = (int) req.getAttribute("jwtRoleId");
        if (roleId != 3 && roleId != 2) {
            resp.setStatus(403);
            resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
            return;
        }

        try {
            String body      = JsonUtil.readBody(req);
            int    transId   = JsonUtil.getInt(body, "transId", -1);
            String returnUrl = JsonUtil.getString(body, "returnUrl");

            if (transId < 0 || returnUrl == null || returnUrl.isEmpty()) {
                resp.setStatus(400);
                resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu transId hoặc returnUrl\"}");
                return;
            }

            Transaction t = transDAO.findById(transId);
            if (t == null) {
                resp.setStatus(404);
                resp.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy giao dịch\"}");
                return;
            }
            if ("paid".equals(t.getPaymentStatus())) {
                resp.setStatus(409);
                resp.getWriter().write("{\"success\":false,\"message\":\"Giao dịch đã được thanh toán\"}");
                return;
            }

            // Lấy thông tin session để build orderInfo
            ParkingSession session = sessionDAO.findById(t.getSessionId());
            String plate    = session != null ? session.getPlateNumber() : "xe";
            String orderInfo = "VPT phi gui xe " + plate + " - GD#" + transId;

            String ip        = getClientIp(req);
            String payUrl    = VNPayUtil.buildPaymentUrl(
                transId,
                (long) t.getAmount(),
                orderInfo,
                returnUrl,
                ip
            );

            // Đánh dấu đang chờ VNPay
            transDAO.markPaymentMethod(transId, "vnpay");

            resp.setStatus(200);
            resp.getWriter().write(
                "{\"success\":true," +
                "\"payUrl\":\"" + JsonUtil.escape(payUrl) + "\"," +
                "\"transId\":"  + transId + "," +
                "\"amount\":"   + (long) t.getAmount() + "}"
            );

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    // ─────────────────────────────────────────────────────────
    // 2. VNPAY REDIRECT VỀ SAU KHI CUSTOMER THANH TOÁN
    // VNPay GET ?vnp_ResponseCode=00&vnp_TxnRef=42&vnp_SecureHash=...
    // Frontend nhận JSON, tự redirect đến trang kết quả
    // ─────────────────────────────────────────────────────────
    private void handleReturn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean valid = VNPayUtil.verifyReturn(req.getParameterMap());
        String  code  = VNPayUtil.getResponseCode(req.getParameterMap());
        int     txnRef = VNPayUtil.getTxnRef(req.getParameterMap());

        if (!valid) {
            resp.setStatus(400);
            resp.getWriter().write("{\"success\":false,\"code\":\"INVALID_HASH\",\"message\":\"Chữ ký không hợp lệ\"}");
            return;
        }

        boolean success = "00".equals(code);
        try {
            if (success && txnRef > 0) {
                transDAO.markPaid(txnRef, "vnpay");
            }
            resp.setStatus(200);
            resp.getWriter().write(
                "{\"success\":" + success + "," +
                "\"code\":\"" + JsonUtil.escape(code) + "\"," +
                "\"transId\":" + txnRef + "," +
                "\"message\":\"" + (success ? "Thanh toán thành công" : vnpayMessage(code)) + "\"}"
            );
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    // ─────────────────────────────────────────────────────────
    // 3. IPN — VNPay server-to-server xác nhận (không cần JWT)
    // Trả "RspCode":"00" để báo VNPay đã nhận
    // ─────────────────────────────────────────────────────────
    private void handleIpn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean valid = VNPayUtil.verifyReturn(req.getParameterMap());
        String  code  = VNPayUtil.getResponseCode(req.getParameterMap());
        int     txnRef = VNPayUtil.getTxnRef(req.getParameterMap());

        if (!valid) {
            resp.setStatus(200); // IPN phải luôn trả 200 dù lỗi
            resp.getWriter().write("{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}");
            return;
        }

        try {
            Transaction t = transDAO.findById(txnRef);
            if (t == null) {
                resp.getWriter().write("{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
                return;
            }
            if ("paid".equals(t.getPaymentStatus())) {
                // Đã xử lý rồi (idempotent)
                resp.getWriter().write("{\"RspCode\":\"02\",\"Message\":\"Already confirmed\"}");
                return;
            }
            if ("00".equals(code)) {
                transDAO.markPaid(txnRef, "vnpay");
            }
            resp.getWriter().write("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
        } catch (Exception e) {
            resp.getWriter().write("{\"RspCode\":\"99\",\"Message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    // ── Helpers ─────────────────────────────────────────────
    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String vnpayMessage(String code) {
        return switch (code) {
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ";
            case "09" -> "Thẻ chưa đăng ký Internet Banking";
            case "10" -> "Xác thực thẻ quá 3 lần";
            case "11" -> "Hết hạn chờ thanh toán";
            case "12" -> "Thẻ bị khóa";
            case "13" -> "Sai OTP";
            case "24" -> "Khách hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Vượt hạn mức giao dịch";
            case "75" -> "Ngân hàng đang bảo trì";
            case "79" -> "Sai mật khẩu quá số lần";
            default   -> "Thanh toán thất bại (mã " + code + ")";
        };
    }
}
