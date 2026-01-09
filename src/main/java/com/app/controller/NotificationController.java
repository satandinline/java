package com.app.controller;

import com.app.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam("user_id") Long userId,
            @RequestParam(value = "is_read", required = false) Integer isRead,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少user_id参数");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = notificationService.getNotifications(userId, isRead, page, pageSize);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markNotificationRead(@PathVariable Long notificationId) {
        Map<String, Object> result = notificationService.markNotificationRead(notificationId);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(404).body(result);
        }
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllNotificationsRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> request) {
        
        // 优先从请求头获取用户ID，如果没有则从请求体获取
        if (userId == null && request != null) {
            Object userIdObj = request.get("user_id");
            if (userIdObj != null) {
                try {
                    userId = Long.parseLong(userIdObj.toString());
                } catch (NumberFormatException e) {
                    Map<String, Object> result = Map.of("success", false, "message", "无效的用户ID");
                    return ResponseEntity.badRequest().body(result);
                }
            }
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少user_id参数");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = notificationService.markAllNotificationsRead(userId);
        return ResponseEntity.ok(result);
    }
}