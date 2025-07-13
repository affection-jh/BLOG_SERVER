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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import com.eos.blog.util.FileValidationUtil;

@Service
@Transactional
public class ImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private ImageProcessor imageProcessor;

    @Autowired
    private S3StorageService s3StorageService;
    
    /**
     * 이미지 업로드
     */
    public ImageUploadResponse uploadImage(MultipartFile file, String uploaderUid) throws IOException {
        logger.info("이미지 업로드 시작: uploaderUid={}, filename={}", uploaderUid, file.getOriginalFilename());
        
        // 파일 검증
        validateFile(file);
        
        // 이미지 처리 및 저장 (uid별 폴더에 저장)
        ImageProcessor.ProcessedImageInfo processedInfo = imageProcessor.processAndSaveImage(file, uploaderUid);
        
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
        
        // 프리사인 URL 생성
        String originalKey = savedImage.getStorageUrl().substring(savedImage.getStorageUrl().indexOf(".com/") + 5);
        String thumbnailKey = savedImage.getThumbnailUrl().substring(savedImage.getThumbnailUrl().indexOf(".com/") + 5);
        
        String accessUrl = s3StorageService.generatePresignedUrl(originalKey);
        String thumbnailAccessUrl = s3StorageService.generatePresignedUrl(thumbnailKey);
        
        return new ImageUploadResponse(
            savedImage.getId(),
            accessUrl,
            thumbnailAccessUrl,
            savedImage.getUploadTime(),
            savedImage.getFileSize(),
            savedImage.getMimeType()
        );
    }

    /**
     * 단일 파일 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일은 필수입니다.");
        }
        
        // 파일 크기 검증
        if (!FileValidationUtil.isValidFileSize(file)) {
            throw new IllegalArgumentException(
                String.format("파일 크기가 너무 큽니다. 최대 %s까지 업로드 가능합니다.", 
                    FileValidationUtil.formatFileSize(FileValidationUtil.getMaxFileSize()))
            );
        }
        
        // 파일 확장자 검증
        if (!FileValidationUtil.isValidFileExtension(file)) {
            throw new IllegalArgumentException(
                String.format("지원하지 않는 파일 형식입니다. 허용된 형식: %s", 
                    String.join(", ", FileValidationUtil.getAllowedExtensions()))
            );
        }
        
        // MIME 타입 검증
        if (!FileValidationUtil.isValidMimeType(file)) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
    }

    /**
     * 다중 파일 검증
     */
    private void validateMultipleFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일은 필수입니다.");
        }
        
        // 파일 개수 제한
        if (files.size() > 10) {
            throw new IllegalArgumentException("최대 10개까지 업로드 가능합니다.");
        }
        
        // 전체 요청 크기 검증
        if (!FileValidationUtil.isValidTotalSize(files)) {
            throw new IllegalArgumentException(
                String.format("전체 요청 크기가 너무 큽니다. 최대 %s까지 업로드 가능합니다.", 
                    FileValidationUtil.formatFileSize(FileValidationUtil.getMaxTotalSize()))
            );
        }
        
        // 개별 파일 검증
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            
            if (file.isEmpty()) {
                throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
            }
            
            // 개별 파일 크기 검증
            if (!FileValidationUtil.isValidFileSize(file)) {
                throw new IllegalArgumentException(
                    String.format("파일 %d의 크기가 너무 큽니다. 최대 %s까지 업로드 가능합니다.", 
                        i + 1, FileValidationUtil.formatFileSize(FileValidationUtil.getMaxFileSize()))
                );
            }
            
            // 파일 확장자 검증
            if (!FileValidationUtil.isValidFileExtension(file)) {
                throw new IllegalArgumentException(
                    String.format("파일 %d의 형식이 지원되지 않습니다. 허용된 형식: %s", 
                        i + 1, String.join(", ", FileValidationUtil.getAllowedExtensions()))
                );
            }
            
            // MIME 타입 검증
            if (!FileValidationUtil.isValidMimeType(file)) {
                throw new IllegalArgumentException(String.format("파일 %d이 이미지 파일이 아닙니다.", i + 1));
            }
        }
    }

    /**
     * 다중 이미지 업로드 (병렬 배치 처리)
     */
    public List<ImageUploadResponse> uploadMultipleImages(List<MultipartFile> files, String uploaderUid) throws IOException {
        logger.info("다중 이미지 업로드 시작: uploaderUid={}, fileCount={}", uploaderUid, files.size());
        
        // 다중 파일 검증
        validateMultipleFiles(files);
        
        // 병렬 처리를 위한 CompletableFuture 리스트
        List<CompletableFuture<ImageUploadResponse>> futures = new ArrayList<>();
        
        // 각 파일을 병렬로 처리
        for (MultipartFile file : files) {
            CompletableFuture<ImageUploadResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    ImageUploadResponse response = uploadImage(file, uploaderUid);
                    logger.info("개별 파일 업로드 성공: {}", file.getOriginalFilename());
                    return response;
                } catch (Exception e) {
                    logger.error("개별 파일 업로드 실패: {}", file.getOriginalFilename(), e);
                    throw new CompletionException(e);
                }
            });
            futures.add(future);
        }
        
        // 모든 비동기 작업 완료 대기
        List<ImageUploadResponse> responses = new ArrayList<>();
        for (CompletableFuture<ImageUploadResponse> future : futures) {
            try {
                ImageUploadResponse response = future.get();
                responses.add(response);
            } catch (Exception e) {
                logger.error("파일 업로드 중 예외 발생: {}", e.getMessage());
                // 실패한 파일은 건너뛰고 계속 진행
            }
        }
        
        logger.info("다중 이미지 업로드 완료: uploaderUid={}, successCount={}, totalCount={}", 
                   uploaderUid, responses.size(), files.size());
        return responses;
    }
    
    /**
     * 이미지 상세 정보 조회 (원본 S3 URL - 메타데이터용)
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
     * 이미지 상세 정보 조회 (프리사인 URL - 접근용)
     */
    public ImageDetailResponse getImageDetailWithAccessUrl(Long imageId) {
        Optional<Image> imageOpt = imageRepository.findByIdAndIsDeletedFalse(imageId);
        
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("이미지를 찾을 수 없습니다: " + imageId);
        }
        
        Image image = imageOpt.get();
        
        // 프리사인 URL 생성
        String originalKey = image.getStorageUrl().substring(image.getStorageUrl().indexOf(".com/") + 5);
        String thumbnailKey = image.getThumbnailUrl().substring(image.getThumbnailUrl().indexOf(".com/") + 5);
        
        String accessUrl = s3StorageService.generatePresignedUrl(originalKey);
        String thumbnailAccessUrl = s3StorageService.generatePresignedUrl(thumbnailKey);
        
        return new ImageDetailResponse(
            image.getId(),
            accessUrl,
            thumbnailAccessUrl,
            image.getUploaderUid(),
            image.getUploadTime(),
            image.getFileSize(),
            image.getMimeType(),
            image.getIsTemp()
        );
    }


        /**
     * 이미지 접근 URL 생성 (5분 만료)
     */
    public String generateImageAccessUrl(Long imageId) {
        ImageDetailResponse imageDetail = getImageDetail(imageId);
        String url = imageDetail.getAccessUrl();
        // S3 presigned URL만 반환 (5분 만료)
        String key = url.substring(url.indexOf(".com/") + 5);
        return s3StorageService.generatePresignedUrl(key);
    }

    /**
     * 썸네일 접근 URL 생성 (5분 만료)
     */
    public String generateThumbnailAccessUrl(Long imageId) {
        ImageDetailResponse imageDetail = getImageDetail(imageId);
        String thumbnailUrl = imageDetail.getThumbnailAccessUrl();
        if (thumbnailUrl == null) {
            throw new RuntimeException("썸네일이 존재하지 않습니다: " + imageId);
        }
        // S3 presigned URL만 반환 (5분 만료)
        String key = thumbnailUrl.substring(thumbnailUrl.indexOf(".com/") + 5);
        return s3StorageService.generatePresignedUrl(key);
    }

    /**
     * 여러 이미지 접근 URL 배치 생성 (병렬 처리, 5분 만료)
     */
    public List<Map<String, String>> generateBatchImageAccessUrls(List<Long> imageIds) {
        logger.info("배치 이미지 접근 URL 생성 시작: imageCount={}", imageIds.size());
        
        // 병렬 처리를 위한 CompletableFuture 리스트
        List<CompletableFuture<Map<String, String>>> futures = new ArrayList<>();
        
        // 각 이미지 ID를 병렬로 처리
        for (Long imageId : imageIds) {
            CompletableFuture<Map<String, String>> future = CompletableFuture.supplyAsync(() -> {
                Map<String, String> result = new HashMap<>();
                result.put("imageId", imageId.toString());
                
                try {
                    String accessUrl = generateImageAccessUrl(imageId);
                    String thumbnailAccessUrl = generateThumbnailAccessUrl(imageId);
                    
                    result.put("accessUrl", accessUrl);
                    result.put("thumbnailAccessUrl", thumbnailAccessUrl);
                    
                } catch (Exception e) {
                    logger.error("이미지 {} 접근 URL 생성 실패: {}", imageId, e.getMessage());
                    result.put("accessUrl", null);
                    result.put("thumbnailAccessUrl", null);
                    result.put("error", "이미지를 찾을 수 없습니다");
                }
                
                return result;
            });
            futures.add(future);
        }
        
        // 모든 비동기 작업 완료 대기
        List<Map<String, String>> results = new ArrayList<>();
        for (CompletableFuture<Map<String, String>> future : futures) {
            try {
                Map<String, String> result = future.get();
                results.add(result);
            } catch (Exception e) {
                logger.error("배치 처리 중 예외 발생: {}", e.getMessage());
            }
        }
        
        logger.info("배치 이미지 접근 URL 생성 완료: successCount={}, totalCount={}", 
                   results.size(), imageIds.size());
        return results;
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
     * 업로더별 이미지 목록 조회 (프리사인 URL)
     */
    public List<ImageDetailResponse> getImagesByUploader(String uploaderUid) {
        List<Image> images = imageRepository.findByUploaderUidAndIsDeletedFalseOrderByUploadTimeDesc(uploaderUid);
        
        return images.stream()
            .map(image -> {
                // 프리사인 URL 생성
                String originalKey = image.getStorageUrl().substring(image.getStorageUrl().indexOf(".com/") + 5);
                String thumbnailKey = image.getThumbnailUrl().substring(image.getThumbnailUrl().indexOf(".com/") + 5);
                
                String accessUrl = s3StorageService.generatePresignedUrl(originalKey);
                String thumbnailAccessUrl = s3StorageService.generatePresignedUrl(thumbnailKey);
                
                return new ImageDetailResponse(
                    image.getId(),
                    accessUrl,
                    thumbnailAccessUrl,
                    image.getUploaderUid(),
                    image.getUploadTime(),
                    image.getFileSize(),
                    image.getMimeType(),
                    image.getIsTemp()
                );
            })
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

    /**
     * 사용자별 폴더의 모든 이미지 삭제
     */
    public void deleteAllUserImages(String uploaderUid) {
        logger.info("사용자 이미지 전체 삭제 시작: uploaderUid={}", uploaderUid);
        
        try {
            // DB에서 사용자의 모든 이미지 조회
            List<Image> userImages = imageRepository.findByUploaderUidAndIsDeletedFalseOrderByUploadTimeDesc(uploaderUid);
            
            // S3에서 사용자 폴더 전체 삭제
            s3StorageService.deleteUserFolder(uploaderUid);
            
            // DB에서 모든 이미지 삭제
            imageRepository.deleteAll(userImages);
            
            logger.info("사용자 이미지 전체 삭제 완료: uploaderUid={}, deletedCount={}", 
                       uploaderUid, userImages.size());
            
        } catch (Exception e) {
            logger.error("사용자 이미지 전체 삭제 실패: uploaderUid={}", uploaderUid, e);
            throw new RuntimeException("사용자 이미지 삭제 중 오류가 발생했습니다.", e);
        }
    }


} 