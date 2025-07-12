package com.eos.blog.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 파일 크기 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        logger.error("파일 크기 초과: {}", e.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "파일 크기가 너무 큽니다");
        response.put("message", "5MB 이하의 이미지를 업로드해주세요");
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }
    
    /**
     * 이미지 처리 실패 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        logger.error("잘못된 요청: {}", e.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "잘못된 요청입니다");
        response.put("message", e.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 권한 없음 예외 처리
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
        logger.error("권한 없음: {}", e.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "권한이 없습니다");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * 리소스 없음 예외 처리
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException e) {
        logger.error("리소스 없음: {}", e.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "리소스를 찾을 수 없습니다");
        response.put("message", e.getMessage());
        
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 일반적인 런타임 예외 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        logger.error("런타임 예외: {}", e.getMessage(), e);
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "서버 오류가 발생했습니다");
        response.put("message", "잠시 후 다시 시도해주세요");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("예상치 못한 예외: {}", e.getMessage(), e);
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "서버 오류가 발생했습니다");
        response.put("message", "잠시 후 다시 시도해주세요");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 리소스 없음 예외 클래스
     */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
} 