package com.pricepulse.exception;

/** The JSON shape of every error the server returns: { "status": 400, "message": "Invalid price" } (FR-40). */
public record ErrorResponse(int status, String message) {
}
