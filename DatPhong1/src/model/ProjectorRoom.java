package model;

import model.TimeSlot;

/**
 * Phòng có máy chiếu - phí 20.000đ/giờ.
 */
public class ProjectorRoom extends Room {

    public static final String TYPE = "Phòng có máy chiếu";

    public ProjectorRoom(String roomId, String roomName, int floor, int maxCapacity) {
        super(roomId, roomName, floor, maxCapacity, new ProjectorRoomFeePolicy());
    }

    @Override
    public String getRoomType() {
        return TYPE;
    }

    @Override
    public boolean isAvailable(TimeSlot slot) {
        return isActive();
    }

    @Override
    public String toCsvString() {
        return String.join(",", getRoomId(), getRoomName(),
            String.valueOf(getFloor()), String.valueOf(getMaxCapacity()),
            TYPE, getStatus());
    }

    /**
     * Tạo ProjectorRoom từ chuỗi CSV.
     */
    public static ProjectorRoom fromCsvString(String csv) {
        String[] p = csv.split(",", -1);
        ProjectorRoom r = new ProjectorRoom(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()));
        r.setStatus(p[5].trim());
        return r;
    }
}
