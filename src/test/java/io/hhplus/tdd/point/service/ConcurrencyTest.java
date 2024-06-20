package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.repository.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
public class ConcurrencyTest {

    private final PointService pointService;
    private final UserPointTable userPointTable;

    @Autowired
    public ConcurrencyTest(PointService pointService, UserPointTable userPointTable) {
        this.pointService = pointService;
        this.userPointTable = userPointTable;
    }

    @Test
    @DisplayName("포인트 충전 + 사용 동시성 테스트")
    void pointChargeAndUseTest() {

        //given
        long id = 1L;
        userPointTable.insertOrUpdate(id, 0L);

        //비동기 처리
        CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> {
                    //100 포인트 충전
                    return pointService.charge(id, 100L);
                }).thenApply(result -> {
                    //50 포인트 사용
                    return pointService.use(id, 50L);
                }).thenApply(result -> {
                    //10 포인트 충전
                    return pointService.charge(id, 10L);
                })
        ).join();

        UserPoint select = pointService.select(id);
        assertThat(select.point()).isEqualTo(100L - 50L + 10L);
    }

    @Test
    @DisplayName("잔여 포인트보다 사용하려는 포인트가 많은 경우")
    void pointGreaterThanBalanceTest() {

        //given
        long id = 1L;
        userPointTable.insertOrUpdate(id, 100L);

        //비동기 처리
        CompletableFuture<Void> future = CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> {
                    //100 포인트 충전
                    return pointService.charge(id, 100L);
                }).thenApply(result -> {
                    //3000 포인트 사용
                    return pointService.use(id, 3000L);
                })
        );

        //join 호출 시, 발생한 exception catch
        Throwable throwable = catchThrowable(future::join);

        //exception 발생 검증
        assertThat(throwable)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);;
    }
}
