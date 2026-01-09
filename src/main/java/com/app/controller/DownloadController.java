package com.app.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/download")
public class DownloadController {

    @GetMapping("/export")
    public ResponseEntity<Resource> downloadExportedFile(@RequestParam("file") String filePath) {
        try {
            // 支持file_path参数（向后兼容）
            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // 安全检查：确保文件路径在允许的目录内
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path basePath = projectRoot.resolve("exports").normalize();
            
            // 如果filePath是相对路径，解析为绝对路径
            Path requestedPath;
            if (Paths.get(filePath).isAbsolute()) {
                requestedPath = Paths.get(filePath).normalize();
            } else {
                // 处理相对路径（可能包含exports/前缀）
                if (filePath.startsWith("exports/") || filePath.startsWith("exports\\")) {
                    requestedPath = projectRoot.resolve(filePath).normalize();
                } else {
                    requestedPath = basePath.resolve(filePath).normalize();
                }
            }
            
            // 检查请求的路径是否在exports目录下
            if (!requestedPath.startsWith(basePath)) {
                return ResponseEntity.badRequest().build();
            }

            File file = requestedPath.toFile();
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/exported-file")
    public ResponseEntity<Resource> downloadExportedFileLegacy(@RequestParam("file_path") String filePath) {
        // 向后兼容的接口
        return downloadExportedFile(filePath);
    }
}