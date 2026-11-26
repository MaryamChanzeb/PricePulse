package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/** Unknown product or watch item (SDS 8: 404). */
public class ProductNotFoundException extends ApiException {

    public ProductNotFoundException() {
        super("Product not found");
    }

    public ProductNotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
