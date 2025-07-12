package com.eos.blog.repository;

import com.eos.blog.model.Image;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    // 업로더별 이미지 조회
    List<Image> findByUploaderUidAndIsDeletedFalseOrderByUploadTimeDesc(String uploaderUid);
    
    // 임시 이미지 조회
    List<Image> findByIsTempTrueAndIsDeletedFalse();
    
    // 만료된 임시 이미지 조회 (24시간 이상)
    @Query("SELECT i FROM Image i WHERE i.isTemp = true AND i.isDeleted = false AND i.uploadTime < :expiryTime")
    List<Image> findExpiredTempImages(@Param("expiryTime") LocalDateTime expiryTime);
    
    // 이미지 존재 여부 확인 (삭제되지 않은 것만)
    Optional<Image> findByIdAndIsDeletedFalse(Long id);
    
    // 업로더 권한 확인
    Optional<Image> findByIdAndUploaderUidAndIsDeletedFalse(Long id, String uploaderUid);
    
    // 임시 이미지를 확정으로 변경
    @Modifying
    @Query("UPDATE Image i SET i.isTemp = false WHERE i.id = :imageId")
    void confirmImage(@Param("imageId") Long imageId);
    
    // 이미지 삭제 표시
    @Modifying
    @Query("UPDATE Image i SET i.isDeleted = true WHERE i.id = :imageId")
    void markAsDeleted(@Param("imageId") Long imageId);
    
    // 만료된 임시 이미지 일괄 삭제
    @Modifying
    @Query("UPDATE Image i SET i.isDeleted = true WHERE i.isTemp = true AND i.uploadTime < :expiryTime")
    void deleteExpiredTempImages(@Param("expiryTime") LocalDateTime expiryTime);
} 