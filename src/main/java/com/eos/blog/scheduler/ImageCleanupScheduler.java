package com.eos.blog.scheduler;

import com.eos.blog.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 이미지 정리 스케줄러
 * - 72시간(3일)마다 만료된 임시 이미지 삭제
 * - 매일 새벽 2시에 실행
 */
@Component
public class ImageCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageCleanupScheduler.class);
    
    private final ImageService imageService;
    
    @Autowired
    public ImageCleanupScheduler(ImageService imageService) {
        this.imageService = imageService;
    }
    
    /**
     * 만료된 임시 이미지 정리
     * 매일 새벽 4시에 실행 (cron: 0 0 4 * * ?)
     * 
     * cron 표현식 설명:
     * - 0: 초 (0초)
     * - 0: 분 (0분) 
     * - 5: 시 (5시) - 사용자 활동이 적은 시간
     * - *: 일 (매일)
     * - *: 월 (매월)
     * - ?: 요일 (요일 무관)
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void cleanupExpiredImages() {
        logger.info("=== 만료된 임시 이미지 정리 스케줄러 시작 ===");
        logger.info("실행 시간: {}", LocalDateTime.now());
        
        try {
            int deletedCount = imageService.cleanupTempImages();
            logger.info("=== 만료된 임시 이미지 정리 완료: {}개 삭제 ===", deletedCount);
        } catch (Exception e) {
            logger.error("=== 만료된 임시 이미지 정리 중 오류 발생 ===", e);
        }
    }

   
} 