package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** The token was valid but its account no longer exists (for example it was deleted). */
public class NotLoggedInException extends ApiException {

    public NotLoggedInException() {
        super("Please log in again");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
