package com.app.controller;

import com.app.service.AuthService;
import com.app.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatistics(@RequestParam("userId") Long userId) {
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "未授权访问，请先登录");
            return ResponseEntity.status(401).body(result);
        }
        
        // 检查用户权限（管理员或超级管理员）
        Map<String, Object> userInfo = authService.getUserById(userId);
        if (userInfo == null) {
            Map<String, Object> result = Map.of("success", false, "message", "用户不存在");
            return ResponseEntity.status(401).body(result);
        }
        
        String role = (String) userInfo.get("role");
        if (!"管理员".equals(role) && !"超级管理员".equals(role)) {
            Map<String, Object> result = Map.of("success", false, "message", "权限不足，仅管理员可访问");
            return ResponseEntity.status(403).body(result);
        }
        
        Map<String, Object> result = statisticsService.getStatistics();
        return ResponseEntity.ok(result);
    }
}

