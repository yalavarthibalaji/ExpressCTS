package com.iispl.entity.outward;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.iispl.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "outward_batch")
public class OutwardBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 30)
    private String batchId;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "UPLOADED";

    @Column(name = "repair_status", nullable = false, length = 20)
    private String repairStatus = "NOT_REQUIRED";

    @Column(name = "cheque_count", nullable = false)
    private int chequeCount = 0;

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", precision = 15, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "file_path", length = 500)
    private String filePath;

    // Unidirectional — maker who submitted
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // Unidirectional — checker who verified
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // Unidirectional — maker who created batch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Two-way: OutwardBatch has many OutwardCheques
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OutwardCheque> cheques;

    // Two-way: OutwardBatch has many OutwardExports
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OutwardExport> exports;

    // Two-way: OutwardBatch has many OutwardCheckerActions
    @OneToMany(mappedBy = "outwardBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OutwardCheckerAction> checkerActions;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }

    public int getChequeCount() { return chequeCount; }
    public void setChequeCount(int chequeCount) { this.chequeCount = chequeCount; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public User getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public User getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(User verifiedBy) { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OutwardCheque> getCheques() { return cheques; }
    public void setCheques(List<OutwardCheque> cheques) { this.cheques = cheques; }

    public List<OutwardExport> getExports() { return exports; }
    public void setExports(List<OutwardExport> exports) { this.exports = exports; }

    public List<OutwardCheckerAction> getCheckerActions() { return checkerActions; }
    public void setCheckerActions(List<OutwardCheckerAction> checkerActions) { this.checkerActions = checkerActions; }
}