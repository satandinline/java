package com.app.service;

import com.app.entity.Notification;
import com.app.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    public Map<String, Object> getNotifications(Long userId, Integer isRead, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 20;
            
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<Notification> notifications;
            
            if (isRead != null) {
                // 根据isRead过滤
                Boolean readStatus = isRead == 1;
                notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(
                    userId, readStatus, pageable);
            } else {
                // 获取所有通知
                notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            }
            
            // 计算未读数量
            long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);
            
            result.put("success", true);
            result.put("notifications", notifications.getContent());
            result.put("total", notifications.getTotalElements());
            result.put("page", page);
            result.put("page_size", pageSize);
            result.put("total_pages", notifications.getTotalPages());
            result.put("unread_count", unreadCount);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取通知失败：" + e.getMessage());
        }
        
        return result;
    }
    
    @Transactional
    public Map<String, Object> markNotificationRead(Long notificationId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
            if (notificationOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "通知不存在");
                return result;
            }
            
            Notification notification = notificationOpt.get();
            notification.setIsRead(true);
            notificationRepository.save(notification);
            
            result.put("success", true);
            result.put("message", "通知已标记为已读");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "标记通知失败：" + e.getMessage());
        }
        
        return result;
    }
    
    @Transactional
    public Map<String, Object> markAllNotificationsRead(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int updated = notificationRepository.markAllAsReadByUserId(userId);
            
            result.put("success", true);
            result.put("message", "所有通知已标记为已读");
            result.put("updated_count", updated);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "标记所有通知失败：" + e.getMessage());
        }
        
        return result;
    }
    
    @Transactional
    public void createNotification(Long userId, String notificationType, String content, Long relatedId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setNotificationType(notificationType);
            notification.setContent(content);
            notification.setRelatedId(relatedId);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响主流程
            e.printStackTrace();
        }
    }
}
