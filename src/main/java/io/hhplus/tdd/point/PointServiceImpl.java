package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorResponse;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

@Service
public class PointServiceImpl implements PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    private final long USER_POINT_LIMIT = 3_000_000L;

    public PointServiceImpl(PointHistoryTable pointHistoryTable, UserPointTable userPointTable) {
        this.pointHistoryTable = pointHistoryTable;
        this.userPointTable = userPointTable;
    }

    @Override
    public UserPoint charge(long id, long amount) {
        if (amount <= 0) {
            throw new PointException("충전 금액은 0 혹은 마이너스 일 수 없습니다", "CHARGE_IS_OVER_ZERO");
        }

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint.point() + amount > USER_POINT_LIMIT) {
            throw new PointException(String.format("1인당 포인트 최대 잔고는 %d원 이하여야 합니다.", USER_POINT_LIMIT), "USER_POINT_LIMIT_EXCEEDED");
        }

        return userPointTable.insertOrUpdate(userPoint.id(), userPoint.point() + amount);
    }
}
