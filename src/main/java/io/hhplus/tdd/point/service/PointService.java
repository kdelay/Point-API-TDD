package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint charge(long id, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액이 0원입니다.");

        long base = userPointTable.selectById(id).point();
        long updateAmount = base + amount;

        //포인트 충전 내역 저장
        PointHistory insert = pointHistoryTable.insert(id, amount, CHARGE, System.currentTimeMillis());
        System.out.println("insert = " + insert);

        return userPointTable.insertOrUpdate(id, updateAmount);
    }

    public UserPoint use(long id, long amount) {
        long base = userPointTable.selectById(id).point();

        long updateAmount = base - amount;
        if (updateAmount < 0) throw new IllegalArgumentException("잔여 포인트보다 많이 사용할 수 없습니다.");

        //포인트 사용 내역 저장
        PointHistory use = pointHistoryTable.insert(id, amount, USE, System.currentTimeMillis());
        System.out.println("use = " + use);

        return userPointTable.insertOrUpdate(id, updateAmount);
    }

    public UserPoint select(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> selectPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}
