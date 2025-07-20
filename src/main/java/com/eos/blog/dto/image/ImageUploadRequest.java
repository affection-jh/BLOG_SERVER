package com.eos.blog.dto.image;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class ImageUploadRequest {
    
    @NotNull(message = "이미지 파일은 필수입니다")
    private MultipartFile file;
    
    @NotBlank(message = "사용자 UID는 필수입니다")
    private String uid;
    
    public ImageUploadRequest() {}
    
    public ImageUploadRequest(MultipartFile file, String uid) {
        this.file = file;
        this.uid = uid;
    }
    
    public MultipartFile getFile() {
        return file;
    }
    
    public void setFile(MultipartFile file) {
        this.file = file;
    }
    
    public String getUid() {
        return uid;
    }
    
    public void setUid(String uid) {
        this.uid = uid;
    }
} 