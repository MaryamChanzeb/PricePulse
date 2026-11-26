package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** Sign-up with an email that already has an account (FR-02). */
public class DuplicateEmailException extends ApiException {

    public DuplicateEmailException() {
        super("This email is already registered");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
