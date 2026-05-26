// balaji - 25/05/26
package com.iispl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "dem_dispatches")
public class DemDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // HAS-A relationship with OutwardBatch
    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private OutwardBatch batch;

    @Column(name = "batch_ref", length = 60)
    private String batchRef;

    @Column(name = "clearing_session_ref", length = 30)
    private String clearingSessionRef;

    @Column(name = "cxf_file_name", length = 150)
    private String cxfFileName;

    @Column(name = "cibf_file_name", length = 150)
    private String cibfFileName;

    @Column(name = "total_cheques")
    private Integer totalCheques;

    @Column(name = "total_amount")
    private Long totalAmount;

    // PENDING / SENT / ACKNOWLEDGED / FAILED
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "sent_by", length = 50)
    private String sentBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "acknowledgement_ref", length = 100)
    private String acknowledgementRef;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Short retryCount;


    // ── Constructors ──────────────────────────────────────

    public DemDispatch() {
    }


    // ── Getters and Setters ───────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OutwardBatch getBatch() {
        return batch;
    }

    public void setBatch(OutwardBatch batch) {
        this.batch = batch;
    }

    public String getBatchRef() {
        return batchRef;
    }

    public void setBatchRef(String batchRef) {
        this.batchRef = batchRef;
    }

    public String getClearingSessionRef() {
        return clearingSessionRef;
    }

    public void setClearingSessionRef(String clearingSessionRef) {
        this.clearingSessionRef = clearingSessionRef;
    }

    public String getCxfFileName() {
        return cxfFileName;
    }

    public void setCxfFileName(String cxfFileName) {
        this.cxfFileName = cxfFileName;
    }

    public String getCibfFileName() {
        return cibfFileName;
    }

    public void setCibfFileName(String cibfFileName) {
        this.cibfFileName = cibfFileName;
    }

    public Integer getTotalCheques() {
        return totalCheques;
    }

    public void setTotalCheques(Integer totalCheques) {
        this.totalCheques = totalCheques;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getAcknowledgementRef() {
        return acknowledgementRef;
    }

    public void setAcknowledgementRef(String acknowledgementRef) {
        this.acknowledgementRef = acknowledgementRef;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Short getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Short retryCount) {
        this.retryCount = retryCount;
    }
}