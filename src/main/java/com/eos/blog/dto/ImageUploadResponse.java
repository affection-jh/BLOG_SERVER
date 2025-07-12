package com.eos.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ImageUploadResponse {
    
    private Long imageId;
    private String storageUrl;
    private String thumbnailUrl;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime uploadTime;
    
    private Long fileSize;
    private String mimeType;
    
    public ImageUploadResponse() {}
    
    public ImageUploadResponse(Long imageId, String storageUrl, String thumbnailUrl, 
                              LocalDateTime uploadTime, Long fileSize, String mimeType) {
        this.imageId = imageId;
        this.storageUrl = storageUrl;
        this.thumbnailUrl = thumbnailUrl;
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
    
    public String getStorageUrl() {
        return storageUrl;
    }
    
    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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