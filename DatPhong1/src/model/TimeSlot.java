package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Đại diện cho một khung thời gian (bắt đầu - kết thúc).
 * Dùng để kiểm tra trùng lịch và tính số giờ sử dụng.
 */
public class TimeSlot {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Thời điểm bắt đầu
    private LocalDateTime startTime;

    // Thời điểm kết thúc
    private LocalDateTime endTime;

    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // ==================== Getters / Setters ====================

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    // ==================== Business Methods ====================

    /**
     * Tính số giờ của khung thời gian này.
     * @return số giờ (dạng thực)
     */
    public double getDurationHours() {
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
        return minutes / 60.0;
    }

    /**
     * Kiểm tra xem TimeSlot này có giao nhau (trùng) với TimeSlot khác không.
     * @param other TimeSlot cần so sánh
     * @return true nếu có trùng
     */
    public boolean overlapsWith(TimeSlot other) {
        // Trùng khi: bắt đầu của this < kết thúc của other VÀ kết thúc của this > bắt đầu của other
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    /**
     * Kiểm tra chính xác hơn bằng logic so sánh rõ ràng.
     */
    public boolean conflictsWith(TimeSlot other) {
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    @Override
    public String toString() {
        return startTime.format(FORMATTER) + " - " + endTime.format(FORMATTER);
    }
}
