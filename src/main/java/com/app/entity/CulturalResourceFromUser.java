package com.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cultural_resources_from_user")
public class CulturalResourceFromUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @Column(name = "content_feature_data", columnDefinition = "LONGTEXT")
    private String contentFeatureData;

    @Column(name = "content_hash", length = 255)
    private String contentHash;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "ai_review_status", length = 20)
    private String aiReviewStatus = "pending";

    @Column(name = "manual_review_status", length = 20)
    private String manualReviewStatus = "pending";

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getContentFeatureData() {
        return contentFeatureData;
    }

    public void setContentFeatureData(String contentFeatureData) {
        this.contentFeatureData = contentFeatureData;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getAiReviewStatus() {
        return aiReviewStatus;
    }

    public void setAiReviewStatus(String aiReviewStatus) {
        this.aiReviewStatus = aiReviewStatus;
    }

    public String getManualReviewStatus() {
        return manualReviewStatus;
    }

    public void setManualReviewStatus(String manualReviewStatus) {
        this.manualReviewStatus = manualReviewStatus;
    }
}

