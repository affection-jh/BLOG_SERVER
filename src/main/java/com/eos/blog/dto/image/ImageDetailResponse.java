package com.eos.blog.dto.image;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ImageDetailResponse {
    
    private Long imageId;
    private String accessUrl;
    private String thumbnailAccessUrl;
    private String uploaderUid;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime uploadTime;
    
    private Long fileSize;
    private String mimeType;
    private Boolean isTemp;
    
    public ImageDetailResponse() {}
    
    public ImageDetailResponse(Long imageId, String accessUrl, String thumbnailAccessUrl, 
                              String uploaderUid, LocalDateTime uploadTime, 
                              Long fileSize, String mimeType, Boolean isTemp) {
        this.imageId = imageId;
        this.accessUrl = accessUrl;
        this.thumbnailAccessUrl = thumbnailAccessUrl;
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