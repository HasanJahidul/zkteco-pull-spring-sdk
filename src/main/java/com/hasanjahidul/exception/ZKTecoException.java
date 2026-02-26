package com.hasanjahidul.exception;

/**
 * Base exception for ZKTeco SDK operations
 */
public class ZKTecoException extends RuntimeException {

    /**
     * Constructs a new ZKTeco exception with the specified detail message
     *
     * @param message the detail message
     */
    public ZKTecoException(String message) {
        super(message);
    }

    /**
     * Constructs a new ZKTeco exception with the specified detail message and cause
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ZKTecoException(String message, Throwable cause) {
        super(message, cause);
    }
}
