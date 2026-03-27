package Controller;

import Dao.UserDAO;
import Model.User;
import Utils.GsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

@WebServlet("/manager/users")
public class UserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    private JsonObject userToJson(User u) {
        String roleStr = u.getRoleId() == 1 ? "manager" : u.getRoleId() == 2 ? "staff" : "customer";
        JsonObject o = new JsonObject();
        o.addProperty("userId",    u.getId());
        o.addProperty("username",  u.getUsername());
        o.addProperty("fullName",  u.getFullName());
        o.addProperty("email",     u.getEmail());
        o.addProperty("phone",     u.getPhone());
        o.addProperty("roleId",    u.getRoleId());
        o.addProperty("role",      roleStr);
        o.addProperty("isActive",  u.isActive());
        o.addProperty("createdAt", u.getCreatedAt());
        return o;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)    { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1) { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            String roleParam   = req.getParameter("role");
            String searchParam = req.getParameter("search");
            Integer roleId     = (roleParam != null && !roleParam.isBlank())
                ? Integer.parseInt(roleParam) : null;

            List<User> users  = userDAO.findAllFiltered(roleId, searchParam);
            int cManager      = userDAO.countByRole(1);
            int cStaff        = userDAO.countByRole(2);
            int cCustomer     = userDAO.countByRole(3);

            JsonObject counts = new JsonObject();
            counts.addProperty("manager",  cManager);
            counts.addProperty("staff",    cStaff);
            counts.addProperty("customer", cCustomer);
            counts.addProperty("all",      cManager + cStaff + cCustomer);

            JsonArray arr = new JsonArray();
            for (User u : users) arr.add(userToJson(u));

            JsonObject res = new JsonObject();
            res.add("counts", counts);
            res.add("data",   arr);
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

            if ("toggleActive".equals(action)) {
                int uid = GsonUtil.getInt(body, "userId", -1);
                if (uid < 0) { GsonUtil.error(resp, 400, "Missing userId"); return; }
                User u = userDAO.findById(uid);
                if (u == null) { GsonUtil.error(resp, 404, "User not found"); return; }
                u.setActive(!u.isActive());
                userDAO.update(u);
                JsonObject res = new JsonObject();
                res.addProperty("isActive", u.isActive());
                res.addProperty("userId",   uid);
                GsonUtil.ok(resp, res);
                return;
            }

            String username = GsonUtil.getString(body, "username");
            String fullName = GsonUtil.getString(body, "fullName");
            String email    = GsonUtil.getString(body, "email");
            String phone    = GsonUtil.getString(body, "phone");
            String role     = GsonUtil.getString(body, "role");
            String password = GsonUtil.getString(body, "password");

            if (username == null || username.isBlank()) { GsonUtil.error(resp, 400, "username bắt buộc"); return; }
            if (fullName == null || fullName.isBlank()) { GsonUtil.error(resp, 400, "fullName bắt buộc");  return; }
            if (email    == null || email.isBlank())    { GsonUtil.error(resp, 400, "email bắt buộc");     return; }

            if (userDAO.findByUsername(username) != null) {
                GsonUtil.error(resp, 409, "Username đã tồn tại"); return;
            }

            int roleId = "manager".equals(role) ? 1 : "staff".equals(role) ? 2 : 3;
            String hash = sha256(password != null && !password.isBlank() ? password : "123456");

            User u = new User();
            u.setUsername(username); u.setPasswordHash(hash);
            u.setFullName(fullName); u.setEmail(email);
            u.setPhone(phone != null ? phone : "");
            u.setRoleId(roleId);
            int newId = userDAO.insert(u);
            u.setId(newId); u.setActive(true);

            JsonObject res = new JsonObject();
            res.add("user", userToJson(u));
            GsonUtil.created(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Object rid = req.getAttribute("jwtRoleId");
        if (rid == null)    { GsonUtil.error(resp, 401, "Unauthorized"); return; }
        if ((int) rid != 1) { GsonUtil.error(resp, 403, "Forbidden");    return; }

        try {
            JsonObject body = GsonUtil.parseBody(req);
            int userId      = GsonUtil.getInt(body, "userId", -1);
            if (userId < 0) { GsonUtil.error(resp, 400, "Missing userId"); return; }

            User u = userDAO.findById(userId);
            if (u == null) { GsonUtil.error(resp, 404, "User not found"); return; }

            String fullName = GsonUtil.getString(body, "fullName");
            String email    = GsonUtil.getString(body, "email");
            String phone    = GsonUtil.getString(body, "phone");

            if (fullName != null && !fullName.isBlank()) u.setFullName(fullName);
            if (email    != null && !email.isBlank())    u.setEmail(email);
            if (phone    != null)                        u.setPhone(phone);

            userDAO.update(u);
            JsonObject res = new JsonObject();
            res.add("user", userToJson(u));
            GsonUtil.ok(resp, res);

        } catch (Exception e) {
            GsonUtil.error(resp, 500, e.getMessage());
        }
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
