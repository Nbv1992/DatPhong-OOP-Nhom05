package exception;

/**
 * Exception khi thời gian đặt phòng không hợp lệ
 * (thời gian kết thúc <= thời gian bắt đầu, hoặc thời gian trong quá khứ).
 */
public class InvalidTimeException extends BookingException {
    public InvalidTimeException(String reason) {
        super("Thời gian đặt phòng không hợp lệ: " + reason);
    }
}
