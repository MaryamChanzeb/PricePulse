package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** Weak or missing input, for example sign-up data (SDS 8: 400, field specific message). */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
