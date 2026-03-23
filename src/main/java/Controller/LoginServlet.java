package Controller;



import Dao.UserDAO;
import Model.User;
import Utils.JsonUtil;
import Utils.JwtUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.MessageDigest;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String username = null;
        String password = null;

        try {
            String contentType = req.getContentType();
            System.out.println("[LOGIN] Content-Type: " + contentType);

            if (contentType != null && contentType.contains("application/json")) {
                String body = JsonUtil.readBody(req);
                System.out.println("[LOGIN] Body: " + body);
                username = JsonUtil.getString(body, "username");
                password = JsonUtil.getString(body, "password");
            } else {
                username = req.getParameter("username");
                password = req.getParameter("password");
            }

            System.out.println("[LOGIN] username=" + username + " password=" + (password != null ? "***" : "null"));

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu username hoặc password\"}");
                return;
            }

            User user = userDAO.findByUsername(username);
            System.out.println("[LOGIN] User found: " + (user != null ? user.getUsername() : "null"));

            if (user != null && user.isActive() && checkPassword(password, user.getPasswordHash())) {
                String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleId());
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(
                    "{\"success\":true," +
                    "\"token\":\"" + token + "\"," +
                    "\"userId\":" + user.getId() + "," +
                    "\"username\":\"" + JsonUtil.escape(user.getUsername()) + "\"," +
                    "\"fullName\":\"" + JsonUtil.escape(user.getFullName()) + "\"," +
                    "\"roleId\":" + user.getRoleId() + "," +
                    "\"role\":\"" + getRoleName(user.getRoleId()) + "\"}"
                );
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"success\":false,\"message\":\"Sai tên đăng nhập hoặc mật khẩu\"}");
            }

        } catch (Exception e) {
            // In toàn bộ stack trace ra console để debug
            System.err.println("[LOGIN] ERROR: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Lỗi server: " + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private boolean checkPassword(String raw, String hash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            String computed = sb.toString();
            System.out.println("[LOGIN] computed hash: " + computed);
            System.out.println("[LOGIN] stored  hash: " + hash);
            return computed.equals(hash);
        } catch (Exception e) { return false; }
    }

    private String getRoleName(int roleId) {
        switch (roleId) {
            case 1: return "manager";
            case 2: return "staff";
            case 3: return "customer";
            default: return "unknown";
        }
    }
}
