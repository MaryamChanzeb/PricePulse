package com.pricepulse.exception;

import org.springframework.http.HttpStatus;

/**
 * A user tried something their role may not do (FR-06). The SDS calls this AccessDeniedException;
 * it is renamed because Spring Security already has a class with that name (see docs/CHANGES.md).
 */
public class RoleAccessDeniedException extends ApiException {

    public static final String MESSAGE = "Access denied for your role";

    public RoleAccessDeniedException() {
        super(MESSAGE);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
