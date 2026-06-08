package com.iispl.entity.outward;

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
@Table(name = "outward_cheque")
public class OutwardCheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many Cheques -> One Batch  (two-way: OutwardBatch.cheques)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private OutwardBatch batch;

    @Column(name = "seq_no", nullable = false)
    private int seqNo;

    @Column(name = "cheque_no", nullable = false, length = 10)
    private String chequeNo;

    // Original raw MICR — never changed after initial parse
    @Column(name = "micr_code", length = 50)
    private String micrCode;

    // Reconstructed MICR after maker repairs sub-fields
    @Column(name = "micr_code_corrected", length = 50)
    private String micrCodeCorrected;

    // MICR sub-fields — updated when maker does repair
    @Column(name = "city_code", length = 3)
    private String cityCode;

    @Column(name = "bank_code", length = 3)
    private String bankCode;

    @Column(name = "branch_code", length = 3)
    private String branchCode;

    @Column(name = "base_number", length = 6)
    private String baseNumber;

    @Column(name = "transaction_code", length = 2)
    private String transactionCode;

    @Column(name = "account_no", length = 20)
    private String accountNo;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @Column(name = "amount", precision = 13, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_in_words", length = 255)
    private String amountInWords;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "payee_name", length = 100)
    private String payeeName;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "repair_status", nullable = false, length = 20)
    private String repairStatus = "NOT_REQUIRED";

    @Column(name = "iqa_status", nullable = false, length = 20)
    private String iqaStatus = "PENDING";

    @Column(name = "front_image_path", length = 500)
    private String frontImagePath;

    @Column(name = "back_image_path", length = 500)
    private String backImagePath;

    @Column(name = "rejected_reason_code", length = 5)
    private String rejectedReasonCode;
    
    /**
     * Which Maker module should fix this cheque after Checker sent it back.
     * Values: 'MICR_REPAIR', 'DATA_ENTRY', or null (not currently referred).
     * Set by Checker when sending the cheque to Maker.
     * Cleared by Maker after fixing the issue.
     */
    @Column(name = "referred_to_module", length = 20)
    private String referredToModule;
    
    @Column(name = "is_micr_error", nullable = false)
    private boolean isMicrError = false;

    // Unidirectional — maker who rejected at entry step
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Two-way: OutwardCheque has many OutwardMicrRepairs
    @OneToMany(mappedBy = "outwardCheque", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OutwardMicrRepair> micrRepairs;

    // Two-way: OutwardCheque has many OutwardCheckerActions
    @OneToMany(mappedBy = "outwardCheque", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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

    public OutwardBatch getBatch() { return batch; }
    public void setBatch(OutwardBatch batch) { this.batch = batch; }

    public int getSeqNo() { return seqNo; }
    public void setSeqNo(int seqNo) { this.seqNo = seqNo; }

    public String getChequeNo() { return chequeNo; }
    public void setChequeNo(String chequeNo) { this.chequeNo = chequeNo; }

    public String getMicrCode() { return micrCode; }
    public void setMicrCode(String micrCode) { this.micrCode = micrCode; }

    public String getMicrCodeCorrected() { return micrCodeCorrected; }
    public void setMicrCodeCorrected(String micrCodeCorrected) { this.micrCodeCorrected = micrCodeCorrected; }

    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getBaseNumber() { return baseNumber; }
    public void setBaseNumber(String baseNumber) { this.baseNumber = baseNumber; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getAmountInWords() { return amountInWords; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }

    public LocalDate getChequeDate() { return chequeDate; }
    public void setChequeDate(LocalDate chequeDate) { this.chequeDate = chequeDate; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }

    public String getIqaStatus() { return iqaStatus; }
    public void setIqaStatus(String iqaStatus) { this.iqaStatus = iqaStatus; }

    public String getFrontImagePath() { return frontImagePath; }
    public void setFrontImagePath(String frontImagePath) { this.frontImagePath = frontImagePath; }

    public String getBackImagePath() { return backImagePath; }
    public void setBackImagePath(String backImagePath) { this.backImagePath = backImagePath; }

    public String getRejectedReasonCode() { return rejectedReasonCode; }
    public void setRejectedReasonCode(String rejectedReasonCode) { this.rejectedReasonCode = rejectedReasonCode; }

    public User getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(User rejectedBy) { this.rejectedBy = rejectedBy; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isMicrError() { return isMicrError; }
    public void setMicrError(boolean micrError) { this.isMicrError = micrError; }

    public List<OutwardMicrRepair> getMicrRepairs() { return micrRepairs; }
    public void setMicrRepairs(List<OutwardMicrRepair> micrRepairs) { this.micrRepairs = micrRepairs; }

    public List<OutwardCheckerAction> getCheckerActions() { return checkerActions; }
    public void setCheckerActions(List<OutwardCheckerAction> checkerActions) { this.checkerActions = checkerActions; }
    
    public String getReferredToModule() { return referredToModule; }
    public void setReferredToModule(String referredToModule) { this.referredToModule = referredToModule; }
}