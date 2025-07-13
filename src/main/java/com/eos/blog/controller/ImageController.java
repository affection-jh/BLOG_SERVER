/*
 * [유효성 보장]
 * - 업로드: file, uid 모두 null/빈값 불가, file은 이미지 파일이어야 함
 * - 삭제/확정: Authorization 헤더(UID) 필수, 권한 체크
 * - 조회: imageId 유효성, 존재 여부 체크
 * - 임시 정리: 삭제 대상만 처리
 */
package com.eos.blog.controller;

import com.eos.blog.dto.ImageDetailResponse;
import com.eos.blog.dto.ImageUploadResponse;
import com.eos.blog.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 5;

    @Autowired
    private ImageService imageService;

    /**
     * 이미지 업로드
     * file, uid 모두 null/빈값 불가, file은 이미지 파일이어야 함
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                         @RequestParam("uid") String uid) {
        //  @AuthenticationPrincipal String uid 실세로는 검증 후 uid반환 해야함
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("이미지 파일은 필수입니다.");
        }
        if (uid == null || uid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("사용자 UID는 필수입니다.");
        }
        
        uid = uid.trim();
        try {
            ImageUploadResponse response = imageService.uploadImage(file, uid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 업로드 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이미지 삭제
     * Authorization 헤더(UID) 필수, 권한 체크
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable Long imageId,
                                         @RequestHeader("Authorization") String authorization) {
        String uid = authorization == null ? null : authorization.trim();
        try {
            imageService.deleteImage(imageId, uid);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("이미지 삭제 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("이미지를 찾을 수 없습니다.");
            } else if (e.getMessage().contains("삭제 권한이 없거나")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제 권한이 없습니다.");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 삭제 중 오류가 발생했습니다.");
        }
    }


    /**
     * 이미지 사용 확정
     * Authorization 헤더(UID) 필수, 권한 체크
     */
    @PatchMapping("/{imageId}/confirm")
    public ResponseEntity<?> confirmImage(@PathVariable Long imageId,
                                          @RequestHeader("Authorization") String authorization) {
        String uid = authorization == null ? null : authorization.trim();
        try {
            imageService.confirmImage(imageId, uid);
            Map<String, String> response = new HashMap<>();
            response.put("message", "이미지가 확정되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("이미지 확정 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한이 없거나")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("확정 권한이 없습니다.");
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 확정 중 오류가 발생했습니다.");
        }
    }

    /**
     * 임시 이미지 정리
     */
    @DeleteMapping("/cleanup-temp")
    public ResponseEntity<?> cleanupTempImages() {
        try {
            int deletedCount = imageService.cleanupTempImages();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "임시 이미지 정리 완료");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("임시 이미지 정리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("임시 이미지 정리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 업로더별 이미지 목록 조회
     */
    @GetMapping("/uploader/{uid}")
    public ResponseEntity<?> getImagesByUploader(@PathVariable String uid) {

        if (uid == null || uid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("업로더 UID는 필수입니다.");
        }
        uid = uid.trim();
        try {
            List<ImageDetailResponse> images = imageService.getImagesByUploader(uid);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            log.error("업로더별 이미지 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 목록 조회 중 오류가 발생했습니다.");
        }
    }


    /**
     * 이미지 접근 URL 생성 (5분 만료 presigned URL)
     */
    @GetMapping("/{imageId}/url")
    public ResponseEntity<?> getImageAccessUrl(@PathVariable Long imageId) {
        try {
            String accessUrl = imageService.generateImageAccessUrl(imageId);
            Map<String, String> response = new HashMap<>();
            response.put("accessUrl", accessUrl);
            response.put("expirationMinutes", String.valueOf(PRESIGNED_URL_EXPIRATION_MINUTES));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("이미지 접근 URL 생성 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 썸네일 접근 URL 생성 (5분 만료 presigned URL)
     */
    @GetMapping("/{imageId}/thumbnail/url")
    public ResponseEntity<?> getThumbnailAccessUrl(@PathVariable Long imageId) {
        try {
            String accessUrl = imageService.generateThumbnailAccessUrl(imageId);
            Map<String, String> response = new HashMap<>();
            response.put("accessUrl", accessUrl);
            response.put("expirationMinutes", String.valueOf(PRESIGNED_URL_EXPIRATION_MINUTES));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("썸네일 접근 URL 생성 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 여러 이미지 접근 URL 배치 생성 (5분 만료 presigned URL)
     */
    @GetMapping("/batch/url")
    public ResponseEntity<?> getBatchImageAccessUrls(@RequestParam List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("이미지 ID 목록은 필수입니다.");
            }
            if (ids.size() > 20) {
                return ResponseEntity.badRequest().body("최대 20개까지 조회 가능합니다.");
            }
            
            List<Map<String, String>> results = imageService.generateBatchImageAccessUrls(ids);
            Map<String, Object> response = new HashMap<>();
            response.put("images", results);
            response.put("expirationMinutes", PRESIGNED_URL_EXPIRATION_MINUTES);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("배치 이미지 접근 URL 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("배치 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 다중 이미지 업로드
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadMultipleImages(@RequestParam(value = "files", required = false) List<MultipartFile> files,
                                                  @RequestParam("uid") String uid) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("'files' 파라미터로 이미지 파일들을 전송해주세요. (FormData 사용)");
        }
        if (uid == null || uid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("사용자 UID는 필수입니다.");
        }
        
        uid = uid.trim();
        
        try {
            List<ImageUploadResponse> responses = imageService.uploadMultipleImages(files, uid);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("다중 이미지 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("이미지 업로드 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이미지 상세 정보 조회 (접근 URL 포함)
     */
    @GetMapping("/{imageId}/detail")
    public ResponseEntity<?> getImageDetailWithAccessUrl(@PathVariable Long imageId) {
        try {
            ImageDetailResponse imageDetail = imageService.getImageDetailWithAccessUrl(imageId);
            return ResponseEntity.ok(imageDetail);
        } catch (Exception e) {
            log.error("이미지 상세 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 사용자별 폴더의 모든 이미지 삭제
     */
    @DeleteMapping("/user/{uid}/all")
    public ResponseEntity<?> deleteAllUserImages(@PathVariable String uid,
                                                 @RequestHeader("Authorization") String authorization) {
        String requesterUid = authorization == null ? null : authorization.trim();
        
        if (!uid.equals(requesterUid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("자신의 이미지만 삭제할 수 있습니다.");
        }
        
        try {
            imageService.deleteAllUserImages(uid);
            Map<String, String> response = new HashMap<>();
            response.put("message", "사용자의 모든 이미지가 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 이미지 전체 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("이미지 삭제 중 오류가 발생했습니다.");
        }
    }


} 