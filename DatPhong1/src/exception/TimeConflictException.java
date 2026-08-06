package exception;

/**
 * Exception khi phòng bị trùng lịch đặt.
 */
public class TimeConflictException extends BookingException {
    public TimeConflictException(String roomId, String timeInfo) {
        super("Phòng " + roomId + " đã có lịch đặt trong khoảng thời gian " + timeInfo + ". Vui lòng chọn khung giờ khác.");
    }
}
