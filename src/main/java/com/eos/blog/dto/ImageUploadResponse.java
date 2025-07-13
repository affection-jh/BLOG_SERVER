package com.eos.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ImageUploadResponse {
    
    private Long imageId;
    private String accessUrl;
    private String thumbnailAccessUrl;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime uploadTime;
    
    private Long fileSize;
    private String mimeType;
    
    public ImageUploadResponse() {}
    
    public ImageUploadResponse(Long imageId, 
                              String accessUrl,
                              String thumbnailAccessUrl,
                              LocalDateTime uploadTime, 
                              Long fileSize, 
                              String mimeType) {
        this.imageId = imageId;
        this.accessUrl = accessUrl;
        this.thumbnailAccessUrl = thumbnailAccessUrl;
        this.uploadTime = uploadTime;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }
    
    // Getter & Setter
    public Long getImageId() {
        return imageId;
    }
    
    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }
    
    public String getAccessUrl() {
        return accessUrl;
    }
    
    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }
    
    public String getThumbnailAccessUrl() {
        return thumbnailAccessUrl;
    }
    
    public void setThumbnailAccessUrl(String thumbnailAccessUrl) {
        this.thumbnailAccessUrl = thumbnailAccessUrl;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
} 