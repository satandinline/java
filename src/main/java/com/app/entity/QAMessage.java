package com.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qa_messages")
public class QAMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "ai_message", columnDefinition = "TEXT")
    private String aiMessage;

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "model", length = 10)
    private String model;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_from_users_url", length = 500)
    private String imageFromUsersUrl;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getAiMessage() {
        return aiMessage;
    }

    public void setAiMessage(String aiMessage) {
        this.aiMessage = aiMessage;
    }

    public String getUserFeedback() {
        return userFeedback;
    }

    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageFromUsersUrl() {
        return imageFromUsersUrl;
    }

    public void setImageFromUsersUrl(String imageFromUsersUrl) {
        this.imageFromUsersUrl = imageFromUsersUrl;
    }
}
