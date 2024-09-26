package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.repository.PointHistory;
import io.hhplus.tdd.point.repository.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.hhplus.tdd.point.repository.TransactionType.CHARGE;
import static io.hhplus.tdd.point.repository.TransactionType.USE;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint select(long id) {
        return userPointTable.selectById(id);
    }

    public synchronized UserPoint charge(long id, long amount) {
        long base = userPointTable.selectById(id).point();
        long updateAmount = userPointTable.chargeAmount(base, amount);

        //포인트 충전 내역 저장
        pointHistoryTable.insert(id, amount, CHARGE, System.currentTimeMillis());
        return userPointTable.insertOrUpdate(id, updateAmount);
    }

    public UserPoint use(long id, long amount) {
        long base = userPointTable.selectById(id).point();
        long updateAmount = userPointTable.useAmount(base, amount);

        //포인트 사용 내역 저장
        pointHistoryTable.insert(id, amount, USE, System.currentTimeMillis());
        return userPointTable.insertOrUpdate(id, updateAmount);
    }

    public List<PointHistory> selectPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}
