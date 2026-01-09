package com.app.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 环境变量初始化器
 * 在Spring Boot读取配置之前加载.env文件
 * 这样application.yml中的${MYSQL_PASSWORD}等变量就能正确读取
 */
public class EnvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Get current working directory (this is where mvn spring-boot:run was executed)
            Path currentDir = Paths.get(System.getProperty("user.dir"));
            Path envFile = null;
            
            // Try multiple locations to find .env file
            // 1. Current directory (if running from backend-java)
            Path currentEnvFile = currentDir.resolve(".env");
            if (currentEnvFile.toFile().exists()) {
                envFile = currentEnvFile;
            } else {
                // 2. If current directory is backend-java, .env should be here (but file doesn't exist)
                if (currentDir.getFileName().toString().equals("backend-java")) {
                    envFile = currentEnvFile;
                } else {
                    // 3. Try backend-java subdirectory (common case when running from project root)
                    Path backendJavaDir = currentDir.resolve("backend-java");
                    Path backendJavaEnvFile = backendJavaDir.resolve(".env");
                    if (backendJavaEnvFile.toFile().exists()) {
                        envFile = backendJavaEnvFile;
                    }
                }
            }
            
            if (envFile != null && envFile.toFile().exists()) {
                System.out.println("[INFO] Found .env file at: " + envFile.toAbsolutePath());
                Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent().toString())
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
                
                int loadedCount = 0;
                // 将.env文件中的变量设置到系统属性中
                // Spring Boot的${VAR}会先查找系统属性，再查找环境变量
                // 强制设置MYSQL相关的变量（即使已存在也要覆盖，确保使用.env中的值）
                for (io.github.cdimascio.dotenv.DotenvEntry entry : dotenv.entries()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    // 对于MYSQL相关的变量，强制设置系统属性（覆盖现有值）
                    if (key.startsWith("MYSQL_")) {
                        System.setProperty(key, value);
                        loadedCount++;
                        System.out.println("[INFO] Loaded from .env: " + key + " = " + (key.contains("PASSWORD") ? "***" : value));
                    } else if (System.getenv(key) == null && System.getProperty(key) == null) {
                        // 其他变量只在未设置时才设置
                        System.setProperty(key, value);
                        loadedCount++;
                    }
                }
                
                System.out.println("[INFO] Successfully loaded .env file from: " + envFile.toAbsolutePath() + " (" + loadedCount + " variables)");
                
                // 验证MYSQL_PASSWORD是否已设置
                String mysqlPassword = System.getProperty("MYSQL_PASSWORD");
                if (mysqlPassword != null && !mysqlPassword.isEmpty()) {
                    System.out.println("[INFO] MYSQL_PASSWORD is set (length: " + mysqlPassword.length() + " characters)");
                } else {
                    System.out.println("[WARN] MYSQL_PASSWORD is NOT set after loading .env file!");
                }
            } else {
                System.out.println("[WARN] .env file not found. Searched at: " + (envFile != null ? envFile.toAbsolutePath() : "null"));
                System.out.println("[WARN] Current working directory: " + currentDir.toAbsolutePath());
                System.out.println("[WARN] Using system environment variables only.");
            }
        } catch (Exception e) {
            System.out.println("[WARN] Failed to load .env file: " + e.getMessage() + ". Using system environment variables only.");
        }
    }
}
