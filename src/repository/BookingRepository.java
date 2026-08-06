package repository;

import model.Booking;
import model.TimeSlot;
import utils.DateTimeUtils;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lưu trữ và đọc dữ liệu lịch đặt phòng từ file.
 * File CSV: bookingId,studentId,roomId,startTime,endTime,numberOfPeople,purpose,status,fee
 */
public class BookingRepository {

    private final String filePath;
    private final List<Booking> bookings = new ArrayList<>();

    public BookingRepository(String filePath) {
        this.filePath = filePath;
        loadFromFile();
    }

    // ==================== File I/O ====================

    private void loadFromFile() {
        bookings.clear();
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    bookings.add(fromCsvString(line));
                } catch (Exception e) {
                    System.err.println("[Repository] Bỏ qua dòng lỗi trong file booking: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[Repository] Không thể đọc file booking: " + e.getMessage());
        }
    }

    /**
     * Parse một dòng CSV thành đối tượng Booking.
     */
    private Booking fromCsvString(String csv) {
        String[] p = csv.split(",", 9);
        TimeSlot slot = new TimeSlot(
            DateTimeUtils.parseFromFile(p[3].trim()),
            DateTimeUtils.parseFromFile(p[4].trim())
        );
        Booking b = new Booking(
            p[0].trim(), p[1].trim(), p[2].trim(),
            slot,
            Integer.parseInt(p[5].trim()),
            p[6].trim()
        );
        b.setStatus(p[7].trim());
        b.setFee(Double.parseDouble(p[8].trim()));
        return b;
    }

    public void saveToFile() {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write("# bookingId,studentId,roomId,startTime,endTime,numberOfPeople,purpose,status,fee");
            bw.newLine();
            for (Booking b : bookings) {
                bw.write(b.toCsvString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[Repository] Không thể ghi file booking: " + e.getMessage());
        }
    }

    // ==================== CRUD ====================

    public List<Booking> findAll() {
        return new ArrayList<>(bookings);
    }

    public Optional<Booking> findById(String bookingId) {
        return bookings.stream()
            .filter(b -> b.getBookingId().equalsIgnoreCase(bookingId))
            .findFirst();
    }

    /**
     * Lấy tất cả lịch đặt của một sinh viên.
     */
    public List<Booking> findByStudentId(String studentId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getStudentId().equalsIgnoreCase(studentId)) result.add(b);
        }
        return result;
    }

    /**
     * Lấy tất cả lịch đặt đang hoạt động (chưa hủy) của một phòng.
     */
    public List<Booking> findActiveByRoomId(String roomId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getRoomId().equalsIgnoreCase(roomId) && b.isActive()) result.add(b);
        }
        return result;
    }

    /**
     * Tính tổng số giờ sinh viên đã đặt trong một ngày cụ thể (chỉ tính lịch chưa hủy).
     */
    public double getTotalHoursBookedByStudentOnDate(String studentId, LocalDate date) {
        double total = 0.0;
        for (Booking b : bookings) {
            if (!b.getStudentId().equalsIgnoreCase(studentId)) continue;
            if (!b.isActive()) continue;
            LocalDate bookingDate = b.getTimeSlot().getStartTime().toLocalDate();
            if (bookingDate.equals(date)) {
                total += b.getTimeSlot().getDurationHours();
            }
        }
        return total;
    }

    public void save(Booking booking) {
        bookings.removeIf(b -> b.getBookingId().equalsIgnoreCase(booking.getBookingId()));
        bookings.add(booking);
        saveToFile();
    }

    public boolean existsById(String bookingId) {
        return bookings.stream().anyMatch(b -> b.getBookingId().equalsIgnoreCase(bookingId));
    }
}
