package com.eos.blog.service;

import com.eos.blog.entity.Blog;
import com.eos.blog.repository.BlogRepository;
import com.eos.blog.dto.blog.BlogCreateRequest;
import com.eos.blog.dto.blog.BlogResponse;
import com.eos.blog.exception.BlogNotFoundException;
import com.eos.blog.exception.ForbiddenException;
import com.eos.blog.exception.ValidationException;
import com.eos.blog.util.ServiceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.ArrayList;

@Service
public class BlogService {

    private final BlogRepository blogRepository;
    private final ImageService imageService;
    
    public BlogService(BlogRepository blogRepository, ImageService imageService) {
        this.blogRepository = blogRepository;
        this.imageService = imageService;
    }

    /**
     * 블로그 생성 (콘텐츠 + 공유 설정 통합)
     */
    @Transactional
    public BlogResponse createBlog(BlogCreateRequest request, String userId) {
        // 필수 필드 검증
        validateBlogRequest(request);
        
        // 블로그 저장
        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setAuthorId(userId);
        
        // 상태 설정 (클라이언트에서 보낸 값 또는 기본값: DRAFT)
        Blog.BlogStatus status = request.getStatus() != null ? 
            request.getStatus() : Blog.BlogStatus.DRAFT;
        blog.setStatus(status);
        
        // 공유 설정 (클라이언트에서 보낸 값 또는 기본값)
        Blog.AccessLevel accessLevel = request.getAccessLevel() != null ? 
            request.getAccessLevel() : Blog.AccessLevel.PRIVATE;
        blog.setAccessLevel(accessLevel);
        
        blog.setTags(ServiceUtils.convertToJsonNode(request.getTags()));
        
        // 공유 그룹 설정 (권한 검증 포함)
        if (request.getSharedGroupIds() != null && !request.getSharedGroupIds().isEmpty()) {
            validateUserGroupAccess(userId, request.getSharedGroupIds());
            blog.setSharedGroups(ServiceUtils.convertToJsonNode(request.getSharedGroupIds()));
        } else {
            blog.setSharedGroups(null);
        }
        
        // 썸네일 처리 (사용자 선택 우선, 없으면 첫 번째 이미지)
        Long thumbnailImageId = determineThumbnailImageId(request);
        if (thumbnailImageId != null) {
            // 해당 이미지를 확정으로 변경
            imageService.confirmImage(thumbnailImageId, userId);
            blog.setThumbnailImageId(thumbnailImageId);
        } else {
            // 썸네일이 없으면 null로 설정
            blog.setThumbnailImageId(null);
        }
        
        Blog savedBlog = blogRepository.save(blog);
        
        ServiceUtils.logOperation("생성", "블로그", savedBlog.getId().toString(), userId);
        return BlogResponse.from(savedBlog);
    }

    /**
     * 썸네일 이미지 ID 결정 (사용자 선택 우선, 없으면 첫 번째 이미지)
     */
    private Long determineThumbnailImageId(BlogCreateRequest request) {
        // 1. 사용자가 명시적으로 선택한 썸네일
        if (request.getSelectedThumbnailImageId() != null) {
            return request.getSelectedThumbnailImageId();
        }
        
        // 2. 콘텐츠에서 첫 번째 이미지 ID 추출
        return ServiceUtils.extractFirstImageId(request.getContent());
    }

    /**
     * 블로그 조회
     */
    public BlogResponse getBlog(Long blogId, String userId) {
        // 블로그 조회
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new BlogNotFoundException(blogId));
        
        // 접근 권한 확인 (간단한 버전)
        if (!canAccessBlog(blog, userId)) {
            throw new ForbiddenException("블로그에 접근할 권한이 없습니다.");
        }
        
