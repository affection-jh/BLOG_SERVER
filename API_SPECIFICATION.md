# 블로그 서버 API 명세서

## 기본 정보
- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **인코딩**: UTF-8

## 공통 응답 형식

### 성공 응답
```json
{
  "content": [...],
  "pageable": {
    "sort": {...},
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 100,
  "totalPages": 10,
  "last": false,
  "first": true,
  "numberOfElements": 10,
  "size": 10,
  "number": 0,
  "sort": {...},
  "empty": false
}
```

### 에러 응답
```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "요청 처리 중 오류가 발생했습니다",
  "path": "/api/blogs",
  "fieldErrors": [
    {
      "field": "title",
      "message": "제목은 필수입니다"
    }
  ],
  "fileUploadErrors": [
    {
      "fileName": "image.jpg",
      "errorCode": "FILE_TOO_LARGE",
      "message": "파일 크기가 너무 큽니다 (최대 10MB)"
    }
  ]
}
```

---

## 1. 블로그 API

### 1.1 블로그 생성
**POST** `/blogs`

#### 요청
```json
{
  "title": "스프링 부트로 블로그 만들기",
  "content": {
    "type": "doc",
    "content": [
      {
        "type": "paragraph",
        "content": [
          {
            "type": "text",
            "text": "스프링 부트를 사용해서 블로그를 만들어보겠습니다."
          }
        ]
      }
    ]
  },
  "status": "PUBLISHED",
  "accessLevel": "PUBLIC",
  "tags": ["기술", "스프링", "블로그"],
  "sharedGroups": [1, 3, 5],
  "thumbnailImageId": 123
}
```

#### 응답 (201 Created)
```json
{
  "id": 1,
  "title": "스프링 부트로 블로그 만들기",
  "thumbnailImageId": 123,
  "authorId": "user123",
  "content": {...},
  "status": "PUBLISHED",
  "accessLevel": "PUBLIC",
  "tags": ["기술", "스프링", "블로그"],
  "sharedGroups": [1, 3, 5],
  "viewCount": 0,
  "likeCount": 0,
  "bookmarkCount": 0,
  "commentCount": 0,
  "createdAt": "2024-01-15T10:30:00.000Z",
  "updatedAt": "2024-01-15T10:30:00.000Z"
}
```

### 1.2 블로그 조회
**GET** `/blogs/{blogId}`

#### 응답 (200 OK)
```json
{
  "id": 1,
  "title": "스프링 부트로 블로그 만들기",
  "thumbnailImageId": 123,
  "authorId": "user123",
  "content": {...},
  "status": "PUBLISHED",
  "accessLevel": "PUBLIC",
  "tags": ["기술", "스프링", "블로그"],
  "sharedGroups": [1, 3, 5],
  "viewCount": 15,
  "likeCount": 3,
  "bookmarkCount": 2,
  "commentCount": 5,
  "createdAt": "2024-01-15T10:30:00.000Z",
  "updatedAt": "2024-01-15T10:30:00.000Z"
}
```

### 1.3 내 블로그 목록
**GET** `/blogs/my`

#### 쿼리 파라미터
- `page` (기본값: 0): 페이지 번호
- `size` (기본값: 10): 페이지 크기

