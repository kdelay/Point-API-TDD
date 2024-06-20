package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.repository.PointHistory;
import io.hhplus.tdd.point.repository.TransactionType;
import io.hhplus.tdd.point.repository.UserPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static io.hhplus.tdd.point.repository.TransactionType.CHARGE;
import static io.hhplus.tdd.point.repository.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        //모의 객체 초기화
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        // 테스트가 끝난 후 자원 해제
        mocks.close();
    }

    // -------------------------------------------------------------------------

    private void getUserPoint(long id, long amount) {
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));
    }

    // -------------------------------------------------------------------------

    //포인트 조회
    //성공 TC
    @Test
    @DisplayName("포인트를 조회하는 경우")
    void selectTest() {

        //사용자 포인트 설정
        long id = 1L;
        long amount = 100L;
        getUserPoint(id, amount);

        //유저 정보에 담긴 포인트 검증
        assertThat(pointService.select(id).point()).isEqualTo(amount);
    }

    // -------------------------------------------------------------------------

    //포인트 충전/이용 내역 조회
    @Test
    @DisplayName("포인트 히스토리 조회")
    void getPointHistoryTest() {
        // given
        long userId = 1L;
        PointHistory history1 = new PointHistory(1L, userId, 100L, CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(2L, userId, 50L, USE, System.currentTimeMillis());
        List<PointHistory> expectedHistory = List.of(history1, history2);

        // when
        when(pointHistoryTable.selectAllByUserId(anyLong())).thenReturn(expectedHistory);

        // then
        List<PointHistory> pointHistory = pointService.selectPointHistory(userId);
        assertThat(pointHistory).isEqualTo(expectedHistory);
    }

    @Test
    @DisplayName("포인트 충전/사용할 때 포인트 히스토리에 추가되는지 검증")
    void isInsertPointHistoryTest() {

        //사용자 포인트 설정
        long id = 1L;
        long amount = 100L;
        getUserPoint(id, amount);

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

    //포인트 충전 - charge
    //실패 TC
    @Test
    @DisplayName("0원을 충전하려고 하는 경우")
    void chargeZeroAmountTest() {

        //사용자 포인트 설정
        long id = 1L;
        getUserPoint(id, anyLong());

        //0원을 충전하려고 하면 IllegalArgumentException 발생
        assertThatThrownBy(() -> pointService.charge(id, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액이 0원입니다.");
    }

    //성공 TC
    @Test
    @DisplayName("유효한 금액을 충전하려고 하는 경우")
    void chargeValidAmountTest() {

        //사용자 포인트 설정
        long id = 1L;
        long base = 50L;
        getUserPoint(id, base);

        //when
        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenAnswer(invocationOnMock -> {
            long invocationID = invocationOnMock.getArgument(0);
            long invocationAmount = invocationOnMock.getArgument(1);
            return new UserPoint(invocationID, invocationAmount, System.currentTimeMillis());
        });

        //충전 시도
        long chargeAmount = 100L;
        UserPoint userPoint = pointService.charge(id, chargeAmount);

        //기존 금액에 충전한 금액이 충전되어있어야 한다.
        assertThat(userPoint.point()).isEqualTo(base + chargeAmount);
    }

    @Test
    @DisplayName("포인트 충전 + 사용 실패")
    void chargeAndUseFailTest() {

        //사용자 1에 대한 포인트 정보 생성
        long id = 1L;
        long base = 50L;
        getUserPoint(id, base);

        //when
        when(userPointTable.insertOrUpdate(anyLong(), longThat(amount -> amount > 0))).thenAnswer(invocationOnMock -> {
            long inId = invocationOnMock.getArgument(0);
            long inAmount = invocationOnMock.getArgument(1);
            return new UserPoint(inId, inAmount, System.currentTimeMillis());
        });

        //포인트 충전 시도
        long chargeAmount = 100L;
        long amountAfterCharge = pointService.charge(id, chargeAmount).point();

        //포인트 충전이 잘 되었는지 검증
        assertThat(amountAfterCharge).isEqualTo(base + chargeAmount);

        //충전된 포인트 저장
        getUserPoint(id, amountAfterCharge);

        //잔여 포인트보다 많이 사용할 수 없다.
        long useAmount = 200L;
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔여 포인트보다 많이 사용할 수 없습니다.");
    }

    // -------------------------------------------------------------------------

    //포인트 사용 - use
    //실패 TC
    @Test
    @DisplayName("잔여 포인트보다 많은 포인트를 사용하려고 할 때") //사용 포인트 > 잔여 포인트
    void useAmountsGreaterThanBalanceTest() {

        //사용자 포인트 설정
        long id = 1L;
        long base = 100L;
        getUserPoint(id, base);

        //when
        long useAmount = 150L;
        when(userPointTable.insertOrUpdate(eq(id), eq(useAmount))).thenThrow(IllegalArgumentException.class);

        //exception 발생 검증
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔여 포인트보다 많이 사용할 수 없습니다.");

        //잔여 포인트는 변하지 않아야 된다.
        assertThat(userPointTable.selectById(id).point()).isEqualTo(base);
    }

    //성공 TC
    @Test
    @DisplayName("잔여 포인트보다 적은 포인트를 사용하려고 할 때") //사용 포인트 < 잔여 포인트
    void useAmountsLessThanBalanceTest() {

        //사용자 포인트 설정
        long id = 1L;
        long base = 100L;
        getUserPoint(id, base);

        //when
        long useAmount = 50L;
        when(userPointTable.insertOrUpdate(eq(id), eq(useAmount))).thenAnswer(invocationOnMock -> {
           long inId = invocationOnMock.getArgument(0);
           long inAmount = invocationOnMock.getArgument(1);

           //포인트 변경
           long baseAmount = userPointTable.selectById(inId).point();
           long updateAmount = baseAmount - inAmount;
           return new UserPoint(inId, updateAmount, System.currentTimeMillis());
        });

        //포인트 사용
        UserPoint userPoint = pointService.use(id, useAmount);

        //잔여 포인트 -= 포인트 검증
        assertThat(userPoint.point()).isEqualTo(base - useAmount);
    }
}