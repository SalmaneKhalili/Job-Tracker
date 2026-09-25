package org.salmanekhalili.jobtrack.exception;

public class ApplicationNotFoundException extends NotFoundException {

    public ApplicationNotFoundException(Long id) {
        super("Application " + id + " not found");
    }
}
