package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorResponse;

public class PointException extends RuntimeException {
    private final String code;

    public PointException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ErrorResponse getErrorResponse() {
        return new ErrorResponse(this.code, this.getMessage());
    }
}
