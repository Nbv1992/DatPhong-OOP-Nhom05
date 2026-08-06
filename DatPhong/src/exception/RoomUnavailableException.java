package exception;

/**
 * Exception khi phòng đang bảo trì hoặc không hoạt động.
 */
public class RoomUnavailableException extends BookingException {
    public RoomUnavailableException(String roomId) {
        super("Phòng " + roomId + " hiện đang bảo trì, không thể đặt.");
    }
}
