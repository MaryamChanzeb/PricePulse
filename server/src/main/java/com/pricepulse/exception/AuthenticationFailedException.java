package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/**
 * Wrong email or password (FR-05). The message never says which of the two was wrong,
 * so nobody can use the login form to find out which emails are registered.
 */
public class AuthenticationFailedException extends ApiException {

    public static final String MESSAGE = "Invalid email or password";

    public AuthenticationFailedException() {
        super(MESSAGE);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
