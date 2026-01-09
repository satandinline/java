package com.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        // 测试数据库连接状态
        String dbStatus = "unknown";
        try {
            // 在实际实现中，这里应该测试数据库连接
            // 为了简单起见，暂时返回 connected
            dbStatus = "connected";
        } catch (Exception e) {
            dbStatus = "error: " + e.getMessage();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("database_status", dbStatus);
        result.put("rag_systems_count", 0); // 暂时设为0，实际应根据系统状态设定
        result.put("image_aigc_systems_count", 0); // 暂时设为0
        result.put("search_rag_initialized", false); // 暂时设为false
        
        return ResponseEntity.ok(result);
    }
}