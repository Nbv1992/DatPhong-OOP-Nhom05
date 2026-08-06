package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Tiện ích xử lý ngày giờ cho hệ thống đặt phòng.
 */
public class DateTimeUtils {

    public static final DateTimeFormatter DISPLAY_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final DateTimeFormatter INPUT_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final DateTimeFormatter FILE_FORMATTER =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Parse chuỗi ngày giờ nhập từ người dùng.
     * @param text chuỗi theo định dạng dd/MM/yyyy HH:mm
     * @return LocalDateTime tương ứng
     * @throws DateTimeParseException nếu định dạng sai
     */
    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text.trim(), INPUT_FORMATTER);
    }

    /**
     * Parse chuỗi ngày giờ từ file (ISO format).
     */
    public static LocalDateTime parseFromFile(String text) {
        return LocalDateTime.parse(text.trim(), FILE_FORMATTER);
    }

    /**
     * Format LocalDateTime ra chuỗi hiển thị.
     */
    public static String format(LocalDateTime dt) {
        return dt.format(DISPLAY_FORMATTER);
    }

    /**
     * Lấy ngày của một LocalDateTime (chỉ phần ngày).
     */
    public static LocalDate toDate(LocalDateTime dt) {
        return dt.toLocalDate();
    }
}
