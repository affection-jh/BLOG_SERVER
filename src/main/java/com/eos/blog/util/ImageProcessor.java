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

        // 썸네일 생성 및 S3 업로드
        BufferedImage thumbnail = Thumbnails.of(originalImage)
                .size(300, 300)
                .asBufferedImage();
        ByteArrayOutputStream thumbBaos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, extension, thumbBaos);
        byte[] thumbBytes = thumbBaos.toByteArray();
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