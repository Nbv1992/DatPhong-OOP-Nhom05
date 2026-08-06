package exception;

/**
 * Exception khi sinh viên đặt phòng vượt quá 4 giờ trong một ngày.
 */
public class DailyHourLimitException extends BookingException {
    public DailyHourLimitException(double usedHours, double requestedHours) {
        super(String.format(
            "Sinh viên đã đặt %.1f giờ hôm nay. Yêu cầu thêm %.1f giờ sẽ vượt giới hạn 4 giờ/ngày.",
            usedHours, requestedHours
        ));
    }
}
