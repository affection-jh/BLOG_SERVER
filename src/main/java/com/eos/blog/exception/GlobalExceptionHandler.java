package com.eos.blog.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    

    
    /**
     * 일반적인 예외 처리 (간소화)
     */
    @ExceptionHandler({IllegalArgumentException.class, SecurityException.class, ResourceNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleCommonException(Exception e) {
        logger.error("요청 처리 실패: {}", e.getMessage());
        
        Map<String, String> response = new HashMap<>();
        
        if (e instanceof SecurityException) {
            response.put("error", "권한이 없습니다");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } else if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.notFound().build();
        } else {
            response.put("error", "잘못된 요청입니다");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 기타 예외 처리 (간소화)
     */
    @ExceptionHandler({RuntimeException.class, Exception.class})
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        logger.error("서버 오류: {}", e.getMessage(), e);
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "서버 오류가 발생했습니다");
        
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