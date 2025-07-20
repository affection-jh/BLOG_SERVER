package com.eos.blog.exception;

public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(message, 401);
    }
    
    public UnauthorizedException() {
        super("인증이 필요합니다.", 401);
    }
} 