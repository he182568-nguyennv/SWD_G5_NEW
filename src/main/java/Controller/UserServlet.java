package Controller;
import Model.User;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/user")
    public class UserServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Cấu hình kiểu trả về (Vẫn nên giữ lại ở đây hoặc tạo BaseServlet)
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            // 2. Logic nghiệp vụ (Chỉ tập trung vào việc tạo data)
            User user = new User("HE182568", "Nguyen van Nguyen",1,22,
                    "Yen Noi Dong Quang Quoc Oai Ha Noi","nguyena7k580","30042004aA");
            String json = new Gson().toJson(user);

            // 3. Trả về
            resp.getWriter().write(json);
        }
    }

