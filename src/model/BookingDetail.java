package model;

/**
 * Chi tiết đầy đủ của một lịch đặt phòng.
 * Kết hợp thông tin từ Booking + Student + Room để hiển thị.
 */
public class BookingDetail {

    private Booking booking;
    private Student student;
    private Room room;

    public BookingDetail(Booking booking, Student student, Room room) {
        this.booking = booking;
        this.student = student;
        this.room = room;
    }

    // ==================== Getters ====================

    public Booking getBooking() {
        return booking;
    }

    public Student getStudent() {
        return student;
    }

    public Room getRoom() {
        return room;
    }

    // ==================== Display ====================

    @Override
    public String toString() {
        return String.format(
            "=== Chi tiết lịch đặt phòng ===\n" +
            "  Mã lịch đặt : %s\n" +
            "  Sinh viên   : %s (Lớp: %s)\n" +
            "  Phòng       : %s - %s (Tầng %d)\n" +
            "  Loại phòng  : %s\n" +
            "  Thời gian   : %s\n" +
            "  Số người    : %d\n" +
            "  Mục đích    : %s\n" +
            "  Trạng thái  : %s\n" +
            "  Phí         : %.0fđ\n" +
            "================================",
            booking.getBookingId(),
            student.getFullName(), student.getClassName(),
            room.getRoomId(), room.getRoomName(), room.getFloor(),
            room.getRoomType(),
            booking.getTimeSlot(),
            booking.getNumberOfPeople(),
            booking.getPurpose(),
            booking.getStatus(),
            booking.getFee()
        );
    }
}
