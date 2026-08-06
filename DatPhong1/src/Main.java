import api.ApiServer;
import exception.BookingException;
import model.*;
import repository.AccountRepository;
import repository.BookingRepository;
import repository.RoomRepository;
import repository.StudentRepository;
import service.BookingService;
import service.RoomService;
import service.StudentService;
import utils.ConsoleUtils;
import utils.DateTimeUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Lớp chính – khởi động Java HttpServer và mở giao diện web.
 * Vẫn giữ console menu như fallback nếu không dùng web.
 */
public class Main {

    // ==================== Resolve data directory ====================
    /**
     * Tìm file data theo thứ tự ưu tiên:
     * 1. File đã tồn tại gần JAR/class nhất (DatPhong/data/)
     * 2. Tạo mới ở thư mục cha của class (DatPhong/data/)
     * 3. Working directory/data/
     */
    private static String resolveDataPath(String filename) {
        try {
            java.nio.file.Path jarDir = java.nio.file.Paths.get(
                Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).getParent();

            // Các vị trí ứng viên — theo thứ tự ưu tiên
            java.nio.file.Path[] candidates = {
                jarDir.getParent().resolve("data").resolve(filename), // DatPhong/data/  ← đúng với IntelliJ
                jarDir.resolve("data").resolve(filename),              // out/data/
                java.nio.file.Paths.get("data").resolve(filename)      // working dir/data/
            };

            // Ưu tiên: file đã tồn tại
            for (java.nio.file.Path c : candidates) {
                if (java.nio.file.Files.exists(c)) {
                    System.out.println("[Main] data file found: " + c.toAbsolutePath());
                    return c.toAbsolutePath().toString();
                }
            }

            // Không có file nào → tạo mới ở DatPhong/data/
            java.nio.file.Path createAt = candidates[0];
            java.nio.file.Files.createDirectories(createAt.getParent());
            System.out.println("[Main] data file will create at: " + createAt.toAbsolutePath());
            return createAt.toAbsolutePath().toString();

        } catch (Exception e) {
            System.err.println("[Main] Cannot resolve data path: " + e.getMessage());
            return "data/" + filename;
        }
    }

    // Đường dẫn file dữ liệu — tự động resolve đúng trên mọi máy
    private static final String ROOMS_FILE    = resolveDataPath("rooms.txt");
    private static final String STUDENTS_FILE = resolveDataPath("students.txt");
    private static final String BOOKINGS_FILE = resolveDataPath("bookings.txt");
    private static final String ACCOUNTS_FILE = resolveDataPath("accounts.txt");

    // Repository
    private static final RoomRepository    roomRepo    = new RoomRepository(ROOMS_FILE);
    private static final StudentRepository studentRepo = new StudentRepository(STUDENTS_FILE);
    private static final BookingRepository bookingRepo = new BookingRepository(BOOKINGS_FILE);
    private static final AccountRepository accountRepo = new AccountRepository(ACCOUNTS_FILE);

    // Service
    private static final RoomService    roomService    = new RoomService(roomRepo);
    private static final StudentService studentService = new StudentService(studentRepo);
    private static final BookingService bookingService = new BookingService(bookingRepo, roomService, studentService);

    private static final Scanner sc = new Scanner(System.in, "UTF-8");

    // ==================== Main ====================

