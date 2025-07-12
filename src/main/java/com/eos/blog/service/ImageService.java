package com.eos.blog.service;

import com.eos.blog.dto.ImageDetailResponse;
import com.eos.blog.dto.ImageUploadResponse;
import com.eos.blog.model.Image;
import com.eos.blog.repository.ImageRepository;
import com.eos.blog.util.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.cache.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private ImageProcessor imageProcessor;
    
    /**
     * 이미지 업로드
     */
    public ImageUploadResponse uploadImage(MultipartFile file, String uploaderUid) throws IOException {
        logger.info("이미지 업로드 시작: uploaderUid={}, filename={}", uploaderUid, file.getOriginalFilename());
        
        // 이미지 처리 및 저장
        ImageProcessor.ProcessedImageInfo processedInfo = imageProcessor.processAndSaveImage(file);
        
        // DB에 메타데이터 저장
        Image image = new Image(
            uploaderUid,
            processedInfo.getOriginalPath(),
            processedInfo.getFileSize(),
            processedInfo.getMimeType()
        );
        image.setThumbnailUrl(processedInfo.getThumbnailPath());
        
        Image savedImage = imageRepository.save(image);
        
        logger.info("이미지 업로드 완료: imageId={}", savedImage.getId());
        
        return new ImageUploadResponse(
            savedImage.getId(),
            savedImage.getStorageUrl(),
            savedImage.getThumbnailUrl(),
            savedImage.getUploadTime(),
            savedImage.getFileSize(),
            savedImage.getMimeType()
        );
    }
    
    /**
     * 이미지 상세 정보 조회
     */
    @Cacheable(value = "images", key = "#imageId")
    public ImageDetailResponse getImageDetail(Long imageId) {
        Optional<Image> imageOpt = imageRepository.findByIdAndIsDeletedFalse(imageId);
        
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("이미지를 찾을 수 없습니다: " + imageId);
        }
        
        Image image = imageOpt.get();
        return new ImageDetailResponse(
            image.getId(),
            image.getStorageUrl(),
            image.getThumbnailUrl(),
            image.getUploaderUid(),
            image.getUploadTime(),
            image.getFileSize(),
            image.getMimeType(),
            image.getIsTemp()
        );
    }
    
    /**
     * 이미지 삭제
     */
    @CacheEvict(value = "images", key = "#imageId")
    public void deleteImage(Long imageId, String requesterUid) {
        Optional<Image> imageOpt = imageRepository.findByIdAndUploaderUidAndIsDeletedFalse(imageId, requesterUid);
        
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("삭제 권한이 없거나 이미지를 찾을 수 없습니다: " + imageId);
        }
        
        Image image = imageOpt.get();
        
        // 파일 시스템에서 삭제
        imageProcessor.deleteImage(image.getStorageUrl());
        if (image.getThumbnailUrl() != null) {
            imageProcessor.deleteImage(image.getThumbnailUrl());
        }
        
        // DB에서 삭제 표시
        imageRepository.markAsDeleted(imageId);
        
        logger.info("이미지 삭제 완료: imageId={}, uploaderUid={}", imageId, requesterUid);
    }
    
    /**
     * 이미지 사용 확정 (임시 → 확정)
     */
    @CacheEvict(value = "images", key = "#imageId")
    public void confirmImage(Long imageId, String requesterUid) {
        Optional<Image> imageOpt = imageRepository.findByIdAndUploaderUidAndIsDeletedFalse(imageId, requesterUid);
        
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("확정 권한이 없거나 이미지를 찾을 수 없습니다: " + imageId);
        }
        
        Image image = imageOpt.get();
        if (!image.getIsTemp()) {
            throw new RuntimeException("이미 확정된 이미지입니다: " + imageId);
        }
        
        imageRepository.confirmImage(imageId);
        logger.info("이미지 확정 완료: imageId={}, uploaderUid={}", imageId, requesterUid);
    }
    
    /**
     * 업로더별 이미지 목록 조회
     */
    public List<ImageDetailResponse> getImagesByUploader(String uploaderUid) {
        List<Image> images = imageRepository.findByUploaderUidAndIsDeletedFalseOrderByUploadTimeDesc(uploaderUid);
        
        return images.stream()
            .map(image -> new ImageDetailResponse(
                image.getId(),
                image.getStorageUrl(),
                image.getThumbnailUrl(),
                image.getUploaderUid(),
                image.getUploadTime(),
                image.getFileSize(),
                image.getMimeType(),
                image.getIsTemp()
            ))
            .toList();
    }
    
    /**
     * 만료된 임시 이미지 정리 (스케줄러)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void cleanupExpiredTempImages() {
        logger.info("만료된 임시 이미지 정리 시작");
        
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(24);
        List<Image> expiredImages = imageRepository.findExpiredTempImages(expiryTime);
        
        for (Image image : expiredImages) {
            try {
                // 파일 시스템에서 삭제
                imageProcessor.deleteImage(image.getStorageUrl());
                if (image.getThumbnailUrl() != null) {
                    imageProcessor.deleteImage(image.getThumbnailUrl());
                }
                
                // DB에서 삭제 표시
                imageRepository.markAsDeleted(image.getId());
                
                logger.info("만료된 임시 이미지 삭제: imageId={}", image.getId());
            } catch (Exception e) {
                logger.error("만료된 임시 이미지 삭제 실패: imageId={}", image.getId(), e);
            }
        }
        
        logger.info("만료된 임시 이미지 정리 완료: {}개 삭제", expiredImages.size());
    }
    
    /**
     * 수동 임시 이미지 정리 (API 호출용)
     */
    public int cleanupTempImages() {
        logger.info("수동 임시 이미지 정리 시작");
        
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(24);
        List<Image> expiredImages = imageRepository.findExpiredTempImages(expiryTime);
        
        int deletedCount = 0;
        for (Image image : expiredImages) {
            try {
                // 파일 시스템에서 삭제
                imageProcessor.deleteImage(image.getStorageUrl());
                if (image.getThumbnailUrl() != null) {
                    imageProcessor.deleteImage(image.getThumbnailUrl());
                }
                
                // DB에서 삭제 표시
                imageRepository.markAsDeleted(image.getId());
                deletedCount++;
                
                logger.info("수동 임시 이미지 삭제: imageId={}", image.getId());
            } catch (Exception e) {
                logger.error("수동 임시 이미지 삭제 실패: imageId={}", image.getId(), e);
            }
        }
        
        logger.info("수동 임시 이미지 정리 완료: {}개 삭제", deletedCount);
        return deletedCount;
    }
} 