package model;

import interfaces.Bookable;
import interfaces.RoomFeePolicy;

/**
 * Lớp trừu tượng đại diện cho phòng học nhóm.
 * Implement Bookable để có thể được đặt lịch.
 * Mỗi subclass sẽ định nghĩa loại phòng và chính sách phí riêng.
 */
public abstract class Room implements Bookable {

    // Các trạng thái phòng
    public static final String STATUS_ACTIVE      = "Đang hoạt động";
    public static final String STATUS_MAINTENANCE = "Đang bảo trì";

    // Mã phòng (dùng làm ID)
    private String roomId;

    // Tên phòng
    private String roomName;

    // Tầng
    private int floor;

    // Sức chứa tối đa (người)
    private int maxCapacity;

    // Trạng thái phòng
    private String status;

    // Chính sách tính phí (Strategy Pattern)
    private RoomFeePolicy feePolicy;

    public Room(String roomId, String roomName, int floor, int maxCapacity, RoomFeePolicy feePolicy) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.floor = floor;
        this.maxCapacity = maxCapacity;
        this.status = STATUS_ACTIVE; // mặc định đang hoạt động
        this.feePolicy = feePolicy;
    }

    // ==================== Getters / Setters ====================

    @Override
    public String getId() {
        return roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RoomFeePolicy getFeePolicy() {
        return feePolicy;
    }

    public void setFeePolicy(RoomFeePolicy feePolicy) {
        this.feePolicy = feePolicy;
    }

    // ==================== Business Methods ====================

    /**
     * Kiểm tra phòng có đang hoạt động không.
     * @return true nếu đang hoạt động
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /**
     * Tính phí đặt phòng dựa trên số giờ.
     * @param hours số giờ sử dụng
     * @return số tiền phí
     */
    public double calculateFee(double hours) {
        return feePolicy.calculateFee(hours);
    }

    /**
     * Lấy loại phòng (do subclass định nghĩa).
     * @return chuỗi loại phòng
     */
    public abstract String getRoomType();

    /**
     * Xuất dữ liệu CSV để lưu file.
     */
    public abstract String toCsvString();

    @Override
    public String toString() {
        return String.format("[%s] %s | Tầng: %d | Sức chứa: %d người | Loại: %s | Trạng thái: %s | Phí: %s",
            roomId, roomName, floor, maxCapacity, getRoomType(), status, feePolicy.getFeeDescription());
    }
}
