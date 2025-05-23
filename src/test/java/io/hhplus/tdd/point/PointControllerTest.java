package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 초기화
        userPointTable.insertOrUpdate(1L, 1000L);
        userPointTable.insertOrUpdate(2L, 500L);
    }

    @Test
    @DisplayName("포인트 조회 성공 - GET /point/{id}")
    void 포인트_조회_성공() throws Exception {
        long id = 1L;

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(1000L));
    }

    @Test
    @DisplayName("포인트 충전 성공 - PATCH /point/{id}/charge")
    void 포인트_충전_성공() throws Exception {
        long id = 1L;
        long chargeAmount = 500L;

        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(1500L));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 0 또는 음수 금액")
    void 포인트_충전_실패_잘못된_금액() throws Exception {
        long id = 1L;
        long chargeAmount = -100L;

        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isInternalServerError())  // 현재 모든 예외가 500으로 처리됨
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 최대 잔고 초과")
    void 포인트_충전_실패_최대_잔고_초과() throws Exception {
        long id = 1L;
        long chargeAmount = 3_000_000L;

        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("포인트 사용 성공 - PATCH /point/{id}/use")
    void 포인트_사용_성공() throws Exception {
        long id = 1L;
        long useAmount = 300L;

        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(700L));
    }

    @Test
    @DisplayName("포인트 사용 실패 - 0 또는 음수 금액")
    void 포인트_사용_실패_잘못된_금액() throws Exception {
        long id = 1L;
        long useAmount = 0L;

        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("포인트 사용 실패 - 잔고 부족")
    void 포인트_사용_실패_잔고_부족() throws Exception {
        long id = 2L;
        long useAmount = 1000L;

        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("포인트 내역 조회 성공 - GET /point/{id}/histories")
    void 포인트_내역_조회_성공() throws Exception {
        long id = 1L;

        // 테스트 데이터 준비: 충전과 사용 내역 생성
        pointHistoryTable.insert(id, 500L, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(id, 200L, TransactionType.USE, System.currentTimeMillis());

        mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].userId").value(id))
                .andExpect(jsonPath("$[0].amount").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].updateMillis").exists());
    }

    @Test
    @DisplayName("포인트 내역 조회 실패 - 내역 없음")
    void 포인트_내역_조회_실패_내역없음() throws Exception {
        long id = 999L;

        mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("동시성 테스트 - 동시에 여러 충전 요청")
    void 동시_충전_요청_실패() throws Exception {
        long id = 3L;
        userPointTable.insertOrUpdate(id, 0L);

        int threadCount = 10;
        long chargeAmount = 100L;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기
                    mockMvc.perform(patch("/point/{id}/charge", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(chargeAmount)));
                } catch (Exception e) {
                    // 테스트 중 예외 처리
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 실행
        endLatch.await();
        executorService.shutdown();

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andDo(result -> {
                    String response = result.getResponse().getContentAsString();
                    System.out.println("Expected: 1000, Actual: " + response);
                })
                .andExpect(jsonPath("$.point").value(1000L)); // 이 assertion은 실패할 것
    }

    @Test
    @DisplayName("동시성 테스트 - 동시에 충전과 사용 요청")
    void 동시_충전_사용_요청_실패() throws Exception {
        long id = 4L;
        userPointTable.insertOrUpdate(id, 1000L);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        Thread chargeThread = new Thread(() -> {
            try {
                startLatch.await(); // 동시 시작 대기
                mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("500"));
            } catch (Exception e) {
                // 테스트 중 예외 처리
            } finally {
                endLatch.countDown();
            }
        });

        Thread useThread = new Thread(() -> {
            try {
                startLatch.await(); // 동시 시작 대기
                mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("300"));
            } catch (Exception e) {
                // 테스트 중 예외 처리
            } finally {
                endLatch.countDown();
            }
        });

        chargeThread.start();
        useThread.start();

        startLatch.countDown(); // 동시 실행 시작
        endLatch.await();

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andDo(result -> {
                    String response = result.getResponse().getContentAsString();
                    // 실제 값을 출력하여 동시성 문제 확인
                    System.out.println("Expected: 1200, Actual: " + response);
                })
                .andExpect(jsonPath("$.point").value(1200L)); // 이 assertion은 실패할 것
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회")
    void 존재하지_않는_사용자_조회() throws Exception {
        long id = 9999L;

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(0L)); // UserPointTable의 기본값은 0
    }
}