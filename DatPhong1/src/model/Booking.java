package model;

/**
 * Đại diện cho một lịch đặt phòng trong hệ thống.
 * Lưu trữ toàn bộ thông tin về một lần đặt phòng.
 */
public class Booking {

    // Trạng thái lịch đặt
    public static final String STATUS_BOOKED    = "Đã đặt";
    public static final String STATUS_CANCELLED = "Đã hủy";

    // Mã lịch đặt (tự sinh)
    private String bookingId;

    // Mã sinh viên đặt phòng
    private String studentId;

    // Mã phòng được đặt
    private String roomId;

    // Khung thời gian đặt
    private TimeSlot timeSlot;

    // Số người tham gia
    private int numberOfPeople;

    // Trạng thái lịch đặt
    private String status;

    // Mục đích sử dụng phòng
    private String purpose;

    // Phí thanh toán (tính sau khi đặt)
    private double fee;

    public Booking(String bookingId, String studentId, String roomId,
                   TimeSlot timeSlot, int numberOfPeople, String purpose) {
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.roomId = roomId;
        this.timeSlot = timeSlot;
        this.numberOfPeople = numberOfPeople;
        this.purpose = purpose;
        this.status = STATUS_BOOKED; // mặc định khi tạo là "Đã đặt"
        this.fee = 0.0;
    }

    // ==================== Getters / Setters ====================

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public int getNumberOfPeople() {
        return numberOfPeople;
    }

    public void setNumberOfPeople(int numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    // ==================== Business Methods ====================

    /**
     * Kiểm tra lịch đặt có đang hoạt động (chưa hủy) không.
     */
    public boolean isActive() {
        return STATUS_BOOKED.equals(status);
    }

    /**
     * Xuất dữ liệu CSV để lưu file.
     * Định dạng: bookingId,studentId,roomId,startTime,endTime,numberOfPeople,purpose,status,fee
     */
    public String toCsvString() {
        return String.join(",",
            bookingId, studentId, roomId,
            timeSlot.getStartTime().toString(),
            timeSlot.getEndTime().toString(),
            String.valueOf(numberOfPeople),
            purpose,
            status,
            String.valueOf(fee)
        );
    }

    @Override
    public String toString() {
        return String.format(
            "Mã đặt: %s | Sinh viên: %s | Phòng: %s | Thời gian: %s | Số người: %d | Mục đích: %s | Trạng thái: %s | Phí: %.0fđ",
            bookingId, studentId, roomId, timeSlot, numberOfPeople, purpose, status, fee
        );
    }
}
