package io.hhplus.tdd.point;

import java.util.List;

public interface PointService {
    UserPoint charge(long id, long amount);
    UserPoint use(long id, long amount);
    UserPoint get(long id);
    List<PointHistory> getHistories(long id);
}
