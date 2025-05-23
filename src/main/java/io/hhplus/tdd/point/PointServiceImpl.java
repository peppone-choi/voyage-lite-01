package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorResponse;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointServiceImpl implements PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    private final long USER_POINT_LIMIT = 3_000_000L;

    private final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();
    // private final ConcurrentHashMap<Long, ReentrantLock> reentrantLockMap = new ConcurrentHashMap<>();
    public PointServiceImpl(PointHistoryTable pointHistoryTable, UserPointTable userPointTable) {
        this.pointHistoryTable = pointHistoryTable;
        this.userPointTable = userPointTable;
    }

    @Override
    public UserPoint charge(long id, long amount) {
        if (amount <= 0) {
            throw new PointException("충전 금액은 0 혹은 마이너스 일 수 없습니다", "CHARGE_POINT_IS_OVER_ZERO");
        } // 충전금액이 0 혹은 마이너스 일 경우

        Object lock = lockMap.computeIfAbsent(id, k -> new Object());
        synchronized (lock) {
            UserPoint userPoint = userPointTable.selectById(id);

            if (userPoint.point() + amount > USER_POINT_LIMIT) {
                throw new PointException(String.format("1인당 포인트 최대 잔고는 %d원 이하여야 합니다.", USER_POINT_LIMIT), "USER_POINT_LIMIT_EXCEEDED");
            } // 충전 후 금액이 최대 잔고를 넘을 경우

            long newAmount = userPoint.point() + amount; // 충전 후 금액

            try {
                pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis()); // 포인트 충전 내역 저장
            } catch (Exception e) {
                throw new PointException("포인트 충전 내역을 저장 하지 못했습니다.", "HISTORY_TABLE_INSERT_FAILED");
            } // 어느 예외라도 터져서 포인트 충전 내역을을 저장하지 못하였을 경우

            return userPointTable.insertOrUpdate(userPoint.id(), newAmount);
        }
    }

    @Override
    public UserPoint use(long id, long amount) {
        Object lock = lockMap.computeIfAbsent(id, k -> new Object());
        synchronized (lock) {
            if (amount <= 0) {
                throw new PointException("사용 금액은 0 혹은 마이너스 일 수 없습니다.", "USE_AMOUNT_IS_OVER_ZERO");
            } // 사용금액이 0 혹은 마이너스 일 경우

            UserPoint userPoint = userPointTable.selectById(id);

            if (userPoint.point() < amount) {
                throw new PointException("포인트가 부족합니다. 포인트를 충전 해주십시오.", "NEED_CHARGE_AMOUNT");
            }

            long newAmount = userPoint.point() - amount; // 사용 후 금액

            try {
                pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis()); // 포인트 사용 내역 저장
            } catch (Exception e) {
                throw new PointException("포인트 사용 내역을 저장 하지 못했습니다.", "HISTORY_TABLE_INSERT_FAILED");
            } // 어느 예외라도 터져서 포인트 사용 내역을을 저장하지 못하였을 경우

            return userPointTable.insertOrUpdate(userPoint.id(), newAmount);
        }
    }

    @Override
    public UserPoint get(long id) {
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint.point() < 0) {
            throw new PointException("포인트 조회 중 오류가 발생했습니다. 포인트는 음수일 수 없습니다.", "POINT_IS_OVER_ZERO");
        }
        return userPoint;
    }

    @Override
    public List<PointHistory> getHistories(long id) {
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(id);
        if (histories.isEmpty()) {
            throw new PointException("해당 ID의 포인트 충전 혹은 사용 내역이 없습니다", "HISTORY_TABLE_SELECT_FAILED");
        }
        boolean isEqual = false;
        for (PointHistory pointHistory : histories)
            if (pointHistory.userId() != id) {
                isEqual = true;
            }
        if (isEqual) {
            throw new PointException("해당 ID의 포인트 충전 및 사용 내역에 문제가 있습니다. 다른 사용자의 포인트 내역이 들어갔습니다.", "HISTORY_TABLE_ANOTHER_USER");
        }
        long calculatedBalance = 0;

        for (PointHistory history : histories) {
            if (history.type() == TransactionType.CHARGE) {
                calculatedBalance += history.amount();
            } else if (history.type() == TransactionType.USE) {
                calculatedBalance -= history.amount();
            }
        }

        if (calculatedBalance < 0) {
            throw new PointException("해당 ID의 포인트 충전 및 사용 내역에 문제가 있습니다. 최종 합계가 0 미만일 수 없습니다.", "HISTORY_TABLE_FINAL_AMOUNT_IS_OVER_ZERO");
        }
        return histories;
    }
}