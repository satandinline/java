package com.app.service;

import com.app.entity.QASession;
import com.app.entity.QAMessage;
import com.app.repository.QASessionRepository;
import com.app.repository.QAMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AIGCService {
    
    @Autowired
    private QASessionRepository sessionRepository;
    
    @Autowired
    private QAMessageRepository messageRepository;
    
    /**
     * 获取用户的AIGC会话列表
     */
    public Map<String, Object> getSessions(Long userId, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (page < 1) {
            page = 1;
        }
        
        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QASession> sessionPage = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            
            List<Map<String, Object>> sessionList = sessionPage.getContent().stream().map(session -> {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("id", session.getId());
                sessionMap.put("user_id", session.getUserId());
                sessionMap.put("created_at", session.getCreatedAt());
                sessionMap.put("summary", session.getSummary());
                sessionMap.put("mode", session.getMode());
                
                // 获取会话的第一条用户消息作为摘要（如果summary为空）
                if (session.getSummary() == null || session.getSummary().trim().isEmpty()) {
                    List<QAMessage> messages = messageRepository.findUserMessagesBySessionId(session.getId());
                    if (!messages.isEmpty()) {
                        String firstMessage = messages.get(0).getUserMessage();
                        if (firstMessage != null && firstMessage.length() > 50) {
                            sessionMap.put("summary", firstMessage.substring(0, 50) + "...");
                        } else if (firstMessage != null) {
                            sessionMap.put("summary", firstMessage);
                        }
                    }
                }
                
                return sessionMap;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("sessions", sessionList);
            result.put("total", sessionPage.getTotalElements());
            result.put("page", page);
            result.put("page_size", pageSize);
            result.put("total_pages", sessionPage.getTotalPages());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取会话列表失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取指定会话的消息列表
     */
    public Map<String, Object> getSessionMessages(Long sessionId, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (page < 1) {
            page = 1;
        }
        
        try {
            // 验证会话是否存在
            Optional<QASession> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "会话不存在");
                return result;
            }
            
            // 先查询所有消息（因为需要拆分，所以不分页查询）
            List<QAMessage> allMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
            
            // 将每条记录拆分为用户消息和AI消息（如果都存在）
            List<Map<String, Object>> allMessageList = new java.util.ArrayList<>();
            for (QAMessage message : allMessages) {
                // 添加用户消息（如果存在）
                if (message.getUserMessage() != null && !message.getUserMessage().trim().isEmpty()) {
                    Map<String, Object> userMessageMap = new HashMap<>();
                    userMessageMap.put("id", message.getId());
                    userMessageMap.put("user_id", message.getUserId());
                    userMessageMap.put("session_id", message.getSessionId());
                    userMessageMap.put("create_time", message.getCreateTime());
                    userMessageMap.put("timestamp", message.getTimestamp());
                    userMessageMap.put("role", "user");
                    userMessageMap.put("content", message.getUserMessage());
                    userMessageMap.put("user_message", message.getUserMessage());
                    userMessageMap.put("image_from_users_url", message.getImageFromUsersUrl());
                    allMessageList.add(userMessageMap);
                }
                
                // 添加AI消息（如果存在）
                if (message.getAiMessage() != null && !message.getAiMessage().trim().isEmpty()) {
                    Map<String, Object> aiMessageMap = new HashMap<>();
                    aiMessageMap.put("id", message.getId());
                    aiMessageMap.put("user_id", message.getUserId());
                    aiMessageMap.put("session_id", message.getSessionId());
                    aiMessageMap.put("create_time", message.getCreateTime());
                    aiMessageMap.put("timestamp", message.getTimestamp());
                    aiMessageMap.put("role", "assistant");
                    aiMessageMap.put("content", message.getAiMessage());
                    aiMessageMap.put("ai_message", message.getAiMessage());
                    aiMessageMap.put("model", message.getModel() != null ? message.getModel() : "text");
                    aiMessageMap.put("image_url", message.getImageUrl());
                    aiMessageMap.put("user_feedback", message.getUserFeedback());
                    allMessageList.add(aiMessageMap);
                }
            }
            
            // 在内存中进行分页
            int total = allMessageList.size();
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            List<Map<String, Object>> messageList = start < total ? 
                allMessageList.subList(start, end) : new java.util.ArrayList<>();
            
            result.put("success", true);
            result.put("messages", messageList);
            result.put("total", total);
            result.put("page", page);
            result.put("page_size", pageSize);
            result.put("total_pages", (int) Math.ceil((double) total / pageSize));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取会话消息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 创建新会话
     */
    public Map<String, Object> createSession(Long userId, String mode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            QASession session = new QASession();
            session.setUserId(userId);
            session.setMode(mode != null ? mode : "text");
            session.setCreatedAt(LocalDateTime.now());
            
            QASession savedSession = sessionRepository.save(session);
            
            // 构建session对象，确保包含所有必要字段
            Map<String, Object> sessionObj = new HashMap<>();
            sessionObj.put("id", savedSession.getId());
            sessionObj.put("user_id", savedSession.getUserId());
            sessionObj.put("mode", savedSession.getMode());
            sessionObj.put("created_at", savedSession.getCreatedAt());
            sessionObj.put("summary", savedSession.getSummary());
            
            result.put("success", true);
            result.put("session_id", savedSession.getId());
            result.put("session", sessionObj);  // 添加完整的session对象
            result.put("message", "会话创建成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建会话失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除会话
     */
    public Map<String, Object> deleteSession(Long sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<QASession> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "会话不存在");
                return result;
            }
            
            // 删除会话下的所有消息（级联删除应该自动处理）
            messageRepository.deleteBySessionId(sessionId);
            sessionRepository.deleteById(sessionId);
            
            result.put("success", true);
            result.put("message", "会话删除成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除会话失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量删除会话
     */
    public Map<String, Object> batchDeleteSessions(List<Long> sessionIds) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            for (Long sessionId : sessionIds) {
                messageRepository.deleteBySessionId(sessionId);
                sessionRepository.deleteById(sessionId);
            }
            
            result.put("success", true);
            result.put("message", "批量删除会话成功");
            result.put("deleted_count", sessionIds.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "批量删除会话失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除用户的所有会话
     */
    public Map<String, Object> deleteAllSessions(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<QASession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
            int count = 0;
            for (QASession session : sessions) {
                messageRepository.deleteBySessionId(session.getId());
                sessionRepository.deleteById(session.getId());
                count++;
            }
            
            result.put("success", true);
            result.put("message", "删除所有会话成功");
            result.put("deleted_count", count);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除所有会话失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新会话摘要
     */
    public Map<String, Object> updateSessionSummary(Long sessionId, String summary) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<QASession> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "会话不存在");
                return result;
            }
            
            QASession session = sessionOpt.get();
            session.setSummary(summary);
            sessionRepository.save(session);
            
            result.put("success", true);
            result.put("message", "会话摘要更新成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新会话摘要失败：" + e.getMessage());
        }
        
        return result;
    }
}
