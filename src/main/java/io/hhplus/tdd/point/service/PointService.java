package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private final UserPointTable userPointTable;

    @Autowired
    public PointService(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    public UserPoint charge(long id, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액이 0원입니다.");
        return userPointTable.insertOrUpdate(id, amount);
    }

    public UserPoint use(long id, long amount) {
        long base = userPointTable.selectById(id).point();

        long updateAmount = base - amount;
        if (updateAmount < 0) throw new IllegalArgumentException("잔여 포인트보다 많이 사용할 수 없습니다.");

        return userPointTable.insertOrUpdate(id, updateAmount);
    }

    public UserPoint select(long id) {
        //TODO function isValidId(id)
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null || userPoint.id() == -1) {
            throw new IllegalArgumentException("아이디가 존재하지 않습니다.");
        }
        return userPoint;
    }
}
