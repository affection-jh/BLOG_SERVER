package com.eos.blog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Entity
@Table(name = "blogs")
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "thumbnail_image_id")
    private Long thumbnailImageId; // 썸네일 이미지 ID

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String contentJson;

    @Column(name = "author_id", nullable = false, length = 100)
    private String authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BlogStatus status = BlogStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level")
    private AccessLevel accessLevel = AccessLevel.PRIVATE;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tagsJson;

    @Column(name = "shared_groups", columnDefinition = "TEXT")
    private String sharedGroupsJson;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "bookmark_count")
    private Integer bookmarkCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getThumbnailImageId() {
        return thumbnailImageId;
    }

    public void setThumbnailImageId(Long thumbnailImageId) {
        this.thumbnailImageId = thumbnailImageId;
    }

    public JsonNode getContent() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return contentJson != null ? mapper.readTree(contentJson) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 오류", e);
        }
    }

    public void setContent(JsonNode content) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.contentJson = content != null ? mapper.writeValueAsString(content) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 오류", e);
        }
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public BlogStatus getStatus() {
        return status;
    }

    public void setStatus(BlogStatus status) {
        this.status = status;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public JsonNode getTags() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return tagsJson != null ? mapper.readTree(tagsJson) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 오류", e);
        }
    }

    public void setTags(JsonNode tags) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.tagsJson = tags != null ? mapper.writeValueAsString(tags) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 오류", e);
        }
    }

    public JsonNode getSharedGroups() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return sharedGroupsJson != null ? mapper.readTree(sharedGroupsJson) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 오류", e);
        }
    }

    public void setSharedGroups(JsonNode sharedGroups) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.sharedGroupsJson = sharedGroups != null ? mapper.writeValueAsString(sharedGroups) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 오류", e);
        }
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getBookmarkCount() {
        return bookmarkCount;
    }

    public void setBookmarkCount(Integer bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Enums
    public enum BlogStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }

    public enum AccessLevel {
        PUBLIC, PRIVATE, SHARED, PASSWORD
    }
} 