package exception;

/**
 * Exception chung cho các lỗi nghiệp vụ liên quan đến đặt phòng.
 */
public class BookingException extends Exception {
    public BookingException(String message) {
        super(message);
    }
}
