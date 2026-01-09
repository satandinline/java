package com.app.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 环境变量配置类
 * 用于从.env文件加载环境变量到系统环境变量中
 * 这样Spring Boot的application.yml就可以通过${MYSQL_PASSWORD}等方式读取
 */
@Configuration
public class EnvConfig {
    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);

    // Note: This is a fallback. EnvInitializer should load .env before Spring Boot reads config.
    @PostConstruct
    public void loadEnvFile() {
        try {
            // Try to load .env file from current working directory
            Path currentDir = Paths.get(System.getProperty("user.dir"));
            Path envFile = currentDir.resolve(".env");
            
            // If .env file doesn't exist, try to find it in backend-java directory
            if (!envFile.toFile().exists()) {
                // If current directory is backend-java, use current directory
                if (currentDir.getFileName().toString().equals("backend-java")) {
                    envFile = currentDir.resolve(".env");
                } else {
                    // Otherwise try to find in backend-java subdirectory
                    Path backendJavaDir = currentDir.resolve("backend-java");
                    if (backendJavaDir.toFile().exists()) {
                        envFile = backendJavaDir.resolve(".env");
                    }
                }
            }
            
            if (envFile.toFile().exists()) {
                Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent().toString())
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
                
                // 将.env文件中的变量设置到系统属性中（Spring Boot会优先读取系统属性）
                int loadedCount = 0;
                for (var entry : dotenv.entries()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    // 如果系统环境变量和系统属性都未设置，则设置系统属性
                    // Spring Boot的${VAR}会先查找系统属性，再查找环境变量
                    if (System.getenv(key) == null && System.getProperty(key) == null) {
                        System.setProperty(key, value);
                        loadedCount++;
                        // 只在INFO级别显示重要的数据库配置变量
                        if (key.startsWith("MYSQL_")) {
                            logger.info("Loaded from .env: {} = {}", key, key.contains("PASSWORD") ? "***" : value);
                        }
                    }
                }
                
                logger.info("Successfully loaded .env file from: {} ({} variables)", envFile, loadedCount);
            } else {
                logger.warn(".env file not found at: {}. Using system environment variables only.", envFile);
            }
        } catch (Exception e) {
            logger.warn("Failed to load .env file: {}. Using system environment variables only.", e.getMessage());
        }
    }
}
