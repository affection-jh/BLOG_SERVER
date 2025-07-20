package com.eos.blog.exception;

import com.eos.blog.dto.ErrorResponse;

public class FileUploadException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public FileUploadException(ErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
} 