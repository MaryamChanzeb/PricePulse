package com.pricepulse.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The ONE place that turns exceptions into JSON answers (SDS 8, FR-40, FR-41).
 * The client always gets { status, message } and never a stack trace; unexpected errors are logged
 * on the server and answered with a general message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String DATABASE_DOWN_MESSAGE = "Service temporarily unavailable, please try again";
    static final String UNEXPECTED_MESSAGE = "Something went wrong, please try again";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** All our own exceptions: each one knows its status (see ApiException). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return respond(ex.getStatus(), ex.getMessage());
    }

    /** @Valid failed on a request body: show the first field message, e.g. "Email is required". */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldError();
        String message = (first != null && first.getDefaultMessage() != null) ? first.getDefaultMessage() : "Invalid request";
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    /** Body is not valid JSON, or a value has the wrong type. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, "The request is not valid JSON");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadParameter(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleWrongMethod(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "This action is not supported here");
    }

    /** Role check inside the application (Spring Security's own class). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, RoleAccessDeniedException.MESSAGE);
    }

    /**
     * FR-41: the database cannot be reached. Spring reports this either as a DataAccessException or,
     * when a transaction cannot even start, as CannotCreateTransactionException. The server keeps running.
     */
    @ExceptionHandler({DataAccessException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseDown(Exception ex) {
        log.error("Database problem: {}", ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, DATABASE_DOWN_MESSAGE);
    }

    /** Anything else: log the details for us, show a general message to the user. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_MESSAGE);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }
}
