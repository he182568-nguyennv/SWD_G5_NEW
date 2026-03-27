package Controller;

import Dao.UserDAO;
import Model.User;
import Utils.GsonUtil;
import Utils.JwtUtil;
import com.google.gson.JsonObject;
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

        try {
            String username, password;
            String contentType = req.getContentType();
            System.out.println("[LOGIN] Content-Type: " + contentType);

            if (contentType != null && contentType.contains("application/json")) {
                JsonObject body = GsonUtil.parseBody(req);
                username = GsonUtil.getString(body, "username");
                password = GsonUtil.getString(body, "password");
            } else {
                username = req.getParameter("username");
                password = req.getParameter("password");
            }

            System.out.println("[LOGIN] username=" + username + " password=" + (password != null ? "***" : "null"));

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                GsonUtil.error(resp, 400, "Thiếu username hoặc password");
                return;
            }

            User user = userDAO.findByUsername(username);
            System.out.println("[LOGIN] User found: " + (user != null ? user.getUsername() : "null"));

            if (user != null && user.isActive() && checkPassword(password, user.getPasswordHash())) {
                String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleId());

                JsonObject res = new JsonObject();
                res.addProperty("token",    token);
                res.addProperty("userId",   user.getId());
                res.addProperty("username", user.getUsername());
                res.addProperty("fullName", user.getFullName());
                res.addProperty("roleId",   user.getRoleId());
                res.addProperty("role",     getRoleName(user.getRoleId()));
                GsonUtil.ok(resp, res);
            } else {
                GsonUtil.error(resp, 401, "Sai tên đăng nhập hoặc mật khẩu");
            }

        } catch (Exception e) {
            System.err.println("[LOGIN] ERROR: " + e.getMessage());
            e.printStackTrace();
            GsonUtil.error(resp, 500, "Lỗi server: " + e.getMessage());
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
        return switch (roleId) {
            case 1 -> "manager";
            case 2 -> "staff";
            case 3 -> "customer";
            default -> "unknown";
        };
    }
}
