package exception;

/**
 * Exception khi cố hủy một lịch đặt đã bị hủy trước đó.
 */
public class BookingAlreadyCancelledException extends BookingException {
    public BookingAlreadyCancelledException(String bookingId) {
        super("Lịch đặt phòng mã " + bookingId + " đã được hủy trước đó.");
    }
}
