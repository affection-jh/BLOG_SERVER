package com.eos.blog.dto.blog;

import com.eos.blog.entity.Blog;
import com.eos.blog.util.ServiceUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public class BlogResponse {
    
    private Long id;
    private String title;
    private JsonNode content;
    private String authorId;
    private Blog.BlogStatus status;
    private Blog.AccessLevel accessLevel;
    private List<String> tags;
    private List<String> sharedGroups;
    private Long thumbnailImageId; // 썸네일 이미지 ID
    private Integer viewCount;
    private Integer likeCount;
    private Integer bookmarkCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public BlogResponse() {}

    public BlogResponse(Long id, String title, JsonNode content, String authorId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
    }

    public static BlogResponse from(Blog blog) {
        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        response.setContent(blog.getContent());
        response.setAuthorId(blog.getAuthorId());
        response.setStatus(blog.getStatus());
        response.setAccessLevel(blog.getAccessLevel());
        response.setThumbnailImageId(blog.getThumbnailImageId());
        response.setViewCount(blog.getViewCount());
        response.setLikeCount(blog.getLikeCount());
        response.setBookmarkCount(blog.getBookmarkCount());
        response.setCommentCount(blog.getCommentCount());
        response.setCreatedAt(blog.getCreatedAt());
        response.setUpdatedAt(blog.getUpdatedAt());
        // JSON 필드들을 List로 변환
        response.setTags(ServiceUtils.parseJsonToList(blog.getTags()));
        response.setSharedGroups(ServiceUtils.parseJsonToList(blog.getSharedGroups()));
        
        return response;
    }



    // Builder pattern
    public static BlogResponseBuilder builder() {
        return new BlogResponseBuilder();
    }

    public static class BlogResponseBuilder {
        private BlogResponse response = new BlogResponse();

        public BlogResponseBuilder id(Long id) {
            response.setId(id);
            return this;
        }

        public BlogResponseBuilder title(String title) {
            response.setTitle(title);
            return this;
        }

        public BlogResponseBuilder content(JsonNode content) {
            response.setContent(content);
            return this;
        }

        public BlogResponseBuilder authorId(String authorId) {
            response.setAuthorId(authorId);
            return this;
        }

        public BlogResponseBuilder status(Blog.BlogStatus status) {
            response.setStatus(status);
            return this;
        }

        public BlogResponseBuilder accessLevel(Blog.AccessLevel accessLevel) {
            response.setAccessLevel(accessLevel);
            return this;
        }

        public BlogResponseBuilder tags(List<String> tags) {
            response.setTags(tags);
            return this;
        }

        public BlogResponseBuilder sharedGroups(List<String> sharedGroups) {
            response.setSharedGroups(sharedGroups);
            return this;
        }

        public BlogResponseBuilder thumbnailImageId(Long thumbnailImageId) {
            response.setThumbnailImageId(thumbnailImageId);
            return this;
        }

        public BlogResponseBuilder viewCount(Integer viewCount) {
            response.setViewCount(viewCount);
            return this;
        }

        public BlogResponseBuilder likeCount(Integer likeCount) {
            response.setLikeCount(likeCount);
            return this;
        }

        public BlogResponseBuilder bookmarkCount(Integer bookmarkCount) {
            response.setBookmarkCount(bookmarkCount);
            return this;
        }

        public BlogResponseBuilder commentCount(Integer commentCount) {
            response.setCommentCount(commentCount);
            return this;
        }

        public BlogResponseBuilder createdAt(LocalDateTime createdAt) {
            response.setCreatedAt(createdAt);
            return this;
        }

        public BlogResponseBuilder updatedAt(LocalDateTime updatedAt) {
            response.setUpdatedAt(updatedAt);
            return this;
        }

        public BlogResponse build() {
            return response;
        }
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

    public JsonNode getContent() {
        return content;
    }

    public void setContent(JsonNode content) {
        this.content = content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public Blog.BlogStatus getStatus() {
        return status;
    }

    public void setStatus(Blog.BlogStatus status) {
        this.status = status;
    }

    public Blog.AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(Blog.AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getSharedGroups() {
        return sharedGroups;
    }

    public void setSharedGroups(List<String> sharedGroups) {
        this.sharedGroups = sharedGroups;
    }

    public Long getThumbnailImageId() {
        return thumbnailImageId;
    }

    public void setThumbnailImageId(Long thumbnailImageId) {
        this.thumbnailImageId = thumbnailImageId;
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
} 