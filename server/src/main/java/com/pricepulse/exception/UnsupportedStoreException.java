package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** The page is not from a store in the supported list, or the store is disabled (FR-11, SDS 8: 422). */
public class UnsupportedStoreException extends ApiException {

    public UnsupportedStoreException() {
        super("This store is not supported");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
