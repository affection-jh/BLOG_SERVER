package com.eos.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ErrorResponse {
    private String error;
    private String message;
    private int status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;
    
    // 정형화된 에러 정보
    private String errorCode;
    private List<FieldError> fieldErrors;
    private Map<String, Object> details;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String error, String message, int status, String path) {
        this();
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
    }

    public ErrorResponse(String error, String message, int status, String path, String errorCode) {
        this(error, message, status, path);
        this.errorCode = errorCode;
    }

    // 정형화된 에러 응답 생성 메서드들
    public static ErrorResponse validationError(String message, List<FieldError> fieldErrors) {
        ErrorResponse response = new ErrorResponse("VALIDATION_ERROR", message, 400, null, "VALIDATION_FAILED");
        response.setFieldErrors(fieldErrors);
        return response;
    }

    public static ErrorResponse fileUploadError(String message, List<FileUploadError> fileErrors) {
        ErrorResponse response = new ErrorResponse("FILE_UPLOAD_ERROR", message, 400, null, "FILE_UPLOAD_FAILED");
        response.setDetails(Map.of("fileErrors", fileErrors));
        return response;
    }

    public static ErrorResponse permissionError(String message) {
        return new ErrorResponse("PERMISSION_ERROR", message, 403, null, "ACCESS_DENIED");
    }

    public static ErrorResponse notFoundError(String message) {
        return new ErrorResponse("NOT_FOUND_ERROR", message, 404, null, "RESOURCE_NOT_FOUND");
    }

    // Getters and Setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    // 필드 에러 클래스
    public static class FieldError {
        private String field;
        private String message;
        private String code;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public FieldError(String field, String message, String code) {
            this.field = field;
            this.message = message;
            this.code = code;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    // 파일 업로드 에러 클래스
    public static class FileUploadError {
        private String fileName;
        private String errorCode;
        private String message;
        private String details;

        public FileUploadError(String fileName, String errorCode, String message) {
            this.fileName = fileName;
            this.errorCode = errorCode;
            this.message = message;
        }

        public FileUploadError(String fileName, String errorCode, String message, String details) {
            this.fileName = fileName;
            this.errorCode = errorCode;
            this.message = message;
            this.details = details;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
} 