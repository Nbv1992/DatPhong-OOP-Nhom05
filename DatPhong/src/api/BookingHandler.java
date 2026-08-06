package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.BookingException;
import model.Booking;
import model.BookingDetail;
import service.BookingService;
import utils.JsonUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Xử lý các HTTP request liên quan đến đặt phòng / hủy phòng.
 *
 * GET    /api/bookings                     - Tất cả lịch đặt
 * GET    /api/bookings?studentId=xxx       - Lịch đặt của sinh viên
 * GET    /api/bookings?roomId=xxx          - Lịch đặt của phòng (active)
 * GET    /api/bookings/{id}/detail         - Chi tiết một lịch đặt
 * POST   /api/bookings                     - Tạo lịch đặt mới
 * DELETE /api/bookings/{id}?studentId=xxx  - Hủy lịch đặt
 */
public class BookingHandler implements HttpHandler {

    private final BookingService bookingService;

    public BookingHandler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendJson(exchange, 204, "");
            return;
        }

        String method  = exchange.getRequestMethod();
        String path    = exchange.getRequestURI().getPath();
        String query   = exchange.getRequestURI().getQuery();
        String[] segs  = path.split("/");

        // GET /api/bookings/{id}/detail
        if ("GET".equals(method) && path.endsWith("/detail") && segs.length >= 5) {
            handleGetDetail(exchange, segs[3]);
            return;
        }

        // DELETE /api/bookings/{id}
        if ("DELETE".equals(method) && segs.length >= 4) {
            handleCancel(exchange, segs[3], query);
            return;
        }

        if ("GET".equals(method)) {
            handleGetBookings(exchange, query);
        } else if ("POST".equals(method)) {
            handleCreate(exchange);
        } else {
            ApiServer.sendJson(exchange, 405, JsonUtils.error("Method not allowed"));
        }
    }

    private void handleGetBookings(HttpExchange exchange, String query) throws IOException {
        try {
            String studentId = ApiServer.getParam(query, "studentId");
            String roomId    = ApiServer.getParam(query, "roomId");

            List<Booking> bookings;
            if (studentId != null && !studentId.isBlank()) {
                bookings = bookingService.getBookingsByStudent(studentId);
            } else if (roomId != null && !roomId.isBlank()) {
                bookings = bookingService.getActiveBookingsByRoom(roomId);
            } else {
                bookings = bookingService.getAllBookings();
            }
            ApiServer.sendJson(exchange, 200, JsonUtils.success(JsonUtils.bookingListToJson(bookings)));
        } catch (Exception e) {
            ApiServer.sendJson(exchange, 500, JsonUtils.error(e.getMessage()));
        }
    }

    private void handleGetDetail(HttpExchange exchange, String bookingId) throws IOException {
        try {
            BookingDetail detail = bookingService.getBookingDetail(bookingId);
            ApiServer.sendJson(exchange, 200, JsonUtils.success(JsonUtils.bookingDetailToJson(detail)));
        } catch (BookingException e) {
            ApiServer.sendJson(exchange, 404, JsonUtils.error(e.getMessage()));
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        try {
            String body      = ApiServer.readBody(exchange);
            String studentId = extractJsonString(body, "studentId");
            String roomId    = extractJsonString(body, "roomId");
            String startStr  = extractJsonString(body, "startTime");
            String endStr    = extractJsonString(body, "endTime");
            String numStr    = extractJsonString(body, "numberOfPeople");
            String purpose   = extractJsonString(body, "purpose");

            if (studentId == null || roomId == null || startStr == null
                    || endStr == null || numStr == null || purpose == null) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error(
                    "Thiếu thông tin. Cần: studentId, roomId, startTime, endTime, numberOfPeople, purpose"));
                return;
            }

            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end   = LocalDateTime.parse(endStr);
            int numPeople = Integer.parseInt(numStr.trim());

            Booking booking = bookingService.createBooking(
                studentId, roomId, start, end, numPeople, purpose);
            ApiServer.sendJson(exchange, 201, JsonUtils.success(JsonUtils.bookingToJson(booking)));

        } catch (DateTimeParseException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(
                "Định dạng thời gian không đúng. Dùng ISO format: yyyy-MM-ddTHH:mm:ss"));
        } catch (NumberFormatException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error("Số người phải là số nguyên hợp lệ."));
        } catch (BookingException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    private void handleCancel(HttpExchange exchange, String bookingId, String query) throws IOException {
        try {
            String studentId = ApiServer.getParam(query, "studentId");
            if (studentId == null || studentId.isBlank()) {
                // Thử đọc từ body
                String body = ApiServer.readBody(exchange);
                studentId = extractJsonString(body, "studentId");
            }
            if (studentId == null || studentId.isBlank()) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error("Thiếu studentId để xác thực hủy phòng."));
                return;
            }
            bookingService.cancelBooking(bookingId, studentId);
            ApiServer.sendJson(exchange, 200, JsonUtils.successMsg("Hủy lịch đặt " + bookingId + " thành công."));
        } catch (BookingException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        // Có thể là string hoặc number
        int q1 = json.indexOf('"', colon + 1);
        // Tìm value không phải string (number)
        int next = colon + 1;
        while (next < json.length() && (json.charAt(next) == ' ' || json.charAt(next) == '\t')) next++;
        if (next < json.length() && json.charAt(next) != '"') {
            // Đọc đến dấu , hoặc }
            int end = next;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(next, end).trim();
        }
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }
}
