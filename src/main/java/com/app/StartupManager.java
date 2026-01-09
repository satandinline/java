package com.app;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 项目启动管理器
 * 负责启动前端服务器和后端服务器
 * 
 * 使用方法：
 * 1. 直接运行：mvn spring-boot:run -Dspring-boot.run.main-class=com.app.StartupManager
 * 2. 或编译后运行：java -cp target/classes com.app.StartupManager
 */
public class StartupManager {
    
    private static Process frontendProcess;

    
    public static void main(String[] args) {
        // 设置控制台输出编码为UTF-8
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");
        // 确保System.out使用UTF-8编码
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 如果设置失败，继续执行
        }
        
        System.out.println("=".repeat(60));
        System.out.println("Starting Java Backend Server (Local Deployment)");
        System.out.println("=".repeat(60));
        
        // 获取Java项目根目录（backend-java目录）
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path javaProjectRoot;
        // 如果当前目录是backend-java，使用当前目录
        if (currentDir.getFileName().toString().equals("backend-java")) {
            javaProjectRoot = currentDir;
        } else {
            // 否则假设当前目录就是项目根目录，查找backend-java
            javaProjectRoot = currentDir.resolve("backend-java");
            if (!Files.exists(javaProjectRoot)) {
                javaProjectRoot = currentDir;
            }
        }
        
        // 1. Start frontend server (using FrontEnd under Java project)
        if (!startFrontend(javaProjectRoot)) {
            System.out.println("[WARN] Frontend server startup failed, continuing with other services...");
        }
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        

        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 3. Load .env file before starting Spring Boot (ensure env vars are set)
        System.out.println("\n[INFO] Loading environment variables from .env file...");
        loadEnvFileBeforeSpringBoot(javaProjectRoot);
        
