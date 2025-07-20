package com.eos.blog.exception;

public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super(message, 400);
    }
    
    public ValidationException(String fieldName, String message) {
        super(String.format("%s: %s", fieldName, message), 400);
    }
} 