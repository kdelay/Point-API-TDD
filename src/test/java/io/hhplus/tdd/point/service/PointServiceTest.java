package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@AutoConfigureMockMvc
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

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

    @Test
    @DisplayName("아이디가 없는 경우")
    void isEmptyId() {
        when(userPointTable.selectById(anyLong())).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> pointService.charge(1L, anyLong()), "아이디가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("아이디가 유효하지 않은 경우")
    void isInvalidId() {
        long id = -1L; //유효하지 않은 ID
        when(userPointTable.selectById(id)).thenReturn(UserPoint.empty(id));
        assertThrows(IllegalArgumentException.class,
                () -> pointService.charge(id, anyLong()), "아이디가 존재하지 않습니다.");
    }

    private void getUserPoint(long id, long amount) {
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));
    }

    //포인트 충전 - charge
    //실패 TC
    @Test
    @DisplayName("0원을 충전하려고 하는 경우")
    void chargeZeroFailTest() {
        //사용자 포인트 설정
        long id = 1L;
        getUserPoint(id, anyLong());

        assertThatThrownBy(() -> pointService.charge(id, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액이 0원입니다.");
    }

    //성공 TC
    @Test
    @DisplayName("유효한 금액을 충전하려고 하는 경우")
    void chargeValidAmountSuccessTest() {
        //사용자 포인트 설정
        long id = 1L;
        long base = 50L;
        getUserPoint(id, base);

        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenAnswer(invocationOnMock -> {
            long invocationID = invocationOnMock.getArgument(0);
            long invocationAmount = invocationOnMock.getArgument(1);
            //기존 유저 point + 충전 point
            long updatedAmount = userPointTable.selectById(invocationID).point() + invocationAmount;
            return new UserPoint(invocationID, updatedAmount, System.currentTimeMillis());
        });

        // 테스트 코드 실행
        long chargeAmount = 100L;
        UserPoint userPoint = pointService.charge(id, chargeAmount);

        assertThat(userPoint.point()).isEqualTo(base + chargeAmount);
    }
}