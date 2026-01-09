package com.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cultural_resources")
public class CulturalResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @Column(name = "source_from", length = 255)
    private String sourceFrom;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "content_feature_data", columnDefinition = "LONGTEXT")
    private String contentFeatureData;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "upload_user_id")
    private Long uploadUserId;

    @Column(name = "ai_review_status", length = 20)
    private String aiReviewStatus = "pending";

    @Column(name = "ai_review_remark", columnDefinition = "TEXT")
    private String aiReviewRemark;

    @Column(name = "manual_review_status", length = 20)
    private String manualReviewStatus = "pending";

    @Column(name = "manual_review_remark", columnDefinition = "TEXT")
    private String manualReviewRemark;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSourceFrom() {
        return sourceFrom;
    }

    public void setSourceFrom(String sourceFrom) {
        this.sourceFrom = sourceFrom;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getContentFeatureData() {
        return contentFeatureData;
    }

    public void setContentFeatureData(String contentFeatureData) {
        this.contentFeatureData = contentFeatureData;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

    public Long getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(Long uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public String getAiReviewStatus() {
        return aiReviewStatus;
    }

    public void setAiReviewStatus(String aiReviewStatus) {
        this.aiReviewStatus = aiReviewStatus;
    }

    public String getAiReviewRemark() {
        return aiReviewRemark;
    }

    public void setAiReviewRemark(String aiReviewRemark) {
        this.aiReviewRemark = aiReviewRemark;
    }

    public String getManualReviewStatus() {
        return manualReviewStatus;
    }

    public void setManualReviewStatus(String manualReviewStatus) {
        this.manualReviewStatus = manualReviewStatus;
    }

    public String getManualReviewRemark() {
        return manualReviewRemark;
    }

    public void setManualReviewRemark(String manualReviewRemark) {
        this.manualReviewRemark = manualReviewRemark;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
}

