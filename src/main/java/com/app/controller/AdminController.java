package com.app.controller;

import com.app.entity.User;
import com.app.repository.UserRepository;
import com.app.service.AuthService;
import com.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;
    
    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics(
            @RequestParam("userId") Long userId) {
        // 检查用户权限（管理员或超级管理员）
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "用户不存在");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        User user = userOpt.get();
        if (!user.getRole().equals(User.UserRole.管理员) && !user.getRole().equals(User.UserRole.超级管理员)) {
            Map<String, Object> result = Map.of("success", false, "message", "权限不足，仅管理员可访问");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        // 实现统计功能 - 根据Python版本逻辑实现
        Map<String, Object> result = adminService.getDashboardStatistics();
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/log-access")
    public ResponseEntity<Map<String, Object>> logAccess(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("user_id");
        Long userId = null;
        if (userIdObj != null) {
            try {
                userId = Long.parseLong(userIdObj.toString());
            } catch (NumberFormatException e) {
                Map<String, Object> result = Map.of("success", false, "message", "无效的用户ID");
                return ResponseEntity.badRequest().body(result);
            }
        }
        
        String accessType = request.get("access_type") != null ? 
            request.get("access_type").toString() : "page_view";
        String accessPath = request.get("access_path") != null ? 
            request.get("access_path").toString() : null;
        
        Long resourceId = null;
        Object resourceIdObj = request.get("resource_id");
        if (resourceIdObj != null) {
            try {
                resourceId = Long.parseLong(resourceIdObj.toString());
            } catch (NumberFormatException e) {
                // 忽略无效的resource_id
            }
        }
        
        String resourceType = request.get("resource_type") != null ? 
            request.get("resource_type").toString() : null;
        
        if (userId == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少user_id参数");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = adminService.logAccess(userId, accessType, accessPath, resourceId, resourceType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(@RequestParam("userId") Long userId) {
        // 检查用户权限（超级管理员）
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "用户不存在");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        User user = userOpt.get();
        if (!user.getRole().equals(User.UserRole.超级管理员)) {
            Map<String, Object> result = Map.of("success", false, "message", "权限不足，仅超级管理员可查看");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        // 获取所有用户列表
        List<User> users = userRepository.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("users", users);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/auth/switch-role")
    public ResponseEntity<Map<String, Object>> switchUserRole(@RequestBody Map<String, Object> request) {
        Long currentUserId = (Long) request.get("current_user_id");
        Long targetUserId = (Long) request.get("target_user_id");
        String newRole = (String) request.get("new_role");

        if (currentUserId == null || targetUserId == null || newRole == null) {
            Map<String, Object> result = Map.of("success", false, "message", "缺少必要参数");
            return ResponseEntity.badRequest().body(result);
        }

        // 验证角色值
        if (!newRole.equals("普通用户") && !newRole.equals("管理员") && !newRole.equals("超级管理员")) {
            Map<String, Object> result = Map.of("success", false, "message", "无效的角色值");
            return ResponseEntity.badRequest().body(result);
        }

        // 检查当前用户权限（超级管理员）
        Optional<User> currentUserOpt = userRepository.findById(currentUserId);
        if (currentUserOpt.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "当前用户不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        User currentUser = currentUserOpt.get();
        if (!currentUser.getRole().equals(User.UserRole.超级管理员)) {
            Map<String, Object> result = Map.of("success", false, "message", "权限不足，仅超级管理员可操作");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        // 检查目标用户是否存在
        Optional<User> targetUserOpt = userRepository.findById(targetUserId);
        if (targetUserOpt.isEmpty()) {
            Map<String, Object> result = Map.of("success", false, "message", "目标用户不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        User targetUser = targetUserOpt.get();

        // 不能修改超级管理员的角色
        if (targetUser.getRole().equals(User.UserRole.超级管理员) && !newRole.equals("超级管理员")) {
            Map<String, Object> result = Map.of("success", false, "message", "不能修改超级管理员的角色");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        // 更新用户角色
        User.UserRole newRoleEnum = User.UserRole.valueOf(newRole);
        targetUser.setRole(newRoleEnum);
        userRepository.save(targetUser);

        Map<String, Object> result = Map.of("success", true, "message", "角色切换成功");
        return ResponseEntity.ok(result);
    }
}