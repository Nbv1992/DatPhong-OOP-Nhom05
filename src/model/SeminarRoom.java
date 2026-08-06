package model;

import model.TimeSlot;

/**
 * Phòng họp seminar - phí 50.000đ/giờ.
 */
public class SeminarRoom extends Room {

    public static final String TYPE = "Phòng họp seminar";

    public SeminarRoom(String roomId, String roomName, int floor, int maxCapacity) {
        super(roomId, roomName, floor, maxCapacity, new SeminarRoomFeePolicy());
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
     * Tạo SeminarRoom từ chuỗi CSV.
     */
    public static SeminarRoom fromCsvString(String csv) {
        String[] p = csv.split(",", -1);
        SeminarRoom r = new SeminarRoom(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()));
        r.setStatus(p[5].trim());
        return r;
    }
}
