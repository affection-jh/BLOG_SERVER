# 블로그 이미지 서버

Spring Boot 기반의 이미지 업로드 및 관리 서버입니다.

## 🚀 주요 기능

### 1. 이미지 업로드 및 관리
- 로그인 인증 적용 (인증된 사용자만 업로드 허용)
- 이미지 파일 서버 저장
- 업로더 ID, 업로드 시간, 파일명, 경로 등 메타데이터 DB 저장

### 2. 이미지 메타데이터 관리
- 이미지별 고유 ID 생성 및 저장
- 업로더 정보, 업로드 시간, 파일 경로 등 저장
- 이미지 수정·삭제 API에서 권한 확인 후 처리

### 3. 이미지 압축 및 리사이징
- 업로드된 이미지 자동 리사이징 (최대 가로/세로 크기 제한)
- JPEG, WebP 등 효율적인 포맷으로 압축
- 썸네일 이미지 별도 생성 및 저장

### 4. 이미지 캐싱 및 최적화
- HTTP 응답에 `Cache-Control` 헤더 설정
- ETag, Last-Modified 헤더로 변경 여부 검증 지원
- 브라우저 캐싱 최대화

### 5. 임시 이미지 관리
- 임시 이미지와 확정 이미지 구분
- 24시간 후 자동 정리 (스케줄러)
- 수동 정리 API 제공

## 📋 API 명세

### 1. 이미지 업로드
```
POST /api/images/upload
Content-Type: multipart/form-data

Parameters:
- file: 이미지 파일
- uid: 사용자 UID

Response:
{
  "imageId": 123,
  "storageUrl": "/uploads/images/abc123.jpg",
  "thumbnailUrl": "/uploads/thumbnails/thumb_abc123.jpg",
  "uploadTime": "2025-01-12T14:00:00Z",
  "fileSize": 204800,
  "mimeType": "image/jpeg"
}
```

### 2. 이미지 삭제
```
DELETE /api/images/{imageId}
Headers:
- Authorization: 사용자 UID

Response: 204 No Content
```

### 3. 이미지 조회
```
GET /api/images/{imageId}

Response:
{
  "imageId": 123,
  "storageUrl": "/uploads/images/abc123.jpg",
  "thumbnailUrl": "/uploads/thumbnails/thumb_abc123.jpg",
  "uploaderUid": "user_abc",
  "uploadTime": "2025-01-12T14:00:00Z",
  "fileSize": 204800,
  "mimeType": "image/jpeg",
  "isTemp": true
}
```

### 4. 이미지 사용 확정
```
PATCH /api/images/{imageId}/confirm
Headers:
- Authorization: 사용자 UID

Response:
{
  "message": "이미지가 확정되었습니다"
}
```

### 5. 임시 이미지 정리
```
DELETE /api/images/cleanup-temp

Response:
{
  "message": "임시 이미지 정리 완료",
  "deletedCount": 5
}
```

### 6. 업로더별 이미지 목록
```
GET /api/images/uploader/{uid}

Response: ImageDetailResponse[]
```

### 7. 이미지 파일 직접 제공
```
GET /api/images/{imageId}/file
GET /api/images/{imageId}/thumbnail

Response: 이미지 바이너리 데이터 (캐싱 헤더 포함)
```

## 🛠️ 기술 스택

- **Framework**: Spring Boot 3.5.3
- **Database**: H2 (개발용), JPA/Hibernate
- **Image Processing**: Thumbnailator
- **Cache**: Caffeine
- **Security**: Spring Security
- **Build Tool**: Gradle

## 📦 설치 및 실행

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd blog-server
```

### 2. 의존성 설치
```bash
./gradlew build
```

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 4. 접속
- 애플리케이션: http://localhost:8080
- H2 콘솔: http://localhost:8080/h2-console

## ⚙️ 설정

### application.properties 주요 설정
```properties
# 파일 업로드 설정
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# 이미지 저장 경로 설정
app.image.upload.path=./uploads/images
app.image.thumbnail.path=./uploads/thumbnails
app.image.max-width=1920
app.image.max-height=1080
app.image.thumbnail.width=300
app.image.thumbnail.height=300

