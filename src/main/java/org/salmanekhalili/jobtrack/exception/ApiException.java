package org.salmanekhalili.jobtrack.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for failures that map onto a deliberate HTTP status and a
 * client-safe message. Anything that is not an {@code ApiException} is treated
 * as a bug and answered with a generic 500.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
