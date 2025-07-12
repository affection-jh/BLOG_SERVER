package com.eos.blog.util;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.web.multipart.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Component
public class ImageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);
    
    @Value("${app.image.upload.path}")
    private String uploadPath;
    
    @Value("${app.image.thumbnail.path}")
    private String thumbnailPath;
    
    @Value("${app.image.max-dimension:2048}")
    private int maxDimension;
    
    @Value("${app.image.thumbnail.max-dimension:300}")
    private int thumbnailMaxDimension;
    
    /**
     * 이미지 파일을 처리하고 저장합니다.
     * @param file 업로드된 이미지 파일
     * @return 저장된 파일 정보
     */
    public ProcessedImageInfo processAndSaveImage(MultipartFile file) throws IOException {
        // 디렉토리 생성
        createDirectoriesIfNotExist();
        
        // 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + extension;
        
        // 원본 이미지 저장
        String originalPath = uploadPath + File.separator + filename;
        File originalFile = new File(originalPath);
        
        // 이미지 리사이징 및 저장
        BufferedImage resizedImage = resizeImage(file);
        ImageIO.write(resizedImage, getImageFormat(extension), originalFile);
        
        // 썸네일 생성 및 저장
        String thumbnailFilename = "thumb_" + filename;
        String thumbnailPath = this.thumbnailPath + File.separator + thumbnailFilename;
        File thumbnailFile = new File(thumbnailPath);
        
        BufferedImage thumbnail = createThumbnail(resizedImage);
        ImageIO.write(thumbnail, getImageFormat(extension), thumbnailFile);
        
        return new ProcessedImageInfo(
            originalPath,
            thumbnailPath,
            originalFile.length(),
            file.getContentType()
        );
    }
    
    /**
     * 이미지 리사이징 (자동 비율 유지)
     */
    private BufferedImage resizeImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        // 원본 크기가 설정값보다 작으면 원본 그대로 사용
        if (originalImage.getWidth() <= maxDimension && originalImage.getHeight() <= maxDimension) {
            return originalImage;
        }
        
        // 자동 비율 유지 리사이징 적용
        return resizeWithAspectRatio(originalImage, maxDimension);
    }
    
    /**
     * 썸네일 생성 (자동 비율 유지 리사이징)
     */
    private BufferedImage createThumbnail(BufferedImage originalImage) throws IOException {
        return resizeWithAspectRatio(originalImage, thumbnailMaxDimension);
    }
    
    /**
     * 자동 비율 유지 리사이징 (비율 왜곡 방지)
     */
    private BufferedImage resizeWithAspectRatio(BufferedImage originalImage, int maxDimension) throws IOException {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double aspectRatio = (double) originalWidth / originalHeight;
        
        int targetWidth, targetHeight;
        
        if (aspectRatio > 1) {
            // 가로형 이미지 (가로가 더 긴 경우)
            targetWidth = Math.min(maxDimension, originalWidth);
            targetHeight = (int) (targetWidth / aspectRatio);
        } else {
            // 세로형 이미지 (세로가 더 긴 경우)
            targetHeight = Math.min(maxDimension, originalHeight);
            targetWidth = (int) (targetHeight * aspectRatio);
        }
        
        // 원본보다 크게 만들지 않음
        if (targetWidth >= originalWidth && targetHeight >= originalHeight) {
            return originalImage; // 원본 그대로 반환
        }
        
        return Thumbnails.of(originalImage)
            .size(targetWidth, targetHeight)
            .keepAspectRatio(true)
            .asBufferedImage();
    }
    
    /**
     * 디렉토리 생성
     */
    private void createDirectoriesIfNotExist() throws IOException {
        Files.createDirectories(Paths.get(uploadPath));
        Files.createDirectories(Paths.get(thumbnailPath));
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 이미지 포맷 추출
     */
    private String getImageFormat(String extension) {
        return extension.substring(1).toLowerCase();
    }
    
    /**
     * 파일 삭제
     */
    public void deleteImage(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("파일 삭제 완료: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("파일 삭제 실패: {}", filePath, e);
        }
    }
    
    /**
     * 처리된 이미지 정보를 담는 내부 클래스
     */
    public static class ProcessedImageInfo {
        private final String originalPath;
        private final String thumbnailPath;
        private final long fileSize;
        private final String mimeType;
        
        public ProcessedImageInfo(String originalPath, String thumbnailPath, long fileSize, String mimeType) {
            this.originalPath = originalPath;
            this.thumbnailPath = thumbnailPath;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
        }
        
        public String getOriginalPath() { return originalPath; }
        public String getThumbnailPath() { return thumbnailPath; }
        public long getFileSize() { return fileSize; }
        public String getMimeType() { return mimeType; }
    }
} 