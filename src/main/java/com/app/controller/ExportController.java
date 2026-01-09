package com.app.controller;

import com.app.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    /**
     * 导出单个用户资源为Excel
     */
    @PostMapping("/user-resource")
    public ResponseEntity<Map<String, Object>> exportUserResource(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Object resourceIdObj = request.get("resource_id");
            Object userIdObj = request.get("user_id");
            
            if (resourceIdObj == null || userIdObj == null) {
                result.put("success", false);
                result.put("message", "资源ID和用户ID不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            Long resourceId = Long.valueOf(resourceIdObj.toString());
            Long userId = Long.valueOf(userIdObj.toString());
            
            String filepath = exportService.exportUserResource(resourceId, userId);
            
            // 生成相对路径用于下载URL
            String relativePath = getRelativePathForDownload(filepath);
            
            result.put("success", true);
            result.put("message", "导出成功");
            result.put("data", Map.of(
                "file_path", filepath,
                "download_url", "/api/download/export?file=" + java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
            ));
            return ResponseEntity.ok(result);
            
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "资源ID或用户ID格式错误");
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导出失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 批量导出用户资源为Excel
     */
    @PostMapping("/user-resources/batch")
    public ResponseEntity<Map<String, Object>> batchExportUserResources(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Object userIdObj = request.get("user_id");
            
            if (userIdObj == null) {
                result.put("success", false);
                result.put("message", "用户ID不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            Long userId = Long.valueOf(userIdObj.toString());
            
            String filepath = exportService.batchExportUserResources(userId);
            
            // 生成相对路径用于下载URL
            String relativePath = getRelativePathForDownload(filepath);
            
            result.put("success", true);
            result.put("message", "批量导出成功");
            result.put("data", Map.of(
                "file_path", filepath,
                "download_url", "/api/download/export?file=" + java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
            ));
            return ResponseEntity.ok(result);
            
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "用户ID格式错误");
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "批量导出失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 获取文件的相对路径用于下载URL
     */
    private String getRelativePathForDownload(String filepath) {
        try {
            java.nio.file.Path projectRoot = java.nio.file.Paths.get(System.getProperty("user.dir"));
            java.nio.file.Path filePathObj = java.nio.file.Paths.get(filepath).normalize();
            java.nio.file.Path projectRootNormalized = projectRoot.normalize();
            try {
                return projectRootNormalized.relativize(filePathObj).toString().replace("\\", "/");
            } catch (IllegalArgumentException e) {
                // 如果路径不在项目根目录下，使用文件名
                return "exports/" + java.nio.file.Paths.get(filepath).getFileName().toString();
            }
        } catch (Exception e) {
            // 如果出错，返回文件名
            return "exports/" + java.nio.file.Paths.get(filepath).getFileName().toString();
        }
    }
}