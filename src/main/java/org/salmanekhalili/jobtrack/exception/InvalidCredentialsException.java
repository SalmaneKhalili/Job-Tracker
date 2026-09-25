package org.salmanekhalili.jobtrack.exception;

import org.springframework.http.HttpStatus;

/**
 * Deliberately carries one fixed message for "no such user" and "wrong
 * password" alike, so login cannot be used to enumerate registered emails.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
