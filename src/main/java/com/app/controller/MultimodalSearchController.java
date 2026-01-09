package com.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MultimodalSearchController {

    @Value("${python.aigc.service.url:http://localhost:7200}")
    private String pythonAigcServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/multimodal/search")
    public ResponseEntity<Map<String, Object>> multimodalSearch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "mode", defaultValue = "text") String mode,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "user_id", required = false) String userIdStr) {
        
        java.util.List<Path> tempFiles = new java.util.ArrayList<>();
        
        try {
            // 获取用户ID
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

            // 构建转发到Python服务的请求
            String pythonUrl = pythonAigcServiceUrl + "/api/multimodal/search";
            
            // 创建MultiValueMap用于multipart/form-data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("mode", mode);
            if (query != null) {
                body.add("query", query);
            }
            body.add("user_id", userId.toString());
            
            // 处理图片文件
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        // 将MultipartFile保存为临时文件
                        String originalFilename = image.getOriginalFilename();
                        if (originalFilename == null) {
                            originalFilename = "image";
                        }
                        Path tempFile = Files.createTempFile("multimodal_", "_" + originalFilename);
                        tempFiles.add(tempFile);
                        image.transferTo(tempFile.toFile());
                        FileSystemResource fileResource = new FileSystemResource(tempFile.toFile());
                        body.add("images", fileResource);
                    }
                }
            }
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-User-Id", userId.toString());
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 转发请求到Python服务
            ResponseEntity<Map> pythonResponse = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            // 返回Python服务的响应
            if (pythonResponse.getBody() != null) {
                return ResponseEntity.ok(pythonResponse.getBody());
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Python服务返回空响应");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (org.springframework.web.client.RestClientException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无法连接到Python AIGC服务，请确保Python服务正在运行在 " + pythonAigcServiceUrl);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "多模态搜索失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } finally {
            // 清理临时文件
            for (Path tempFile : tempFiles) {
                try {
                    if (Files.exists(tempFile)) {
                        Files.delete(tempFile);
                    }
                } catch (IOException e) {
                    // 忽略删除失败
                }
            }
        }
    }
}