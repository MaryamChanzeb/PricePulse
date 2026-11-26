package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** Price is zero, negative, not a number or above the maximum (FR-10, SDS 8: 400 "Invalid price"). */
public class PriceValidationException extends ApiException {

    public static final String MESSAGE = "Invalid price";

    public PriceValidationException() {
        super(MESSAGE);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
