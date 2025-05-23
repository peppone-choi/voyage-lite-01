package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    public PointController(final PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return pointService.get(id); // TODO : 통합 테스트 필요
    }

    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return pointService.getHistories(id); // TODO : 통합 테스트 필요
    }

    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.charge(id, amount); // TODO : 통합 테스트 필요
    }

    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.use(id, amount); // TODO : 통합 테스트 필요
    }

    @ExceptionHandler(PointException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePointException(final PointException ex) {
        log.error(ex.getMessage(), ex);
        return ex.getErrorResponse();
    }
}
