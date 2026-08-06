package model;

/**
 * Lớp đại diện cho sinh viên trong hệ thống đặt phòng.
 * Kế thừa từ User, bổ sung thêm thông tin lớp học.
 */
public class Student extends User {

    // Tên lớp học của sinh viên
    private String className;

    public Student(String studentId, String fullName, String phone, String email, String className) {
        super(studentId, fullName, phone, email);
        this.className = className;
    }

    // ==================== Getters / Setters ====================

    public String getStudentId() {
        return getUserId();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    // ==================== Override ====================

    @Override
    public String getRole() {
        return "Sinh viên";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Lớp: %s | SĐT: %s | Email: %s",
            getStudentId(), getFullName(), className, getPhone(), getEmail());
    }

    /**
     * Xuất dữ liệu dạng chuỗi CSV để lưu file.
     * Định dạng: studentId,fullName,phone,email,className
     */
    public String toCsvString() {
        return String.join(",", getStudentId(), getFullName(), getPhone(), getEmail(), className);
    }

    /**
     * Tạo Student từ chuỗi CSV.
     * @param csv chuỗi CSV định dạng: studentId,fullName,phone,email,className
     */
    public static Student fromCsvString(String csv) {
        String[] parts = csv.split(",", -1);
        return new Student(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim());
    }
}
