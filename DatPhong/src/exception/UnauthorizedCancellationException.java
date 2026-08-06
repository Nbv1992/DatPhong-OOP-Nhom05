package exception;

/**
 * Exception khi sinh viên cố hủy lịch không thuộc về mình.
 */
public class UnauthorizedCancellationException extends BookingException {
    public UnauthorizedCancellationException(String bookingId) {
        super("Bạn không có quyền hủy lịch đặt phòng mã: " + bookingId + ". Lịch đặt này không thuộc về bạn.");
    }
}
