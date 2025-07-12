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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

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
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("이미지 파일만 업로드 가능합니다.");
        }
        try {
            ImageUploadResponse response = imageService.uploadImage(file, uid);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 업로드 중 오류가 발생했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
            if (e.getMessage().contains("권한이 없거나")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제 권한이 없습니다.");
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이미지 상세 조회
     * imageId 유효성, 존재 여부 체크
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<?> getImageDetail(@PathVariable Long imageId) {
        try {
            ImageDetailResponse response = imageService.getImageDetail(imageId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("이미지 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
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
     * 이미지 파일 직접 제공 (캐싱 헤더 포함)
     */
    @GetMapping("/{imageId}/file")
    public ResponseEntity<?> getImageFile(@PathVariable Long imageId) {
        try {
            ImageDetailResponse imageDetail = imageService.getImageDetail(imageId);
            Path imagePath = Paths.get(imageDetail.getStorageUrl());
            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] imageBytes = Files.readAllBytes(imagePath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(imageDetail.getMimeType()));
            headers.setCacheControl("public, max-age=31536000");
            headers.setETag("\"" + imageDetail.getImageId() + "\"");
            return ResponseEntity.ok().headers(headers).body(imageBytes);
        } catch (Exception e) {
            log.error("이미지 파일 제공 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 썸네일 파일 직접 제공
     */
    @GetMapping("/{imageId}/thumbnail")
    public ResponseEntity<?> getThumbnailFile(@PathVariable Long imageId) {
        try {
            ImageDetailResponse imageDetail = imageService.getImageDetail(imageId);
            if (imageDetail.getThumbnailUrl() == null) {
                return ResponseEntity.notFound().build();
            }
            Path thumbnailPath = Paths.get(imageDetail.getThumbnailUrl());
            if (!Files.exists(thumbnailPath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] thumbnailBytes = Files.readAllBytes(thumbnailPath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(imageDetail.getMimeType()));
            headers.setCacheControl("public, max-age=31536000");
            headers.setETag("\"" + imageDetail.getImageId() + "_thumb\"");
            return ResponseEntity.ok().headers(headers).body(thumbnailBytes);
        } catch (Exception e) {
            log.error("썸네일 파일 제공 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


} 