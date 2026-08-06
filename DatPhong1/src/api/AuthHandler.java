package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Account;
import model.Student;
import service.AuthService;
import service.StudentService;
import utils.JsonUtils;

import java.io.IOException;

/**
 * Xử lý các HTTP request xác thực.
 *
 * POST /api/auth/login           - Đăng nhập → trả về studentId, role, thông tin SV
 * POST /api/auth/register        - Đăng ký (role HOCVIEN)
 * POST /api/auth/reset-password  - Reset mật khẩu bằng email
 * POST /api/auth/change-password - Đổi mật khẩu (khi đã đăng nhập)
 */
public class AuthHandler implements HttpHandler {

    private final AuthService    authService;
    private final StudentService studentService;

    public AuthHandler(AuthService authService, StudentService studentService) {
        this.authService    = authService;
        this.studentService = studentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendJson(exchange, 204, "");
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            ApiServer.sendJson(exchange, 405, JsonUtils.error("Method not allowed"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String body = ApiServer.readBody(exchange);

        if      (path.endsWith("/login"))           handleLogin(exchange, body);
        else if (path.endsWith("/register"))        handleRegister(exchange, body);
        else if (path.endsWith("/reset-password"))  handleResetPassword(exchange, body);
        else if (path.endsWith("/change-password")) handleChangePassword(exchange, body);
        else ApiServer.sendJson(exchange, 404, JsonUtils.error("Endpoint không tồn tại"));
    }

    // ==================== Login ====================

    private void handleLogin(HttpExchange exchange, String body) throws IOException {
        try {
            String studentId = extract(body, "studentId");
            String password  = extract(body, "password");

            Account acc = authService.login(studentId, password);

            // Lấy thông tin sinh viên (nếu là HOCVIEN)
            Student sv = null;
            if (acc.isHocVien()) {
                try { sv = studentService.getStudentById(acc.getStudentId()); }
                catch (Exception ignored) {}
            }

            String svJson = sv != null ? JsonUtils.studentToJson(sv) : "null";

            // Trả về: studentId, role, email, displayName, student object
            String displayName = (sv != null) ? sv.getFullName() : acc.getStudentId();
            String resp = "{\"success\":true,\"data\":{"
                + "\"studentId\":\"" + esc(acc.getStudentId()) + "\","
                + "\"role\":\"" + esc(acc.getRole()) + "\","
                + "\"email\":\"" + esc(acc.getEmail()) + "\","
                + "\"displayName\":\"" + esc(displayName) + "\","
                + "\"student\":" + svJson
                + "}}";
            ApiServer.sendJson(exchange, 200, resp);

        } catch (IllegalArgumentException e) {
            ApiServer.sendJson(exchange, 401, JsonUtils.error(e.getMessage()));
        }
    }

    // ==================== Register ====================

    private void handleRegister(HttpExchange exchange, String body) throws IOException {
        try {
            String studentId = extract(body, "studentId");
            String fullName  = extract(body, "fullName");
            String phone     = extract(body, "phone");
            String email     = extract(body, "email");
            String className = extract(body, "className");
            String password  = extract(body, "password");

            if (studentId == null || fullName == null || phone == null
                    || email == null || className == null || password == null) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error(
                    "Thiếu thông tin. Cần: studentId, fullName, phone, email, className, password"));
                return;
            }

            authService.register(studentId, fullName, phone, email, className, password);
            ApiServer.sendJson(exchange, 201, JsonUtils.successMsg(
                "Đăng ký thành công! Chào mừng " + fullName + ". Hãy đăng nhập để tiếp tục."));

        } catch (IllegalArgumentException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    // ==================== Reset Password ====================

    private void handleResetPassword(HttpExchange exchange, String body) throws IOException {
        try {
            String studentId   = extract(body, "studentId");
            String email       = extract(body, "email");
            String newPassword = extract(body, "newPassword");

            if (studentId == null || email == null || newPassword == null) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error(
                    "Thiếu thông tin. Cần: studentId, email, newPassword"));
                return;
            }

            authService.resetPassword(studentId, email, newPassword);
            ApiServer.sendJson(exchange, 200, JsonUtils.successMsg(
                "Đặt lại mật khẩu thành công! Hãy đăng nhập với mật khẩu mới."));

        } catch (IllegalArgumentException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    // ==================== Change Password ====================

    private void handleChangePassword(HttpExchange exchange, String body) throws IOException {
        try {
            String studentId   = extract(body, "studentId");
            String oldPassword = extract(body, "oldPassword");
            String newPassword = extract(body, "newPassword");

            if (studentId == null || oldPassword == null || newPassword == null) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error(
                    "Thiếu thông tin. Cần: studentId, oldPassword, newPassword"));
                return;
            }

            authService.changePassword(studentId, oldPassword, newPassword);
            ApiServer.sendJson(exchange, 200, JsonUtils.successMsg("Đổi mật khẩu thành công!"));

        } catch (IllegalArgumentException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    // ==================== Helpers ====================

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extract(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int next = colon + 1;
        while (next < json.length() && (json.charAt(next) == ' ' || json.charAt(next) == '\t')) next++;
        if (next >= json.length()) return null;
        if (json.charAt(next) == '"') {
            int q2 = json.indexOf('"', next + 1);
            return q2 < 0 ? null : json.substring(next + 1, q2);
        }
        int end = next;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(next, end).trim();
    }
}
