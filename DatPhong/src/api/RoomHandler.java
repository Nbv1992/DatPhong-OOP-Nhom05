package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.BookingException;
import model.*;
import service.RoomService;
import utils.JsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * Xử lý HTTP request liên quan đến phòng.
 *
 * GET  /api/rooms              - Tất cả phòng (query: active=true, type=xxx)
 * GET  /api/rooms/{id}         - Phòng theo mã
 * POST /api/rooms              - Thêm phòng mới (admin)
 * PUT  /api/rooms/{id}/status  - Cập nhật trạng thái
 */
public class RoomHandler implements HttpHandler {

    private final RoomService roomService;

    public RoomHandler(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendJson(exchange, 204, ""); return;
        }

        String method   = exchange.getRequestMethod();
        String path     = exchange.getRequestURI().getPath();
        String query    = exchange.getRequestURI().getQuery();
        String[] segs   = path.split("/");

        if ("PUT".equals(method) && path.contains("/status")) {
            handleUpdateStatus(exchange, segs); return;
        }
        if ("POST".equals(method)) {
            handleCreate(exchange); return;
        }
        if ("GET".equals(method) && segs.length >= 4 && !segs[3].isEmpty()) {
            handleGetById(exchange, segs[3]); return;
        }
        if ("GET".equals(method)) {
            handleGetRooms(exchange, query);
        }
    }

    // GET /api/rooms hoặc /api/rooms?active=true&type=xxx
    private void handleGetRooms(HttpExchange exchange, String query) throws IOException {
        try {
            String active = ApiServer.getParam(query, "active");
            String type   = ApiServer.getParam(query, "type");
            List<Room> rooms;
            if ("true".equals(active))              rooms = roomService.getActiveRooms();
            else if (type != null && !type.isBlank()) rooms = roomService.getRoomsByType(type);
            else                                    rooms = roomService.getAllRooms();
            ApiServer.sendJson(exchange, 200, JsonUtils.success(JsonUtils.roomListToJson(rooms)));
        } catch (Exception e) {
            ApiServer.sendJson(exchange, 500, JsonUtils.error(e.getMessage()));
        }
    }

    // GET /api/rooms/{id}
    private void handleGetById(HttpExchange exchange, String roomId) throws IOException {
        try {
            Room room = roomService.getRoomById(roomId);
            ApiServer.sendJson(exchange, 200, JsonUtils.success(JsonUtils.roomToJson(room)));
        } catch (BookingException e) {
            ApiServer.sendJson(exchange, 404, JsonUtils.error(e.getMessage()));
        }
    }

    // POST /api/rooms — thêm phòng mới
    private void handleCreate(HttpExchange exchange) throws IOException {
        try {
            String body     = ApiServer.readBody(exchange);
            String roomId   = extract(body, "roomId");
            String roomName = extract(body, "roomName");
            String floorStr = extract(body, "floor");
            String capStr   = extract(body, "maxCapacity");
            String roomType = extract(body, "roomType");

            if (roomId == null || roomName == null || floorStr == null
                    || capStr == null || roomType == null) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error(
                    "Thiếu thông tin. Cần: roomId, roomName, floor, maxCapacity, roomType")); return;
            }

            int floor    = Integer.parseInt(floorStr.trim());
            int capacity = Integer.parseInt(capStr.trim());

            Room room = switch (roomType.trim()) {
                case "Phòng có máy chiếu" -> new ProjectorRoom(roomId, roomName, floor, capacity);
                case "Phòng họp seminar"  -> new SeminarRoom(roomId, roomName, floor, capacity);
                default                   -> new NormalRoom(roomId, roomName, floor, capacity);
            };

            roomService.addRoom(room);
            ApiServer.sendJson(exchange, 201, JsonUtils.success(JsonUtils.roomToJson(room)));

        } catch (NumberFormatException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error("Tầng và sức chứa phải là số nguyên."));
        } catch (IllegalArgumentException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
    }

    // PUT /api/rooms/{id}/status
    private void handleUpdateStatus(HttpExchange exchange, String[] segs) throws IOException {
        try {
            String roomId = segs.length >= 4 ? segs[3] : "";
            String body   = ApiServer.readBody(exchange);
            String status = extract(body, "status");
            if (status == null || status.isBlank()) {
                ApiServer.sendJson(exchange, 400, JsonUtils.error("Thiếu trường 'status'")); return;
            }
            roomService.updateRoomStatus(roomId, status);
            ApiServer.sendJson(exchange, 200, JsonUtils.successMsg("Cập nhật trạng thái phòng " + roomId + " thành công."));
        } catch (BookingException e) {
            ApiServer.sendJson(exchange, 400, JsonUtils.error(e.getMessage()));
        }
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
