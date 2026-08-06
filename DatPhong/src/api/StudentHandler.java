package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.BookingException;
import model.Student;
import service.StudentService;
import utils.JsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * Xử lý HTTP request liên quan đến sinh viên.
 *
 * GET  /api/students        - Danh sách tất cả SV
 * GET  /api/students/{id}   - Lấy SV theo mã
 * POST /api/students        - Thêm SV mới (admin)
 * PUT  /api/students/{id}   - Cập nhật thông tin SV (chính SV đó hoặc admin)
 */
public class StudentHandler implements HttpHandler {

    private final StudentService studentService;

    public StudentHandler(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendJson(exchange, 204, ""); return;
        }

        String method   = exchange.getRequestMethod();
        String path     = exchange.getRequestURI().getPath();
        String[] segs   = path.split("/");

        if ("GET".equals(method) && segs.length >= 4 && !segs[3].isEmpty()) {
            handleGetById(exchange, segs[3]);
        } else if ("GET".equals(method)) {
            handleGetAll(exchange);
        } else if ("POST".equals(method)) {
            handleCreate(exchange);
        } else if ("PUT".equals(method) && segs.length >= 4) {
            handleUpdate(exchange, segs[3]);
        } else {
            ApiServer.sendJson(exchange, 405, JsonUtils.error("Method not allowed"));
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try {
            List<Student> list = studentService.getAllStudents();
            ApiServer.sendJson(exchange, 200, JsonUtils.success(JsonUtils.studentListToJson(list)));
        } catch (Exception e) {
            ApiServer.sendJson(exchange, 500, JsonUtils.error(e.getMessage()));
        }
    }

    private void handleGetById(HttpExchange exchange, String studentId) throws IOException {
        try {
            Student s = studentService.getStudentById(studentId);
            ApiServer.sendJson(exchange, 200, JsonUtils.success(JsonUtils.studentToJson(s)));
        } catch (BookingException e) {
            ApiServer.sendJson(exchange, 404, JsonUtils.error(e.getMessage()));
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        try {
            String body  = ApiServer.readBody(exchange);
            Student s    = parseStudent(body, null);
            if (s == null) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error("Thiếu thông tin. Cần: studentId, fullName, phone, email, className")); return;
            }
            studentService.addStudent(s);
            ApiServer.sendJson(exchange, 201, JsonUtils.success(JsonUtils.studentToJson(s)));
        } catch (IllegalArgumentException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    // PUT /api/students/{id} — cập nhật thông tin (fullName, phone, email, className)
    private void handleUpdate(HttpExchange exchange, String studentId) throws IOException {
        try {
            String body  = ApiServer.readBody(exchange);
            Student s    = parseStudent(body, studentId);
            if (s == null) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error("Thiếu thông tin. Cần: fullName, phone, email, className")); return;
            }
            studentService.updateStudent(s);
            ApiServer.sendJson(exchange, 200, JsonUtils.success(JsonUtils.studentToJson(s)));
        } catch (BookingException e) {
            ApiServer.sendJson(exchange, 404, JsonUtils.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    /** Parse Student từ JSON body. forcedId: nếu không null thì dùng id này thay vì từ body */
    private static Student parseStudent(String body, String forcedId) {
        String id    = forcedId != null ? forcedId : extract(body, "studentId");
        String name  = extract(body, "fullName");
        String phone = extract(body, "phone");
        String email = extract(body, "email");
        String clazz = extract(body, "className");
        if (id == null || name == null || phone == null || email == null || clazz == null) return null;
        return new Student(id, name, phone, email, clazz);
    }

    private static String extract(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int next = colon + 1;
        while (next < json.length() && Character.isWhitespace(json.charAt(next))) next++;
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
