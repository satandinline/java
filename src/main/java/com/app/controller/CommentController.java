package com.app.controller;

import com.app.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getComments(@RequestParam("resource_id") Long resourceId) {
        if (resourceId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少resource_id参数");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = commentService.getComments(resourceId);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createComment(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> request) {
        // 优先从header获取userId，如果没有则从request body获取
        if (userId == null) {
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
        
        Object resourceIdObj = request.get("resource_id");
        Long resourceId = null;
        if (resourceIdObj != null) {
            try {
                resourceId = Long.parseLong(resourceIdObj.toString());
            } catch (NumberFormatException e) {
                Map<String, Object> result = Map.of("success", false, "message", "无效的资源ID");
                return ResponseEntity.badRequest().body(result);
            }
        }
        
        String commentContent = request.get("comment_content") != null ? 
            request.get("comment_content").toString() : null;
        
        if (resourceId == null || userId == null || commentContent == null || commentContent.trim().isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少必要参数");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = commentService.createComment(resourceId, userId, commentContent);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{commentId}/reply")
    public ResponseEntity<Map<String, Object>> addReply(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> request) {
        // 优先从header获取userId，如果没有则从request body获取
        if (userId == null) {
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
        
        String replyContent = request.get("reply_content") != null ? 
            request.get("reply_content").toString() : null;
        
        if (userId == null || replyContent == null || replyContent.trim().isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少必要参数");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = commentService.addReply(commentId, userId, replyContent);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<Map<String, Object>> likeComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> request) {
        // 优先从header获取userId，如果没有则从request body获取
        if (userId == null) {
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
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户信息");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = commentService.likeComment(commentId, userId);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> getComment(@PathVariable Long commentId) {
        Map<String, Object> result = commentService.getComment(commentId);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(404).body(result);
        }
    }
    
    @GetMapping("/{commentId}/resource-id")
    public ResponseEntity<Map<String, Object>> getCommentResourceId(@PathVariable Long commentId) {
        Map<String, Object> result = commentService.getCommentResourceId(commentId);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(404).body(result);
        }
    }
}

