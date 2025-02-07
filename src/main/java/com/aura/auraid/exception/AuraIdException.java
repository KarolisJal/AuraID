package com.aura.auraid.exception;

public class AuraIdException extends RuntimeException {
    public AuraIdException(String message) {
        super(message);
    }

    public AuraIdException(String message, Throwable cause) {
        super(message, cause);
    }
} 