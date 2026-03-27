package Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GsonUtil {

    /** Gson instance dùng chung — thread-safe, tái sử dụng */
    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    /** Ghi JSON response thành công */
    public static void ok(HttpServletResponse resp, Object data) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("success", true);
        body.add("data", GSON.toJsonTree(data));
        write(resp, 200, body);
    }

    /** Ghi JSON response thành công không có data */
    public static void ok(HttpServletResponse resp) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("success", true);
        write(resp, 200, body);
    }

    /** Ghi JSON response thành công với custom body */
    public static void ok(HttpServletResponse resp, JsonObject body) throws IOException {
        body.addProperty("success", true);
        write(resp, 200, body);
    }

    /** Ghi JSON response lỗi */
    public static void error(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("success", false);
        body.addProperty("message", message);
        write(resp, status, body);
    }

    /** Ghi JSON response tạo mới thành công */
    public static void created(HttpServletResponse resp, JsonObject body) throws IOException {
        body.addProperty("success", true);
        write(resp, 201, body);
    }

    private static void write(HttpServletResponse resp, int status, JsonObject body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(GSON.toJson(body));
    }

    /** Parse body từ request thành JsonObject */
    public static JsonObject parseBody(jakarta.servlet.http.HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader r = req.getReader()) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        String body = sb.toString().trim();
        if (body.isEmpty()) return new JsonObject();
        try {
            return GSON.fromJson(body, JsonObject.class);
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    /** Lấy string từ JsonObject an toàn */
    public static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    /** Lấy int từ JsonObject an toàn */
    public static int getInt(JsonObject obj, String key, int def) {
        try {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
            return obj.get(key).getAsInt();
        } catch (Exception e) { return def; }
    }
}
