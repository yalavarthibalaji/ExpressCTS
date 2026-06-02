package com.iispl.entity.inward;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "inward_batch")
public class InwardBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 30)
    private String batchId;

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "source_file_path", length = 500)
    private String sourceFilePath;

    @Column(name = "total_cheques", nullable = false)
    private int totalCheques = 0;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "micr_error_count", nullable = false)
    private int micrErrorCount = 0;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "RECEIVED";

    @Column(name = "repair_status", nullable = false, length = 20)
    private String repairStatus = "NOT_REQUIRED";

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    // Unidirectional — maker who received the file
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", nullable = false)
    private User receivedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Two-way: InwardBatch has many InwardCheques
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InwardCheque> cheques;

    // Two-way: InwardBatch has many InwardExports
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InwardExport> exports;

    // Two-way: InwardBatch has many InwardCheckerActions
    @OneToMany(mappedBy = "inwardBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InwardCheckerAction> checkerActions;

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

    public LocalDate getBatchDate() { return batchDate; }
    public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getSourceFilePath() { return sourceFilePath; }
    public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }

    public int getTotalCheques() { return totalCheques; }
    public void setTotalCheques(int totalCheques) { this.totalCheques = totalCheques; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public int getMicrErrorCount() { return micrErrorCount; }
    public void setMicrErrorCount(int micrErrorCount) { this.micrErrorCount = micrErrorCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }

    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }

    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<InwardCheque> getCheques() { return cheques; }
    public void setCheques(List<InwardCheque> cheques) { this.cheques = cheques; }

    public List<InwardExport> getExports() { return exports; }
    public void setExports(List<InwardExport> exports) { this.exports = exports; }

    public List<InwardCheckerAction> getCheckerActions() { return checkerActions; }
    public void setCheckerActions(List<InwardCheckerAction> checkerActions) { this.checkerActions = checkerActions; }
}
