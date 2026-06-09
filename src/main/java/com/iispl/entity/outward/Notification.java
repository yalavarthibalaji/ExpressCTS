package com.iispl.entity.outward;

import java.time.LocalDateTime;

import com.iispl.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * File    : com/iispl/entity/Notification.java
 * Purpose : Stores in-app notifications for CTS users.
 *
 * Two kinds of events use this table:
 *
 *   1. REFER_BACK   — Checker referred a batch back to Maker.
 *                     recipient = the maker who owns the batch.
 *                     message   = "Batch B-xxx referred back: MICR_REPAIR, DATA_ENTRY"
 *                     link_url  = "/outward/viewBatches/viewBatches.zul"
 *
 *   2. RESUBMITTED  — Maker re-submitted a corrected batch to Checker.
 *                     recipient = every active CHECKER_OUTWARD user.
 *                     message   = "Batch B-xxx re-submitted by Maker John"
 *                     link_url  = "/outward/checkerQueue/checkerQueue.zul"
 *
 * Columns:
 *   recipient_id  — FK to users.id (who should see this notification)
 *   batch_id_str  — the human-readable batch ID, e.g. B-2026-0609-001
 *   event_type    — REFER_BACK | RESUBMITTED
 *   modules       — comma-separated module names, e.g. "MICR_REPAIR,DATA_ENTRY"
 *                   populated only for REFER_BACK events
 *   message       — full display text shown in the popup
 *   link_url      — the page to navigate to when the notification is clicked
 *   is_read       — false = unread (shows red dot); true = already read
 *   created_at    — when the notification was created
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who should receive this notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "batch_id_str", nullable = false, length = 50)
    private String batchIdStr;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    // Comma-separated modules, e.g. "MICR_REPAIR" or "MICR_REPAIR,DATA_ENTRY"
    // Only set for REFER_BACK events.
    @Column(name = "modules", length = 100)
    private String modules;

    @Column(name = "message", nullable = false, length = 300)
    private String message;

    @Column(name = "link_url", nullable = false, length = 200)
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public String getBatchIdStr() { return batchIdStr; }
    public void setBatchIdStr(String batchIdStr) { this.batchIdStr = batchIdStr; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getModules() { return modules; }
    public void setModules(String modules) { this.modules = modules; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}