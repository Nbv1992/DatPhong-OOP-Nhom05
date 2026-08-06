package utils;

/**
 * Tiện ích kiểm tra và validate dữ liệu đầu vào.
 */
public class ValidationUtils {

    /**
     * Kiểm tra chuỗi có rỗng hoặc null không.
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Kiểm tra số điện thoại hợp lệ (10-11 chữ số, bắt đầu bằng 0).
     */
    public static boolean isValidPhone(String phone) {
        if (isBlank(phone)) return false;
        return phone.matches("^0\\d{9,10}$");
    }

    /**
     * Kiểm tra email hợp lệ (đơn giản).
     */
    public static boolean isValidEmail(String email) {
        if (isBlank(email)) return false;
        return email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Kiểm tra số nguyên dương.
     */
    public static boolean isPositiveInt(int value) {
        return value > 0;
    }

    /**
     * Parse số nguyên an toàn, trả về -1 nếu không hợp lệ.
     */
    public static int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Yêu cầu chuỗi không rỗng, ném IllegalArgumentException nếu rỗng.
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        return value.trim();
    }
}
