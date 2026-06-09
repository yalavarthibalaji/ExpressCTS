package com.iispl.dto;

import java.time.LocalDateTime;

/**
 * File    : com/iispl/dto/NotificationDto.java
 * Purpose : Carries notification data from service to TopbarComposer.
 *           Simple POJO — no Hibernate entity, no lazy loading issues.
 */
public class NotificationDto {

    private Long          id;
    private String        batchIdStr;
    private String        eventType;
    private String        message;
    private String        linkUrl;
    private boolean       read;
    private LocalDateTime createdAt;

    public NotificationDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchIdStr() { return batchIdStr; }
    public void setBatchIdStr(String batchIdStr) { this.batchIdStr = batchIdStr; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}