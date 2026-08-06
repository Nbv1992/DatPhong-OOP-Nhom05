package service;

import model.Account;
import model.Student;
import repository.AccountRepository;
import repository.StudentRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Xử lý nghiệp vụ xác thực: đăng nhập, đăng ký, đổi/reset mật khẩu.
 *
 * Role mặc định khi đăng ký: HOCVIEN
 * Tài khoản ADMIN được tạo sẵn khi seed dữ liệu (Main.java hoặc AccountRepository).
 */
public class AuthService {

    private final AccountRepository accountRepo;
    private final StudentRepository studentRepo;

    public AuthService(AccountRepository accountRepo, StudentRepository studentRepo) {
        this.accountRepo = accountRepo;
        this.studentRepo = studentRepo;
        seedAdminIfNotExists(); // Tạo admin mặc định nếu chưa có
    }

    // ==================== Seed admin mặc định ====================

    /**
     * Tạo tài khoản admin mặc định nếu chưa tồn tại.
     * Đăng nhập: admin / Admin@123
     */
    private void seedAdminIfNotExists() {
        if (!accountRepo.existsByStudentId("admin")) {
            Account adminAcc = new Account(
                "admin",
                hashPassword("Admin@123"),
                "admin@eduroom.edu.vn",
                Account.ROLE_ADMIN
            );
            accountRepo.save(adminAcc);
            System.out.println("[AuthService] Đã tạo tài khoản admin mặc định: admin / Admin@123");
        }
    }

    // ==================== Hash ====================

    /** Hash mật khẩu bằng SHA-256. */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 không khả dụng", e);
        }
    }

    // ==================== Login ====================

    /**
     * Đăng nhập.
     * @return Account nếu thành công
     * @throws IllegalArgumentException nếu sai thông tin
     */
    public Account login(String studentId, String password) {
        if (studentId == null || studentId.isBlank())
            throw new IllegalArgumentException("Mã sinh viên không được để trống.");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Mật khẩu không được để trống.");

        Optional<Account> opt = accountRepo.findByStudentId(studentId.trim());
        if (opt.isEmpty())
            throw new IllegalArgumentException("Tài khoản không tồn tại. Vui lòng đăng ký trước.");

        Account acc = opt.get();
        if (!acc.getPasswordHash().equals(hashPassword(password)))
            throw new IllegalArgumentException("Mật khẩu không đúng.");

        return acc;
    }

    // ==================== Register ====================

    /**
     * Đăng ký tài khoản mới — role mặc định HOCVIEN.
     * Sẽ tạo mới cả Student lẫn Account nếu chưa tồn tại.
     */
    public Account register(String studentId, String fullName, String phone,
                            String email, String className, String password) {
        if (studentId == null || studentId.isBlank())
            throw new IllegalArgumentException("Mã sinh viên không được để trống.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự.");
        if (accountRepo.existsByStudentId(studentId.trim()))
            throw new IllegalArgumentException("Mã sinh viên này đã có tài khoản. Vui lòng đăng nhập.");
        if (phone == null || !phone.matches("^0\\d{9,10}$"))
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (10-11 chữ số, bắt đầu bằng 0).");
        if (email == null || !email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            throw new IllegalArgumentException("Email không hợp lệ.");
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Họ tên không được để trống.");
        if (className == null || className.isBlank())
            throw new IllegalArgumentException("Tên lớp không được để trống.");

        // Tạo Student nếu chưa có
        if (!studentRepo.existsById(studentId.trim())) {
            studentRepo.save(new Student(
                studentId.trim(), fullName.trim(), phone.trim(), email.trim(), className.trim()
            ));
        }

        // Tạo Account với role HOCVIEN
        Account acc = new Account(studentId.trim(), hashPassword(password), email.trim(), Account.ROLE_HOCVIEN);
        accountRepo.save(acc);
        return acc;
    }

    // ==================== Reset Password ====================

    /** Đặt lại mật khẩu — xác thực bằng mã SV + email. */
    public void resetPassword(String studentId, String email, String newPassword) {
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        Optional<Account> opt = accountRepo.findByStudentId(studentId.trim());
        if (opt.isEmpty())
            throw new IllegalArgumentException("Mã sinh viên không tồn tại.");
        Account acc = opt.get();
        if (!acc.getEmail().equalsIgnoreCase(email.trim()))
            throw new IllegalArgumentException("Email không khớp với tài khoản này.");
        acc.setPasswordHash(hashPassword(newPassword));
        accountRepo.save(acc);
    }

    // ==================== Change Password ====================

    /** Đổi mật khẩu khi đã đăng nhập. */
    public void changePassword(String studentId, String oldPassword, String newPassword) {
        Account acc = login(studentId, oldPassword);
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        acc.setPasswordHash(hashPassword(newPassword));
        accountRepo.save(acc);
    }

    public boolean accountExists(String studentId) {
        return accountRepo.existsByStudentId(studentId);
    }
}
