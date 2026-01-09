package com.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResourceController {

    @GetMapping("/home/resources")
    public ResponseEntity<Map<String, Object>> getHomeResources(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "8") int pageSize) {
        try {
            // 检索结果每页8条数据
            if (pageSize <= 0) {
                pageSize = 8;
            }
            if (page < 1) {
                page = 1;
            }
            
            Map<String, Object> result = resourceService.getHomeResources(page, pageSize);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(500).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = Map.of("success", false, "message", "获取资源失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @Autowired
    private com.app.service.ResourceService resourceService;
    
    @GetMapping("/resource/detail")
    public ResponseEntity<Map<String, Object>> getResourceDetail(
            @RequestParam(value = "festival_name", required = false) String festivalName,
            @RequestParam(value = "resource_id", required = false) Long resourceId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "table", required = false) String table) {
        try {
            // 支持多种参数方式
            // 1. festival_name参数
            // 2. resource_id参数（兼容旧版本）
            // 3. id + table参数
            
            if (festivalName == null && resourceId == null && id == null) {
                Map<String, Object> result = Map.of("success", false, "message", "缺少必要参数");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 如果使用resource_id参数，转换为id+table方式
            if (resourceId != null && id == null) {
                id = resourceId;
                table = "cultural_resources";
            }
            
            Map<String, Object> result = resourceService.getResourceDetail(festivalName, id, table);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(404).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = Map.of("success", false, "message", "获取资源详情失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

}