        // 4. Start backend server
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Starting Java Backend Server (Port 7210)");
        System.out.println("=".repeat(60));
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[INFO] Shutting down all services...");
            if (frontendProcess != null && frontendProcess.isAlive()) {
                frontendProcess.destroy();
                try {
                    frontendProcess.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    frontendProcess.destroyForcibly();
                }
            }

        }));
        
        // 启动Spring Boot应用
        // 使用CulturalResourcesApplication.main()而不是直接调用SpringApplication.run()
        // 这样可以确保EnvInitializer被正确执行
        CulturalResourcesApplication.main(args);
    }
    
    /**
     * Start frontend server
     */
    private static boolean startFrontend(Path projectRoot) {
        System.out.println("=".repeat(60));
        System.out.println("Starting Frontend Server (Port 5193)");
        System.out.println("=".repeat(60));
        
        // FrontEnd目录在Java项目根目录下（backend-java/FrontEnd）
        Path frontendDir = projectRoot.resolve("FrontEnd");
        if (!Files.exists(frontendDir)) {
            System.out.println("[ERROR] FrontEnd directory does not exist");
            return false;
        }
        
        // Find npm
        String npmCmd = findNpm();
        if (npmCmd == null) {
            System.out.println("[ERROR] npm not found, please ensure Node.js is installed");
            return false;
        }
        
        // Check node_modules
        Path nodeModules = frontendDir.resolve("node_modules");
        if (!Files.exists(nodeModules)) {
            System.out.println("[INFO] node_modules not found, installing dependencies...");
            try {
                Process installProcess = new ProcessBuilder(npmCmd, "install")
                    .directory(frontendDir.toFile())
                    .inheritIO()
                    .start();
                int exitCode = installProcess.waitFor();
                if (exitCode != 0) {
                    System.out.println("[ERROR] Dependency installation failed");
                    return false;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Dependency installation failed: " + e.getMessage());
                return false;
            }
        }
        
        // Start frontend server
        System.out.println("[INFO] Starting frontend server (port 5193)...");
        try {
            ProcessBuilder pb = new ProcessBuilder(npmCmd, "run", "dev", "--", "--port", "5193")
                .directory(frontendDir.toFile())
                .redirectErrorStream(true);
            
            frontendProcess = pb.start();
            
            // Start thread to read frontend output (using UTF-8 encoding, filter ANSI escape codes)
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(frontendProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Filter ANSI escape codes to avoid garbled text in Windows CMD
                        String cleanedLine = line.replaceAll("\u001B\\[[;\\d]*m", "");
                        if (!cleanedLine.trim().isEmpty()) {
                            System.out.println("[Frontend] " + cleanedLine);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[ERROR] Failed to read frontend output: " + e.getMessage());
                }
            });
            
            Thread.sleep(3000);
            System.out.println("[OK] Frontend server started");
            
            // Detect actual frontend port (range 5193-5200)
            int actualPort = detectFrontendPort(5193, 5200);
            System.out.println("[INFO] Frontend access address: http://localhost:" + actualPort);
            return true;
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to start frontend server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    

    
    /**
     * Detect actual frontend port
     */
    private static int detectFrontendPort(int startPort, int endPort) {
        try {
            // Use netstat command to detect port (Windows)
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                Process process = new ProcessBuilder("netstat", "-ano")
                    .start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        for (int port = startPort; port <= endPort; port++) {
                            if (line.contains(":" + port) && line.contains("LISTENING")) {
                                System.out.println("[INFO] Detected frontend server running on port: " + port);
                                return port;
                            }
                        }
                    }
                }
            } else {
                // Linux/Mac use lsof
                for (int port = startPort; port <= endPort; port++) {
                    try {
                        Process process = new ProcessBuilder("lsof", "-ti", ":" + port)
                            .start();
                        if (process.waitFor() == 0) {
                            System.out.println("[INFO] Detected frontend server running on port: " + port);
                            return port;
                        }
                    } catch (Exception e) {
                        // 继续检查下一个端口
                    }
                }
            }
        } catch (Exception e) {
            // If detection fails, return default port
        }
        System.out.println("[WARN] Frontend server not detected, using default port: " + startPort);
        return startPort;
    }
    
    /**
     * 查找npm可执行文件路径
     */
    private static String findNpm() {
        // 首先尝试直接使用npm（如果在PATH中）
        try {
            Process process = new ProcessBuilder("npm", "--version").start();
            if (process.waitFor() == 0) {
                return "npm";
            }
        } catch (Exception e) {
            // 继续查找
        }
        
        // Windows上尝试常见路径
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            String[] commonPaths = {
                System.getenv("ProgramFiles") + "\\nodejs\\npm.cmd",
                System.getenv("ProgramFiles(x86)") + "\\nodejs\\npm.cmd",
                System.getenv("LOCALAPPDATA") + "\\Programs\\nodejs\\npm.cmd",
                "C:\\Program Files\\nodejs\\npm.cmd",
                "C:\\Program Files (x86)\\nodejs\\npm.cmd"
            };
            for (String path : commonPaths) {
                if (path != null && new File(path).exists()) {
                    return path;
                }
            }
            // 尝试使用where命令查找npm
            try {
                Process whereProcess = new ProcessBuilder("where", "npm").start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(whereProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            // 优先选择.cmd文件
                            if (line.endsWith(".cmd")) {
                                if (new File(line).exists()) {
                                    return line;
                                }
                            } else if (line.endsWith(".exe") || !line.contains(".")) {
                                // 如果是.exe或没有扩展名，尝试添加.cmd
                                String cmdPath = line;
                                if (!line.endsWith(".exe")) {
                                    cmdPath = line + ".cmd";
                                } else {
                                    cmdPath = line.replace(".exe", ".cmd");
                                }
                                if (new File(cmdPath).exists()) {
                                    return cmdPath;
                                }
                                // 如果.cmd不存在，使用原路径
                                if (new File(line).exists()) {
                                    return line;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略错误，继续
            }
        }
        
        return null;
    }
    
    /**
     * Load .env file before Spring Boot starts
     * This ensures environment variables are set before Spring Boot reads application.yml
     */
    private static void loadEnvFileBeforeSpringBoot(Path projectRoot) {
        try {
            Path envFile = projectRoot.resolve(".env");
            if (envFile.toFile().exists()) {
                System.out.println("[INFO] Found .env file at: " + envFile.toAbsolutePath());
                Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent().toString())
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
                
                int loadedCount = 0;
                // Force set MYSQL-related variables (override existing values to ensure .env values are used)
                for (io.github.cdimascio.dotenv.DotenvEntry entry : dotenv.entries()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    // For MYSQL-related variables, force set system property (override existing)
                    if (key.startsWith("MYSQL_")) {
                        System.setProperty(key, value);
                        loadedCount++;
                        System.out.println("[INFO] Loaded from .env: " + key + " = " + (key.contains("PASSWORD") ? "***" : value));
                    } else if (System.getenv(key) == null && System.getProperty(key) == null) {
                        // Other variables only set if not already set
                        System.setProperty(key, value);
                        loadedCount++;
                    }
                }
                
                System.out.println("[INFO] Successfully loaded .env file (" + loadedCount + " variables)");
                
                // Verify MYSQL_PASSWORD is set
                String mysqlPassword = System.getProperty("MYSQL_PASSWORD");
                if (mysqlPassword != null && !mysqlPassword.isEmpty()) {
                    System.out.println("[INFO] MYSQL_PASSWORD is set (length: " + mysqlPassword.length() + " characters)");
                } else {
                    System.out.println("[WARN] MYSQL_PASSWORD is NOT set after loading .env file!");
                }
            } else {
                System.out.println("[WARN] .env file not found at: " + envFile.toAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("[WARN] Failed to load .env file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


