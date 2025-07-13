package com.eos.blog.util;

import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

public class FileValidationUtil {
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_TOTAL_SIZE = 50 * 1024 * 1024; // 50MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    /**
     * 개별 파일 크기 검증
     */
    public static boolean isValidFileSize(MultipartFile file) {
        return file != null && file.getSize() <= MAX_FILE_SIZE;
    }
    
    /**
     * 전체 요청 크기 검증
     */
    public static boolean isValidTotalSize(List<MultipartFile> files) {
        if (files == null) return true;
        long totalSize = files.stream()
            .mapToLong(MultipartFile::getSize)
            .sum();
        return totalSize <= MAX_TOTAL_SIZE;
    }
    
    /**
     * 파일 확장자 검증
     */
    public static boolean isValidFileExtension(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) return false;
        
        String extension = getFileExtension(file.getOriginalFilename());
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * MIME 타입 검증
     */
    public static boolean isValidMimeType(MultipartFile file) {
        if (file == null || file.getContentType() == null) return false;
        return ALLOWED_MIME_TYPES.contains(file.getContentType().toLowerCase());
    }
    
    /**
     * 파일 확장자 추출
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 파일 크기 포맷팅 (사람이 읽기 쉬운 형태)
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    /**
     * 최대 파일 크기 반환
     */
    public static long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
    
    /**
     * 최대 전체 크기 반환
     */
    public static long getMaxTotalSize() {
        return MAX_TOTAL_SIZE;
    }
    
    /**
     * 허용된 확장자 목록 반환
     */
    public static List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }
} 