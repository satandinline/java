package com.app.controller;

import com.app.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String nickname = request.get("nickname");
        String avatarPath = request.get("avatar_path");
        String securityQuestion = request.get("security_question");
        String securityAnswer = request.get("security_answer");
        
        Map<String, Object> result = authService.register(password, nickname, avatarPath, securityQuestion, securityAnswer);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String account = request.get("account");
            String password = request.get("password");
            
            if (account == null || account.isEmpty()) {
                Map<String, Object> result = Map.of("success", false, "message", "账号不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (password == null || password.isEmpty()) {
                Map<String, Object> result = Map.of("success", false, "message", "密码不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            Map<String, Object> result = authService.login(account, password);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                String message = (String) result.get("message");
                if (message != null && (message.contains("不存在") || message.contains("not found"))) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
                } else if (message != null && (message.contains("密码") || message.contains("password"))) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
                }
            }
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("[ERROR] Login exception: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "message", "登录失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUser(@RequestParam("user_id") Long userId) {
        Map<String, Object> userInfo = authService.getUserById(userId);
        if (userInfo == null) {
            Map<String, Object> result = Map.of("success", false, "message", "用户不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        
        Map<String, Object> result = Map.of("success", true, "user_info", userInfo);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/update-nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户信息");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String nickname = request.get("nickname");
        Map<String, Object> result = authService.updateNickname(userId, nickname);
        
        if ((Boolean) result.get("success")) {
            Map<String, Object> userInfo = authService.getUserById(userId);
            if (userInfo != null) {
                result.put("user_info", userInfo);
            }
        }
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/update-signature")
    public ResponseEntity<Map<String, Object>> updateSignature(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户信息");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String signature = request.get("signature");
        if (signature != null && signature.length() > 500) {
            Map<String, Object> result = Map.of("success", false, "message", "个人签名长度不能超过500个字符");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.updateSignature(userId, signature);
        if ((Boolean) result.get("success")) {
            Map<String, Object> userInfo = authService.getUserById(userId);
            if (userInfo != null) {
                result.put("user_info", userInfo);
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String oldPassword = request.get("old_password");
        String newPassword = request.get("new_password");
        
        if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "旧密码和新密码不能为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        if (oldPassword.equals(newPassword)) {
            Map<String, Object> result = Map.of("success", false, "message", "新密码不能与原密码相同");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.updatePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/change-password-by-security")
    public ResponseEntity<Map<String, Object>> changePasswordBySecurity(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String securityAnswer = request.get("security_answer");
        String newPassword = request.get("new_password");
        
        if (securityAnswer == null || securityAnswer.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "二级密码答案和新密码不能为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> verifyResult = authService.verifySecurityQuestion(userId, securityAnswer);
        if (!(Boolean) verifyResult.get("success")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(verifyResult);
        }
        
        // 检查新密码是否与原密码相同（需要查询用户当前密码）
        Map<String, Object> currentUserInfo = authService.getUserById(userId);
        String currentAccount = currentUserInfo.get("account").toString();
        
        // 通过数据库查询获取用户当前密码哈希
        String currentPasswordHash = authService.getCurrentPasswordHash(userId);
        String newPasswordHash = com.app.util.PasswordUtil.hashPassword(newPassword);
        
        if (currentPasswordHash != null && currentPasswordHash.equals(newPasswordHash)) {
            Map<String, Object> result = Map.of("success", false, "message", "新密码不能与原密码相同");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.resetPassword(currentAccount, newPassword);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/verify-security-answer")
    public ResponseEntity<Map<String, Object>> verifySecurityAnswer(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String answer = request.get("answer");
        if (answer == null || answer.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入答案");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.verifySecurityQuestion(userId, answer);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/change-security-question")
    public ResponseEntity<Map<String, Object>> changeSecurityQuestion(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String question = request.get("question");
        String answer = request.get("answer");
        
        if (question == null || question.isEmpty() || answer == null || answer.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "问题和答案不能为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.updateSecurityQuestion(userId, question, answer);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password/question")
    public ResponseEntity<Map<String, Object>> getSecurityQuestionForReset(@RequestBody Map<String, String> request) {
        String account = request.get("account");
        if (account == null || account.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入账号");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.getSecurityQuestion(account);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<Map<String, Object>> verifySecurityAnswerForReset(@RequestBody Map<String, String> request) {
        String account = request.get("account");
        String answer = request.get("answer");
        
        if (account == null || account.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入账号");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        if (answer == null || answer.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入安全问题答案");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.verifySecurityAnswer(account, answer);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, Object>> resetPasswordViaSecurity(@RequestBody Map<String, String> request) {
        String account = request.get("account");
        String answer = request.get("answer");
        String newPassword = request.get("new_password");
        
        if (account == null || account.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入账号");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        if (answer == null || answer.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入安全问题答案");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        if (newPassword == null || newPassword.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入新密码");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        if (newPassword.length() < 6) {
            Map<String, Object> result = Map.of("success", false, "message", "密码至少需要6个字符");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> verifyResult = authService.verifySecurityAnswer(account, answer);
        if (!(Boolean) verifyResult.get("success")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(verifyResult);
        }
        
        Map<String, Object> result = authService.resetPassword(account, newPassword);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/delete-account")
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户信息");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "请输入密码确认");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.deleteAccount(userId, password);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/change-avatar")
    public ResponseEntity<Map<String, Object>> changeAvatar(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        String avatarPath = request.get("avatar_path");
        Map<String, Object> result = authService.updateAvatar(userId, avatarPath);
        if ((Boolean) result.get("success")) {
            Map<String, Object> userInfo = authService.getUserById(userId);
            if (userInfo != null) {
                result.put("user_info", userInfo);
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody(required = false) Map<String, String> request) {
        if (userId == null && request != null) {
            userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id")) : null;
        }
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少用户ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        Map<String, Object> result = authService.logout(userId);
        return ResponseEntity.ok(result);
    }
}

