package com.app.controller;

import com.app.entity.CulturalResourceFromUser;
import com.app.entity.AnnotationTask;
import com.app.repository.CulturalResourceFromUserRepository;
import com.app.repository.AnnotationTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {
    
    @Autowired
    private CulturalResourceFromUserRepository resourceUserRepository;
    
    @Autowired
    private AnnotationTaskRepository annotationTaskRepository;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResource(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "userId", required = false) String userIdStr,
            @RequestParam("file") MultipartFile file,
            @RequestParam("resourceType") String resourceType,
            @RequestParam(value = "annotation", required = false) String annotationJson,
            @RequestParam(value = "textContent", required = false) String textContent) {
        
        if (userId == null && userIdStr != null) {
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                Map<String, Object> result = Map.of("success", false, "message", "无效的用户ID");
                return ResponseEntity.status(401).body(result);
            }
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "请先登录");
            return ResponseEntity.status(401).body(result);
        }
        
        if (file == null || file.isEmpty()) {
            if (textContent == null || textContent.trim().isEmpty()) {
                Map<String, Object> result = Map.of("success", false, "message", "请选择要上传的文件或输入文本内容");
                return ResponseEntity.badRequest().body(result);
            }
        }
        
        if (resourceType == null || (!resourceType.equals("文本") && !resourceType.equals("图像"))) {
            Map<String, Object> result = Map.of("success", false, "message", "不支持的资源类型：仅支持\"文本\"或\"图像\"");
            return ResponseEntity.badRequest().body(result);
        }
        
        // TODO: 实现资源上传逻辑
        // 以下是基本实现框架，实际项目中需要完善
        try {
            // 1. 处理文件上传
            String fileName = null;
            String filePath = null;
            if (file != null && !file.isEmpty()) {
                // 保存文件到uploads目录（相对于项目根目录）
                java.nio.file.Path projectRoot = java.nio.file.Paths.get(System.getProperty("user.dir"));
                java.nio.file.Path uploadDir = projectRoot.resolve("uploads");
                
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                
                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null || originalFileName.isEmpty()) {
                    originalFileName = "file";
                }
                
                // 添加时间戳避免文件名冲突
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new java.util.Date());
                int lastDotIndex = originalFileName.lastIndexOf('.');
                String extension = lastDotIndex > 0 ? originalFileName.substring(lastDotIndex) : "";
                String baseName = lastDotIndex > 0 ? originalFileName.substring(0, lastDotIndex) : originalFileName;
                fileName = userId + "-" + baseName + "-" + timestamp + extension;
                filePath = uploadDir.resolve(fileName).toString();
                
                // 保存文件
                java.io.File targetFile = new java.io.File(filePath);
                file.transferTo(targetFile);
                
                // 存储相对路径（相对于项目根目录，用于数据库存储）
                filePath = "uploads/" + fileName;
            }
            
            // 2. 保存资源到数据库
            com.app.entity.CulturalResourceFromUser resource = new com.app.entity.CulturalResourceFromUser();
            resource.setUserId(userId);
            resource.setResourceType(resourceType);
            
            // 设置标题（从文件名或文本内容提取）
            if (textContent != null && !textContent.trim().isEmpty()) {
                // 如果文本内容较短，直接作为标题
                String title = textContent.trim();
                if (title.length() > 255) {
                    title = title.substring(0, 255);
                }
                resource.setTitle(title);
            } else if (fileName != null) {
                // 使用文件名（去除扩展名和时间戳）作为标题
                String baseName = fileName;
                if (baseName.contains("-")) {
                    // 移除用户ID和时间戳部分
                    String[] parts = baseName.split("-");
                    if (parts.length > 2) {
                        baseName = String.join("-", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
                    }
                }
                int lastDotIndex = baseName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    baseName = baseName.substring(0, lastDotIndex);
                }
                resource.setTitle(baseName);
            }
            
            if (fileName != null) {
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                    resource.setFileFormat(fileName.substring(lastDotIndex + 1));
                }
            }
            resource.setStoragePath(filePath);
            resource.setContentFeatureData(textContent != null ? "{\"text\": \"" + textContent + "\"}" : null);
            resource.setUploadTime(java.time.LocalDateTime.now());
            resource.setAiReviewStatus("pending");
            
            // 3. 保存到数据库
            CulturalResourceFromUser savedResource = resourceUserRepository.save(resource);
            
            // 4. 创建标注任务
            AnnotationTask annotationTask = new AnnotationTask();
            annotationTask.setResourceId(savedResource.getId());
            annotationTask.setResourceSource("cultural_resources_from_user");
            annotationTask.setTaskType("自动标注");
            annotationTask.setStatus("待处理");
            annotationTask.setAnnotationMethod("AI自动标注");
            annotationTask.setCreatedAt(LocalDateTime.now());
            annotationTask.setUpdatedAt(LocalDateTime.now());
            annotationTaskRepository.save(annotationTask);
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "资源上传成功",
                "resource_id", savedResource.getId(), // 返回实际保存的资源ID
                "task_id", annotationTask.getId() // 返回创建的标注任务ID
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = Map.of(
                "success", false,
                "message", "资源上传失败：" + e.getMessage()
            );
            return ResponseEntity.status(500).body(result);
        }
    }

}

