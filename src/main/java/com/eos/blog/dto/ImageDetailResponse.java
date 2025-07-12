package com.eos.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ImageDetailResponse {
    
    private Long imageId;
    private String storageUrl;
    private String thumbnailUrl;
    private String uploaderUid;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime uploadTime;
    
    private Long fileSize;
    private String mimeType;
    private Boolean isTemp;
    
    public ImageDetailResponse() {}
    
    public ImageDetailResponse(Long imageId, String storageUrl, String thumbnailUrl, 
                              String uploaderUid, LocalDateTime uploadTime, 
                              Long fileSize, String mimeType, Boolean isTemp) {
        this.imageId = imageId;
        this.storageUrl = storageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.uploaderUid = uploaderUid;
        this.uploadTime = uploadTime;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.isTemp = isTemp;
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
    
    public String getUploaderUid() {
        return uploaderUid;
    }
    
    public void setUploaderUid(String uploaderUid) {
        this.uploaderUid = uploaderUid;
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
    
    public Boolean getIsTemp() {
        return isTemp;
    }
    
    public void setIsTemp(Boolean isTemp) {
        this.isTemp = isTemp;
    }
} 