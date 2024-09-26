package io.hhplus.tdd.database;

import io.hhplus.tdd.point.repository.UserPoint;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
@Component
public class UserPointTable {

    private final ConcurrentHashMap<Long, UserPoint> table = new ConcurrentHashMap<>();

    public UserPointTable() {
        // 초기 데이터 설정
        table.put(1L, new UserPoint(1L, 100L, System.currentTimeMillis()));
        table.put(2L, new UserPoint(2L, 50L, System.currentTimeMillis()));
    }

    public UserPoint selectById(Long id) {
        throttle(200);
        return table.getOrDefault(id, UserPoint.empty(id));
    }

    public UserPoint insertOrUpdate(long id, long amount) {
        throttle(300);
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());
        table.put(id, userPoint);
        return userPoint;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }

    public long chargeAmount(long base, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액이 0원입니다.");
        return base + amount;
    }

    public long useAmount(long base, long amount) {
        long updateAmount = base - amount;
        if (updateAmount < 0) throw new IllegalArgumentException("잔여 포인트보다 많이 사용할 수 없습니다.");
        return updateAmount;
    }
}
