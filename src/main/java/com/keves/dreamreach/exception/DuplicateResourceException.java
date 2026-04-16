package com.keves.dreamreach.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an attempt is made to create a resource (User, etc.)
 * that already exists in the database.
 */
@ResponseStatus(HttpStatus.CONFLICT) // Automatically maps this error to HTTP 409
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}