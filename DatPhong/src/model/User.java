package model;

/**
 * Lớp trừu tượng đại diện cho người dùng chung trong hệ thống.
 * Tất cả người dùng đều có các thông tin cơ bản: mã, họ tên, số điện thoại, email.
 */
public abstract class User {

    // Mã định danh người dùng
    private String userId;

    // Họ và tên
    private String fullName;

    // Số điện thoại
    private String phone;

    // Email
    private String email;

    public User(String userId, String fullName, String phone, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    // ==================== Getters / Setters ====================

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // ==================== Abstract Methods ====================

    /**
     * Trả về vai trò của người dùng trong hệ thống.
     * @return chuỗi mô tả vai trò
     */
    public abstract String getRole();

    @Override
    public String toString() {
        return String.format("[%s] %s | SĐT: %s | Email: %s", userId, fullName, phone, email);
    }
}
