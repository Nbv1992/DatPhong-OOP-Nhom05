package api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import repository.AccountRepository;
import repository.BookingRepository;
import repository.RoomRepository;
import repository.StudentRepository;
import service.AuthService;
import service.BookingService;
import service.RoomService;
import service.StudentService;
import utils.JsonUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP Server dùng com.sun.net.httpserver (có sẵn trong JDK).
 * Serve static files (HTML/CSS/JS) từ thư mục web/ — tìm tự động.
 * Không cần cài thêm thư viện.
 */
public class ApiServer {

    private static final int PORT = 8080;
    private final HttpServer server;

    /** Thư mục web/ — được resolve 1 lần khi server start */
    private final Path webDir;

    public ApiServer(RoomRepository roomRepo,
                     StudentRepository studentRepo,
                     BookingRepository bookingRepo,
                     AccountRepository accountRepo) throws IOException {

        // Tìm thư mục web/ trước khi làm gì khác
        this.webDir = findWebDir();
        System.out.println("[Server] webDir = " + webDir.toAbsolutePath());

        RoomService    roomService    = new RoomService(roomRepo);
        StudentService studentService = new StudentService(studentRepo);
        BookingService bookingService = new BookingService(bookingRepo, roomService, studentService);
        AuthService    authService    = new AuthService(accountRepo, studentRepo);

        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // ---- API endpoints (đăng ký TRƯỚC / để tránh conflict) ----
        server.createContext("/api/auth",     new AuthHandler(authService, studentService));
        server.createContext("/api/rooms",    new RoomHandler(roomService));
        server.createContext("/api/students", new StudentHandler(studentService));
        server.createContext("/api/bookings", new BookingHandler(bookingService));

        // ---- Static files ----
        server.createContext("/", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 204, ""); return;
            }
            String reqPath = exchange.getRequestURI().getPath();
            // Root → login page
            if (reqPath.equals("/") || reqPath.isEmpty()) reqPath = "/login.html";
            serveStaticFile(exchange, reqPath);
        });

        server.setExecutor(null);
    }

    // ========================= Web Dir Resolver =========================

    /**
     * Tìm thư mục web/ theo thứ tự:
     * 1. [JAR location]/web/
     * 2. [JAR location]/../web/   ← IntelliJ: classes ở out/, web/ ở DatPhong/
     * 3. [working directory]/web/
     * 4. [working directory]/../web/
     * 5. Các ổ đĩa phổ biến: C:\, D:\, ...
     */
    private static Path findWebDir() {
        List<Path> candidates = new java.util.ArrayList<>();

        // Từ vị trí class/JAR
        try {
            Path codeBase = Paths.get(
                ApiServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            // Nếu là file .class thì getParent() = thư mục chứa package
            // IntelliJ: .../DatPhong/out/api/ApiServer.class → codeBase = .../DatPhong/out/
            Path base = Files.isDirectory(codeBase) ? codeBase : codeBase.getParent();
            candidates.add(base.resolve("web"));                // out/web
            candidates.add(base.getParent().resolve("web"));    // DatPhong/web  ← đây là đúng với IntelliJ
            if (base.getParent() != null && base.getParent().getParent() != null)
                candidates.add(base.getParent().getParent().resolve("web")); // thêm 1 cấp nữa
        } catch (URISyntaxException e) {
            System.err.println("[Server] URISyntaxException: " + e.getMessage());
        }

        // Từ working directory
        Path cwd = Paths.get("").toAbsolutePath();
        candidates.add(cwd.resolve("web"));
        candidates.add(cwd.resolve("DatPhong").resolve("web"));
        if (cwd.getParent() != null) candidates.add(cwd.getParent().resolve("web"));

        // Tìm file trên toàn bộ ổ đĩa (fallback cuối)
        for (String drive : Arrays.asList("C:\\", "D:\\", "E:\\")) {
            try {
                // Tìm thư mục DatPhong/web trên ổ đĩa
                Path p = Paths.get(drive + "DatPhong\\web");
                candidates.add(p);
            } catch (Exception ignored) {}
        }

        for (Path c : candidates) {
            try {
                if (c != null && Files.isDirectory(c) && Files.exists(c.resolve("index.html"))) {
                    System.out.println("[Server] Found web/ at: " + c.toAbsolutePath());
                    return c.toAbsolutePath();
                }
            } catch (Exception ignored) {}
        }

        // Cuối cùng: dùng relative path và hy vọng working dir đúng
        System.err.println("[Server] WARNING: Cannot find web/ directory. Using relative path.");
        System.err.println("[Server] Tried: " + candidates.stream()
            .map(p -> p == null ? "null" : p.toAbsolutePath().toString())
            .reduce("", (a, b) -> a + "\n  " + b));
        return Paths.get("web").toAbsolutePath();
    }

    // ========================= Static File Server =========================

    private void serveStaticFile(HttpExchange exchange, String urlPath) throws IOException {
        // Normalize path — loại bỏ / ở đầu
        String rel = urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
        if (rel.isEmpty()) rel = "login.html";

        // Bảo mật: không cho phép path traversal
        if (rel.contains("..")) {
            sendResponse(exchange, 400, "text/plain", "400 Bad Request");
            return;
        }

        Path filePath = webDir.resolve(rel).normalize();

        // Kiểm tra file có trong webDir không (bảo mật)
        if (!filePath.startsWith(webDir)) {
            sendResponse(exchange, 403, "text/plain", "403 Forbidden");
            return;
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendResponse(exchange, 404, "text/plain",
                "404 Not Found: " + urlPath + "\n[Server] Looking in: " + webDir.toAbsolutePath());
            return;
        }

        byte[] bytes = Files.readAllBytes(filePath);
        String contentType = getContentType(rel);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "text/plain";
    }

    // ========================= Server Control =========================

    public void start() {
        server.start();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Server đang chạy tại: http://localhost:" + PORT + "          ║");
        System.out.println("║  Mở trình duyệt và truy cập địa chỉ trên để dùng.  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }

    public void stop() { server.stop(0); }

    // ========================= Utilities =========================

    public static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if (code == 204) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private static void sendResponse(HttpExchange exchange, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String getParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try { return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8); }
                catch (Exception e) { return kv[1]; }
            }
        }
        return null;
    }

    public static String getPathSegment(String path, int index) {
        String[] parts = path.split("/");
        return index < parts.length ? parts[index] : null;
    }
}