    public static void main(String[] args) throws IOException {
        printBanner();

        // Khởi động Web API Server
        try {
            ApiServer server = new ApiServer(roomRepo, studentRepo, bookingRepo, accountRepo);
            server.start();

            // Tự động mở trình duyệt
            openBrowser("http://localhost:8080");

        } catch (IOException e) {
            System.out.println("[WARN] Không thể khởi động web server: " + e.getMessage());
            System.out.println("[INFO] Chuyển sang chế độ console...");
            runConsoleMode();
            return;
        }

        // Giữ server chạy – console mode vẫn hoạt động song song
        System.out.println();
        System.out.println("  Nhấn ENTER để chuyển sang chế độ console (tuỳ chọn)...");
        System.out.println("  Hoặc dùng giao diện web tại http://localhost:8080");
        System.out.println();

        String input = sc.nextLine().trim();
        if (input.equalsIgnoreCase("console") || input.isEmpty()) {
            runConsoleMode();
        } else {
            // Giữ process sống để server tiếp tục chạy
            System.out.println("Server đang chạy. Đóng cửa sổ này để tắt server.");
            try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║    HỆ THỐNG QUẢN LÝ ĐẶT PHÒNG HỌC NHÓM - NHÓM 05      ║");
        System.out.println("║              Giao diện Web + REST API                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("  ✓ Đã mở trình duyệt tại: " + url);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback: thử mở bằng lệnh hệ thống
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
            System.out.println("  ✓ Đã mở trình duyệt tại: " + url);
        } catch (Exception e) {
            System.out.println("  ! Không thể tự mở trình duyệt. Vui lòng mở thủ công: " + url);
        }
    }

    // ==================== Console Mode ====================

    private static void runConsoleMode() {
        System.out.println();
        System.out.println("=== CHẾ ĐỘ CONSOLE ===");
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Nhập lựa chọn: ");
            switch (choice) {
                case 1 -> menuPhong();
                case 2 -> menuSinhVien();
                case 3 -> menuDatPhong();
                case 4 -> menuHuyPhong();
                case 5 -> menuXemLich();
                case 0 -> {
                    ConsoleUtils.printInfo("Cảm ơn bạn đã sử dụng hệ thống. Tạm biệt!");
                    running = false;
                }
                default -> ConsoleUtils.printError("Lựa chọn không hợp lệ.");
            }
        }
        sc.close();
    }

    private static void printMainMenu() {
        System.out.println();
        ConsoleUtils.printSeparator();
        System.out.println("  MENU CHÍNH");
        ConsoleUtils.printThinSeparator();
        System.out.println("  1. Quản lý phòng học nhóm");
        System.out.println("  2. Quản lý sinh viên");
        System.out.println("  3. Đặt phòng");
        System.out.println("  4. Hủy lịch đặt phòng");
        System.out.println("  5. Xem lịch đặt phòng");
        System.out.println("  0. Thoát");
        ConsoleUtils.printSeparator();
    }

    // ==================== Menu Phòng ====================

    private static void menuPhong() {
        boolean back = false;
        while (!back) {
            System.out.println();
            ConsoleUtils.printHeader("QUẢN LÝ PHÒNG HỌC NHÓM");
            System.out.println("  1. Xem danh sách tất cả phòng");
            System.out.println("  2. Xem phòng đang hoạt động");
            System.out.println("  3. Tìm phòng theo mã");
            System.out.println("  4. Tìm phòng theo loại");
            System.out.println("  5. Xem lịch trống của phòng");
            System.out.println("  6. Cập nhật trạng thái phòng");
            System.out.println("  0. Quay lại");
            ConsoleUtils.printThinSeparator();
            int choice = readInt("Nhập lựa chọn: ");
            switch (choice) {
                case 1 -> hienThiDanhSachPhong(roomService.getAllRooms());
                case 2 -> hienThiDanhSachPhong(roomService.getActiveRooms());
                case 3 -> timPhongTheoMa();
                case 4 -> timPhongTheoLoai();
                case 5 -> xemLichTrongPhong();
                case 6 -> capNhatTrangThaiPhong();
                case 0 -> back = true;
                default -> ConsoleUtils.printError("Lựa chọn không hợp lệ.");
            }
        }
    }

