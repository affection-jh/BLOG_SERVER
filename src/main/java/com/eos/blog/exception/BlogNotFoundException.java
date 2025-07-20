package com.eos.blog.exception;

public class BlogNotFoundException extends RuntimeException {
    
    public BlogNotFoundException(Long blogId) {
        super("블로그를 찾을 수 없습니다. ID: " + blogId);
    }
    
    public BlogNotFoundException(String message) {
        super(message);
    }
    
    public BlogNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 