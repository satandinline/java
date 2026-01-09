package com.app.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    // 提供爬虫图片服务
    @GetMapping("/crawled/{filename:.+}")
    public ResponseEntity<Resource> serveCrawledImage(@PathVariable String filename) {
        try {
            // 构建安全的文件路径，防止路径遍历攻击（相对于项目根目录）
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path basePath = projectRoot.resolve("crawled_images").normalize();
            Path requestedPath = basePath.resolve(filename).normalize();
            
            // 确保请求的路径在允许的目录下
            if (!requestedPath.startsWith(basePath)) {
                return ResponseEntity.badRequest().build();
            }
            
            File file = requestedPath.toFile();
            
            // 如果文件不存在，尝试使用basename
            if (!file.exists()) {
                String basename = Paths.get(filename).getFileName().toString();
                Path alternativePath = basePath.resolve(basename).normalize();
                file = alternativePath.toFile();
            }
            
            if (!file.exists()) {
                // 查找默认图片（相对于项目根目录）
                Path projectRootForDefault = Paths.get(System.getProperty("user.dir"));
                File defaultFile = projectRootForDefault.resolve("public").resolve("default.jpg").toFile();
                if (defaultFile.exists()) {
                    file = defaultFile;
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 提供AIGC图片服务
    @GetMapping("/aigc/{filename:.+}")
    public ResponseEntity<Resource> serveAigcImage(@PathVariable String filename) {
        try {
            // 构建安全的文件路径，防止路径遍历攻击（相对于项目根目录）
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path basePath = projectRoot.resolve("AIGC_graph").normalize();
            Path requestedPath = basePath.resolve(filename).normalize();
            
            // 确保请求的路径在允许的目录下
            if (!requestedPath.startsWith(basePath)) {
                return ResponseEntity.badRequest().build();
            }
            
            File file = requestedPath.toFile();
            
            // 如果文件不存在，尝试使用basename
            if (!file.exists()) {
                String basename = Paths.get(filename).getFileName().toString();
                Path alternativePath = basePath.resolve(basename).normalize();
                file = alternativePath.toFile();
            }
            
            if (!file.exists()) {
                // 查找默认图片（相对于项目根目录）
                Path projectRootForDefault = Paths.get(System.getProperty("user.dir"));
                File defaultFile = projectRootForDefault.resolve("public").resolve("default.jpg").toFile();
                if (defaultFile.exists()) {
                    file = defaultFile;
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 提供用户上传图片服务
    @GetMapping("/user/{filename:.+}")
    public ResponseEntity<Resource> serveUserImage(@PathVariable String filename) {
        try {
            // 构建安全的文件路径，防止路径遍历攻击（相对于项目根目录）
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path basePath = projectRoot.resolve("uploads").normalize();
            Path requestedPath = basePath.resolve(filename).normalize();
            
            // 确保请求的路径在允许的目录下
            if (!requestedPath.startsWith(basePath)) {
                return ResponseEntity.badRequest().build();
            }
            
            File file = requestedPath.toFile();
            
            // 如果文件不存在，尝试使用basename
            if (!file.exists()) {
                String basename = Paths.get(filename).getFileName().toString();
                Path alternativePath = basePath.resolve(basename).normalize();
                file = alternativePath.toFile();
            }
            
            if (!file.exists()) {
                // 查找默认图片（相对于项目根目录）
                Path projectRoot2 = Paths.get(System.getProperty("user.dir"));
                File defaultFile = projectRoot2.resolve("public").resolve("default.jpg").toFile();
                if (defaultFile.exists()) {
                    file = defaultFile;
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}