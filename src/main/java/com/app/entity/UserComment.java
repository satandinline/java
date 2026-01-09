package com.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_comments")
public class UserComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_content", columnDefinition = "TEXT", nullable = false)
    private String commentContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_status", length = 20)
    private CommentStatus commentStatus = CommentStatus.approved;

    @Column(name = "is_academic_discussion")
    private Boolean isAcademicDiscussion = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public CommentStatus getCommentStatus() {
        return commentStatus;
    }

    public void setCommentStatus(CommentStatus commentStatus) {
        this.commentStatus = commentStatus;
    }

    public Boolean getIsAcademicDiscussion() {
        return isAcademicDiscussion;
    }

    public void setIsAcademicDiscussion(Boolean isAcademicDiscussion) {
        this.isAcademicDiscussion = isAcademicDiscussion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum CommentStatus {
        approved, pending, rejected
    }
}

