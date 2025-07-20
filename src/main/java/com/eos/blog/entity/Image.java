/*
 * [유효성 보장]
 * - uploaderUid, storageUrl, mimeType: null/빈값 불가, 길이 제한(128/1024/100)
 * - fileSize: null 불가
 * - isTemp, isDeleted: null 불가, 기본값 true/false
 * - uploadTime: 자동 생성
 */
package com.eos.blog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "images")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uploader_uid", nullable = false, length = 128)
    private String uploaderUid;

    @Column(name = "storage_url", nullable = false, length = 1024)
    private String storageUrl;

    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;

    @CreationTimestamp
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "is_temp", nullable = false)
    private Boolean isTemp = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;



    // 생성자
    public Image() {}

    public Image(String uploaderUid, String storageUrl, Long fileSize, String mimeType) {
        this.uploaderUid = uploaderUid;
        this.storageUrl = storageUrl;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUploaderUid() { return uploaderUid; }
    public void setUploaderUid(String uploaderUid) { this.uploaderUid = uploaderUid; }

    public String getStorageUrl() { return storageUrl; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Boolean getIsTemp() { return isTemp; }
    public void setIsTemp(Boolean isTemp) { this.isTemp = isTemp; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }


    // 비즈니스 메서드
    public void confirm() { this.isTemp = false; }
    public void markAsDeleted() { this.isDeleted = true; }
    public boolean isExpired() {
        // 72시간(3일) 이상 된 임시 이미지는 만료된 것으로 간주
        return isTemp && uploadTime != null && uploadTime.plusHours(72).isBefore(LocalDateTime.now());
    }
} 