    private static void hienThiDanhSachPhong(List<Room> rooms) {
        System.out.println();
        ConsoleUtils.printThinSeparator();
        if (rooms.isEmpty()) { ConsoleUtils.printInfo("Không có phòng nào."); return; }
        System.out.printf("%-8s %-25s %-6s %-10s %-22s %-18s %-15s%n",
            "Mã phòng","Tên phòng","Tầng","Sức chứa","Loại phòng","Trạng thái","Phí");
        ConsoleUtils.printThinSeparator();
        for (Room r : rooms) {
            System.out.printf("%-8s %-25s %-6d %-10d %-22s %-18s %-15s%n",
                r.getRoomId(), r.getRoomName(), r.getFloor(), r.getMaxCapacity(),
                r.getRoomType(), r.getStatus(), r.getFeePolicy().getFeeDescription());
        }
        ConsoleUtils.printThinSeparator();
        System.out.println("Tổng: " + rooms.size() + " phòng");
    }

    private static void timPhongTheoMa() {
        String id = readString("Nhập mã phòng: ");
        try { System.out.println(roomService.getRoomById(id)); }
        catch (BookingException e) { ConsoleUtils.printError(e.getMessage()); }
    }

    private static void timPhongTheoLoai() {
        System.out.println("  1. Phòng thường  2. Máy chiếu  3. Seminar");
        int c = readInt("Chọn: ");
        String type = switch (c) {
            case 1 -> NormalRoom.TYPE;
            case 2 -> ProjectorRoom.TYPE;
            case 3 -> SeminarRoom.TYPE;
            default -> "";
        };
        if (type.isEmpty()) { ConsoleUtils.printError("Không hợp lệ."); return; }
        hienThiDanhSachPhong(roomService.getRoomsByType(type));
    }

    private static void xemLichTrongPhong() {
        String roomId = readString("Mã phòng: ");
        try {
            Room room = roomService.getRoomById(roomId);
            List<Booking> active = bookingService.getActiveBookingsByRoom(roomId);
            System.out.println("Phòng: " + room.getRoomName());
            if (active.isEmpty()) { ConsoleUtils.printInfo("Phòng trống hoàn toàn."); return; }
            for (Booking b : active)
                System.out.printf("  - %s | %d người | %s%n", b.getTimeSlot(), b.getNumberOfPeople(), b.getPurpose());
        } catch (BookingException e) { ConsoleUtils.printError(e.getMessage()); }
    }

    private static void capNhatTrangThaiPhong() {
        String roomId = readString("Mã phòng: ");
        System.out.println("  1. Đang hoạt động  2. Đang bảo trì");
        int c = readInt("Chọn: ");
        String status = switch (c) {
            case 1 -> Room.STATUS_ACTIVE;
            case 2 -> Room.STATUS_MAINTENANCE;
            default -> "";
        };
        if (status.isEmpty()) { ConsoleUtils.printError("Không hợp lệ."); return; }
        try { roomService.updateRoomStatus(roomId, status); ConsoleUtils.printSuccess("Đã cập nhật."); }
        catch (BookingException e) { ConsoleUtils.printError(e.getMessage()); }
    }

    // ==================== Menu Sinh Viên ====================

    private static void menuSinhVien() {
        boolean back = false;
        while (!back) {
            System.out.println();
            ConsoleUtils.printHeader("QUẢN LÝ SINH VIÊN");
            System.out.println("  1. Xem danh sách  2. Tìm theo mã  3. Thêm mới  0. Quay lại");
            ConsoleUtils.printThinSeparator();
            int c = readInt("Nhập lựa chọn: ");
            switch (c) {
                case 1 -> hienThiSinhVien();
                case 2 -> { String id = readString("Mã SV: ");
                    try { System.out.println(studentService.getStudentById(id)); }
                    catch (BookingException e) { ConsoleUtils.printError(e.getMessage()); } }
                case 3 -> themSinhVien();
                case 0 -> back = true;
                default -> ConsoleUtils.printError("Không hợp lệ.");
            }
        }
    }

    private static void hienThiSinhVien() {
        List<Student> list = studentService.getAllStudents();
        if (list.isEmpty()) { ConsoleUtils.printInfo("Chưa có sinh viên."); return; }
        System.out.printf("%-12s %-25s %-8s %-15s %-30s%n","Mã SV","Họ tên","Lớp","SĐT","Email");
        ConsoleUtils.printThinSeparator();
        for (Student s : list)
            System.out.printf("%-12s %-25s %-8s %-15s %-30s%n",
                s.getStudentId(), s.getFullName(), s.getClassName(), s.getPhone(), s.getEmail());
    }

