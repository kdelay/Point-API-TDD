package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.repository.PointHistory;
import io.hhplus.tdd.point.repository.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.hhplus.tdd.point.repository.TransactionType.CHARGE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PointController.class)
class PointControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PointService pointService;

    @MockBean
    private PointHistoryTable pointHistoryTable;

    @Test
    @DisplayName("GET /point/{id} 특정 유저의 포인트 조회")
    void selectTest() throws Exception {

        //given
        long id = 1L;
        long amount = 100L;
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());

        //when
        when(pointService.select(id)).thenReturn(userPoint);

        //then
        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /point/{id}/histories 특정 유저의 포인트 충전/이용 내역 조회")
    void historyTest() throws Exception {

        //given
        long id = 1L, chargeAmount = 100L;

        //when
        when(pointHistoryTable.selectAllByUserId(eq(id)))
                .thenReturn(List.of(
                        new PointHistory(1L, id, chargeAmount, CHARGE, System.currentTimeMillis())
                ));

        //then
        mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge 특정 유저의 포인트 충전")
    void chargeTest() throws Exception {

        //given
        long id = 1L;
        long amount = 100L;
        UserPoint mockUserPoint = new UserPoint(id, amount, System.currentTimeMillis());

        //when
        when(pointService.charge(anyLong(), anyLong())).thenReturn(mockUserPoint);

        //JSON to String
        String jsonContent = objectMapper.writeValueAsString(amount);

        //then
        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk()) //200 response
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(amount));
    }

    @Test
    @DisplayName("PATCH /point/{id}/use 특정 유저의 포인트 사용")
    void useTest() throws Exception {

        //given
        long id = 1L;
        long amount = 100L;

        //when
        when(pointService.use(id, amount)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));

        //JSON to String
        String jsonContent = objectMapper.writeValueAsString(amount);

        //then
        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }
}