# 캐시 설정
spring.cache.type=caffeine
spring.cache.cache-names=images,thumbnails
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=1h
```

## 🔄 이미지 처리 플로우

### 1. 앱에서 바로 글 쓸 때
1. 클라이언트: 이미지 + 글 데이터 함께 서버에 업로드
2. 서버: 인증 및 권한 확인
3. 서버: 이미지 압축 및 리사이징 처리
4. 서버: 원본 이미지 및 썸네일 저장
5. 서버: 이미지 메타데이터 DB 저장
6. 서버: 글 내용 DB 저장 (이미지 URL 포함)
7. 서버 → 클라이언트: 저장 완료 응답 전송

### 2. 웹에서 미리 이미지 일괄 업로드 후 글 쓸 때
1. 웹 클라이언트: 이미지 파일 일괄 업로드 요청
2. 서버: 인증 및 권한 확인
3. 서버: 이미지 압축 및 리사이징 처리
4. 서버: 원본 및 썸네일 저장
5. 서버: 이미지 메타데이터 DB 저장 (임시 상태)
6. 서버 → 클라이언트: 업로드 완료 및 URL 리스트 반환
7. 웹 클라이언트: 글 작성 시 저장된 이미지 불러오기
8. 웹 클라이언트: 글 저장 요청 전송
9. 서버: 글 데이터 저장 (이미지 URL 포함)
10. 서버: 임시 이미지 중 실제 사용 이미지로 상태 변경
11. 서버: 미사용 임시 이미지 삭제 처리 (별도 스케줄러)
12. 서버 → 클라이언트: 저장 완료 응답 전송

## 🧪 테스트

### API 테스트 예시 (curl)

#### 이미지 업로드
```bash
curl -X POST http://localhost:8080/api/images/upload \
  -F "file=@test-image.jpg" \
  -F "uid=test_user_123"
```

#### 이미지 조회
```bash
curl http://localhost:8080/api/images/1
```

#### 이미지 삭제
```bash
curl -X DELETE http://localhost:8080/api/images/1 \
  -H "Authorization: test_user_123"
```

## 📁 프로젝트 구조

```
src/main/java/com/eos/blog/
├── BlogApplication.java          # 메인 실행 클래스
├── config/                       # 설정 관련
│   ├── SecurityConfig.java       # 보안 설정
│   ├── CacheConfig.java          # 캐시 설정
│   └── SchedulingConfig.java     # 스케줄링 설정
├── controller/                   # API 요청 처리
│   └── ImageController.java      # 이미지 컨트롤러
├── service/                      # 비즈니스 로직
│   └── ImageService.java         # 이미지 서비스
├── repository/                   # DB 접근 계층
│   └── ImageRepository.java      # 이미지 리포지토리
├── model/                        # 도메인 모델
│   └── Image.java                # 이미지 엔티티
├── dto/                          # 데이터 전송 객체
│   ├── ImageUploadRequest.java   # 업로드 요청 DTO
│   ├── ImageUploadResponse.java  # 업로드 응답 DTO
│   └── ImageDetailResponse.java  # 상세 정보 응답 DTO
├── util/                         # 공통 유틸리티
│   └── ImageProcessor.java       # 이미지 처리 유틸리티
└── exception/                    # 예외 처리
    └── GlobalExceptionHandler.java # 전역 예외 핸들러
```

## 🔒 보안 고려사항

1. **인증**: 모든 이미지 업로드/삭제 작업은 사용자 인증 필요
2. **권한**: 이미지 삭제는 업로더만 가능
3. **파일 검증**: 이미지 파일 타입 및 크기 검증
4. **경로 보안**: 파일 경로 조작 방지
5. **임시 파일**: 미사용 임시 파일 자동 정리

## 🚀 향후 개선 계획

1. **CDN 연동**: AWS S3, CloudFront 등 CDN 서비스 연동
2. **이미지 포맷 최적화**: WebP, AVIF 등 최신 포맷 지원
3. **이미지 편집**: 자르기, 회전, 필터 등 편집 기능
4. **배치 처리**: 대량 이미지 업로드 최적화
5. **모니터링**: 이미지 처리 성능 모니터링
6. **백업**: 이미지 파일 백업 및 복구 기능

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 