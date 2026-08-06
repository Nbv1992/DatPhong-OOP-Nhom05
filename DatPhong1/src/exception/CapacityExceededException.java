package exception;

/**
 * Exception khi số người tham gia vượt quá sức chứa phòng.
 */
public class CapacityExceededException extends BookingException {
    public CapacityExceededException(int requested, int capacity) {
        super("Số người tham gia (" + requested + ") vượt quá sức chứa tối đa của phòng (" + capacity + " người).");
    }
}
