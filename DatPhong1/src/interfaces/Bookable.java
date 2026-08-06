package interfaces;

import model.TimeSlot;

/**
 * Interface mô tả hành vi có thể được đặt lịch.
 * Bất kỳ đối tượng nào implement interface này đều có thể được đặt lịch.
 */
public interface Bookable {
    /**
     * Kiểm tra xem đối tượng có sẵn sàng để đặt lịch trong khung giờ đã cho không.
     * @param slot khung thời gian cần kiểm tra
     * @return true nếu có thể đặt, false nếu không
     */
    boolean isAvailable(TimeSlot slot);

    /**
     * Lấy mã định danh của đối tượng có thể đặt lịch.
     * @return mã định danh
     */
    String getId();
}
