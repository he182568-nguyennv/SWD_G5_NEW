package Filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * CorsFilter — cho phép frontend (localhost:5173) gọi API.
 * Phải chạy TRƯỚC AuthFilter — đặt @WebFilter order thấp hơn.
 * Với @WebFilter không hỗ trợ order trực tiếp, đặt tên class bắt đầu bằng
 * "A" hoặc dùng web.xml để đảm bảo CorsFilter chạy trước AuthFilter.
 */
@WebFilter("/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Cho phép frontend gọi API
        String origin = request.getHeader("Origin");
        if (origin != null && (
                origin.startsWith("http://localhost:5173") ||
                origin.startsWith("http://localhost:3000") || origin.startsWith("https://2ppgj8xt-5173.asse.devtunnels.ms") ||
                origin.startsWith("http://127.0.0.1"))) {
            response.setHeader("Access-Control-Allow-Origin",      origin);
        } else {
            response.setHeader("Access-Control-Allow-Origin",      "http://localhost:5173");
        }
        response.setHeader("Access-Control-Allow-Credentials",  "true");
        response.setHeader("Access-Control-Allow-Methods",      "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers",
            "Content-Type, Authorization, X-Requested-With");
        response.setHeader("Access-Control-Max-Age",            "3600");

        // Trả lời ngay với OPTIONS — không cần đi tiếp
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override public void init(FilterConfig cfg) {}
    @Override public void destroy() {}
}
