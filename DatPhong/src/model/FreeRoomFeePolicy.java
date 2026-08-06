package model;

import interfaces.RoomFeePolicy;

/**
 * Chính sách miễn phí áp dụng cho phòng thường.
 */
public class FreeRoomFeePolicy implements RoomFeePolicy {

    @Override
    public double calculateFee(double hours) {
        return 0.0; // miễn phí
    }

    @Override
    public String getFeeDescription() {
        return "Miễn phí";
    }
}
