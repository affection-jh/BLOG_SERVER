package com.eos.blog.repository;

import com.eos.blog.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    // 내가 쓴 블로그 목록 (삭제되지 않은 것)
    @Query("SELECT b FROM Blog b WHERE b.authorId = :authorId AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<Blog> findByAuthorIdAndIsDeletedFalse(@Param("authorId") String authorId, Pageable pageable);

    // 공개 최신 블로그 목록
    @Query("SELECT b FROM Blog b WHERE b.accessLevel = 'PUBLIC' AND b.status = 'PUBLISHED' AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<Blog> findPublicLatest(Pageable pageable);

    // 인기 블로그 목록
    @Query("SELECT b FROM Blog b WHERE b.accessLevel = 'PUBLIC' AND b.status = 'PUBLISHED' AND b.isDeleted = false ORDER BY b.likeCount DESC, b.viewCount DESC")
    Page<Blog> findPopular(Pageable pageable);

    // 여러 태그로 블로그 검색 - Native Query 사용
    @Query(value = "SELECT * FROM blogs b WHERE JSON_OVERLAPS(b.tags, :tags) AND b.access_level = 'PUBLIC' AND b.status = 'PUBLISHED' AND b.is_deleted = false ORDER BY b.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM blogs b WHERE JSON_OVERLAPS(b.tags, :tags) AND b.access_level = 'PUBLIC' AND b.status = 'PUBLISHED' AND b.is_deleted = false",
           nativeQuery = true)
    Page<Blog> findByTags(@Param("tags") String tags, Pageable pageable);

    // 제목으로 검색 (FULLTEXT) - Native Query 사용
    @Query(value = "SELECT * FROM blogs b WHERE MATCH(b.title) AGAINST(:keyword IN BOOLEAN MODE) AND b.access_level = 'PUBLIC' AND b.status = 'PUBLISHED' AND b.is_deleted = false ORDER BY b.created_at DESC", 
           countQuery = "SELECT COUNT(*) FROM blogs b WHERE MATCH(b.title) AGAINST(:keyword IN BOOLEAN MODE) AND b.access_level = 'PUBLIC' AND b.status = 'PUBLISHED' AND b.is_deleted = false",
           nativeQuery = true)
    Page<Blog> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    // 조회수 증가
    @Modifying
    @Query("UPDATE Blog b SET b.viewCount = b.viewCount + 1 WHERE b.id = :blogId")
    void incrementViewCount(@Param("blogId") Long blogId);
    
    // 썸네일 이미지로 사용 중인 블로그 존재 여부 확인
    boolean existsByThumbnailImageIdAndIsDeletedFalse(Long thumbnailImageId);
    
    // content에서 특정 이미지 ID를 포함하는 블로그 존재 여부 확인
    @Query(value = "SELECT COUNT(*) > 0 FROM blogs b WHERE b.is_deleted = false AND b.content LIKE CONCAT('%', :imageId, '%')", nativeQuery = true)
    boolean existsByContentContainingImageId(@Param("imageId") Long imageId);
    
    // 특정 사용자의 발행된 블로그들을 최신순으로 조회
    @Query("SELECT b FROM Blog b WHERE b.authorId = :authorId AND b.status = :status AND b.isDeleted = false ORDER BY b.updatedAt DESC")
    List<Blog> findByAuthorIdAndStatusOrderByUpdatedAtDesc(@Param("authorId") String authorId, @Param("status") Blog.BlogStatus status, Pageable pageable);
} 