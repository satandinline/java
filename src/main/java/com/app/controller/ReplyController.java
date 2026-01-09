package com.app.controller;

import com.app.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/replies")
public class ReplyController {
    
    @Autowired
    private CommentService commentService;

    @PostMapping("/{replyId}/like")
    public ResponseEntity<Map<String, Object>> likeReply(
            @PathVariable Long replyId,
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

        Map<String, Object> result = commentService.likeReply(replyId, userId);
        return ResponseEntity.ok(result);
    }
}