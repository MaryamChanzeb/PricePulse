package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** The product is already on this shopper's watchlist (FR-25, SDS 8: 409). */
public class DuplicateWatchException extends ApiException {

    public DuplicateWatchException() {
        super("Already in your watchlist");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
