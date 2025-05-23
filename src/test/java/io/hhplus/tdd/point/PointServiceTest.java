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

import java.util.List;

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
        long currentTimeMillis = System.currentTimeMillis();
        UserPoint existingUserPoint = new UserPoint(id, 0, currentTimeMillis);
        UserPoint newUserPoint = new UserPoint(id, point, currentTimeMillis);
        PointHistory pointHistory = new PointHistory(0L, id, point, TransactionType.CHARGE, currentTimeMillis); // 생성자에서 anyLong() 대신 실제 값 사용

        // when
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(id, point)).thenReturn(newUserPoint);
        when(pointHistoryTable.insert(eq(id), eq(point), eq(TransactionType.CHARGE), anyLong())).thenReturn(pointHistory);  // 인자 매처를 사용할 때 모두 매처이거나 모두 구체적인 값이어야 함.

        // then
        UserPoint userPoint = pointService.charge(id, point);
        verify(userPointTable, times(1)).selectById(id);
        verify(userPointTable, times(1)).insertOrUpdate(id, point);
        verify(pointHistoryTable, times(1)).insert(eq(id), eq(point), eq(TransactionType.CHARGE), anyLong()); // 각 메서드별로 호출 여부 검증
        assertEquals(point, userPoint.point());
        assertEquals(id, userPoint.id());
    }

    @Test
    @DisplayName("0 포인트 금액을 충전할 수 없다")
    void 포인트_충전_실패_0포인트() {
        // given
        long id = 0L;
        long point = 0L;

        // when
        // then
        verify(userPointTable, never()).selectById(id);
        verify(userPointTable, never()).insertOrUpdate(id, point);
        verify(pointHistoryTable, never()).insert(eq(id), eq(point), eq(TransactionType.CHARGE), anyLong());
        assertThrows(PointException.class, () -> pointService.charge(id, point));
    }

    @Test
    @DisplayName("마이너스 포인트 금액을 충전할 수 없다")
    void 포인트_충전_실패_마이너스_포인트() {
        // given
        long id = 0L;
        long point = -200L;

        // when
        // then
        verify(userPointTable, never()).selectById(id);
        verify(userPointTable, never()).insertOrUpdate(id, point);
        verify(pointHistoryTable, never()).insert(eq(id), eq(point), eq(TransactionType.CHARGE), anyLong());
        verifyNoInteractions(pointHistoryTable, userPointTable);
        assertThrows(PointException.class, () -> pointService.charge(id, point));
    }

    @Test
    @DisplayName("3_000_000 포인트 이상을 유저는 가질 수 없다")
    void 포인트_충전_실패_3_000_000_포인트() {
        //  given
        long id = 0L;
        long point = 3_000_000L;

        // when
        UserPoint existingUserPoint = new UserPoint(id, 100L, System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);

        // then
        assertThrows(PointException.class, () -> pointService.charge(id, point));
        verify(userPointTable, times(1)).selectById(id);
        verify(userPointTable, never()).insertOrUpdate(id, point);
        verify(pointHistoryTable, never()).insert(eq(id), eq(point), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("포인트를 사용할 수 있다.")
    void 포인트를_사용_할_수_있다() {
        // given
        long id = 0L;
        long point = 500L;
        long usePoint = 200L;
        UserPoint existingUserPoint = new UserPoint(id, point, System.currentTimeMillis());
        UserPoint newUserPoint = new UserPoint(id, point - usePoint, System.currentTimeMillis());
        long currentTimeMillis = System.currentTimeMillis();
        PointHistory pointHistory = new PointHistory(0L, id, usePoint, TransactionType.USE, currentTimeMillis); // 생성자에서 anyLong() 대신 실제 값 사용

        // when
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(id, point - usePoint)).thenReturn(newUserPoint);
        when(pointHistoryTable.insert(eq(id), eq(usePoint), eq(TransactionType.USE), anyLong())).thenReturn(pointHistory);

        // then
        UserPoint userPoint = pointService.use(id, usePoint);
        verify(userPointTable, times(1)).selectById(id);
        verify(userPointTable, times(1)).insertOrUpdate(id, point - usePoint);
        verify(pointHistoryTable, times(1)).insert(eq(id), eq(usePoint), eq(TransactionType.USE), anyLong());
        assertEquals(300L, userPoint.point());
    }

    @Test
    @DisplayName("0포인트를 사용할 수 없다.")
    void 포인트_사용_실패_0포인트() {
        // given
        long id = 0L;
        long usePoint = 0L;

        // when

        // then
        verify(userPointTable, never()).selectById(id);
        verify(userPointTable, never()).insertOrUpdate(id, usePoint);
        verify(pointHistoryTable, never()).insert(eq(id), eq(usePoint), eq(TransactionType.USE), anyLong());
        assertThrows(PointException.class, () -> pointService.use(id, usePoint));
    }

    @Test
    @DisplayName("마이너스 포인트를 사용할 수 없다.")
    void 포인트_사용_실패_마이너스_포인트() {
        // given
        long id = 0L;
        long usePoint = -200L;

        // when

        // then
        verify(userPointTable, never()).selectById(id);
        verify(userPointTable, never()).insertOrUpdate(id, usePoint);
        verify(pointHistoryTable, never()).insert(eq(id), eq(usePoint), eq(TransactionType.USE), anyLong());
        assertThrows(PointException.class, () -> pointService.use(id, usePoint));
    }

    @Test
    @DisplayName("가지고 있는 포인트가 사용액보다 부족할 시 포인트를 사용할 수 있다.")
    void 실패_포인트_부족() {
        // given
        long id = 0L;
        long point = 500L;
        long usePoint = 600L;
        UserPoint existingUserPoint = new UserPoint(id, point, System.currentTimeMillis());

        // when
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);

        // then
        assertThrows(PointException.class, () -> pointService.use(id, usePoint));
        verify(userPointTable, times(1)).selectById(id);
        verify(userPointTable, never()).insertOrUpdate(id, usePoint);
        verify(pointHistoryTable, never()).insert(eq(id), eq(usePoint), eq(TransactionType.USE), anyLong());
    }
    @Test
    @DisplayName("포인트를 조회할 수 있다")
    void 포인트를_조회_할_수_있다() {
        // given
        long id = 0L;
        UserPoint existingUserPoint = new UserPoint(id, 100L, System.currentTimeMillis());

        // when
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);

        // then
        UserPoint foundPoint = pointService.get(id);
        assertEquals(100L, foundPoint.point());
        verify(userPointTable, times(1)).selectById(id);
    }

    @Test
    @DisplayName("조회 된 포인트는 마이너스일 수 없다.")
    void 실패_마이너스_포인트() {
        long id = 0L;
        UserPoint existingUserPoint = new UserPoint(id, -100L, System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);

        assertThrows(PointException.class, () -> pointService.get(id));
        verify(userPointTable, times(1)).selectById(id);
    }

    @Test
    @DisplayName("포인트 충전 / 사용 내역을 조회할 수 있다.")
    void 포인트_충전_사용_내역을_조회할_수_있다() {
        // given
        long id = 0L;
        List<PointHistory> pointHistory = List.of(
                new PointHistory(0L, 0L, 1000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(1L, 0L, 100, TransactionType.USE, System.currentTimeMillis())
        );

        // when
        when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistory);

        // then
        List<PointHistory> foundHistory = pointService.getHistories(id);

        assertEquals(pointHistory, foundHistory);
        verify(pointHistoryTable, times(1)).selectAllByUserId(id);
    }

    @Test
    @DisplayName("포인트 충전 / 혹은 사용 내역이 없으면 에러가 발생한다.")
    void 실패_사용내역_없음() {
        // given
        long id = 0L;
        List<PointHistory> pointHistory = List.of();

        // when
        when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistory);

        // then
        assertThrows(PointException.class, () -> pointService.getHistories(id));
        verify(pointHistoryTable, times(1)).selectAllByUserId(id);
    }

    @Test
    @DisplayName("최종 금액이 마이너스면 에러가 발생한다.")
    void 실패_최종금액_마이너스() {
        long id = 0L;
        List<PointHistory> pointHistory = List.of(
                new PointHistory(0L, 0L, 300L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(1L, 0L, 400L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistory);

        assertThrows(PointException.class, () -> pointService.getHistories(id));
        verify(pointHistoryTable, times(1)).selectAllByUserId(id);
    }

    @Test
    @DisplayName("다른 유저의 내역이 합쳐들어오면 에러가 발생한다.")
    void 실패_여러_사람의_내역() {
        long id = 0L;
        List<PointHistory> pointHistory = List.of(
                new PointHistory(0L, 0L, 300L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(1L, 1L, 400L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistory);

        assertThrows(PointException.class, () -> pointService.getHistories(id));
        verify(pointHistoryTable, times(1)).selectAllByUserId(id);
    }
}
