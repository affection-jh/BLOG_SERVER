/*
 * [유효성 보장]
 * - 업로드: file, uid 모두 null/빈값 불가, file은 이미지 파일이어야 함
 * - 삭제/확정: Authorization 헤더(UID) 필수, 권한 체크
 * - 조회: imageId 유효성, 존재 여부 체크
 * - 임시 정리: 삭제 대상만 처리
 */
package com.eos.blog.controller;

import com.eos.blog.exception.ValidationException;
import com.eos.blog.dto.image.ImageDetailResponse;
import com.eos.blog.dto.image.ImageUploadResponse;
import com.eos.blog.exception.ForbiddenException;
import com.eos.blog.exception.UnauthorizedException;
import com.eos.blog.service.ImageService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 5;
    final private ImageService imageService;

    @Autowired
    ImageController(ImageService imageService){
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file,
                                         @RequestParam("uid") String uid) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ValidationException("이미지 파일은 필수입니다.");
        }
        if (uid == null || uid.trim().isEmpty()) {
            throw new ValidationException("사용자 UID는 필수입니다.");
        }
        
        uid = uid.trim();
        ImageUploadResponse response = imageService.uploadImage(file, uid);
        return ResponseEntity.ok(response);
    }

    /**
     * 이미지 삭제
     * Authorization 헤더(UID) 필수, 권한 체크
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId,
                                         @RequestHeader("Authorization") String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        String uid = authorization.trim();
        imageService.deleteImage(imageId, uid);
        return ResponseEntity.noContent().build();
    }

    /**
     * 이미지 사용 확정
     * Authorization 헤더(UID) 필수, 권한 체크
     */
    @PatchMapping("/{imageId}/confirm")
    public ResponseEntity<Map<String, String>> confirmImage(@PathVariable Long imageId,
                                          @RequestHeader("Authorization") String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        String uid = authorization.trim();
        imageService.confirmImage(imageId, uid);
        Map<String, String> response = new HashMap<>();
        response.put("message", "이미지가 확정되었습니다");
        return ResponseEntity.ok(response);
    }

    /**
     * 임시 이미지 정리
     */
    @DeleteMapping("/cleanup-temp")
    public ResponseEntity<Map<String, Object>> cleanupTempImages() {
        int deletedCount = imageService.cleanupTempImages();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "임시 이미지 정리 완료");
        response.put("deletedCount", deletedCount);
        return ResponseEntity.ok(response);
    }

    /**
     * 업로더별 이미지 목록 조회
     */
    @GetMapping("/uploader/{uid}")
    public ResponseEntity<List<ImageDetailResponse>> getImagesByUploader(@PathVariable String uid) {

        if (uid == null || uid.trim().isEmpty()) {
            throw new ValidationException("업로더 UID는 필수입니다.");
        }
        uid = uid.trim();
        List<ImageDetailResponse> images = imageService.getImagesByUploader(uid);
        return ResponseEntity.ok(images);
    }

    /**
     * 이미지 접근 URL 생성 (5분 만료 presigned URL)
     */
    @GetMapping("/{imageId}/url")
    public ResponseEntity<Map<String, String>> getImageAccessUrl(@PathVariable Long imageId) {
        String accessUrl = imageService.generateImageAccessUrl(imageId);
        Map<String, String> response = new HashMap<>();
        response.put("accessUrl", accessUrl);
        response.put("expirationMinutes", String.valueOf(PRESIGNED_URL_EXPIRATION_MINUTES));
        return ResponseEntity.ok(response);
    }

    /**
     * 썸네일 접근 URL 생성 (5분 만료 presigned URL)
     */
    @GetMapping("/{imageId}/thumbnail/url")
    public ResponseEntity<Map<String, String>> getThumbnailAccessUrl(@PathVariable Long imageId) {
        String accessUrl = imageService.generateThumbnailAccessUrl(imageId);
        Map<String, String> response = new HashMap<>();
        response.put("accessUrl", accessUrl);
        response.put("expirationMinutes", String.valueOf(PRESIGNED_URL_EXPIRATION_MINUTES));
        return ResponseEntity.ok(response);
    }

    /**
     * 여러 이미지 접근 URL 배치 생성 (5분 만료 presigned URL)
     */
    @GetMapping("/batch/url")
    public ResponseEntity<Map<String, Object>> getBatchImageAccessUrls(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ValidationException("이미지 ID 목록은 필수입니다.");
        }
        if (ids.size() > 20) {
            throw new ValidationException("최대 20개까지 조회 가능합니다.");
        }
        
        List<Map<String, String>> results = imageService.generateBatchImageAccessUrls(ids);
        Map<String, Object> response = new HashMap<>();
        response.put("images", results);
        response.put("expirationMinutes", PRESIGNED_URL_EXPIRATION_MINUTES);
        return ResponseEntity.ok(response);
    }

    /**
     * 다중 이미지 업로드
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<List<ImageUploadResponse>> uploadMultipleImages(@RequestParam(value = "files", required = false) List<MultipartFile> files,
                                                  @RequestParam("uid") String uid) throws IOException {
        return uploadMultipleImages(files, uid, false);
    }

    /**
     * 다중 이미지 업로드 (실패 허용 옵션 포함)
     */
    @PostMapping("/upload-multiple-flexible")
    public ResponseEntity<List<ImageUploadResponse>> uploadMultipleImages(@RequestParam(value = "files", required = false) List<MultipartFile> files,
                                                  @RequestParam("uid") String uid,
                                                  @RequestParam(value = "allowPartialFailure", defaultValue = "false") boolean allowPartialFailure) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("'files' 파라미터로 이미지 파일들을 전송해주세요. (FormData 사용)");
        }
        if (uid == null || uid.trim().isEmpty()) {
            throw new ValidationException("사용자 UID는 필수입니다.");
        }
        
        uid = uid.trim();
        List<ImageUploadResponse> responses = imageService.uploadMultipleImages(files, uid, allowPartialFailure);
        return ResponseEntity.ok(responses);
    }

    /**
     * 이미지 상세 정보 조회 (접근 URL 포함)
     */
    @GetMapping("/{imageId}/detail")
    public ResponseEntity<ImageDetailResponse> getImageDetailWithAccessUrl(@PathVariable Long imageId) {
        ImageDetailResponse imageDetail = imageService.getImageDetailWithAccessUrl(imageId);
        return ResponseEntity.ok(imageDetail);
    }

    /**
     * 사용자별 폴더의 모든 이미지 삭제
     */
    @DeleteMapping("/user/{uid}/all")
    public ResponseEntity<Map<String, String>> deleteAllUserImages(@PathVariable String uid,
                                                 @RequestHeader("Authorization") String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        String requesterUid = authorization.trim();
        
        if (!uid.equals(requesterUid)) {
            throw new ForbiddenException("자신의 이미지만 삭제할 수 있습니다.");
        }
        
        imageService.deleteAllUserImages(uid);
        Map<String, String> response = new HashMap<>();
        response.put("message", "사용자의 모든 이미지가 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }
} 