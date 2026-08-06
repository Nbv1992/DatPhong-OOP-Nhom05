package utils;

import model.*;
import java.util.List;

/**
 * Tiện ích chuyển đổi các đối tượng model sang chuỗi JSON.
 * Không dùng thư viện bên ngoài — tự build JSON string thủ công.
 */
public class JsonUtils {

    // ==================== String helpers ====================

    /** Escape ký tự đặc biệt trong chuỗi JSON */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /** Tạo field JSON: "key": "value" */
    private static String field(String key, String value) {
        return "\"" + key + "\": \"" + esc(value) + "\"";
    }

    /** Tạo field JSON: "key": number */
    private static String fieldNum(String key, Object value) {
        return "\"" + key + "\": " + value;
    }

    // ==================== Room ====================

    public static String roomToJson(Room r) {
        return "{"
            + field("roomId", r.getRoomId()) + ","
            + field("roomName", r.getRoomName()) + ","
            + fieldNum("floor", r.getFloor()) + ","
            + fieldNum("maxCapacity", r.getMaxCapacity()) + ","
            + field("roomType", r.getRoomType()) + ","
            + field("status", r.getStatus()) + ","
            + field("feeDescription", r.getFeePolicy().getFeeDescription()) + ","
            + fieldNum("feePerHour", r.getFeePolicy().calculateFee(1))
            + "}";
    }

    public static String roomListToJson(List<Room> rooms) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rooms.size(); i++) {
            sb.append(roomToJson(rooms.get(i)));
            if (i < rooms.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ==================== Student ====================

    public static String studentToJson(Student s) {
        return "{"
            + field("studentId", s.getStudentId()) + ","
            + field("fullName", s.getFullName()) + ","
            + field("phone", s.getPhone()) + ","
            + field("email", s.getEmail()) + ","
            + field("className", s.getClassName())
            + "}";
    }

    public static String studentListToJson(List<Student> students) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < students.size(); i++) {
            sb.append(studentToJson(students.get(i)));
            if (i < students.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ==================== Booking ====================

    public static String bookingToJson(Booking b) {
        return "{"
            + field("bookingId", b.getBookingId()) + ","
            + field("studentId", b.getStudentId()) + ","
            + field("roomId", b.getRoomId()) + ","
            + field("startTime", b.getTimeSlot().getStartTime().toString()) + ","
            + field("endTime", b.getTimeSlot().getEndTime().toString()) + ","
            + fieldNum("numberOfPeople", b.getNumberOfPeople()) + ","
            + field("purpose", b.getPurpose()) + ","
            + field("status", b.getStatus()) + ","
            + fieldNum("fee", b.getFee())
            + "}";
    }

    public static String bookingListToJson(List<Booking> bookings) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bookings.size(); i++) {
            sb.append(bookingToJson(bookings.get(i)));
            if (i < bookings.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ==================== BookingDetail ====================

    public static String bookingDetailToJson(BookingDetail d) {
        Booking b = d.getBooking();
        Student s = d.getStudent();
        Room    r = d.getRoom();
        return "{"
            + field("bookingId", b.getBookingId()) + ","
            + field("studentId", s.getStudentId()) + ","
            + field("studentName", s.getFullName()) + ","
            + field("className", s.getClassName()) + ","
            + field("roomId", r.getRoomId()) + ","
            + field("roomName", r.getRoomName()) + ","
            + field("roomType", r.getRoomType()) + ","
            + fieldNum("floor", r.getFloor()) + ","
            + field("startTime", b.getTimeSlot().getStartTime().toString()) + ","
            + field("endTime", b.getTimeSlot().getEndTime().toString()) + ","
            + fieldNum("numberOfPeople", b.getNumberOfPeople()) + ","
            + field("purpose", b.getPurpose()) + ","
            + field("status", b.getStatus()) + ","
            + fieldNum("fee", b.getFee())
            + "}";
    }

    // ==================== Response wrappers ====================

    public static String success(String dataJson) {
        return "{\"success\": true, \"data\": " + dataJson + "}";
    }

    public static String successMsg(String message) {
        return "{\"success\": true, \"message\": \"" + esc(message) + "\"}";
    }

    public static String error(String message) {
        return "{\"success\": false, \"message\": \"" + esc(message) + "\"}";
    }
}
