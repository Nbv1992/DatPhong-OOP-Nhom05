package model;

/**
 * Tài khoản đăng nhập.
 * Lưu mã sinh viên + password hash (SHA-256) + email + role.
 *
 * Role có 2 giá trị:
 *   ADMIN   – quản trị viên (quản lý phòng, quản lý SV, xem tất cả lịch)
 *   HOCVIEN – sinh viên     (đặt phòng, hủy phòng của mình, xem lịch của mình)
 */
public class Account {

    public static final String ROLE_ADMIN   = "ADMIN";
    public static final String ROLE_HOCVIEN = "HOCVIEN";

    private String studentId;    // Mã sinh viên (username)
    private String passwordHash; // SHA-256 hash
    private String email;        // Email để reset password
    private String role;         // ADMIN hoặc HOCVIEN

    public Account(String studentId, String passwordHash, String email, String role) {
        this.studentId    = studentId;
        this.passwordHash = passwordHash;
        this.email        = email;
        this.role         = (role != null && role.equalsIgnoreCase(ROLE_ADMIN)) ? ROLE_ADMIN : ROLE_HOCVIEN;
    }

    // ==================== Getters / Setters ====================

    public String getStudentId()    { return studentId; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail()        { return email; }
    public String getRole()         { return role; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEmail(String email)               { this.email = email; }
    public void setRole(String role)                 { this.role = role; }

    public boolean isAdmin()   { return ROLE_ADMIN.equals(role); }
    public boolean isHocVien() { return ROLE_HOCVIEN.equals(role); }

    // ==================== CSV ====================

    /** Định dạng: studentId,passwordHash,email,role */
    public String toCsvString() {
        return String.join(",", studentId, passwordHash, email, role);
    }

    /** Parse từ CSV — tương thích ngược với file cũ chỉ có 3 cột (mặc định HOCVIEN) */
    public static Account fromCsvString(String csv) {
        String[] p = csv.split(",", -1);
        if (p.length < 3) return null;
        String role = (p.length >= 4) ? p[3].trim() : ROLE_HOCVIEN;
        return new Account(p[0].trim(), p[1].trim(), p[2].trim(), role);
    }

    @Override
    public String toString() {
        return "Account{id=" + studentId + ", role=" + role + ", email=" + email + "}";
    }
}