#### 응답 (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "title": "스프링 부트로 블로그 만들기",
      "thumbnailImageId": 123,
      "authorId": "user123",
      "tags": ["기술", "스프링", "블로그"],
      "viewCount": 15,
      "likeCount": 3,
      "commentCount": 5,
      "createdAt": "2024-01-15T10:30:00.000Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0
}
```

### 1.4 블로그 수정
**PUT** `/blogs/{blogId}`

#### 요청
```json
{
  "title": "수정된 제목",
  "content": {...},
  "status": "PUBLISHED",
  "accessLevel": "PUBLIC",
  "tags": ["기술", "수정"],
  "sharedGroups": [1, 2]
}
```

#### 응답 (200 OK)
```json
{
  "id": 1,
  "title": "수정된 제목",
  "thumbnailImageId": 123,
  "authorId": "user123",
  "content": {...},
  "status": "PUBLISHED",
  "accessLevel": "PUBLIC",
  "tags": ["기술", "수정"],
  "sharedGroups": [1, 2],
  "viewCount": 15,
  "likeCount": 3,
  "bookmarkCount": 2,
  "commentCount": 5,
  "createdAt": "2024-01-15T10:30:00.000Z",
  "updatedAt": "2024-01-15T11:00:00.000Z"
}
```

### 1.5 블로그 삭제
**DELETE** `/blogs/{blogId}`

#### 응답 (204 No Content)

### 1.6 블로그 복구
**POST** `/blogs/{blogId}/restore`

#### 응답 (204 No Content)

### 1.7 썸네일 수정
**PUT** `/blogs/{blogId}/thumbnail`

#### 요청
```json
{
  "thumbnailImageId": 456
}
```

#### 응답 (200 OK)
```json
{
  "id": 1,
  "title": "스프링 부트로 블로그 만들기",
  "thumbnailImageId": 456,
  "authorId": "user123",
  "tags": ["기술", "스프링", "블로그"],
  "viewCount": 15,
  "likeCount": 3,
  "commentCount": 5,
  "createdAt": "2024-01-15T10:30:00.000Z"
}
```

### 1.8 블로그 발행
**POST** `/blogs/{blogId}/publish`

#### 설명
- 임시저장(DRAFT) 상태의 블로그를 발행(PUBLISHED) 상태로 변경
- 작성자만 발행 가능
- 이미 발행된 블로그는 재발행으로 처리 (덮어씌우기)

#### 응답 (200 OK)
```json
{
  "id": 1,
  "title": "스프링 부트로 블로그 만들기",
  "thumbnailImageId": 123,
  "authorId": "user123",
  "content": {...},
  "status": "PUBLISHED",
  "accessLevel": "PUBLIC",
  "tags": ["기술", "스프링", "블로그"],
  "sharedGroups": [1, 3, 5],
  "viewCount": 0,
  "likeCount": 0,
  "bookmarkCount": 0,
  "commentCount": 0,
  "createdAt": "2024-01-15T10:30:00.000Z",
  "updatedAt": "2024-01-15T11:00:00.000Z"
}
```

### 1.9 블로그 상태 변경
**PUT** `/blogs/{blogId}/status`

#### 쿼리 파라미터
- `status`: 변경할 상태 (DRAFT, PUBLISHED, ARCHIVED)

#### 설명
- 블로그 상태를 직접 변경
- 작성자만 상태 변경 가능
- DRAFT: 임시저장, PUBLISHED: 발행, ARCHIVED: 보관

#### 응답 (200 OK)
```json
{
  "id": 1,
  "title": "스프링 부트로 블로그 만들기",
  "thumbnailImageId": 123,
  "authorId": "user123",
  "content": {...},
  "status": "ARCHIVED",
  "accessLevel": "PUBLIC",
  "tags": ["기술", "스프링", "블로그"],
  "sharedGroups": [1, 3, 5],
  "viewCount": 15,
  "likeCount": 3,
  "bookmarkCount": 2,
  "commentCount": 5,
  "createdAt": "2024-01-15T10:30:00.000Z",
  "updatedAt": "2024-01-15T11:00:00.000Z"
}
```

### 1.10 공개 최신 블로그 목록
**GET** `/blogs/public/latest`

#### 쿼리 파라미터
- `page` (기본값: 0): 페이지 번호
- `size` (기본값: 10): 페이지 크기

#### 응답 (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "title": "스프링 부트로 블로그 만들기",
      "thumbnailImageId": 123,
      "authorId": "user123",
      "tags": ["기술", "스프링", "블로그"],
      "viewCount": 15,
      "likeCount": 3,
      "commentCount": 5,
      "createdAt": "2024-01-15T10:30:00.000Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

### 1.11 인기 블로그 목록
**GET** `/blogs/popular`

#### 쿼리 파라미터
- `page` (기본값: 0): 페이지 번호
- `size` (기본값: 10): 페이지 크기

#### 응답 (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "title": "인기 블로그 제목",
      "thumbnailImageId": 123,
      "authorId": "user123",
      "tags": ["인기", "기술"],
      "viewCount": 150,
      "likeCount": 30,
      "commentCount": 25,
      "createdAt": "2024-01-15T10:30:00.000Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

### 1.12 제목으로 블로그 검색
**GET** `/blogs/search`

#### 쿼리 파라미터
- `keyword` (필수): 검색 키워드
- `page` (기본값: 0): 페이지 번호
- `size` (기본값: 10): 페이지 크기

#### 응답 (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "title": "스프링 부트로 블로그 만들기",
      "thumbnailImageId": 123,
      "authorId": "user123",
      "tags": ["기술", "스프링", "블로그"],
      "viewCount": 15,
      "likeCount": 3,
      "commentCount": 5,
      "createdAt": "2024-01-15T10:30:00.000Z"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

### 1.13 태그로 블로그 검색
**GET** `/blogs/search/tags`

#### 쿼리 파라미터
- `tags` (필수): 태그 목록 (쉼표로 구분)
- `page` (기본값: 0): 페이지 번호
- `size` (기본값: 10): 페이지 크기

#### 예시
```
GET /api/blogs/search/tags?tags=기술,스프링&page=0&size=10
```

#### 응답 (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "title": "스프링 부트로 블로그 만들기",
      "thumbnailImageId": 123,
      "authorId": "user123",
      "tags": ["기술", "스프링", "블로그"],
      "viewCount": 15,
      "likeCount": 3,
      "commentCount": 5,
      "createdAt": "2024-01-15T10:30:00.000Z"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

---

## 2. 이미지 API

### 2.1 단일 이미지 업로드
**POST** `/images/upload`

#### 요청 (multipart/form-data)
- `file`: 이미지 파일 (JPG, PNG, GIF, WebP)

#### 응답 (200 OK)
```json
{
  "imageId": 123,
  "fileName": "blog-image.jpg",
  "fileSize": 1024000,
  "contentType": "image/jpeg",
  "presignedUrl": "https://storage.example.com/presigned-url?token=...",
  "status": "TEMPORARY"
}
```

### 2.2 다중 이미지 업로드
**POST** `/images/upload/multiple`

#### 요청 (multipart/form-data)
- `files`: 이미지 파일들 (JPG, PNG, GIF, WebP)

#### 응답 (200 OK)
```json
{
  "successful": [
    {
      "imageId": 123,
      "fileName": "image1.jpg",
      "fileSize": 1024000,
      "contentType": "image/jpeg",
      "presignedUrl": "https://storage.example.com/presigned-url?token=...",
      "status": "TEMPORARY"
    }
  ],
  "failed": [
    {
      "fileName": "large-image.jpg",
      "errorCode": "FILE_TOO_LARGE",
      "message": "파일 크기가 너무 큽니다 (최대 10MB)"
    }
  ]
}
```

### 2.3 이미지 확정
**POST** `/images/{imageId}/confirm`

#### 응답 (200 OK)
```json
{
  "imageId": 123,
  "fileName": "blog-image.jpg",
  "fileSize": 1024000,
  "contentType": "image/jpeg",
  "presignedUrl": "https://storage.example.com/presigned-url?token=...",
  "status": "CONFIRMED"
}
```

### 2.4 이미지 삭제
**DELETE** `/images/{imageId}`

#### 응답 (204 No Content)

### 2.5 이미지 정보 조회
**GET** `/images/{imageId}`

#### 응답 (200 OK)
```json
{
  "imageId": 123,
  "fileName": "blog-image.jpg",
  "fileSize": 1024000,
  "contentType": "image/jpeg",
  "presignedUrl": "https://storage.example.com/presigned-url?token=...",
  "status": "CONFIRMED",
  "uploadedBy": "user123",
  "createdAt": "2024-01-15T10:30:00.000Z"
}
```

## 2.6 스케줄러 관리

### 2.6.1 스케줄러 상태 확인
**GET** `/api/scheduler/status`

#### 응답 (200 OK)
```json
{
  "schedulerEnabled": true,
  "imageCleanupScheduler": "매일 새벽 4시 실행 (72시간 만료 이미지 정리)",
  "systemHealthCheck": "매일 새벽 5시 실행",
  "lastCheck": "2024-01-15T10:30:00.000Z"
}
```

### 2.6.2 수동 이미지 정리
**POST** `/api/scheduler/cleanup-images`

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "이미지 정리 완료",
  "deletedCount": 5,
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

### 2.6.3 테스트 이미지 정리 (개발용)
**POST** `/api/scheduler/test-cleanup`

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "테스트 이미지 정리 완료",
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

---

## 3. 에러 코드

### 3.1 일반 에러
- `400`: 잘못된 요청
- `401`: 인증 필요
- `403`: 권한 없음
- `404`: 리소스를 찾을 수 없음
- `500`: 서버 내부 오류

### 3.2 파일 업로드 에러
- `FILE_TOO_LARGE`: 파일 크기 초과
- `INVALID_FILE_TYPE`: 지원하지 않는 파일 형식
- `UPLOAD_FAILED`: 업로드 실패
- `IMAGE_PROCESSING_FAILED`: 이미지 처리 실패

### 3.3 블로그 에러
- `BLOG_NOT_FOUND`: 블로그를 찾을 수 없음
- `ACCESS_DENIED`: 접근 권한 없음
- `INVALID_STATUS`: 잘못된 상태값
- `INVALID_ACCESS_LEVEL`: 잘못된 접근 레벨

---

## 4. 데이터 모델

### 4.1 Blog
```json
{
  "id": "Long",
  "title": "String (필수, 최대 255자)",
  "thumbnailImageId": "Long (선택)",
  "content": "JSON (필수)",
  "authorId": "String (필수)",
  "status": "ENUM (DRAFT, PUBLISHED, ARCHIVED)",
  "accessLevel": "ENUM (PUBLIC, PRIVATE, SHARED, PASSWORD)",
  "tags": "JSON Array",
  "sharedGroups": "JSON Array",
  "viewCount": "Integer (기본값: 0)",
  "likeCount": "Integer (기본값: 0)",
  "bookmarkCount": "Integer (기본값: 0)",
  "commentCount": "Integer (기본값: 0)",
  "isDeleted": "Boolean (기본값: false)",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

### 4.2 Image
```json
{
  "imageId": "Long",
  "fileName": "String",
  "fileSize": "Long",
  "contentType": "String",
  "presignedUrl": "String",
  "status": "ENUM (TEMPORARY, CONFIRMED)",
  "uploadedBy": "String",
  "createdAt": "Timestamp"
}
```

---

## 5. 사용 예시

### 5.1 블로그 작성 플로우
1. 이미지 업로드 or 실시간 업로드: `POST /api/images/upload`
2. 블로그 생성: `POST /api/blogs`
3. 이미지 확정: `POST /api/images/{imageId}/confirm`

### 5.2 블로그 조회 플로우
1. 공개 블로그 목록: `GET /api/blogs/public/latest`
2. 특정 블로그 조회: `GET /api/blogs/{blogId}`
3. 제목 검색: `GET /api/blogs/search?keyword=스프링`
4. 태그 검색: `GET /api/blogs/search/tags?tags=기술,블로그`

### 5.3 블로그 관리 플로우
1. 내 블로그 목록: `GET /api/blogs/my`
2. 블로그 수정: `PUT /api/blogs/{blogId}`
3. 썸네일 변경: `PUT /api/blogs/{blogId}/thumbnail`
4. 블로그 삭제: `DELETE /api/blogs/{blogId}`

### 5.4 스케줄러 관리 플로우
1. 스케줄러 상태 확인: `GET /api/scheduler/status`
2. 수동 이미지 정리: `POST /api/scheduler/cleanup-images`
3. 테스트 이미지 정리: `POST /api/scheduler/test-cleanup` 