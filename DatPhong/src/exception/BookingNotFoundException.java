package exception;

/**
 * Exception khi không tìm thấy lịch đặt phòng.
 */
public class BookingNotFoundException extends BookingException {
    public BookingNotFoundException(String bookingId) {
        super("Không tìm thấy lịch đặt phòng với mã: " + bookingId);
    }
}
