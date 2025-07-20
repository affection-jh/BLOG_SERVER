package com.eos.blog.controller;

import com.eos.blog.dto.blog.BlogCreateRequest;
import com.eos.blog.dto.blog.BlogResponse;
import com.eos.blog.service.BlogService;
import com.eos.blog.entity.Blog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@CrossOrigin(origins = "*")  // 모든 origin 허용 나중에 꼭 바꿔줘야 해
public class BlogController {

    @Autowired
    private BlogService blogService;

    /**
     * 블로그 생성 (기본값 설정 및 권한 검증)
     */
    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(
            @RequestBody BlogCreateRequest request,
            @RequestHeader("Authorization") String userId) {
        
        BlogResponse response = blogService.createBlog(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 블로그 조회
     */
    @GetMapping("/{blogId}")
    public ResponseEntity<BlogResponse> getBlog(
            @PathVariable Long blogId,
            @RequestHeader("Authorization") String userId) {
        
        BlogResponse response = blogService.getBlog(blogId, userId);
        
        // 조회수 증가 (published 상태일 때만)
        if (Blog.BlogStatus.PUBLISHED.equals(response.getStatus())) {
            blogService.incrementViewCount(blogId);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 블로그 수정
     */
    @PutMapping("/{blogId}")
    public ResponseEntity<BlogResponse> updateBlog(
            @PathVariable Long blogId,
            @RequestBody BlogCreateRequest request,
            @RequestHeader("Authorization") String userId) {
        
        BlogResponse response = blogService.updateBlog(blogId, request, userId);
        return ResponseEntity.ok(response);
    }



    /**
     * 블로그 삭제 (Soft Delete)
     */
    @DeleteMapping("/{blogId}")
    public ResponseEntity<Void> deleteBlog(
            @PathVariable Long blogId,
            @RequestHeader("Authorization") String userId) {
        
        blogService.deleteBlog(blogId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 블로그 복구
     */
    @PostMapping("/{blogId}/restore")
    public ResponseEntity<Void> restoreBlog(
            @PathVariable Long blogId,
            @RequestHeader("Authorization") String userId) {
        
        blogService.restoreBlog(blogId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 썸네일 수정
     */
    @PutMapping("/{blogId}/thumbnail")
    public ResponseEntity<BlogResponse> updateThumbnail(
            @PathVariable Long blogId,
            @RequestParam Long thumbnailImageId,
            @RequestHeader("Authorization") String userId) {
        
        BlogResponse response = blogService.updateThumbnail(blogId, thumbnailImageId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 블로그 발행
     */
    @PostMapping("/{blogId}/publish")
    public ResponseEntity<BlogResponse> publishBlog(
            @PathVariable Long blogId,
            @RequestHeader("Authorization") String userId) {
        
        BlogResponse response = blogService.publishBlog(blogId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 블로그 상태 변경
     */
    @PutMapping("/{blogId}/status")
    public ResponseEntity<BlogResponse> updateBlogStatus(
            @PathVariable Long blogId,
            @RequestParam Blog.BlogStatus status,
            @RequestHeader("Authorization") String userId) {
        
        BlogResponse response = blogService.updateBlogStatus(blogId, status, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 공개 최신 블로그 목록
     */
    @GetMapping("/public/latest")
    public ResponseEntity<Page<BlogResponse>> getPublicLatestBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<BlogResponse> blogs = blogService.getPublicLatestBlogs(page, size);
        return ResponseEntity.ok(blogs);
    }

    /**
     * 인기 블로그 목록
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<BlogResponse>> getPopularBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<BlogResponse> blogs = blogService.getPopularBlogs(page, size);
        return ResponseEntity.ok(blogs);
    }

    /**
     * 내가 쓴 블로그 목록
     */
    @GetMapping("/my")
    public ResponseEntity<Page<BlogResponse>> getMyBlogs(
            @RequestHeader("Authorization") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<BlogResponse> blogs = blogService.getMyBlogs(userId, page, size);
        return ResponseEntity.ok(blogs);
    }

    /**
     * 태그로 블로그 검색
     */
    @GetMapping("/search/tags")
    public ResponseEntity<Page<BlogResponse>> searchByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<BlogResponse> blogs = blogService.searchByTags(tags, page, size);
        return ResponseEntity.ok(blogs);
    }

    /**
     * 제목으로 블로그 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<BlogResponse>> searchByTitle(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<BlogResponse> blogs = blogService.searchByTitle(keyword, page, size);
        return ResponseEntity.ok(blogs);
    }


} 