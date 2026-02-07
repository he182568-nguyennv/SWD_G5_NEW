package Controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// @WebFilter("/*") nghĩa là áp dụng cho TẤT CẢ các đường dẫn
@WebFilter("/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpServletRequest req = (HttpServletRequest) request;

        // 1. Cấu hình CORS (Dùng chung cho cả dự án)
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:3000"); // Cho phép React
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");

        // 2. Xử lý Preflight Request (Quan trọng cho POST/PUT)
        // Khi React gửi request POST, trình duyệt sẽ gửi 1 request OPTIONS trước để "hỏi đường".
        // Server phải trả về OK ngay lập tức.
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 3. Cho phép request đi tiếp vào Servlet đích
        chain.doFilter(request, response);
    }
}