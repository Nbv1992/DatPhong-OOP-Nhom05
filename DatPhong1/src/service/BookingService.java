package service;

import exception.*;
import interfaces.BookingValidator;
import model.*;
import repository.BookingRepository;
import utils.IDGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý toàn bộ nghiệp vụ đặt phòng và hủy phòng.
 * Áp dụng BookingValidator (interface) để kiểm tra từng quy tắc.
 */
public class BookingService {

    private static final double MAX_HOURS_PER_DAY = 4.0;

    private final BookingRepository bookingRepository;
    private final RoomService roomService;
    private final StudentService studentService;

    // Danh sách các validator theo thứ tự kiểm tra
    private final List<BookingValidator> validators = new ArrayList<>();

    public BookingService(BookingRepository bookingRepository,
                          RoomService roomService,
                          StudentService studentService) {
        this.bookingRepository = bookingRepository;
        this.roomService = roomService;
        this.studentService = studentService;

        // Đăng ký các validator theo thứ tự ưu tiên
        registerValidators();
    }

    /**
     * Đăng ký tất cả các validator kiểm tra điều kiện đặt phòng.
     * Mỗi validator kiểm tra một quy tắc nghiệp vụ riêng.
     */
    private void registerValidators() {

        // 1. Kiểm tra thời gian hợp lệ (kết thúc > bắt đầu)
        validators.add(booking -> {
            TimeSlot slot = booking.getTimeSlot();
            if (!slot.getEndTime().isAfter(slot.getStartTime())) {
                throw new InvalidTimeException("Thời gian kết thúc phải sau thời gian bắt đầu.");
            }
            if (slot.getStartTime().isBefore(LocalDateTime.now())) {
                throw new InvalidTimeException("Không thể đặt phòng trong quá khứ.");
            }
        });

        // 2. Kiểm tra sinh viên tồn tại
        validators.add(booking -> {
            if (!studentService.exists(booking.getStudentId())) {
                throw new StudentNotFoundException(booking.getStudentId());
            }
        });

        // 3. Kiểm tra phòng tồn tại
        validators.add(booking -> {
            if (!roomService.exists(booking.getRoomId())) {
                throw new RoomNotFoundException(booking.getRoomId());
            }
        });

        // 4. Kiểm tra phòng đang hoạt động (không bảo trì)
        validators.add(booking -> {
            try {
                Room room = roomService.getRoomById(booking.getRoomId());
                if (!room.isActive()) {
                    throw new RoomUnavailableException(booking.getRoomId());
                }
            } catch (RoomNotFoundException e) {
                throw new BookingException(e.getMessage());
            }
        });

        // 5. Kiểm tra sức chứa phòng
        validators.add(booking -> {
            try {
                Room room = roomService.getRoomById(booking.getRoomId());
                if (booking.getNumberOfPeople() > room.getMaxCapacity()) {
                    throw new CapacityExceededException(booking.getNumberOfPeople(), room.getMaxCapacity());
                }
                if (booking.getNumberOfPeople() <= 0) {
                    throw new BookingException("Số người tham gia phải lớn hơn 0.");
                }
            } catch (RoomNotFoundException e) {
                throw new BookingException(e.getMessage());
            }
        });

        // 6. Kiểm tra trùng lịch phòng
        validators.add(booking -> {
            List<Booking> activeBookings = bookingRepository.findActiveByRoomId(booking.getRoomId());
            for (Booking existing : activeBookings) {
                if (existing.getTimeSlot().conflictsWith(booking.getTimeSlot())) {
                    throw new TimeConflictException(booking.getRoomId(), booking.getTimeSlot().toString());
                }
            }
        });

        // 7. Kiểm tra sinh viên không đặt quá 4 giờ/ngày
        validators.add(booking -> {
            LocalDate bookingDate = booking.getTimeSlot().getStartTime().toLocalDate();
            double alreadyBooked = bookingRepository.getTotalHoursBookedByStudentOnDate(
                booking.getStudentId(), bookingDate
            );
            double requested = booking.getTimeSlot().getDurationHours();
            if (alreadyBooked + requested > MAX_HOURS_PER_DAY) {
                throw new DailyHourLimitException(alreadyBooked, requested);
            }
        });
    }

