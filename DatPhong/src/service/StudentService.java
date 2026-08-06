package service;

import exception.StudentNotFoundException;
import model.Student;
import repository.StudentRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ liên quan đến sinh viên.
 */
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Lấy toàn bộ danh sách sinh viên.
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Tìm sinh viên theo mã. Ném exception nếu không tìm thấy.
     */
    public Student getStudentById(String studentId) throws StudentNotFoundException {
        Optional<Student> opt = studentRepository.findById(studentId);
        if (opt.isEmpty()) {
            throw new StudentNotFoundException(studentId);
        }
        return opt.get();
    }

    /**
     * Thêm sinh viên mới vào hệ thống.
     * @throws IllegalArgumentException nếu mã sinh viên đã tồn tại hoặc dữ liệu không hợp lệ
     */
    public void addStudent(Student student) {
        if (studentRepository.existsById(student.getStudentId())) {
            throw new IllegalArgumentException("Mã sinh viên " + student.getStudentId() + " đã tồn tại trong hệ thống.");
        }
        validateStudent(student);
        studentRepository.save(student);
    }

    /**
     * Cập nhật thông tin sinh viên.
     */
    public void updateStudent(Student student) throws StudentNotFoundException {
        if (!studentRepository.existsById(student.getStudentId())) {
            throw new StudentNotFoundException(student.getStudentId());
        }
        validateStudent(student);
        studentRepository.save(student);
    }

    /**
     * Kiểm tra tính hợp lệ của thông tin sinh viên.
     */
    private void validateStudent(Student student) {
        if (student.getStudentId() == null || student.getStudentId().isBlank()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống.");
        }
        if (student.getFullName() == null || student.getFullName().isBlank()) {
            throw new IllegalArgumentException("Họ tên sinh viên không được để trống.");
        }
        if (student.getPhone() == null || !student.getPhone().matches("^0\\d{9,10}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải có 10-11 chữ số, bắt đầu bằng 0).");
        }
        if (student.getEmail() == null || !student.getEmail().matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (student.getClassName() == null || student.getClassName().isBlank()) {
            throw new IllegalArgumentException("Tên lớp không được để trống.");
        }
    }

    /**
     * Kiểm tra sinh viên có tồn tại không.
     */
    public boolean exists(String studentId) {
        return studentRepository.existsById(studentId);
    }
}
