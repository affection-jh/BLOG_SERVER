package com.eos.blog.dto.blog;

import com.eos.blog.entity.Blog;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class BlogCreateRequest {
    
    private String title;
    private JsonNode content;
    private List<String> tags;
    private Blog.AccessLevel accessLevel;
    private List<Long> sharedGroupIds;
    private Long selectedThumbnailImageId; // 사용자가 선택한 썸네일 이미지 ID
    private Blog.BlogStatus status; // 블로그 상태 (DRAFT, PUBLISHED, ARCHIVED)

    public BlogCreateRequest(String title, JsonNode content, List<String> tags, 
                           Blog.AccessLevel accessLevel, List<Long> sharedGroupIds) {
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.accessLevel = accessLevel;
        this.sharedGroupIds = sharedGroupIds;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonNode getContent() {
        return content;
    }

    public void setContent(JsonNode content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Blog.AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(Blog.AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public List<Long> getSharedGroupIds() {
        return sharedGroupIds;
    }

    public void setSharedGroupIds(List<Long> sharedGroupIds) {
        this.sharedGroupIds = sharedGroupIds;
    }

    public Long getSelectedThumbnailImageId() {
        return selectedThumbnailImageId;
    }

    public void setSelectedThumbnailImageId(Long selectedThumbnailImageId) {
        this.selectedThumbnailImageId = selectedThumbnailImageId;
    }

    public Blog.BlogStatus getStatus() {
        return status;
    }

    public void setStatus(Blog.BlogStatus status) {
        this.status = status;
    }
} 