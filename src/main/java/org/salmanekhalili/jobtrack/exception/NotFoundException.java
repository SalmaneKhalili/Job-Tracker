package org.salmanekhalili.jobtrack.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a resource does not exist <em>for the calling user</em>. Missing
 * and "belongs to somebody else" are deliberately indistinguishable to the
 * client: answering 404 for both is what stops the API from confirming that
 * another user's ids exist.
 */
public abstract class NotFoundException extends ApiException {

    protected NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
