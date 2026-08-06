package repository;

import model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lưu trữ và đọc dữ liệu phòng học nhóm từ file.
 * File định dạng CSV: roomId,roomName,floor,maxCapacity,roomType,status
 */
public class RoomRepository {

    private final String filePath;
    private final List<Room> rooms = new ArrayList<>();

    public RoomRepository(String filePath) {
        this.filePath = filePath;
        loadFromFile();
    }

    // ==================== File I/O ====================

    /**
     * Đọc danh sách phòng từ file CSV.
     * Tự động phân loại đúng subclass (NormalRoom, ProjectorRoom, SeminarRoom).
     */
    private void loadFromFile() {
        rooms.clear();
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    Room room = parseRoom(line);
                    if (room != null) rooms.add(room);
                } catch (Exception e) {
                    System.err.println("[Repository] Bỏ qua dòng lỗi trong file phòng: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[Repository] Không thể đọc file phòng: " + e.getMessage());
        }
    }

    /**
     * Parse một dòng CSV thành đối tượng Room phù hợp.
     */
    private Room parseRoom(String csv) {
        String[] parts = csv.split(",", -1);
        if (parts.length < 6) return null;

        String type = parts[4].trim();
        switch (type) {
            case NormalRoom.TYPE:
                return NormalRoom.fromCsvString(csv);
            case ProjectorRoom.TYPE:
                return ProjectorRoom.fromCsvString(csv);
            case SeminarRoom.TYPE:
                return SeminarRoom.fromCsvString(csv);
            default:
                System.err.println("[Repository] Loại phòng không xác định: " + type);
                return null;
        }
    }

    /**
     * Ghi toàn bộ danh sách phòng ra file CSV.
     */
    public void saveToFile() {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write("# roomId,roomName,floor,maxCapacity,roomType,status");
            bw.newLine();
            for (Room r : rooms) {
                bw.write(r.toCsvString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[Repository] Không thể ghi file phòng: " + e.getMessage());
        }
    }

    // ==================== CRUD ====================

    public List<Room> findAll() {
        return new ArrayList<>(rooms);
    }

    public Optional<Room> findById(String roomId) {
        return rooms.stream()
            .filter(r -> r.getRoomId().equalsIgnoreCase(roomId))
            .findFirst();
    }

    /**
     * Tìm phòng theo loại phòng.
     */
    public List<Room> findByType(String roomType) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getRoomType().equalsIgnoreCase(roomType)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Chỉ lấy phòng đang hoạt động.
     */
    public List<Room> findAllActive() {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.isActive()) result.add(r);
        }
        return result;
    }

    public void save(Room room) {
        rooms.removeIf(r -> r.getRoomId().equalsIgnoreCase(room.getRoomId()));
        rooms.add(room);
        saveToFile();
    }

    public boolean existsById(String roomId) {
        return rooms.stream().anyMatch(r -> r.getRoomId().equalsIgnoreCase(roomId));
    }
}
