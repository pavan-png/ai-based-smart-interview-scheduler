package com.interview.platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an interview state transition is invalid.
 * E.g. trying to confirm an already-cancelled interview.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InterviewStateException extends RuntimeException {

    public InterviewStateException(String message) {
        super(message);
    }
}
