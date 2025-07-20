package com.eos.blog.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * 서비스 레이어에서 공통으로 사용하는 유틸리티 클래스
 */
public class ServiceUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 작성자 권한 검증
     */
    public static void validateAuthorPermission(String authorId, String userId, String resourceType) {
        if (!authorId.equals(userId)) {
            logger.warn("권한 없음: {} 작성자={}, 요청자={}", resourceType, authorId, userId);
            throw new com.eos.blog.exception.ForbiddenException(
                String.format("%s 수정/삭제 권한이 없습니다.", resourceType)
            );
        }
    }
    
    /**
     * 리소스 존재 여부 확인 및 권한 검증
     */
    public static <T> T validateResourceAndPermission(
            T resource, 
            String resourceId, 
            String userId, 
            String resourceType,
            java.util.function.Function<T, String> authorIdExtractor) {
        
        if (resource == null) {
            logger.warn("리소스 없음: {} ID={}", resourceType, resourceId);
            throw new com.eos.blog.exception.ResourceNotFoundException(
                String.format("%s를 찾을 수 없습니다. ID: %s", resourceType, resourceId)
            );
        }
        
        String authorId = authorIdExtractor.apply(resource);
        validateAuthorPermission(authorId, userId, resourceType);
        
        return resource;
    }
    
    /**
     * JSON Node를 List<String>으로 변환
     */
    public static List<String> parseJsonToList(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return new ArrayList<>();
        }
        
        List<String> result = new ArrayList<>();
        if (jsonNode.isArray()) {
            for (JsonNode node : jsonNode) {
                if (node.isTextual()) {
                    result.add(node.asText());
                } else if (node.isNumber()) {
                    result.add(node.asText());
                }
            }
        }
        return result;
    }
    
    /**
     * Object를 JsonNode로 변환
     */
    public static JsonNode convertToJsonNode(Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.valueToTree(obj);
    }
    
    /**
     * JSON 콘텐츠에서 첫 번째 이미지 URL 추출
     */
    public static String extractFirstImageUrl(JsonNode content) {
        if (content == null || !content.isArray()) {
            return null;
        }
        
        for (JsonNode block : content) {
            if (block.has("type") && "image".equals(block.get("type").asText())) {
                JsonNode data = block.get("data");
                if (data != null && data.has("url")) {
                    return data.get("url").asText();
                }
            }
            
            // 중첩된 블록 검색
            if (block.has("content") && block.get("content").isArray()) {
                String nestedUrl = extractFirstImageUrl(block.get("content"));
                if (nestedUrl != null) {
                    return nestedUrl;
                }
            }
        }
        
        return null;
    }

    /**
     * JSON 콘텐츠에서 첫 번째 이미지 ID 추출
     */
    public static Long extractFirstImageId(JsonNode content) {
        if (content == null || !content.isArray()) {
            return null;
        }
        
        for (JsonNode block : content) {
            if (block.has("type") && "image".equals(block.get("type").asText())) {
                JsonNode data = block.get("data");
                if (data != null && data.has("imageId")) {
                    JsonNode imageIdNode = data.get("imageId");
                    if (imageIdNode.isNumber()) {
                        return imageIdNode.asLong();
                    } else if (imageIdNode.isTextual()) {
                        try {
                            return Long.parseLong(imageIdNode.asText());
                        } catch (NumberFormatException e) {
                            logger.warn("이미지 ID 파싱 실패: {}", imageIdNode.asText());
                        }
                    }
                }
            }
            
            // 중첩된 블록 검색
            if (block.has("content") && block.get("content").isArray()) {
                Long nestedImageId = extractFirstImageId(block.get("content"));
                if (nestedImageId != null) {
                    return nestedImageId;
                }
            }
        }
        
        return null;
    }

    /**
     * 작업 로그 기록
     */
    public static void logOperation(String operation, String resourceType, String resourceId, String userId) {
        logger.info("{} {}: ID={}, User={}", operation, resourceType, resourceId, userId);
    }
    
    /**
     * 작업 실패 로그 기록
     */
    public static void logOperationFailure(String operation, String resourceType, String resourceId, String userId, Exception e) {
        logger.error("{} {} 실패: ID={}, User={}", operation, resourceType, resourceId, userId, e);
    }
} 