package com.app.service;

import com.app.entity.UserAccessLog;
import com.app.repository.UserAccessLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    
    @Autowired
    private UserAccessLogRepository accessLogRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            
            // 1. 历史总访问人次（从user_access_logs表统计，access_type='page_view'）
            long totalUsers = accessLogRepository.countDistinctUsersByAccessType("page_view");
            
            // 如果user_access_logs表为空，回退到qa_messages表统计
            if (totalUsers == 0) {
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT user_id) FROM qa_messages WHERE user_message IS NOT NULL AND user_message != ''",
                        Long.class);
                    totalUsers = count != null ? count : 0;
                } catch (Exception e) {
                    totalUsers = 0;
                }
            }
            
            // 2. 今日访问人次
            long todayUsers = accessLogRepository.countDistinctUsersByAccessTypeAndTime("page_view", todayStart);
            if (todayUsers == 0) {
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT user_id) FROM qa_messages WHERE create_time >= ? AND user_message IS NOT NULL AND user_message != ''",
                        Long.class, todayStart);
                    todayUsers = count != null ? count : 0;
                } catch (Exception e) {
                    todayUsers = 0;
                }
            }
            
            // 3. 历史文字AIGC使用人次
            long totalTextUsers = accessLogRepository.countDistinctUsersByAccessType("aigc_text");
            if (totalTextUsers == 0) {
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT user_id) FROM qa_messages WHERE model = 'text' AND user_message IS NOT NULL AND user_message != ''",
                        Long.class);
                    totalTextUsers = count != null ? count : 0;
                } catch (Exception e) {
                    totalTextUsers = 0;
                }
            }
            
            // 4. 今日文字AIGC使用人次
            long todayTextUsers = accessLogRepository.countDistinctUsersByAccessTypeAndTime("aigc_text", todayStart);
            if (todayTextUsers == 0) {
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT user_id) FROM qa_messages WHERE model = 'text' AND create_time >= ? AND user_message IS NOT NULL AND user_message != ''",
                        Long.class, todayStart);
                    todayTextUsers = count != null ? count : 0;
                } catch (Exception e) {
                    todayTextUsers = 0;
                }
            }
            
            // 5. 历史图片AIGC使用人次
            long totalImageUsers = accessLogRepository.countDistinctUsersByAccessType("aigc_image");
            if (totalImageUsers == 0) {
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT user_id) FROM qa_messages WHERE model = 'image' AND user_message IS NOT NULL AND user_message != ''",
                        Long.class);
                    totalImageUsers = count != null ? count : 0;
                } catch (Exception e) {
                    totalImageUsers = 0;
                }
            }
            
            // 6. 今日图片AIGC使用人次
            long todayImageUsers = accessLogRepository.countDistinctUsersByAccessTypeAndTime("aigc_image", todayStart);
            if (todayImageUsers == 0) {
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT user_id) FROM qa_messages WHERE model = 'image' AND create_time >= ? AND user_message IS NOT NULL AND user_message != ''",
                        Long.class, todayStart);
                    todayImageUsers = count != null ? count : 0;
                } catch (Exception e) {
                    todayImageUsers = 0;
                }
            }
            
            // 7. 历史文字AIGC使用次数
            long totalTextCount = 0;
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM qa_messages WHERE model = 'text' AND user_message IS NOT NULL AND user_message != ''",
                    Long.class);
                totalTextCount = count != null ? count : 0;
            } catch (Exception e) {
                totalTextCount = 0;
            }
            
            // 8. 今日文字AIGC使用次数
            long todayTextCount = 0;
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM qa_messages WHERE model = 'text' AND create_time >= ? AND user_message IS NOT NULL AND user_message != ''",
                    Long.class, todayStart);
                todayTextCount = count != null ? count : 0;
            } catch (Exception e) {
                todayTextCount = 0;
            }
            
            // 9. 历史图片AIGC使用次数
            long totalImageCount = 0;
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM qa_messages WHERE model = 'image' AND user_message IS NOT NULL AND user_message != ''",
                    Long.class);
                totalImageCount = count != null ? count : 0;
            } catch (Exception e) {
                totalImageCount = 0;
            }
            
            // 10. 今日图片AIGC使用次数
            long todayImageCount = 0;
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM qa_messages WHERE model = 'image' AND create_time >= ? AND user_message IS NOT NULL AND user_message != ''",
                    Long.class, todayStart);
                todayImageCount = count != null ? count : 0;
            } catch (Exception e) {
                todayImageCount = 0;
            }
            
            // 11. 最近7天的使用趋势
            LocalDateTime sevenDaysAgo = todayStart.minusDays(6);
            List<Map<String, Object>> trendData = new ArrayList<>();
            
            for (int i = 6; i >= 0; i--) {
                LocalDate targetDate = LocalDate.now().minusDays(i);
                LocalDateTime dayStart = LocalDateTime.of(targetDate, LocalTime.MIN);
                LocalDateTime dayEnd = LocalDateTime.of(targetDate, LocalTime.MAX);
                String dateStr = targetDate.toString();
                
                // 每日访问次数
                long dailyUsers = 0;
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_access_logs WHERE access_type = 'page_view' AND access_time >= ? AND access_time <= ?",
                        Long.class, dayStart, dayEnd);
                    dailyUsers = count != null ? count : 0;
                } catch (Exception e) {
                    dailyUsers = 0;
                }
                
                // 每日文字AIGC使用次数
                long textCount = 0;
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM qa_messages WHERE model = 'text' AND create_time >= ? AND create_time <= ? AND user_message IS NOT NULL AND user_message != ''",
                        Long.class, dayStart, dayEnd);
                    textCount = count != null ? count : 0;
                } catch (Exception e) {
                    textCount = 0;
                }
                
                // 每日图片AIGC使用次数
                long imageCount = 0;
                try {
                    Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM qa_messages WHERE model = 'image' AND create_time >= ? AND create_time <= ? AND user_message IS NOT NULL AND user_message != ''",
                        Long.class, dayStart, dayEnd);
                    imageCount = count != null ? count : 0;
                } catch (Exception e) {
                    imageCount = 0;
                }
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", dateStr);
                dayData.put("daily_users", dailyUsers);
                dayData.put("text_count", textCount);
                dayData.put("image_count", imageCount);
                dayData.put("total_aigc_count", textCount + imageCount);
                trendData.add(dayData);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("total_users", totalUsers);
            data.put("today_users", todayUsers);
            data.put("total_text_users", totalTextUsers);
            data.put("today_text_users", todayTextUsers);
            data.put("total_image_users", totalImageUsers);
            data.put("today_image_users", todayImageUsers);
            data.put("total_text_count", totalTextCount);
            data.put("today_text_count", todayTextCount);
            data.put("total_image_count", totalImageCount);
            data.put("today_image_count", todayImageCount);
            data.put("trend_data", trendData);
            
            result.put("success", true);
            result.put("data", data);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取统计信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    @Transactional
    public Map<String, Object> logAccess(Long userId, String accessType, String accessPath, Long resourceId, String resourceType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            UserAccessLog log = new UserAccessLog();
            log.setUserId(userId);
            log.setAccessType(accessType != null ? accessType : "page_view");
            log.setAccessPath(accessPath);
            log.setResourceId(resourceId);
            log.setResourceType(resourceType);
            log.setAccessTime(LocalDateTime.now());
            
            accessLogRepository.save(log);
            
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "记录访问日志失败：" + e.getMessage());
        }
        
        return result;
    }
}
