package com.app.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class RootController {
    
    @GetMapping("/")
    public ResponseEntity<String> root(HttpServletRequest request) {
        // 返回自动重定向页面
        // 获取当前请求的协议和主机，用于判断是否是公网访问
        String scheme = request.getScheme();
        String host = request.getHeader("Host");
        if (host == null) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        
        // 如果是公网访问（不是localhost），返回提示页面
        if (!host.contains("localhost") && !host.contains("127.0.0.1")) {
            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html lang=\"zh-CN\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta http-equiv=\"refresh\" content=\"3;url=http://localhost:5193\">\n" +
                    "    <title>正在跳转...</title>\n" +
                    "    <style>\n" +
                    "        body { font-family: sans-serif; text-align: center; padding: 50px; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h1>正在跳转到前端登录界面...</h1>\n" +
                    "    <p>如果3秒后没有自动跳转，请访问前端地址（通常在终端输出中显示）</p>\n" +
                    "</body>\n" +
                    "</html>";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            return new ResponseEntity<>(htmlContent, headers, HttpStatus.OK);
        }
        
        // 如果是本地访问，直接重定向到本地前端
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create("http://localhost:5193"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}

