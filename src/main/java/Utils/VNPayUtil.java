package Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VNPay sandbox integration utility.
 *
 * Sandbox credentials (thay bằng live key khi deploy production):
 *   TMN_CODE  = DEMO1234        (từ merchant.vnpay.vn)
 *   HASH_SECRET = vptparkingsecretkey2026
 *
 * Docs: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
 */
public class VNPayUtil {

    // ── Sandbox config ──────────────────────────────────────────
    public static final String TMN_CODE    = System.getProperty("vnpay.tmnCode",   "DEMO1234");
    public static final String HASH_SECRET = System.getProperty("vnpay.hashSecret","vptparkingsecretkey2026");
    public static final String PAY_URL     = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String API_VERSION = "2.1.0";
    public static final String CURRENCY    = "VND";
    public static final String LOCALE      = "vn";
    public static final String ORDER_TYPE  = "other"; // parking

    /**
     * Tạo URL redirect đến VNPay cho customer thanh toán.
     *
     * @param txnRef      Mã đơn hàng duy nhất (transId từ DB)
     * @param amount      Số tiền VND (nhân 100 theo spec VNPay)
     * @param orderInfo   Mô tả đơn hàng (vd: "Phi gui xe 30A-12345")
     * @param returnUrl   URL callback sau khi customer thanh toán xong
     * @param ipAddr      IP của customer
     */
    public static String buildPaymentUrl(
            int txnRef, long amount, String orderInfo,
            String returnUrl, String ipAddr) throws Exception {

        String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String expireDate = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // 15 phút

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version",    API_VERSION);
        params.put("vnp_Command",    "pay");
        params.put("vnp_TmnCode",    TMN_CODE);
        params.put("vnp_Amount",     String.valueOf(amount * 100L));
        params.put("vnp_CurrCode",   CURRENCY);
        params.put("vnp_TxnRef",     String.valueOf(txnRef));
        params.put("vnp_OrderInfo",  orderInfo);
        params.put("vnp_OrderType",  ORDER_TYPE);
        params.put("vnp_Locale",     LOCALE);
        params.put("vnp_ReturnUrl",  returnUrl);
        params.put("vnp_IpAddr",     ipAddr);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // Build query string (sorted, URL-encoded)
        StringBuilder query    = new StringBuilder();
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String encodedKey = URLEncoder.encode(e.getKey(),   StandardCharsets.US_ASCII);
            String encodedVal = URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII);
            query.append("&").append(encodedKey).append("=").append(encodedVal);
            hashData.append("&").append(e.getKey()).append("=").append(e.getValue());
        }

        String rawQuery  = query.substring(1);
        String rawHash   = hashData.substring(1);
        String secureHash = hmacSHA512(HASH_SECRET, rawHash);

        return PAY_URL + "?" + rawQuery + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xác minh chữ ký VNPay trả về (dùng cho returnUrl và IPN).
     * Trả về true nếu hợp lệ.
     */
    public static boolean verifyReturn(Map<String, String[]> params) {
        String receivedHash = getSingle(params, "vnp_SecureHash");
        if (receivedHash == null) return false;

        // Lọc bỏ vnp_SecureHash và vnp_SecureHashType
        Map<String, String> data = new TreeMap<>();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String k = e.getKey();
            if (!"vnp_SecureHash".equals(k) && !"vnp_SecureHashType".equals(k)) {
                data.put(k, e.getValue()[0]);
            }
        }

        StringBuilder rawHash = new StringBuilder();
        for (Map.Entry<String, String> e : data.entrySet()) {
            rawHash.append("&").append(e.getKey()).append("=").append(e.getValue());
        }

        try {
            String expected = hmacSHA512(HASH_SECRET, rawHash.substring(1));
            return expected.equalsIgnoreCase(receivedHash);
        } catch (Exception ex) {
            return false;
        }
    }

    /** Lấy response code VNPay: "00" = thành công */
    public static String getResponseCode(Map<String, String[]> params) {
        return getSingle(params, "vnp_ResponseCode");
    }

    /** Lấy txnRef (= transId trong DB) từ VNPay callback */
    public static int getTxnRef(Map<String, String[]> params) {
        String v = getSingle(params, "vnp_TxnRef");
        return v != null ? Integer.parseInt(v) : -1;
    }

    // ── Internal ────────────────────────────────────────────────
    private static String getSingle(Map<String, String[]> params, String key) {
        String[] v = params.get(key);
        return (v != null && v.length > 0) ? v[0] : null;
    }

    public static String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
