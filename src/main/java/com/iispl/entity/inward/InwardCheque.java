package com.iispl.entity.inward;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
@Table(name = "inward_cheque")
public class InwardCheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private InwardBatch batch;

    @Column(name = "seq_no", nullable = false)
    private int seqNo;

    @Column(name = "cheque_no", nullable = false, length = 10)
    private String chequeNo;

    // ── From CXF file (declared by presenting bank) ──
    @Column(name = "cheque_date", nullable = false)
    private LocalDate chequeDate;

    @Column(name = "amount", nullable = false, precision = 13, scale = 2)
    private BigDecimal amount;

    // ── From OCR scan of cheque image (NEW Step 2 comparison) ──
    @Column(name = "cheque_date_ocr")
    private LocalDate chequeDateOcr;

    @Column(name = "amount_ocr", precision = 13, scale = 2)
    private BigDecimal amountOcr;

    @Column(name = "amount_in_words", length = 255)
    private String amountInWords;

    @Column(name = "city_code", length = 3)
    private String cityCode;

    @Column(name = "bank_code", length = 3)
    private String bankCode;

    @Column(name = "branch_code", length = 3)
    private String branchCode;

    @Column(name = "micr_code_raw", length = 50)
    private String micrCodeRaw;

    @Column(name = "micr_code_corrected", length = 50)
    private String micrCodeCorrected;

    @Column(name = "is_micr_error", nullable = false)
    private boolean isMicrError = false;

    @Column(name = "presenting_bank_code", length = 10)
    private String presentingBankCode;

    @Column(name = "presenting_bank_name", length = 100)
    private String presentingBankName;

    @Column(name = "drawee_account_number", length = 20)
    private String draweeAccountNumber;

    @Column(name = "drawee_account_holder", length = 100)
    private String draweeAccountHolder;

    @Column(name = "account_balance", precision = 15, scale = 2)
    private BigDecimal accountBalance;

    @Column(name = "is_account_valid")
    private Boolean isAccountValid = false;

    @Column(name = "is_bank_matched")
    private Boolean isBankMatched = false;

    @Column(name = "payee_name", length = 100)
    private String payeeName;

    @Column(name = "iqa_status", nullable = false, length = 20)
    private String iqaStatus = "PENDING";

    // RECEIVED | ENTRY_DONE | SUBMITTED | ACCEPTED | RETURNED | SEND_BACK
    @Column(name = "status", nullable = false, length = 30)
    private String status = "RECEIVED";

    // NOT_REQUIRED | NEEDS_REPAIR | REPAIRED  (covers Step 1 MICR + Step 2 Date/Amt)
    @Column(name = "repair_status", nullable = false, length = 20)
    private String repairStatus = "NOT_REQUIRED";

    @Column(name = "front_image_path", length = 500)
    private String frontImagePath;

    @Column(name = "back_image_path", length = 500)
    private String backImagePath;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "inwardCheque", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InwardMicrRepair> micrRepairs;

    @OneToMany(mappedBy = "inwardCheque", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InwardCheckerAction> checkerActions;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InwardBatch getBatch() { return batch; }
    public void setBatch(InwardBatch batch) { this.batch = batch; }

    public int getSeqNo() { return seqNo; }
    public void setSeqNo(int seqNo) { this.seqNo = seqNo; }

    public String getChequeNo() { return chequeNo; }
    public void setChequeNo(String chequeNo) { this.chequeNo = chequeNo; }

    public LocalDate getChequeDate() { return chequeDate; }
    public void setChequeDate(LocalDate chequeDate) { this.chequeDate = chequeDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getChequeDateOcr() { return chequeDateOcr; }
    public void setChequeDateOcr(LocalDate chequeDateOcr) { this.chequeDateOcr = chequeDateOcr; }

    public BigDecimal getAmountOcr() { return amountOcr; }
    public void setAmountOcr(BigDecimal amountOcr) { this.amountOcr = amountOcr; }

    public String getAmountInWords() { return amountInWords; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }

    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getMicrCodeRaw() { return micrCodeRaw; }
    public void setMicrCodeRaw(String micrCodeRaw) { this.micrCodeRaw = micrCodeRaw; }

    public String getMicrCodeCorrected() { return micrCodeCorrected; }
    public void setMicrCodeCorrected(String micrCodeCorrected) { this.micrCodeCorrected = micrCodeCorrected; }

    public boolean isMicrError() { return isMicrError; }
    public void setMicrError(boolean micrError) { isMicrError = micrError; }

    public String getPresentingBankCode() { return presentingBankCode; }
    public void setPresentingBankCode(String presentingBankCode) { this.presentingBankCode = presentingBankCode; }

    public String getPresentingBankName() { return presentingBankName; }
    public void setPresentingBankName(String presentingBankName) { this.presentingBankName = presentingBankName; }

    public String getDraweeAccountNumber() { return draweeAccountNumber; }
    public void setDraweeAccountNumber(String draweeAccountNumber) { this.draweeAccountNumber = draweeAccountNumber; }

    public String getDraweeAccountHolder() { return draweeAccountHolder; }
    public void setDraweeAccountHolder(String draweeAccountHolder) { this.draweeAccountHolder = draweeAccountHolder; }

    public BigDecimal getAccountBalance() { return accountBalance; }
    public void setAccountBalance(BigDecimal accountBalance) { this.accountBalance = accountBalance; }

    public Boolean getIsAccountValid() { return isAccountValid; }
    public void setIsAccountValid(Boolean isAccountValid) { this.isAccountValid = isAccountValid; }

    public Boolean getIsBankMatched() { return isBankMatched; }
    public void setIsBankMatched(Boolean isBankMatched) { this.isBankMatched = isBankMatched; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getIqaStatus() { return iqaStatus; }
    public void setIqaStatus(String iqaStatus) { this.iqaStatus = iqaStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }

    public String getFrontImagePath() { return frontImagePath; }
    public void setFrontImagePath(String frontImagePath) { this.frontImagePath = frontImagePath; }

    public String getBackImagePath() { return backImagePath; }
    public void setBackImagePath(String backImagePath) { this.backImagePath = backImagePath; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<InwardMicrRepair> getMicrRepairs() { return micrRepairs; }
    public void setMicrRepairs(List<InwardMicrRepair> micrRepairs) { this.micrRepairs = micrRepairs; }

    public List<InwardCheckerAction> getCheckerActions() { return checkerActions; }
    public void setCheckerActions(List<InwardCheckerAction> checkerActions) { this.checkerActions = checkerActions; }
}