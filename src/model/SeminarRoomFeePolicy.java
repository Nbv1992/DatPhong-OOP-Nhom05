package model;

import interfaces.RoomFeePolicy;

/**
 * Chính sách tính phí cho phòng họp seminar: 50.000đ/giờ.
 */
public class SeminarRoomFeePolicy implements RoomFeePolicy {

    private static final double FEE_PER_HOUR = 50_000.0;

    @Override
    public double calculateFee(double hours) {
        return hours * FEE_PER_HOUR;
    }

    @Override
    public String getFeeDescription() {
        return "50.000đ/giờ";
    }
}
