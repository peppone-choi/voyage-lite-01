# 동시성 제어 방식 분석 보고서

## 1. 개요

동시성 제어는 여러 스레드가 공유 자원에 동시에 접근할 때 발생하는 문제를 해결하기 위한 메커니즘입니다. 본 보고서에서는 Java 기반의 포인트 관리 시스템에 적용 가능한 다양한 동시성 제어 방식을 분석하고, 각각의 장단점을 비교합니다.

## 2. 동시성 문제 유형

### 2.1 Race Condition
- 여러 스레드가 동시에 같은 자원을 수정할 때 발생
- 실행 순서에 따라 결과가 달라짐

### 2.2 Lost Update
- 두 트랜잭션이 동시에 같은 데이터를 읽고 수정할 때, 한 트랜잭션의 수정사항이 손실

### 2.3 Dirty Read
- 커밋되지 않은 데이터를 다른 트랜잭션이 읽는 문제

## 3. 동시성 제어 방식

### 3.1 Pessimistic Locking (비관적 락)

#### 3.1.1 synchronized 키워드

```java
public synchronized UserPoint charge(long id, long amount) {
    // 메서드 전체 동기화
}

// 또는
public UserPoint charge(long id, long amount) {
    synchronized(this) {
        // 블록 단위 동기화
    }
}
```

**장점:**
- 구현이 가장 간단
- Java 언어 차원에서 지원
- 데드락 발생 시 JVM이 자동으로 감지

**단점:**
- 성능 오버헤드가 큼
- 세밀한 제어 불가능
- 메서드/블록 전체가 임계영역이 됨

**적용 시나리오:**
- 간단한 동시성 제어가 필요한 경우
- 성능보다 안정성이 중요한 경우

#### 3.1.2 ReentrantLock

```java
private final ReentrantLock lock = new ReentrantLock();

public UserPoint charge(long id, long amount) {
    lock.lock();
    try {
        // 임계 영역
    } finally {
        lock.unlock();
    }
}
```

**장점:**
- tryLock() 으로 타임아웃 설정 가능
- 공정성(fairness) 보장 옵션
- Condition을 통한 세밀한 제어
- 락 획득 대기 중 인터럽트 가능

**단점:**
- synchronized보다 복잡한 코드
- unlock을 명시적으로 호출해야 함

**적용 시나리오:**
- 타임아웃이 필요한 경우
- 공정성이 중요한 경우
- 복잡한 동기화 패턴이 필요한 경우

#### 3.1.3 사용자별 락 (Fine-grained Locking)

```java
private final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();

public UserPoint charge(long id, long amount) {
    Object lock = lockMap.computeIfAbsent(id, k -> new Object());
    synchronized(lock) {
        // 사용자별 동기화
    }
}
```

**장점:**
- 사용자별로 독립적인 동시성 제어
- 다른 사용자 간 병렬 처리 가능
- 성능 향상

**단점:**
- 메모리 사용량 증가 (사용자 수만큼 락 객체 생성)
- 구현 복잡도 증가

**적용 시나리오:**
- 사용자가 많고 독립적인 작업이 대부분인 경우
- 높은 동시성이 요구되는 경우

### 3.2 Optimistic Locking (낙관적 락)

#### 3.2.1 버전 관리 방식

```java
public class UserPoint {
    private long id;
    private long point;
    private long version;
    
    // 업데이트 시 버전 체크
    public UserPoint update(long newPoint, long expectedVersion) {
        if (this.version != expectedVersion) {
            throw new OptimisticLockException();
        }
        this.point = newPoint;
        this.version++;
        return this;
    }
}
```

**장점:**
- 락을 획득하지 않아 성능이 좋음
- 읽기 작업이 많은 경우 효율적
- 데드락 발생 없음

**단점:**
- 충돌 시 재시도 로직 필요
- 쓰기 경합이 많으면 성능 저하

**적용 시나리오:**
- 읽기가 많고 쓰기가 적은 경우
- 충돌이 드물게 발생하는 경우

#### 3.2.2 CAS (Compare-And-Swap) 연산

```java
private final AtomicLong point = new AtomicLong();

public void charge(long amount) {
    long current, next;
    do {
        current = point.get();
        next = current + amount;
    } while (!point.compareAndSet(current, next));
}
```

**장점:**
- Lock-free 알고리즘
- 높은 성능
- 데드락 없음

**단점:**
- 복잡한 로직에는 부적합
- ABA 문제 가능성

**적용 시나리오:**
- 단순한 연산 (증가/감소)
- 높은 성능이 필요한 경우

### 3.3 Lock-free 자료구조

#### 3.3.1 ConcurrentHashMap

```java
private final ConcurrentHashMap<Long, UserPoint> userPoints = new ConcurrentHashMap<>();

public UserPoint charge(long id, long amount) {
    return userPoints.compute(id, (k, v) -> {
        if (v == null) v = new UserPoint(id, 0);
        return new UserPoint(id, v.point() + amount);
    });
}
```

**장점:**
- 내부적으로 세그먼트 락 사용
- 높은 동시성
- Thread-safe

**단점:**
- 복잡한 비즈니스 로직 구현 어려움
- 메모리 오버헤드

### 3.4 메시지 큐 기반 동시성 제어

```java
@Service
public class PointCommandHandler {
    private final BlockingQueue<PointCommand> commandQueue;
    
    @Async
    public void processCommands() {
        while (true) {
            PointCommand command = commandQueue.take();
            // 순차적으로 처리
        }
    }
}
```

**장점:**
- 완벽한 순차 처리 보장
- 복잡한 동시성 제어 불필요
- 확장성 좋음

**단점:**
- 응답 지연 발생
- 구현 복잡도 높음
- 추가 인프라 필요

## 4. 성능 비교

| 방식 | 처리량 | 응답시간 | 메모리 사용 | 구현 복잡도 |
|------|--------|----------|-------------|-------------|
| synchronized | 낮음 | 높음 | 낮음 | 매우 낮음 |
| ReentrantLock | 중간 | 중간 | 낮음 | 중간 |
| 사용자별 락 | 높음 | 낮음 | 높음 | 중간 |
| Optimistic Lock | 매우 높음* | 매우 낮음* | 중간 | 높음 |
| CAS | 매우 높음 | 매우 낮음 | 낮음 | 중간 |
| Message Queue | 중간 | 높음 | 높음 | 매우 높음 |

*충돌이 적은 경우

## 5. 선택 기준:
- **간단한 시스템**: synchronized
- **중간 규모**: 사용자별 ReentrantLock
- **대규모 시스템**: 낙관적 락 + 메시지 큐
- **분산 시스템**: 분산 락 + 이벤트 소싱