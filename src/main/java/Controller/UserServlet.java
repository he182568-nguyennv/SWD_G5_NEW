package Controller;


import Dao.UserDAO;
import Model.User;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

@WebServlet("/manager/users")
public class UserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    // ── GET /manager/users?role=2&search=nam ─────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isManager(req)) { forbidden(resp); return; }

        try {
            String roleParam   = req.getParameter("role");
            String searchParam = req.getParameter("search");
            Integer roleId     = (roleParam != null && !roleParam.isBlank()) ? Integer.parseInt(roleParam) : null;

            List<User> users = userDAO.findAllFiltered(roleId, searchParam);

            // Count per role for badge
            int cManager  = userDAO.countByRole(1);
            int cStaff    = userDAO.countByRole(2);
            int cCustomer = userDAO.countByRole(3);

            StringBuilder sb = new StringBuilder("{\"success\":true,")
                .append("\"counts\":{\"manager\":").append(cManager)
                .append(",\"staff\":").append(cStaff)
                .append(",\"customer\":").append(cCustomer)
                .append(",\"all\":").append(cManager + cStaff + cCustomer).append("},")
                .append("\"data\":[");

            for (int i = 0; i < users.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(userJson(users.get(i)));
            }
            sb.append("]}");
            resp.setStatus(200);
            resp.getWriter().write(sb.toString());

        } catch (Exception e) {
            error(resp, e.getMessage());
        }
    }

    // ── POST — create user OR toggleActive ───────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        if (!isManager(req)) { forbidden(resp); return; }

        try {
            String body   = JsonUtil.readBody(req);
            String action = JsonUtil.getString(body, "action");

            // Toggle active/inactive
            if ("toggleActive".equals(action)) {
                int uid = JsonUtil.getInt(body, "userId", -1);
                if (uid < 0) { badRequest(resp, "Missing userId"); return; }
                User u = userDAO.findById(uid);
                if (u == null) { resp.setStatus(404); resp.getWriter().write("{\"success\":false,\"message\":\"User not found\"}"); return; }
                u.setActive(!u.isActive());
                userDAO.update(u);
                resp.setStatus(200);
                resp.getWriter().write("{\"success\":true,\"isActive\":" + u.isActive() + ",\"userId\":" + uid + "}");
                return;
            }

            // Create new user
            String username = JsonUtil.getString(body, "username");
            String fullName = JsonUtil.getString(body, "fullName");
            String email    = JsonUtil.getString(body, "email");
            String phone    = JsonUtil.getString(body, "phone");
            String role     = JsonUtil.getString(body, "role");
            String password = JsonUtil.getString(body, "password");

            if (username == null || username.isBlank()) { badRequest(resp, "username bắt buộc"); return; }
            if (fullName == null || fullName.isBlank()) { badRequest(resp, "fullName bắt buộc");  return; }
            if (email    == null || email.isBlank())    { badRequest(resp, "email bắt buộc");     return; }

            // Check username duplicate
            if (userDAO.findByUsername(username) != null) {
                resp.setStatus(409);
                resp.getWriter().write("{\"success\":false,\"message\":\"Username đã tồn tại\"}");
                return;
            }

            int roleId = "manager".equals(role) ? 1 : "staff".equals(role) ? 2 : 3;
            String hash = sha256(password != null && !password.isBlank() ? password : "123456");

            User u = new User();
            u.setUsername(username);  u.setPasswordHash(hash);
            u.setFullName(fullName);  u.setEmail(email);
            u.setPhone(phone != null ? phone : "");
            u.setRoleId(roleId);
            int newId = userDAO.insert(u);
            u.setId(newId); u.setActive(true);

            resp.setStatus(201);
            resp.getWriter().write("{\"success\":true,\"user\":" + userJson(u) + "}");

        } catch (Exception e) {
            error(resp, e.getMessage());
        }
    }

    // ── PUT /manager/users — chỉnh sửa thông tin ────────────
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        if (!isManager(req)) { forbidden(resp); return; }

        try {
            String body   = JsonUtil.readBody(req);
            int    userId = JsonUtil.getInt(body, "userId", -1);
            if (userId < 0) { badRequest(resp, "Missing userId"); return; }

            User u = userDAO.findById(userId);
            if (u == null) { resp.setStatus(404); resp.getWriter().write("{\"success\":false,\"message\":\"User not found\"}"); return; }

            String fullName = JsonUtil.getString(body, "fullName");
            String email    = JsonUtil.getString(body, "email");
            String phone    = JsonUtil.getString(body, "phone");

            if (fullName != null && !fullName.isBlank()) u.setFullName(fullName);
            if (email    != null && !email.isBlank())    u.setEmail(email);
            if (phone    != null)                        u.setPhone(phone);

            userDAO.update(u);
            resp.setStatus(200);
            resp.getWriter().write("{\"success\":true,\"user\":" + userJson(u) + "}");

        } catch (Exception e) {
            error(resp, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────
    private String userJson(User u) {
        String roleStr = u.getRoleId() == 1 ? "manager" : u.getRoleId() == 2 ? "staff" : "customer";
        return "{" +
            "\"userId\":"    + u.getId()                           + "," +
            "\"username\":\"" + JsonUtil.escape(u.getUsername())      + "\"," +
            "\"fullName\":\"" + JsonUtil.escape(u.getFullName())      + "\"," +
            "\"email\":\""    + JsonUtil.escape(u.getEmail())         + "\"," +
            "\"phone\":\""    + JsonUtil.escape(u.getPhone())         + "\"," +
            "\"roleId\":"     + u.getRoleId()                         + "," +
            "\"role\":\""     + roleStr                               + "\"," +
            "\"isActive\":"   + u.isActive()                         + "," +
            "\"createdAt\":\"" + JsonUtil.escape(u.getCreatedAt())    + "\"" +
            "}";
    }

    private boolean isManager(HttpServletRequest req) {
        Object r = req.getAttribute("jwtRoleId");
        return r != null && (int) r == 1;
    }

    private void forbidden(HttpServletResponse r) throws IOException {
        r.setStatus(403); r.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
    }

    private void badRequest(HttpServletResponse r, String msg) throws IOException {
        r.setStatus(400); r.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(msg) + "\"}");
    }

    private void error(HttpServletResponse r, String msg) throws IOException {
        r.setStatus(500); r.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(msg) + "\"}");
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return raw; }
    }
}
