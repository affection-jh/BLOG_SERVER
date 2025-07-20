package com.eos.blog.controller;

import com.eos.blog.scheduler.ImageCleanupScheduler;
import com.eos.blog.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 스케줄러 관리 컨트롤러
 * - 스케줄러 수동 실행
 * - 스케줄러 상태 확인
 */
@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {
    
    private final ImageService imageService;
    
    @Autowired
    public SchedulerController(ImageCleanupScheduler imageCleanupScheduler, ImageService imageService) {
        this.imageService = imageService;
    }
    
    /**
     * 만료된 이미지 정리 수동 실행
     */
    @PostMapping("/cleanup-images")
    public ResponseEntity<Map<String, Object>> manualImageCleanup() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int deletedCount = imageService.cleanupTempImages();
            
            response.put("success", true);
            response.put("message", "이미지 정리 완료");
            response.put("deletedCount", deletedCount);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이미지 정리 실패: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 스케줄러 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("schedulerEnabled", true);
        response.put("imageCleanupScheduler", "매일 새벽 4시 실행 (72시간 만료 이미지 정리)");
        response.put("systemHealthCheck", "매일 새벽 5시 실행");
        response.put("lastCheck", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

} 