package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/**
 * Parent of all Price Pulse exceptions that should reach the user (SDS 8).
 * Each subclass knows its own HTTP status, so GlobalExceptionHandler needs only ONE method for all of them:
 * it calls getStatus() through this parent type (polymorphism again).
 */
public abstract class ApiException extends RuntimeException {

    protected ApiException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();
}
