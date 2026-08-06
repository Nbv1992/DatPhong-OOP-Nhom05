package service;

import exception.RoomNotFoundException;
import model.Room;
import repository.RoomRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ liên quan đến phòng học nhóm.
 */
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Lấy toàn bộ danh sách phòng.
     */
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Lấy danh sách phòng đang hoạt động.
     */
    public List<Room> getActiveRooms() {
        return roomRepository.findAllActive();
    }

    /**
     * Tìm phòng theo mã. Ném exception nếu không tìm thấy.
     */
    public Room getRoomById(String roomId) throws RoomNotFoundException {
        Optional<Room> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) {
            throw new RoomNotFoundException(roomId);
        }
        return opt.get();
    }

    /**
     * Tìm phòng theo loại phòng.
     */
    public List<Room> getRoomsByType(String roomType) {
        return roomRepository.findByType(roomType);
    }

    /**
     * Cập nhật trạng thái phòng (hoạt động / bảo trì).
     */
    public void updateRoomStatus(String roomId, String newStatus) throws RoomNotFoundException {
        Room room = getRoomById(roomId);
        room.setStatus(newStatus);
        roomRepository.save(room);
    }

    /**
     * Thêm phòng mới vào hệ thống.
     */
    public void addRoom(Room room) {
        if (roomRepository.existsById(room.getRoomId())) {
            throw new IllegalArgumentException("Mã phòng " + room.getRoomId() + " đã tồn tại.");
        }
        roomRepository.save(room);
    }

    /**
     * Kiểm tra phòng có tồn tại không.
     */
    public boolean exists(String roomId) {
        return roomRepository.existsById(roomId);
    }
}
