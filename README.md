# Blog Server - 이미지 업로드 시스템

이 프로젝트는 AWS S3만 지원하는 이미지 업로드 시스템을 구현한 Spring Boot 애플리케이션입니다.

## 기능

- **이미지 업로드 및 관리**: 서버를 통한 이미지 업로드, 리사이징, 썸네일 생성
- **S3 스토리지 지원**
- **메타데이터 관리**: 이미지 정보 DB 저장 및 관리

## API 엔드포인트

### 1. 이미지 업로드
```
POST /api/images/upload
```
**파라미터:**
- `file`: 이미지 파일 (필수)
- `uid`: 사용자 UID (필수)

**응답:**
```json
{
  "imageId": 123,
  "storageUrl": "https://your-bucket.s3.ap-northeast-2.amazonaws.com/abc123.jpg",
  "thumbnailUrl": "https://your-bucket.s3.ap-northeast-2.amazonaws.com/thumb_abc123.jpg",
  "uploadTime": "2025-01-12T14:00:00Z",
  "fileSize": 204800,
  "mimeType": "image/jpeg"
}
```

### 2. 이미지 presigned URL 조회
```
GET /api/images/{imageId}/url
```

### 3. 이미지 삭제
```
DELETE /api/images/{imageId}
```

## 환경 설정

### S3 설정 방법

`application.yml` 파일에서 AWS 자격 증명 및 버킷 정보를 설정합니다:

```yaml
cloud:
  aws:
    s3:
      enabled: true
      bucket: your-bucket-name
    credentials:
      access-key: your-access-key
      secret-key: your-secret-key
    region:
      static: ap-northeast-2
```

### 실행 방법
```bash
# 서버 시작
./gradlew bootRun
```

## 프로젝트 구조

```
src/main/java/com/eos/blog/
├── controller/
│   └── ImageController.java           # 이미지 업로드 API
├── service/
│   ├── StorageService.java            # S3 스토리지 서비스 인터페이스
│   └── S3StorageService.java          # S3 스토리지 구현
├── util/
│   └── ImageProcessor.java            # 이미지 리사이즈/썸네일/저장
└── config/
    └── StorageConfig.java             # 스토리지 설정

src/main/resources/
└── application.yml                    # 통합 설정
```

## 의존성

- Spring Boot 3.x
- AWS SDK v2 for S3
- Spring Web
- Spring Boot Auto Configuration

## 주의사항

1. **S3 환경**: AWS S3에만 이미지 저장
2. **보안**: 프로덕션 환경에서는 적절한 인증/인가를 추가해야 합니다
3. **자격 증명**: AWS 자격 증명은 환경 변수나 AWS CLI 설정을 통해 제공하는 것을 권장합니다 