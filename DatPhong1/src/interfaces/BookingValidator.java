package interfaces;

import model.Booking;
import exception.BookingException;

/**
 * Interface kiểm tra điều kiện đặt phòng.
 * Mỗi validator thực hiện một quy tắc nghiệp vụ riêng biệt.
 */
public interface BookingValidator {
    /**
     * Kiểm tra tính hợp lệ của một yêu cầu đặt phòng.
     * @param booking đối tượng Booking cần kiểm tra
     * @throws BookingException nếu vi phạm quy tắc nghiệp vụ
     */
    void validate(Booking booking) throws BookingException;
}
