package com.eos.blog.util;

import com.eos.blog.service.S3StorageService;
import net.coobird.thumbnailator.Thumbnails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Component
public class ImageProcessor {


    private final S3StorageService s3StorageService;

    @Autowired
    public ImageProcessor(S3StorageService s3StorageService) {
        this.s3StorageService = s3StorageService;
    }

  
    public ProcessedImageInfo processAndSaveImage(MultipartFile file, String uploaderUid) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        String uuid = UUID.randomUUID().toString();
        String extension = getExtension(file.getOriginalFilename());
        String fileName = uuid + "." + extension;

        // uid별 폴더 경로 생성
        String userFolder = "users/" + uploaderUid + "/";
        String originalKey = userFolder + fileName;
        String thumbnailKey = userFolder + "thumb_" + fileName;

        // 원본 이미지 S3 업로드
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalImage, extension, baos);
        byte[] imageBytes = baos.toByteArray();
        String s3Url = s3StorageService.upload(imageBytes, originalKey, file.getContentType());

        // 썸네일 생성 및 S3 업로드 (더 나은 품질로 조정)
        BufferedImage thumbnail = Thumbnails.of(originalImage)
                .size(400, 400)  // 크기를 400x400으로 증가
                .keepAspectRatio(true)  // 비율 유지
                .outputQuality(0.9)  // 품질을 90%로 설정 (더 높은 품질)
                .asBufferedImage();
        ByteArrayOutputStream thumbBaos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, extension, thumbBaos);
        byte[] thumbBytes = thumbBaos.toByteArray();
        
        // 썸네일이 너무 크면 품질을 낮춰서 재생성
        if (thumbBytes.length > 100 * 1024) { // 100KB 이상이면
            thumbnail = Thumbnails.of(originalImage)
                    .size(400, 400)
                    .keepAspectRatio(true)
                    .outputQuality(0.75)  // 품질을 75%로 낮춤
                    .asBufferedImage();
            thumbBaos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, extension, thumbBaos);
            thumbBytes = thumbBaos.toByteArray();
        }
        
        String thumbS3Url = s3StorageService.upload(thumbBytes, thumbnailKey, file.getContentType());

        return new ProcessedImageInfo(s3Url, thumbS3Url, imageBytes.length, file.getContentType());
    }

    public void deleteImage(String url) {
        if (url == null || !url.contains(".com/")) return;
        String key = url.substring(url.indexOf(".com/") + 5);
        s3StorageService.delete(key);
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0) ? filename.substring(idx + 1) : "jpg";
    }

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