    private static void themSinhVien() {
        String id    = readString("Mã SV: ");
        String name  = readString("Họ tên: ");
        String phone = readString("SĐT: ");
        String email = readString("Email: ");
        String clazz = readString("Lớp: ");
        try {
            studentService.addStudent(new Student(id, name, phone, email, clazz));
            ConsoleUtils.printSuccess("Thêm sinh viên thành công.");
        } catch (Exception e) { ConsoleUtils.printError(e.getMessage()); }
    }

    // ==================== Menu Đặt Phòng ====================

    private static void menuDatPhong() {
        ConsoleUtils.printHeader("ĐẶT PHÒNG HỌC NHÓM");
        hienThiDanhSachPhong(roomService.getActiveRooms());
        System.out.println("Định dạng ngày giờ: dd/MM/yyyy HH:mm");
        String studentId = readString("Mã sinh viên: ");
        String roomId    = readString("Mã phòng: ");
        String startStr  = readString("Thời gian bắt đầu: ");
        String endStr    = readString("Thời gian kết thúc: ");
        String numStr    = readString("Số người: ");
        String purpose   = readString("Mục đích: ");
        try {
            LocalDateTime start = DateTimeUtils.parse(startStr);
            LocalDateTime end   = DateTimeUtils.parse(endStr);
            int num = Integer.parseInt(numStr.trim());
            Booking b = bookingService.createBooking(studentId, roomId, start, end, num, purpose);
            ConsoleUtils.printSuccess("ĐẶT PHÒNG THÀNH CÔNG! Mã: " + b.getBookingId()
                + " | Phí: " + ConsoleUtils.formatCurrency(b.getFee()));
        } catch (DateTimeParseException e) {
            ConsoleUtils.printError("Sai định dạng ngày giờ.");
        } catch (NumberFormatException e) {
            ConsoleUtils.printError("Số người không hợp lệ.");
        } catch (BookingException e) {
            ConsoleUtils.printError(e.getMessage());
        }
    }

    // ==================== Menu Hủy Phòng ====================

    private static void menuHuyPhong() {
        ConsoleUtils.printHeader("HỦY LỊCH ĐẶT PHÒNG");
        String studentId = readString("Mã sinh viên: ");
        String bookingId = readString("Mã lịch đặt: ");
        try {
            bookingService.cancelBooking(bookingId, studentId);
            ConsoleUtils.printSuccess("Hủy lịch đặt " + bookingId + " thành công.");
        } catch (BookingException e) { ConsoleUtils.printError(e.getMessage()); }
    }

    // ==================== Menu Xem Lịch ====================

    private static void menuXemLich() {
        boolean back = false;
        while (!back) {
            ConsoleUtils.printHeader("XEM LỊCH ĐẶT PHÒNG");
            System.out.println("  1. Theo sinh viên  2. Tất cả  3. Chi tiết  0. Quay lại");
            ConsoleUtils.printThinSeparator();
            int c = readInt("Nhập lựa chọn: ");
            switch (c) {
                case 1 -> { String id = readString("Mã SV: ");
                    bookingService.getBookingsByStudent(id).forEach(System.out::println); }
                case 2 -> bookingService.getAllBookings().forEach(System.out::println);
                case 3 -> { String id = readString("Mã lịch đặt: ");
                    try { System.out.println(bookingService.getBookingDetail(id)); }
                    catch (BookingException e) { ConsoleUtils.printError(e.getMessage()); } }
                case 0 -> back = true;
                default -> ConsoleUtils.printError("Không hợp lệ.");
            }
        }
    }

    // ==================== Input Helpers ====================

    private static String readString(String prompt) {
        String input;
        do { System.out.print(prompt); input = sc.nextLine(); } while (input.isBlank());
        return input.trim();
    }

    private static int readInt(String prompt) {
        System.out.print(prompt);
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}
