package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PointServiceTest {

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    private PointService pointService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
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

    @Test
    @DisplayName("포인트를 충전할 수 있다.")
    void 포인트를_충전할_수_있다() {
        // given
        long id = 0L;
        long point = 30L;
        UserPoint existingUserPoint = new UserPoint(id, 0, System.currentTimeMillis());
        UserPoint newUserPoint = new UserPoint(id, point, System.currentTimeMillis());
        // when
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(id, point)).thenReturn(newUserPoint);
        UserPoint userPoint = pointService.charge(id, point);
        // then
        verify(userPointTable, times(1)).selectById(id);
        verify(userPointTable, times(1)).insertOrUpdate(id, point);
        assertEquals(point, userPoint.point());
        assertEquals(id, userPoint.id());
    }

    @Test
    @DisplayName("0 포인트 금액을 충전할 수 없다")
    void 실패_0포인트() {
        // given
        long id = 0L;
        long point = 0L;

        // when
        // then
        verifyNoInteractions(pointHistoryTable, userPointTable);
        assertThrows(PointException.class, () -> pointService.charge(id, point));
    }

    @Test
    @DisplayName("마이너스 포인트 금액을 충전할 수 없다")
    void 실패_마이너스_포인트() {
        // given
        long id = 0L;
        long point = -200L;

        // when
        // then
        verifyNoInteractions(pointHistoryTable, userPointTable);
        assertThrows(PointException.class, () -> pointService.charge(id, point));
    }

    @Test
    @DisplayName("3_000_000 포인트 이상을 유저는 가질 수 없다")
    void 실패_3_000_000_포인트() {
        //  given
        long id = 0L;
        long point = 3_000_000L;

        // when
        UserPoint existingUserPoint = new UserPoint(id, 100L, System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);

        verifyNoInteractions(pointHistoryTable, userPointTable);
        // then
        assertThrows(PointException.class, () -> pointService.charge(id, point));
    }
}
