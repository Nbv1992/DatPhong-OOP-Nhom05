package interfaces;

/**
 * Interface mô tả chính sách tính phí phòng.
 * Thiết kế theo Strategy Pattern để dễ mở rộng thêm loại phòng mới.
 */
public interface RoomFeePolicy {
    /**
     * Tính phí sử dụng phòng dựa trên số giờ sử dụng.
     * @param hours số giờ sử dụng phòng
     * @return tổng phí cần thanh toán (đơn vị: VNĐ)
     */
    double calculateFee(double hours);

    /**
     * Lấy mô tả chính sách phí (dùng để hiển thị cho người dùng).
     * @return chuỗi mô tả chính sách
     */
    String getFeeDescription();
}
