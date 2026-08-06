# Hệ thống quản lý đặt phòng học nhóm

Bài tập lớn OOP — Nhóm 05. Hệ thống đặt phòng học nhóm trong trường: đăng ký tài khoản, đăng nhập, xem phòng, đặt phòng, hủy lịch và tính phí sử dụng.

---

## Yêu cầu cài đặt

- **Java 17+** (đã có sẵn trong IntelliJ IDEA)
- **IntelliJ IDEA** 2023+ (Community hoặc Ultimate)
- **Trình duyệt** Chrome / Edge / Firefox

> Không cần cài thêm Maven, Gradle, Node.js hay bất kỳ thư viện nào khác.  
> Toàn bộ project dùng **Java thuần** + **HTML/CSS/JS** tĩnh.

---

## Cấu trúc project

```
DatPhong/
├── src/
│   ├── Main.java                  ← Entry point, khởi động HTTP server
│   ├── api/                       ← REST API handlers
│   │   ├── ApiServer.java
│   │   ├── AuthHandler.java       ← /api/auth/*
│   │   ├── RoomHandler.java       ← /api/rooms/*
│   │   ├── StudentHandler.java    ← /api/students/*
│   │   └── BookingHandler.java    ← /api/bookings/*
│   ├── model/                     ← Các class OOP
│   │   ├── User.java  (abstract)
│   │   ├── Student.java
│   │   ├── Room.java  (abstract)
│   │   ├── NormalRoom.java
│   │   ├── ProjectorRoom.java
│   │   ├── SeminarRoom.java
│   │   ├── Booking.java
│   │   ├── BookingDetail.java
│   │   ├── TimeSlot.java
│   │   └── Account.java
│   ├── service/                   ← Business logic
│   │   ├── AuthService.java
│   │   ├── RoomService.java
│   │   ├── StudentService.java
│   │   └── BookingService.java
│   ├── repository/                ← Đọc/ghi file CSV
│   │   ├── RoomRepository.java
│   │   ├── StudentRepository.java
│   │   ├── BookingRepository.java
│   │   └── AccountRepository.java
│   ├── interfaces/
│   │   ├── Bookable.java
│   │   ├── RoomFeePolicy.java
│   │   └── BookingValidator.java
│   ├── exception/                 ← Custom exceptions
│   └── utils/
├── data/                          ← Dữ liệu lưu dạng CSV
│   ├── rooms.txt
│   ├── students.txt
│   ├── bookings.txt
│   └── accounts.txt
└── web/                           ← Giao diện web
    ├── index.html
    ├── login.html
    ├── style.css
    └── app.js
```

---

## 1. Mở project trong IntelliJ

1. Mở **IntelliJ IDEA**
2. Chọn **File → Open** → chọn thư mục `DatPhong`
3. Đợi IntelliJ index xong

---

## 2. Cấu hình Working Directory

Vào **Run → Edit Configurations → Main**  
Đặt **Working directory** là:

```
C:\Users\Administrator\deptraicogisai\DatPhong
```

> Bắt buộc phải đặt đúng — server cần đọc thư mục `data/` và `web/` từ đây.

---

## 3. Chạy project

Nhấn **Run** (▶) hoặc `Shift+F10` để chạy `Main.java`.

Console sẽ hiện:

```
╔══════════════════════════════════════════════════════╗
║  Server đang chạy tại: http://localhost:8080          ║
║  Mở trình duyệt và truy cập địa chỉ trên để dùng.   ║
╚══════════════════════════════════════════════════════╝
```

Trình duyệt sẽ **tự động mở** `http://localhost:8080/login.html`.  
Nếu không tự mở, truy cập thủ công vào địa chỉ trên.

---

## 4. Tài khoản mặc định

| Role | Tài khoản | Mật khẩu |
|------|-----------|----------|
| Admin | `admin` | `Admin@123` |
| Học viên | Tự đăng ký | Tự đặt (≥ 6 ký tự) |

---

## 5. Chức năng theo role

### 👑 Admin
| Chức năng | Mô tả |
|-----------|-------|
| Quản lý phòng | Xem, thêm phòng, đổi trạng thái (hoạt động / bảo trì) |
| Quản lý sinh viên | Xem danh sách, thêm sinh viên mới |
| Tất cả lịch đặt | Xem, lọc, hủy bất kỳ lịch đặt nào |
| Dashboard | Thống kê tổng quan toàn hệ thống |

### 🎓 Học viên
| Chức năng | Mô tả |
|-----------|-------|
| Xem phòng | Danh sách phòng, lọc theo loại, tìm kiếm |
| Xem lịch trống | Kiểm tra lịch đã đặt của từng phòng |
| Đặt phòng | Chọn phòng, thời gian, số người, mục đích |
| Lịch đặt của tôi | Xem, xem chi tiết, hủy lịch của mình |
| Thông tin cá nhân | Cập nhật họ tên, SĐT, email, lớp |
| Đổi mật khẩu | Đổi mật khẩu khi đã đăng nhập |

---

## 6. Business rules

