package com.eos.blog.service;

import com.eos.blog.dto.image.ImageDetailResponse;
import com.eos.blog.dto.image.ImageUploadResponse;
import com.eos.blog.exception.*;
import com.eos.blog.repository.ImageRepository;
import com.eos.blog.util.ImageProcessor;
import com.eos.blog.util.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.cache.annotation.*;

import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.*;

// 명시적으로 우리가 만든 Image 엔티티만 import
import com.eos.blog.entity.Image;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.eos.blog.util.FileValidationUtil;
import com.eos.blog.dto.ErrorResponse;
import com.eos.blog.exception.FileUploadException;
import com.eos.blog.repository.BlogRepository;

@Service
@Transactional
public class ImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    final private ImageRepository imageRepository;
    final private ImageProcessor imageProcessor;
    final private S3StorageService s3StorageService;
    final private BlogRepository blogRepository;

    @Autowired
    ImageService(ImageRepository imageRepository, ImageProcessor imageProcessor, S3StorageService s3StorageService, BlogRepository blogRepository){
        this.imageProcessor = imageProcessor;
        this.imageRepository = imageRepository;
        this.s3StorageService = s3StorageService;
        this.blogRepository = blogRepository;
    }
    
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
            throw new ValidationException("이미지 파일은 필수입니다.");
        }
        
        // 파일 크기 검증
        if (!FileValidationUtil.isValidFileSize(file)) {
            throw new ValidationException(
                String.format("파일 크기가 너무 큽니다. 최대 %s까지 업로드 가능합니다.", 
                    FileValidationUtil.formatFileSize(FileValidationUtil.getMaxFileSize()))
            );
        }
        
        // 파일 확장자 검증
        if (!FileValidationUtil.isValidFileExtension(file)) {
            throw new ValidationException(
                String.format("지원하지 않는 파일 형식입니다. 허용된 형식: %s", 
                    String.join(", ", FileValidationUtil.getAllowedExtensions()))
            );
        }
        
        // MIME 타입 검증
        if (!FileValidationUtil.isValidMimeType(file)) {
            throw new ValidationException("이미지 파일만 업로드 가능합니다.");
        }
    }

    /**
     * 다중 파일 검증
     */
    private void validateMultipleFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("이미지 파일은 필수입니다.");
        }
        
        // 파일 개수 제한
        if (files.size() > 10) {
            throw new ValidationException("최대 10개까지 업로드 가능합니다.");
        }
        
        // 전체 요청 크기 검증
        if (!FileValidationUtil.isValidTotalSize(files)) {
            throw new ValidationException(
                String.format("전체 요청 크기가 너무 큽니다. 최대 %s까지 업로드 가능합니다.", 
                    FileValidationUtil.formatFileSize(FileValidationUtil.getMaxTotalSize()))
            );
        }
        
        // 개별 파일 검증
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            
            if (file.isEmpty()) {
                throw new ValidationException("빈 파일은 업로드할 수 없습니다.");
            }
            
            // 개별 파일 크기 검증
            if (!FileValidationUtil.isValidFileSize(file)) {
                throw new ValidationException(
                    String.format("파일 %d의 크기가 너무 큽니다. 최대 %s까지 업로드 가능합니다.", 
                        i + 1, FileValidationUtil.formatFileSize(FileValidationUtil.getMaxFileSize()))
                );
            }
            
            // 파일 확장자 검증
            if (!FileValidationUtil.isValidFileExtension(file)) {
                throw new ValidationException(
                    String.format("파일 %d의 형식이 지원되지 않습니다. 허용된 형식: %s", 
                        i + 1, String.join(", ", FileValidationUtil.getAllowedExtensions()))
                );
            }
            
            // MIME 타입 검증
            if (!FileValidationUtil.isValidMimeType(file)) {
                throw new ValidationException(String.format("파일 %d이 이미지 파일이 아닙니다.", i + 1));
            }
        }
    }

    /**
     * 다중 이미지 업로드 (병렬 배치 처리)
     */
    public List<ImageUploadResponse> uploadMultipleImages(List<MultipartFile> files, String uploaderUid) throws IOException {
        return uploadMultipleImages(files, uploaderUid, false);
    }

    /**
     * 다중 이미지 업로드 (병렬 배치 처리) - 실패 허용 옵션 포함
     */
    public List<ImageUploadResponse> uploadMultipleImages(List<MultipartFile> files, String uploaderUid, boolean allowPartialFailure) throws IOException {
        logger.info("다중 이미지 업로드 시작: uploaderUid={}, fileCount={}, allowPartialFailure={}", 
                   uploaderUid, files.size(), allowPartialFailure);
        
        // 다중 파일 검증
        validateMultipleFiles(files);
        
        // 병렬 처리를 위한 CompletableFuture 리스트
        List<CompletableFuture<ImageUploadResult>> futures = new ArrayList<>();
        
        // 각 파일을 병렬로 처리
        for (MultipartFile file : files) {
            CompletableFuture<ImageUploadResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    ImageUploadResponse response = uploadImage(file, uploaderUid);
                    logger.info("개별 파일 업로드 성공: {}", file.getOriginalFilename());
                    return new ImageUploadResult(true, response, null, file.getOriginalFilename(), null);
                } catch (Exception e) {
                    logger.error("개별 파일 업로드 실패: {}", file.getOriginalFilename(), e);
                    String errorCode = determineErrorCode(e);
                    String errorMessage = String.format("파일 '%s' 업로드 실패: %s", 
                        file.getOriginalFilename(), e.getMessage());
                    return new ImageUploadResult(false, null, errorMessage, file.getOriginalFilename(), errorCode);
                }
            });
            futures.add(future);
        }
        
        // 모든 비동기 작업 완료 대기
        List<ImageUploadResponse> responses = new ArrayList<>();
        List<ErrorResponse.FileUploadError> fileErrors = new ArrayList<>();
        
        for (CompletableFuture<ImageUploadResult> future : futures) {
            try {
                ImageUploadResult result = future.get();
                if (result.isSuccess()) {
                    responses.add(result.getResponse());
                } else {
                    fileErrors.add(new ErrorResponse.FileUploadError(
                        result.getFileName(), 
                        result.getErrorCode(), 
                        result.getErrorMessage()
                    ));
                }
            } catch (Exception e) {
                logger.error("파일 업로드 중 예외 발생: {}", e.getMessage());
                fileErrors.add(new ErrorResponse.FileUploadError(
                    "unknown", 
                    "UNKNOWN_ERROR", 
                    "알 수 없는 오류로 인한 업로드 실패"
                ));
            }
        }
        
        // 실패한 파일이 있으면 처리
        if (!fileErrors.isEmpty()) {
            if (!allowPartialFailure) {
                // 모든 파일이 성공해야 하는 경우
                String message = String.format("일부 파일 업로드 실패 (%d/%d)", fileErrors.size(), files.size());
                throw new FileUploadException(ErrorResponse.fileUploadError(message, fileErrors));
            } else {
                // 부분 실패 허용하는 경우 경고 로그만 남김
                logger.warn("다중 이미지 업로드 부분 실패: uploaderUid={}, successCount={}, failureCount={}", 
                           uploaderUid, responses.size(), fileErrors.size());
            }
        }
        
        logger.info("다중 이미지 업로드 완료: uploaderUid={}, successCount={}, totalCount={}", 
                   uploaderUid, responses.size(), files.size());
        return responses;
    }

    /**
     * 예외 타입에 따른 에러 코드 결정
     */
    private String determineErrorCode(Exception e) {
        if (e instanceof ValidationException) {
            return "VALIDATION_ERROR";
        } else if (e instanceof IOException) {
            return "IO_ERROR";
        } else if (e instanceof IllegalArgumentException) {
            return "INVALID_ARGUMENT";
        } else {
            return "UNKNOWN_ERROR";
        }
    }

    /**
     * 이미지 업로드 결과를 담는 내부 클래스
     */
    private static class ImageUploadResult {
        private final boolean success;
        private final ImageUploadResponse response;
        private final String errorMessage;
        private final String fileName;
        private final String errorCode;

        public ImageUploadResult(boolean success, ImageUploadResponse response, String errorMessage, String fileName, String errorCode) {
            this.success = success;
            this.response = response;
            this.errorMessage = errorMessage;
            this.fileName = fileName;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() { return success; }
        public ImageUploadResponse getResponse() { return response; }
        public String getErrorMessage() { return errorMessage; }
        public String getFileName() { return fileName; }
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * 이미지 상세 정보 조회 (원본 S3 URL - 메타데이터용)
     */
    @Cacheable(value = "images", key = "#imageId")
    public ImageDetailResponse getImageDetail(Long imageId) {
        Optional<Image> imageOpt = imageRepository.findByIdAndIsDeletedFalse(imageId);
        
        if (imageOpt.isEmpty()) {
            throw new ResourceNotFoundException(String.format("이미지를 찾을 수 없습니다. ID: %d", imageId));
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
            throw new ResourceNotFoundException(String.format("이미지를 찾을 수 없습니다. ID: %d", imageId));
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
            throw new ResourceNotFoundException(String.format("썸네일을 찾을 수 없습니다. 이미지 ID: %d", imageId));
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
            throw new ForbiddenException("삭제 권한이 없거나 이미지를 찾을 수 없습니다: " + imageId);
        }
        
        Image image = imageOpt.get();
        
        // 공통 권한 검증 적용
        ServiceUtils.validateAuthorPermission(image.getUploaderUid(), requesterUid, "이미지");
        ServiceUtils.logOperation("삭제", "이미지", imageId.toString(), requesterUid);
        
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
            throw new ForbiddenException("확정 권한이 없거나 이미지를 찾을 수 없습니다: " + imageId);
        }
        
        Image image = imageOpt.get();
        
        // 공통 권한 검증 적용
        ServiceUtils.validateAuthorPermission(image.getUploaderUid(), requesterUid, "이미지");
        
        if (!image.getIsTemp()) {
            // 이미 확정된 이미지라면 그냥 성공으로 처리 (덮어씌우기)
            ServiceUtils.logOperation("재확정", "이미지", imageId.toString(), requesterUid);
            return;
        }
        
        imageRepository.confirmImage(imageId);
        ServiceUtils.logOperation("확정", "이미지", imageId.toString(), requesterUid);
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
     * 만료된 임시 이미지 정리 (내부 메서드)
     * 스케줄러에서 호출하는 메서드
     * 블로그에서 사용 중인 이미지는 보호
     */
    public void cleanupExpiredTempImages() {
        logger.info("만료된 임시 이미지 정리 시작");
        
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(72); // 72시간(3일) 전
        List<Image> expiredImages = imageRepository.findExpiredTempImages(expiryTime);
        
        int deletedCount = 0;
        int protectedCount = 0;
        
        for (Image image : expiredImages) {
            try {
                // 블로그에서 사용 중인지 확인
                if (isImageUsedInBlog(image.getId())) {
                    logger.info("블로그에서 사용 중인 이미지 보호: imageId={}", image.getId());
                    protectedCount++;
                    continue;
                }
                
                // 파일 시스템에서 삭제
                imageProcessor.deleteImage(image.getStorageUrl());
                if (image.getThumbnailUrl() != null) {
                    imageProcessor.deleteImage(image.getThumbnailUrl());
                }
                
                // DB에서 삭제 표시
                imageRepository.markAsDeleted(image.getId());
                deletedCount++;
                
                logger.info("만료된 임시 이미지 삭제: imageId={}", image.getId());
            } catch (Exception e) {
                logger.error("만료된 임시 이미지 삭제 실패: imageId={}", image.getId(), e);
            }
        }
        
        logger.info("만료된 임시 이미지 정리 완료: {}개 삭제, {}개 보호", deletedCount, protectedCount);
    }
    
    /**
     * 수동 임시 이미지 정리 (API 호출용)
     */
    public int cleanupTempImages() {
        logger.info("수동 임시 이미지 정리 시작");
        
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(72); // 72시간(3일) 전
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
     * 이미지가 블로그에서 사용 중인지 확인
     */
    private boolean isImageUsedInBlog(Long imageId) {
        try {
            // 1. 블로그의 썸네일 이미지로 사용 중인지 확인
            boolean isThumbnail = blogRepository.existsByThumbnailImageIdAndIsDeletedFalse(imageId);
            if (isThumbnail) {
                logger.debug("이미지가 썸네일로 사용 중: imageId={}", imageId);
                return true;
            }
            
            // 2. 블로그 content에서 이미지 ID 검색
            // JSON content에서 imageId 필드 검색
            boolean isUsedInContent = blogRepository.existsByContentContainingImageId(imageId);
            if (isUsedInContent) {
                logger.debug("이미지가 content에서 사용 중: imageId={}", imageId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("이미지 사용 여부 확인 실패: imageId={}", imageId, e);
            // 확인 실패 시 안전하게 보호
            return true;
        }
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
            throw new BusinessException("사용자 이미지 삭제 중 오류가 발생했습니다.", e);
        }
    }


} 