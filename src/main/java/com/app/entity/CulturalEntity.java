package com.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cultural_entities")
public class CulturalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name", nullable = false, length = 255)
    private String entityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 20)
    private EntityType entityType = EntityType.其他;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source", columnDefinition = "TEXT")
    private String source;

    @Column(name = "period_era", length = 100)
    private String periodEra;

    @Column(name = "cultural_region", length = 100)
    private String culturalRegion;

    @Column(name = "style_features", columnDefinition = "TEXT")
    private String styleFeatures;

    @Column(name = "cultural_value", columnDefinition = "TEXT")
    private String culturalValue;

    @Column(name = "related_images_url", columnDefinition = "TEXT")
    private String relatedImagesUrl;

    @Column(name = "digital_resource_link", columnDefinition = "TEXT")
    private String digitalResourceLink;

    public enum EntityType {
        人物, 作品, 事件, 地点, 其他
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPeriodEra() {
        return periodEra;
    }

    public void setPeriodEra(String periodEra) {
        this.periodEra = periodEra;
    }

    public String getCulturalRegion() {
        return culturalRegion;
    }

    public void setCulturalRegion(String culturalRegion) {
        this.culturalRegion = culturalRegion;
    }

    public String getStyleFeatures() {
        return styleFeatures;
    }

    public void setStyleFeatures(String styleFeatures) {
        this.styleFeatures = styleFeatures;
    }

    public String getCulturalValue() {
        return culturalValue;
    }

    public void setCulturalValue(String culturalValue) {
        this.culturalValue = culturalValue;
    }

    public String getRelatedImagesUrl() {
        return relatedImagesUrl;
    }

    public void setRelatedImagesUrl(String relatedImagesUrl) {
        this.relatedImagesUrl = relatedImagesUrl;
    }

    public String getDigitalResourceLink() {
        return digitalResourceLink;
    }

    public void setDigitalResourceLink(String digitalResourceLink) {
        this.digitalResourceLink = digitalResourceLink;
    }
}
