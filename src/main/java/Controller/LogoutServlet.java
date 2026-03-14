package Controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        req.getSession().invalidate();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"success\":true,\"message\":\"Đăng xuất thành công\"}");
    }
}