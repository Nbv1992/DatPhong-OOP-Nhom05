package exception;

/**
 * Exception khi không tìm thấy phòng trong hệ thống.
 */
public class RoomNotFoundException extends BookingException {
    public RoomNotFoundException(String roomId) {
        super("Không tìm thấy phòng với mã: " + roomId);
    }
}
