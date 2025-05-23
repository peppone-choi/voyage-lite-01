package io.hhplus.tdd.point;

public interface PointService {
    UserPoint charge(long id, long amount);
    UserPoint use(long id, long amount);
    UserPoint get(long id);
}
