package io.hhplus.tdd.point.repository;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
