package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.repository.PointHistory;
import io.hhplus.tdd.point.repository.TransactionType;
import io.hhplus.tdd.point.repository.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.hhplus.tdd.point.repository.TransactionType.CHARGE;
import static io.hhplus.tdd.point.repository.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    private final long id = 1L;

    private void getUserPoint(long amount) {
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));
    }

    @Test
    @DisplayName("포인트 조회 성공 테스트")
    void selectTest() {
        //사용자 포인트 설정
        long amount = 100L;
        getUserPoint(amount);

        //유저 정보에 담긴 포인트 검증
        assertThat(pointService.select(id).point()).isEqualTo(amount);
    }

    @Test
    @DisplayName("포인트 히스토리 조회 성공 테스트")
    void getPointHistoryTest() {
        //given
        PointHistory history1 = new PointHistory(1L, id, 100L, CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(2L, id, 50L, USE, System.currentTimeMillis());
        List<PointHistory> expectedHistory = List.of(history1, history2);

        //when
        when(pointHistoryTable.selectAllByUserId(anyLong())).thenReturn(expectedHistory);

        //then
        List<PointHistory> pointHistory = pointService.selectPointHistory(id);
        assertThat(pointHistory).isEqualTo(expectedHistory);
    }

    @Test
    @DisplayName("포인트 충전/사용할 때 포인트 히스토리에 추가되는지 검증")
    void isInsertPointHistoryTest() {

        //사용자 포인트 설정
        long amount = 100L;
        getUserPoint(amount);

        when(pointHistoryTable.insert(eq(id), anyLong(), any(), anyLong()))
                .thenAnswer(invocationOnMock -> {
                    long inId = invocationOnMock.getArgument(0);
                    long inAmount = invocationOnMock.getArgument(1);
                    TransactionType inType = invocationOnMock.getArgument(2);
                    long inUpdatedMills = System.currentTimeMillis();

                    return new PointHistory(1L, inId, inAmount, inType, inUpdatedMills);
                });

        //포인트 충전 검증
        pointService.charge(id, 100L);
        verify(pointHistoryTable, times(1)).insert(eq(id), anyLong(), eq(CHARGE), anyLong());
        pointService.charge(id, 200L);
        verify(pointHistoryTable, times(2)).insert(eq(id), anyLong(), eq(CHARGE), anyLong());

        //포인트 사용 검증
        pointService.use(id, 100L);
        verify(pointHistoryTable, times(1)).insert(eq(id), anyLong(), eq(USE), anyLong());
    }

    // -------------------------------------------------------------------------

    @Test
    @DisplayName("0원을 충전하려고 하는 경우 exception 발생")
    void chargeZeroAmountTest() {

        //사용자 포인트 설정
        getUserPoint(anyLong());

        when(pointService.charge(id, 0L)).thenThrow(new IllegalArgumentException("충전 금액이 0원입니다."));

        //0원을 충전하려고 하면 IllegalArgumentException 발생
        assertThatThrownBy(() -> pointService.charge(id, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액이 0원입니다.");
    }

    @Test
    @DisplayName("유효한 금액을 충전하려고 하는 경우 성공")
    void chargeValidAmountTest() {

        //사용자 포인트 설정
        long base = 50L;
        getUserPoint(base);

        //when
        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenAnswer(invocationOnMock -> {
            long invocationID = invocationOnMock.getArgument(0);
            long invocationAmount = invocationOnMock.getArgument(1);
            return new UserPoint(invocationID, invocationAmount, System.currentTimeMillis());
        });

        //충전 시도
        long chargeAmount = 100L;
        when(userPointTable.chargeAmount(base, chargeAmount)).thenReturn(base + chargeAmount);
        UserPoint userPoint = pointService.charge(id, chargeAmount);

        //기존 금액에 충전한 금액이 충전되어있어야 한다.
        assertThat(userPoint.point()).isEqualTo(base + chargeAmount);
    }

    @Test
    @DisplayName("잔여 포인트보다 많은 포인트를 사용하려고 할 경우 실패")
    void useAmountsGreaterThanBalanceTest() {

        //사용자 포인트 설정
        long base = 100L;
        getUserPoint(base);

        //when
        long useAmount = 150L;
        when(userPointTable.useAmount(base, useAmount))
                .thenThrow(new IllegalArgumentException("잔여 포인트보다 많이 사용할 수 없습니다."));

        //exception 발생 검증
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔여 포인트보다 많이 사용할 수 없습니다.");

        //잔여 포인트는 변하지 않아야 된다.
        assertThat(userPointTable.selectById(id).point()).isEqualTo(base);
    }

    //성공 TC
    @Test
    @DisplayName("잔여 포인트보다 적은 포인트를 사용하려고 할 경우 성공")
    void useAmountsLessThanBalanceTest() {

        //사용자 포인트 설정
        long base = 100L;
        getUserPoint(base);

        //when
        long useAmount = 50L;
        when(userPointTable.insertOrUpdate(id, useAmount)).thenAnswer(invocationOnMock -> {
           long inId = invocationOnMock.getArgument(0);
           long inAmount = invocationOnMock.getArgument(1);

           //포인트 변경
           long baseAmount = userPointTable.selectById(inId).point();
           long updateAmount = baseAmount - inAmount;
           return new UserPoint(inId, updateAmount, System.currentTimeMillis());
        });
        when(userPointTable.useAmount(base, useAmount)).thenReturn(base - useAmount);

        //포인트 사용
        UserPoint userPoint = pointService.use(id, useAmount);

        //잔여 포인트 -= 포인트 검증
        assertThat(userPoint.point()).isEqualTo(base - useAmount);
    }
}