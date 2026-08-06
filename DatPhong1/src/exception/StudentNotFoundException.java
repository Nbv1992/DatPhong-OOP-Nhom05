package exception;

/**
 * Exception khi không tìm thấy sinh viên trong hệ thống.
 */
public class StudentNotFoundException extends BookingException {
    public StudentNotFoundException(String studentId) {
        super("Không tìm thấy sinh viên với mã: " + studentId);
    }
}
