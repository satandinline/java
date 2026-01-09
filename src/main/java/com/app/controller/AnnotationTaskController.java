package com.app.controller;

import com.app.service.AnnotationTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/annotation")
public class AnnotationTaskController {
    @Autowired
    private AnnotationTaskService annotationTaskService;

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getAnnotationTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "user_id", required = false) String userIdStr,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "12") int pageSize) {
        
        if (userId == null && userIdStr != null) {
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                Map<String, Object> result = Map.of("success", false, "message", "无效的用户ID");
                return ResponseEntity.badRequest().body(result);
            }
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户信息");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = annotationTaskService.getAnnotationTasks(userId, status, page, pageSize);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @GetMapping("/tasks/{taskId}/details")
    public ResponseEntity<Map<String, Object>> getAnnotationDetails(@PathVariable Long taskId) {
        Map<String, Object> result = Map.of("success", false, "message", "功能暂未实现");
        return ResponseEntity.status(500).body(result);
    }
    
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> updateAnnotation(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = Map.of("success", false, "message", "功能暂未实现");
        return ResponseEntity.status(500).body(result);
    }
    
    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<Map<String, Object>> approveAnnotation(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-User-Id") Long userId) {
        Map<String, Object> result = Map.of("success", false, "message", "功能暂未实现");
        return ResponseEntity.status(500).body(result);
    }
    
    @PostMapping("/tasks/{taskId}/pause")
    public ResponseEntity<Map<String, Object>> pauseAiAnnotation(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-User-Id") Long userId) {
        Map<String, Object> result = Map.of("success", false, "message", "功能暂未实现");
        return ResponseEntity.status(500).body(result);
    }
    
    @PostMapping("/tasks/{taskId}/start-ai")
    public ResponseEntity<Map<String, Object>> startAiAnnotation(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-User-Id") Long userId) {
        Map<String, Object> result = Map.of("success", false, "message", "功能暂未实现");
        return ResponseEntity.status(500).body(result);
    }
    
    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<Map<String, Object>> rejectAnnotation(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = Map.of("success", false, "message", "功能暂未实现");
        return ResponseEntity.status(500).body(result);
    }
}

