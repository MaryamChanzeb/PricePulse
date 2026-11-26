package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** The watchlist already holds the maximum number of items (FR-26, SDS 8: 409). */
public class WatchLimitExceededException extends ApiException {

    public WatchLimitExceededException(int limit) {
        super("Watchlist limit reached (" + limit + ")");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
