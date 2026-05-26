// balaji - 25/05/26

package com.iispl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // INFO / WARN / ERROR
    @Column(name = "log_type", length = 20)
    private String logType;

    // e.g. BATCH_CREATED / CHEQUE_APPROVED / USER_LOGIN
    @Column(name = "action_code", length = 30)
    private String actionCode;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "user_role", length = 30)
    private String userRole;

    @Column(name = "batch_ref", length = 60)
    private String batchRef;

    @Column(name = "cheque_ref", length = 20)
    private String chequeRef;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;


    // ── Constructors ──────────────────────────────────────

    public AuditLog() {
    }


    // ── Getters and Setters ───────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getBatchRef() {
        return batchRef;
    }

    public void setBatchRef(String batchRef) {
        this.batchRef = batchRef;
    }

    public String getChequeRef() {
        return chequeRef;
    }

    public void setChequeRef(String chequeRef) {
        this.chequeRef = chequeRef;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
}