package repository;

import model.Student;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lưu trữ và đọc dữ liệu sinh viên từ file.
 * File định dạng CSV: studentId,fullName,phone,email,className
 */
public class StudentRepository {

    private final String filePath;
    private final List<Student> students = new ArrayList<>();

    public StudentRepository(String filePath) {
        this.filePath = filePath;
        loadFromFile();
    }

    // ==================== File I/O ====================

    /**
     * Đọc danh sách sinh viên từ file CSV.
     */
    private void loadFromFile() {
        students.clear();
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    students.add(Student.fromCsvString(line));
                } catch (Exception e) {
                    System.err.println("[Repository] Bỏ qua dòng lỗi trong file sinh viên: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[Repository] Không thể đọc file sinh viên: " + e.getMessage());
        }
    }

    /**
     * Ghi toàn bộ danh sách sinh viên ra file CSV.
     */
    public void saveToFile() {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write("# studentId,fullName,phone,email,className");
            bw.newLine();
            for (Student s : students) {
                bw.write(s.toCsvString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[Repository] Không thể ghi file sinh viên: " + e.getMessage());
        }
    }

    // ==================== CRUD ====================

    /**
     * Lấy toàn bộ danh sách sinh viên.
     */
    public List<Student> findAll() {
        return new ArrayList<>(students);
    }

    /**
     * Tìm sinh viên theo mã.
     */
    public Optional<Student> findById(String studentId) {
        return students.stream()
            .filter(s -> s.getStudentId().equalsIgnoreCase(studentId))
            .findFirst();
    }

    /**
     * Thêm sinh viên mới.
     */
    public void save(Student student) {
        // Nếu đã tồn tại thì cập nhật
        students.removeIf(s -> s.getStudentId().equalsIgnoreCase(student.getStudentId()));
        students.add(student);
        saveToFile();
    }

    /**
     * Xóa sinh viên theo mã.
     */
    public boolean deleteById(String studentId) {
        boolean removed = students.removeIf(s -> s.getStudentId().equalsIgnoreCase(studentId));
        if (removed) saveToFile();
        return removed;
    }

    /**
     * Kiểm tra mã sinh viên đã tồn tại chưa.
     */
    public boolean existsById(String studentId) {
        return students.stream().anyMatch(s -> s.getStudentId().equalsIgnoreCase(studentId));
    }
}
