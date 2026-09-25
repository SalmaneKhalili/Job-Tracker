package org.salmanekhalili.jobtrack.exception;

public class NoteNotFoundException extends NotFoundException {

    public NoteNotFoundException(Long id) {
        super("Note " + id + " not found");
    }
}