    // ==================== Đặt phòng ====================

    /**
     * Thực hiện đặt phòng sau khi đã kiểm tra đầy đủ các điều kiện.
     *
     * @param studentId     mã sinh viên đặt phòng
     * @param roomId        mã phòng cần đặt
     * @param startTime     thời gian bắt đầu
     * @param endTime       thời gian kết thúc
     * @param numberOfPeople số người tham gia
     * @param purpose       mục đích sử dụng
     * @return đối tượng Booking đã tạo thành công
     * @throws BookingException nếu vi phạm bất kỳ quy tắc nghiệp vụ nào
     */
    public Booking createBooking(String studentId, String roomId,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  int numberOfPeople, String purpose) throws BookingException {

        // Sinh mã đặt phòng
        String bookingId = IDGenerator.generateBookingId();
        TimeSlot slot = new TimeSlot(startTime, endTime);
        Booking booking = new Booking(bookingId, studentId, roomId, slot, numberOfPeople, purpose);

        // Chạy qua tất cả validator
        for (BookingValidator validator : validators) {
            validator.validate(booking);
        }

        // Tính phí
        try {
            Room room = roomService.getRoomById(roomId);
            double fee = room.calculateFee(slot.getDurationHours());
            booking.setFee(fee);
        } catch (RoomNotFoundException e) {
            throw new BookingException(e.getMessage());
        }

        // Lưu lịch đặt
        bookingRepository.save(booking);
        return booking;
    }

    // ==================== Hủy phòng ====================

    /**
     * Hủy lịch đặt phòng.
     *
     * @param bookingId mã lịch đặt cần hủy
     * @param studentId mã sinh viên yêu cầu hủy
     * @throws BookingNotFoundException         nếu không tìm thấy mã đặt phòng
     * @throws BookingAlreadyCancelledException nếu lịch đã bị hủy trước đó
     * @throws UnauthorizedCancellationException nếu lịch không thuộc về sinh viên này
     */
    public void cancelBooking(String bookingId, String studentId) throws BookingException {
        // Kiểm tra lịch có tồn tại không
        Optional<Booking> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            throw new BookingNotFoundException(bookingId);
        }

        Booking booking = opt.get();

        // Kiểm tra đã hủy chưa
        if (!booking.isActive()) {
            throw new BookingAlreadyCancelledException(bookingId);
        }

        // Kiểm tra quyền sở hữu
        if (!booking.getStudentId().equalsIgnoreCase(studentId)) {
            throw new UnauthorizedCancellationException(bookingId);
        }

        // Cập nhật trạng thái
        booking.setStatus(Booking.STATUS_CANCELLED);
        bookingRepository.save(booking);
    }

    // ==================== Truy vấn ====================

    /**
     * Lấy danh sách lịch đặt của một sinh viên.
     */
    public List<Booking> getBookingsByStudent(String studentId) {
        return bookingRepository.findByStudentId(studentId);
    }

    /**
     * Lấy lịch đặt đang hoạt động (chưa hủy) của một phòng.
     */
    public List<Booking> getActiveBookingsByRoom(String roomId) {
        return bookingRepository.findActiveByRoomId(roomId);
    }

    /**
     * Lấy tất cả lịch đặt trong hệ thống.
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * Lấy chi tiết BookingDetail (kết hợp thông tin booking + student + room).
     */
    public BookingDetail getBookingDetail(String bookingId) throws BookingException {
        Optional<Booking> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) throw new BookingNotFoundException(bookingId);

        Booking booking = opt.get();

        Student student;
        try {
            student = studentService.getStudentById(booking.getStudentId());
        } catch (StudentNotFoundException e) {
            throw new BookingException(e.getMessage());
        }

        Room room;
        try {
            room = roomService.getRoomById(booking.getRoomId());
        } catch (RoomNotFoundException e) {
            throw new BookingException(e.getMessage());
        }

        return new BookingDetail(booking, student, room);
    }
}
