package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class PointServiceTest {

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    private PointService pointService;

    @BeforeEach
    public void setUp() {
        pointService = new PointServiceImpl(pointHistoryTable, userPointTable);
    }

    @Test
    @DisplayName("PointService 생성 확인")
    void 서비스가_존재한다() {
        // given
        // when
        // then
        Assertions.assertInstanceOf(PointServiceImpl.class, pointService);
    }
}
