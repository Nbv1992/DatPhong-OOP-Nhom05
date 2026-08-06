package model;

import interfaces.RoomFeePolicy;

/**
 * Chính sách tính phí cho phòng có máy chiếu: 20.000đ/giờ.
 */
public class ProjectorRoomFeePolicy implements RoomFeePolicy {

    private static final double FEE_PER_HOUR = 20_000.0;

    @Override
    public double calculateFee(double hours) {
        return hours * FEE_PER_HOUR;
    }

    @Override
    public String getFeeDescription() {
        return "20.000đ/giờ";
    }
}
