package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PointController.class)
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PointService pointService;

    // ---------------------------------------------------------------------------

    // /point/{id}
    @Test
    @DisplayName("GET 포인트 조회(아이디 O)")
    void selectTest_isExistsId() throws Exception {

        //mocking
        long id = 1L;
        long amount = 100L;
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());

        when(pointService.select(id)).thenReturn(userPoint);

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET 포인트 조회(아이디 X)")
    void selectTest_isEmptyId() throws Exception {

        //mocking
        long id = -1L;

        when(pointService.select(id)).thenThrow(new IllegalArgumentException("아이디가 존재하지 않습니다."));

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isInternalServerError());
    }

    // ---------------------------------------------------------------------------

    // /point/{id}/charge
    @Test
    @DisplayName("PATCH 포인트 충전(아이디 O)")
    void chargeTest_isExistsId() throws Exception {

        long id = 1L;
        long amount = 100L;
        UserPoint mockUserPoint = new UserPoint(id, amount, System.currentTimeMillis());

        // mocking
        when(pointService.charge(anyLong(), anyLong())).thenReturn(mockUserPoint);

        //JSON 형식으로 변환된 amount를 전달
        String jsonContent = objectMapper.writeValueAsString(amount);

        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk()) //200 response
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(amount));
    }

    @Test
    @DisplayName("PATCH 포인트 충전(아이디 X)")
    void chargeTest_isEmptyId() throws Exception {
        long userId = -1L; //Invalid ID
        long amount = 100L;

        //mocking
        given(pointService.charge(userId, amount)).willThrow(new IllegalArgumentException("아이디가 존재하지 않습니다."));

        //JSON 형식으로 변환된 amount를 전달
        String jsonContent = objectMapper.writeValueAsString(amount);

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isInternalServerError());
    }

    // ---------------------------------------------------------------------------

    // /point/{id}/use
    @Test
    @DisplayName("PATCH 포인트 사용(아이디 O)")
    void useTest_isExistsId() throws Exception {
        long id = 1L;
        long amount = 100L;

        // mocking
        when(pointService.use(id, amount)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));

        // JSON 형식으로 변환된 amount를 전달
        String jsonContent = objectMapper.writeValueAsString(amount);

        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("PATCH 포인트 사용(아이디 X)")
    void useTest_isEmptyId() throws Exception {
        long id = -1L; //Invalid ID
        long amount = 100L;

        // mocking
        when(pointService.use(id, amount)).thenThrow(new IllegalArgumentException("아이디가 존재하지 않습니다."));

        // JSON 형식으로 변환된 amount를 전달
        String jsonContent = objectMapper.writeValueAsString(amount);

        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isInternalServerError());
    }
}