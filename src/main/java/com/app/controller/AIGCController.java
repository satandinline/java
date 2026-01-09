package com.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aigc")
public class AIGCController {

    @Value("${python.aigc.service.url:http://localhost:7200}")
    private String pythonAigcServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        try {
            String url = pythonAigcServiceUrl + "/api/aigc/chat";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 设置用户ID头
            if (request.containsKey("user_id")) {
                headers.set("X-User-Id", String.valueOf(request.get("user_id")));
            }
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            
            // 对于流式响应，直接转发流
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (org.springframework.web.client.RestClientException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无法连接到Python AIGC服务: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "AIGC聊天失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/extract-title")
    public ResponseEntity<Map<String, Object>> extractTitle(@RequestBody Map<String, Object> request) {
        try {
            String url = pythonAigcServiceUrl + "/api/aigc/extract-title";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (org.springframework.web.client.RestClientException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无法连接到Python AIGC服务: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "标题提取失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @Autowired
    private com.app.service.AIGCService aigcService;

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "user_id", required = false) String userIdStr,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        try {
            if (userId == null && userIdStr != null) {
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("message", "无效的用户ID");
                    return ResponseEntity.badRequest().body(result);
                }
            }
            
            if (userId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "缺少用户ID");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 从本地数据库读取会话列表
            Map<String, Object> result = aigcService.getSessions(userId, page, pageSize);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取会话列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> request) {
        try {
            // 优先从请求头获取用户ID
            if (userId == null && request.containsKey("user_id")) {
                Object userIdObj = request.get("user_id");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
            }
            
            if (userId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "缺少用户ID，请先登录");
                return ResponseEntity.badRequest().body(result);
            }
            
            String mode = request.containsKey("mode") ? (String) request.get("mode") : "text";
            
            // 在本地数据库创建会话
            Map<String, Object> result = aigcService.createSession(userId, mode);
            if ((Boolean) result.getOrDefault("success", false)) {
                // AIGCService已经返回了完整的session对象，直接返回
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "创建会话失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Map<String, Object>> getSessionMessages(
            @PathVariable Long sessionId,
            @RequestParam(value = "user_id", required = false) String userIdStr,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        try {
            // 从本地数据库读取会话消息
            Map<String, Object> result = aigcService.getSessionMessages(sessionId, page, pageSize);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取会话消息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> addSessionMessage(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request) {
        try {
            String url = pythonAigcServiceUrl + "/api/aigc/sessions/" + sessionId + "/messages";
            
            // 处理multipart/form-data请求（如果包含文件）
            if (request.containsKey("images") || request.containsKey("files")) {
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    body.add(entry.getKey(), entry.getValue());
                }
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, Map.class);
                return ResponseEntity.ok(response.getBody());
            } else {
                // 普通JSON请求
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, Map.class);
                return ResponseEntity.ok(response.getBody());
            }
        } catch (org.springframework.web.client.RestClientException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无法连接到Python AIGC服务: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "添加会话消息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable Long sessionId) {
        try {
            // 从本地数据库删除会话
            Map<String, Object> result = aigcService.deleteSession(sessionId);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "删除会话失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @DeleteMapping("/sessions/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteSessions(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> sessionIdsInt = (List<Integer>) request.get("session_ids");
            if (sessionIdsInt == null || sessionIdsInt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "缺少会话ID列表");
                return ResponseEntity.badRequest().body(result);
            }
            
            List<Long> sessionIds = sessionIdsInt.stream()
                .map(Integer::longValue)
                .collect(java.util.stream.Collectors.toList());
            
            // 从本地数据库批量删除会话
            Map<String, Object> result = aigcService.batchDeleteSessions(sessionIds);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "批量删除会话失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @DeleteMapping("/sessions/all")
    public ResponseEntity<Map<String, Object>> deleteAllSessions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "user_id", required = false) String userIdStr) {
        try {
            if (userId == null && userIdStr != null) {
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("message", "无效的用户ID");
                    return ResponseEntity.badRequest().body(result);
                }
            }
            
            if (userId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "缺少用户ID");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 从本地数据库删除用户的所有会话
            Map<String, Object> result = aigcService.deleteAllSessions(userId);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "删除所有会话失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PutMapping("/sessions/{sessionId}/summary")
    public ResponseEntity<Map<String, Object>> updateSessionSummary(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request) {
        try {
            String summary = request.containsKey("summary") ? (String) request.get("summary") : "";
            
            // 在本地数据库更新会话摘要
            Map<String, Object> result = aigcService.updateSessionSummary(sessionId, summary);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "更新会话摘要失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> getAigcResources(@RequestParam("ids") String ids) {
        try {
            String url = pythonAigcServiceUrl + "/api/aigc/resources?ids=" + 
                java.net.URLEncoder.encode(ids, java.nio.charset.StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (org.springframework.web.client.RestClientException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无法连接到Python AIGC服务: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取资源详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/edit-resource")
    public ResponseEntity<Map<String, Object>> editResource(@RequestBody Map<String, Object> request) {
        try {
            String url = pythonAigcServiceUrl + "/api/aigc/edit-resource";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (org.springframework.web.client.RestClientException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无法连接到Python AIGC服务: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "编辑资源失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/save-edited-resource")
    public ResponseEntity<Map<String, Object>> saveEditedResource(@RequestBody Map<String, Object> request) {
        try {
            String url = pythonAigcServiceUrl + "/api/aigc/save-edited-resource";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (org.springframework.web.client.RestClientException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无法连接到Python AIGC服务: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "保存编辑后的资源失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}