        return BlogResponse.builder()
            .id(blog.getId())
            .title(blog.getTitle())
            .content(blog.getContent())
            .authorId(blog.getAuthorId())
            .status(blog.getStatus())
            .accessLevel(blog.getAccessLevel())
            .tags(ServiceUtils.parseJsonToList(blog.getTags()))
            .sharedGroups(ServiceUtils.parseJsonToList(blog.getSharedGroups()))
            .thumbnailImageId(blog.getThumbnailImageId())
            .viewCount(blog.getViewCount())
            .likeCount(blog.getLikeCount())
            .bookmarkCount(blog.getBookmarkCount())
            .commentCount(blog.getCommentCount())
            .createdAt(blog.getCreatedAt())
            .updatedAt(blog.getUpdatedAt())
            .build();
    }


    /**
     * 내가 쓴 블로그 목록
     */
    public Page<BlogResponse> getMyBlogs(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Blog> blogs = blogRepository.findByAuthorIdAndIsDeletedFalse(userId, pageable);
        
        return blogs.map(blog -> BlogResponse.builder()
            .id(blog.getId())
            .title(blog.getTitle())
            .thumbnailImageId(blog.getThumbnailImageId())
            .authorId(blog.getAuthorId())
            .status(blog.getStatus())
            .accessLevel(blog.getAccessLevel())
            .tags(ServiceUtils.parseJsonToList(blog.getTags()))
            .viewCount(blog.getViewCount())
            .likeCount(blog.getLikeCount())
            .commentCount(blog.getCommentCount())
            .createdAt(blog.getCreatedAt())
            .updatedAt(blog.getUpdatedAt())
            .build());
    }

    /**
     * 블로그 수정 (콘텐츠만 수정)
     */
    @Transactional
    public BlogResponse updateBlog(Long blogId, BlogCreateRequest request, String userId) {
        // 필수 필드 검증
        validateBlogRequest(request);
        
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new BlogNotFoundException(blogId));
        
        // 공통 권한 검증 적용
        ServiceUtils.validateAuthorPermission(blog.getAuthorId(), userId, "블로그");
        
        // 블로그 콘텐츠만 업데이트 (공유 설정은 별도 API 사용)
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setTags(ServiceUtils.convertToJsonNode(request.getTags()));
        
        // 썸네일 처리 (사용자 선택 우선, 없으면 첫 번째 이미지)
        Long thumbnailImageId = determineThumbnailImageId(request);
        if (thumbnailImageId != null) {
            // 해당 이미지를 확정으로 변경
            imageService.confirmImage(thumbnailImageId, userId);
            blog.setThumbnailImageId(thumbnailImageId);
        } else {
            // 썸네일이 없으면 null로 설정
            blog.setThumbnailImageId(null);
        }
        
        Blog updatedBlog = blogRepository.save(blog);
        ServiceUtils.logOperation("수정", "블로그", blogId.toString(), userId);
        return BlogResponse.from(updatedBlog);
    }

    /**
     * 블로그 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteBlog(Long blogId, String userId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new BlogNotFoundException(blogId));
        
        // 공통 권한 검증 적용
        ServiceUtils.validateAuthorPermission(blog.getAuthorId(), userId, "블로그");
        
        blog.setIsDeleted(true);
        blogRepository.save(blog);
        ServiceUtils.logOperation("삭제", "블로그", blogId.toString(), userId);
    }

    /**
     * 블로그 복구
     */
    @Transactional
    public void restoreBlog(Long blogId, String userId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new BlogNotFoundException(blogId));
        
        // 공통 권한 검증 적용
        ServiceUtils.validateAuthorPermission(blog.getAuthorId(), userId, "블로그");
        
        blog.setIsDeleted(false);
        blogRepository.save(blog);
        ServiceUtils.logOperation("복구", "블로그", blogId.toString(), userId);
    }

    /**
     * 썸네일 수정
     */
    @Transactional
    public BlogResponse updateThumbnail(Long blogId, Long thumbnailImageId, String userId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new BlogNotFoundException(blogId));
        
        // 공통 권한 검증 적용
        ServiceUtils.validateAuthorPermission(blog.getAuthorId(), userId, "블로그");
        
        if (thumbnailImageId != null) {
            // 해당 이미지를 확정으로 변경
            imageService.confirmImage(thumbnailImageId, userId);
            blog.setThumbnailImageId(thumbnailImageId);
        } else {
            // 썸네일 제거
            blog.setThumbnailImageId(null);
        }
        
        Blog updatedBlog = blogRepository.save(blog);
        ServiceUtils.logOperation("썸네일 수정", "블로그", blogId.toString(), userId);
        return BlogResponse.from(updatedBlog);
    }

    /**
     * 사용자 그룹 접근 권한 검증
     */
    private void validateUserGroupAccess(String userId, List<Long> groupIds) {
        // 나중에 구현: 사용자가 실제로 이 그룹들에 속해있는지 확인
        // for (Long groupId : groupIds) {
        //     if (!userGroupService.isUserInGroup(userId, groupId)) {
        //         throw new ForbiddenException("그룹 " + groupId + "에 속하지 않습니다.");
        //     }
        // }
    }

    /**
     * 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long blogId) {
        blogRepository.incrementViewCount(blogId);
    }

    /**
     * 공개 최신 블로그 목록
     */
    public Page<BlogResponse> getPublicLatestBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Blog> blogs = blogRepository.findPublicLatest(pageable);
        
        return blogs.map(blog -> BlogResponse.builder()
            .id(blog.getId())
            .title(blog.getTitle())
            .thumbnailImageId(blog.getThumbnailImageId())
            .authorId(blog.getAuthorId())
            .tags(ServiceUtils.parseJsonToList(blog.getTags()))
            .viewCount(blog.getViewCount())
            .likeCount(blog.getLikeCount())
            .commentCount(blog.getCommentCount())
            .createdAt(blog.getCreatedAt())
            .build());
    }

    /**
     * 인기 블로그 목록
     */
    public Page<BlogResponse> getPopularBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Blog> blogs = blogRepository.findPopular(pageable);
        
        return blogs.map(blog -> BlogResponse.builder()
            .id(blog.getId())
            .title(blog.getTitle())
            .thumbnailImageId(blog.getThumbnailImageId())
            .authorId(blog.getAuthorId())
            .tags(ServiceUtils.parseJsonToList(blog.getTags()))
            .viewCount(blog.getViewCount())
            .likeCount(blog.getLikeCount())
            .commentCount(blog.getCommentCount())
            .createdAt(blog.getCreatedAt())
            .build());
    }

    /**
     * 태그로 블로그 검색
     */
    public Page<BlogResponse> searchByTags(List<String> tags, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // List<String>을 JSON 문자열로 변환
        String tagsJson = ServiceUtils.convertToJsonNode(tags).toString();
        Page<Blog> blogs = blogRepository.findByTags(tagsJson, pageable);
        
        return blogs.map(blog -> BlogResponse.builder()
            .id(blog.getId())
            .title(blog.getTitle())
            .thumbnailImageId(blog.getThumbnailImageId())
            .authorId(blog.getAuthorId())
            .tags(ServiceUtils.parseJsonToList(blog.getTags()))
            .viewCount(blog.getViewCount())
            .likeCount(blog.getLikeCount())
            .commentCount(blog.getCommentCount())
            .createdAt(blog.getCreatedAt())
            .build());
    }

    /**
     * 제목으로 블로그 검색
     */
    public Page<BlogResponse> searchByTitle(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Blog> blogs = blogRepository.searchByTitle(keyword, pageable);
        
        return blogs.map(blog -> BlogResponse.builder()
            .id(blog.getId())
            .title(blog.getTitle())
            .thumbnailImageId(blog.getThumbnailImageId())
            .authorId(blog.getAuthorId())
            .tags(ServiceUtils.parseJsonToList(blog.getTags()))
            .viewCount(blog.getViewCount())
            .likeCount(blog.getLikeCount())
            .commentCount(blog.getCommentCount())
            .createdAt(blog.getCreatedAt())
            .build());
    }

    /**
     * 블로그 요청 데이터 검증
     */
    private void validateBlogRequest(BlogCreateRequest request) {
        List<String> errors = new ArrayList<>();
        
        // 제목 검증
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            errors.add("제목은 필수입니다.");
        } else if (request.getTitle().trim().length() > 255) {
            errors.add("제목은 255자를 초과할 수 없습니다.");
        }
        
        // 내용 검증
        if (request.getContent() == null) {
            errors.add("내용은 필수입니다.");
        } else {
            String contentStr = request.getContent().toString();
            if (contentStr.trim().isEmpty() || contentStr.equals("null")) {
                errors.add("내용은 필수입니다.");
            }
        }
        
        // 태그 검증 (선택사항이지만 유효성 검사)
        if (request.getTags() != null) {
            for (String tag : request.getTags()) {
                if (tag != null && tag.trim().length() > 50) {
                    errors.add("태그는 50자를 초과할 수 없습니다.");
                    break;
                }
            }
        }
        
        // 에러가 있으면 ValidationException 발생
        if (!errors.isEmpty()) {
            throw new ValidationException("블로그 생성 요청이 유효하지 않습니다: " + String.join(", ", errors));
        }
    }

    /**
     * 간단한 접근 권한 확인
     */
    private boolean canAccessBlog(Blog blog, String userId) {
        // 삭제된 블로그는 접근 불가
        if (blog.getIsDeleted()) {
            return false;
        }

        // 공개 블로그
        if (Blog.AccessLevel.PUBLIC.equals(blog.getAccessLevel())) {
            return true;
        }

        // 작성자 본인
        if (blog.getAuthorId().equals(userId)) {
            return true;
        }

        // 비밀번호 보호 블로그는 별도 처리 필요
        if (Blog.AccessLevel.PASSWORD.equals(blog.getAccessLevel())) {
            return false;
        }

        // 그룹 공유 블로그는 나중에 구현
        if (Blog.AccessLevel.SHARED.equals(blog.getAccessLevel())) {
            return false;
        }

        return false;
    }

    /**
     * 블로그 발행
     */
    @Transactional
    public BlogResponse publishBlog(Long blogId, String userId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new BlogNotFoundException(blogId));
        
        // 권한 검증 (작성자만 발행 가능)
        if (!blog.getAuthorId().equals(userId)) {
            throw new ForbiddenException("블로그를 발행할 권한이 없습니다.");
        }
        
        // 이미 발행된 블로그라면 그냥 성공으로 처리 (덮어씌우기)
        if (Blog.BlogStatus.PUBLISHED.equals(blog.getStatus())) {
            // 이미 발행된 상태이므로 그대로 반환 (200 OK)
            ServiceUtils.logOperation("재발행", "블로그", blogId.toString(), userId);
            return BlogResponse.from(blog);
        }
        
        // 상태를 PUBLISHED로 변경
        blog.setStatus(Blog.BlogStatus.PUBLISHED);
        
        Blog publishedBlog = blogRepository.save(blog);
        ServiceUtils.logOperation("발행", "블로그", blogId.toString(), userId);
        
        return BlogResponse.from(publishedBlog);
    }

    /**
     * 블로그 상태 변경
     */
    @Transactional
    public BlogResponse updateBlogStatus(Long blogId, Blog.BlogStatus status, String userId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new BlogNotFoundException(blogId));
        
        // 권한 검증 (작성자만 상태 변경 가능)
        if (!blog.getAuthorId().equals(userId)) {
            throw new ForbiddenException("블로그 상태를 변경할 권한이 없습니다.");
        }
        
        // 상태 변경
        blog.setStatus(status);
        
        Blog updatedBlog = blogRepository.save(blog);
        ServiceUtils.logOperation("상태변경", "블로그", blogId.toString(), userId);
        
        return BlogResponse.from(updatedBlog);
    }

} 