- ❌ Không đặt phòng đang **bảo trì**
- ❌ Không đặt phòng **trùng lịch**
- ❌ Thời gian kết thúc phải **sau** thời gian bắt đầu
- ❌ Số người không được **vượt sức chứa** phòng
- ❌ Sinh viên không được đặt quá **4 giờ/ngày**
- ❌ Chỉ được hủy **lịch của chính mình**
- ❌ Không được hủy lịch **đã hủy trước đó**

---

## 7. Bảng giá phòng

| Loại phòng | Phí |
|------------|-----|
| Phòng thường | Miễn phí |
| Phòng có máy chiếu | 20.000đ / giờ |
| Phòng họp seminar | 50.000đ / giờ |

---

## 8. API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/auth/login` | Đăng nhập |
| `POST` | `/api/auth/register` | Đăng ký tài khoản |
| `POST` | `/api/auth/reset-password` | Reset mật khẩu |
| `POST` | `/api/auth/change-password` | Đổi mật khẩu |
| `GET` | `/api/rooms` | Danh sách phòng |
| `GET` | `/api/rooms?active=true` | Phòng đang hoạt động |
| `POST` | `/api/rooms` | Thêm phòng (admin) |
| `PUT` | `/api/rooms/{id}/status` | Đổi trạng thái phòng |
| `GET` | `/api/students` | Danh sách sinh viên |
| `GET` | `/api/students/{id}` | Lấy sinh viên theo mã |
| `POST` | `/api/students` | Thêm sinh viên |
| `PUT` | `/api/students/{id}` | Cập nhật thông tin SV |
| `GET` | `/api/bookings` | Tất cả lịch đặt |
| `GET` | `/api/bookings?studentId=xxx` | Lịch đặt của SV |
| `GET` | `/api/bookings?roomId=xxx` | Lịch đặt của phòng |
| `POST` | `/api/bookings` | Tạo lịch đặt |
| `DELETE` | `/api/bookings/{id}` | Hủy lịch đặt |
| `GET` | `/api/bookings/{id}/detail` | Chi tiết lịch đặt |

---

## 9. Áp dụng OOP

| Nguyên lý | Ví dụ trong project |
|-----------|-------------------|
| **Encapsulation** | Tất cả thuộc tính `private`, truy cập qua getter/setter |
| **Inheritance** | `Student` kế thừa `User`; `NormalRoom`, `ProjectorRoom`, `SeminarRoom` kế thừa `Room` |
| **Polymorphism** | `room.getFeePolicy().calculateFee(hours)` — mỗi loại phòng tính phí khác nhau |
| **Abstraction** | `Room` (abstract class), `User` (abstract class) |
| **Interface** | `Bookable`, `RoomFeePolicy`, `BookingValidator` |
| **Collections** | `List`, `Map` trong Repository, Service |
| **Exception Handling** | 11 custom exception classes, try-catch trong toàn bộ service |
| **File I/O** | CSV đọc/ghi trong `RoomRepository`, `StudentRepository`, `BookingRepository`, `AccountRepository` |

---

## 10. Phân công công việc

| Thành viên | Công việc |
|------------|-----------|
| Thành viên 1 | Model classes, Interfaces, Exception classes |
| Thành viên 2 | Repository layer (File I/O), Utils |
| Thành viên 3 | Service layer (Business logic, Validators) |
| Thành viên 4 | API Server, Handler classes |
| Thành viên 5 | Frontend (HTML/CSS/JS), Auth system |

---

## 11. Tình huống test

### ✅ Test 1 — Đặt phòng thành công
1. Đăng nhập học viên → Đặt phòng
2. Chọn phòng còn trống, thời gian hợp lệ, số người ≤ sức chứa
3. **Kết quả:** Đặt phòng thành công, hiện mã đặt phòng

### ❌ Test 2 — Đặt phòng đang bảo trì
1. Admin đặt trạng thái phòng thành "Đang bảo trì"
2. Học viên cố đặt phòng đó
3. **Kết quả:** Lỗi "Phòng đang bảo trì, không thể đặt"

### ❌ Test 3 — Đặt phòng trùng lịch
1. Đặt phòng P001 từ 08:00–10:00
2. Đặt lại phòng P001 từ 09:00–11:00 (trùng)
3. **Kết quả:** Lỗi "Phòng đã được đặt trong khoảng thời gian này"

### ❌ Test 4 — Vượt quá sức chứa
1. Phòng sức chứa 10 người
2. Đặt với 15 người
3. **Kết quả:** Lỗi "Số người vượt quá sức chứa tối đa"

### ❌ Test 5 — Vượt 4 giờ/ngày
1. Đặt phòng 3 giờ buổi sáng
2. Đặt thêm 2 giờ buổi chiều cùng ngày (tổng > 4h)
3. **Kết quả:** Lỗi "Vượt quá giới hạn 4 giờ/ngày"

### ✅ Test 6 — Hủy lịch của mình
1. Học viên vào "Lịch đặt của tôi"
2. Nhấn hủy lịch đang "Đã đặt"
3. **Kết quả:** Trạng thái chuyển thành "Đã hủy"

### ❌ Test 7 — Hủy lịch không thuộc về mình (Admin test)
1. Admin hủy lịch với `studentId` sai
2. **Kết quả:** Lỗi "Bạn không có quyền hủy lịch đặt này"

---

*EduRoom v1.0 — Nhóm 05*
# DatPhong-OOP-Nhom05
