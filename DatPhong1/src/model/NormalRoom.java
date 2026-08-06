package model;

import model.TimeSlot;

/**
 * Phòng thường - miễn phí sử dụng.
 */
public class NormalRoom extends Room {

    public static final String TYPE = "Phòng thường";

    public NormalRoom(String roomId, String roomName, int floor, int maxCapacity) {
        super(roomId, roomName, floor, maxCapacity, new FreeRoomFeePolicy());
    }

    @Override
    public String getRoomType() {
        return TYPE;
    }

    /**
     * Phòng luôn sẵn sàng để kiểm tra lịch (việc kiểm tra trùng lịch do BookingService đảm nhiệm).
     */
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
     * Tạo NormalRoom từ chuỗi CSV.
     */
    public static NormalRoom fromCsvString(String csv) {
        String[] p = csv.split(",", -1);
        NormalRoom r = new NormalRoom(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()));
        r.setStatus(p[5].trim());
        return r;
    }
}
