package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tiện ích sinh mã định danh tự động cho lịch đặt phòng.
 */
public class IDGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Sinh mã đặt phòng theo định dạng: BP-yyyyMMddHHmmss-XXX
     * @return mã đặt phòng duy nhất
     */
    public static String generateBookingId() {
        String timestamp = LocalDateTime.now().format(FMT);
        int seq = counter.getAndIncrement();
        return String.format("BP-%s-%03d", timestamp, seq);
    }

    /**
     * Reset bộ đếm (dùng khi load dữ liệu từ file để tránh trùng).
     * @param nextValue giá trị tiếp theo
     */
    public static void resetCounter(int nextValue) {
        counter.set(nextValue);
    }
}
