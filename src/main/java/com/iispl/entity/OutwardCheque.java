package com.iispl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "outward_cheques")
public class OutwardCheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cheque_id", nullable = false, unique = true, length = 20)
    private String chequeId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 30)
    private String transactionId;

    // HAS-A relationship with OutwardBatch
    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private OutwardBatch batch;

    @Column(name = "cheque_number", nullable = false, length = 20)
    private String chequeNumber;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "micr_code", length = 20)
    private String micrCode;

    @Column(name = "micr_status", length = 20)
    private String micrStatus;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "presentation_date")
    private LocalDate presentationDate;

    @Column(name = "drawer_name", length = 150)
    private String drawerName;

    @Column(name = "drawer_account_number", length = 30)
    private String drawerAccountNumber;

    @Column(name = "payee_name", length = 150)
    private String payeeName;

    @Column(name = "amount_in_words", columnDefinition = "TEXT")
    private String amountInWords;

    @Column(name = "amount_in_figures")
    private Long amountInFigures;

    @Column(name = "hv_category", length = 10)
    private String hvCategory;

    @Column(name = "depositor_account_number", length = 30)
    private String depositorAccountNumber;

    @Column(name = "image_front_path", length = 255)
    private String imageFrontPath;

    @Column(name = "image_back_path", length = 255)
    private String imageBackPath;

    @Column(name = "iqa_status", length = 10)
    private String iqaStatus;

    @Column(name = "maker_status", length = 20)
    private String makerStatus;

    @Column(name = "maker_remarks", columnDefinition = "TEXT")
    private String makerRemarks;

    @Column(name = "maker_user_id", length = 50)
    private String makerUserId;

    @Column(name = "maker_done_at")
    private LocalDateTime makerDoneAt;

    @Column(name = "checker_status", length = 20)
    private String checkerStatus;

    @Column(name = "checker_remarks", columnDefinition = "TEXT")
    private String checkerRemarks;

    @Column(name = "checker_user_id", length = 50)
    private String checkerUserId;

    @Column(name = "checker_done_at")
    private LocalDateTime checkerDoneAt;

    @Column(name = "reviewed")
    private Boolean reviewed;


    // ── Constructors ──────────────────────────────────

    public OutwardCheque() {
    }


    // ── Getters and Setters ───────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChequeId() {
        return chequeId;
    }

    public void setChequeId(String chequeId) {
        this.chequeId = chequeId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public OutwardBatch getBatch() {
        return batch;
    }

    public void setBatch(OutwardBatch batch) {
        this.batch = batch;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getMicrCode() {
        return micrCode;
    }

    public void setMicrCode(String micrCode) {
        this.micrCode = micrCode;
    }

    public String getMicrStatus() {
        return micrStatus;
    }

    public void setMicrStatus(String micrStatus) {
        this.micrStatus = micrStatus;
    }

    public LocalDate getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(LocalDate chequeDate) {
        this.chequeDate = chequeDate;
    }

    public LocalDate getPresentationDate() {
        return presentationDate;
    }

    public void setPresentationDate(LocalDate presentationDate) {
        this.presentationDate = presentationDate;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    public String getDrawerAccountNumber() {
        return drawerAccountNumber;
    }

    public void setDrawerAccountNumber(String drawerAccountNumber) {
        this.drawerAccountNumber = drawerAccountNumber;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getAmountInWords() {
        return amountInWords;
    }

    public void setAmountInWords(String amountInWords) {
        this.amountInWords = amountInWords;
    }

    public Long getAmountInFigures() {
        return amountInFigures;
    }

    public void setAmountInFigures(Long amountInFigures) {
        this.amountInFigures = amountInFigures;
    }

    public String getHvCategory() {
        return hvCategory;
    }

    public void setHvCategory(String hvCategory) {
        this.hvCategory = hvCategory;
    }

    public String getDepositorAccountNumber() {
        return depositorAccountNumber;
    }

    public void setDepositorAccountNumber(String depositorAccountNumber) {
        this.depositorAccountNumber = depositorAccountNumber;
    }

    public String getImageFrontPath() {
        return imageFrontPath;
    }

    public void setImageFrontPath(String imageFrontPath) {
        this.imageFrontPath = imageFrontPath;
    }

    public String getImageBackPath() {
        return imageBackPath;
    }

    public void setImageBackPath(String imageBackPath) {
        this.imageBackPath = imageBackPath;
    }

    public String getIqaStatus() {
        return iqaStatus;
    }

    public void setIqaStatus(String iqaStatus) {
        this.iqaStatus = iqaStatus;
    }

    public String getMakerStatus() {
        return makerStatus;
    }

    public void setMakerStatus(String makerStatus) {
        this.makerStatus = makerStatus;
    }

    public String getMakerRemarks() {
        return makerRemarks;
    }

    public void setMakerRemarks(String makerRemarks) {
        this.makerRemarks = makerRemarks;
    }

    public String getMakerUserId() {
        return makerUserId;
    }

    public void setMakerUserId(String makerUserId) {
        this.makerUserId = makerUserId;
    }

    public LocalDateTime getMakerDoneAt() {
        return makerDoneAt;
    }

    public void setMakerDoneAt(LocalDateTime makerDoneAt) {
        this.makerDoneAt = makerDoneAt;
    }

    public String getCheckerStatus() {
        return checkerStatus;
    }

    public void setCheckerStatus(String checkerStatus) {
        this.checkerStatus = checkerStatus;
    }

    public String getCheckerRemarks() {
        return checkerRemarks;
    }

    public void setCheckerRemarks(String checkerRemarks) {
        this.checkerRemarks = checkerRemarks;
    }

    public String getCheckerUserId() {
        return checkerUserId;
    }

    public void setCheckerUserId(String checkerUserId) {
        this.checkerUserId = checkerUserId;
    }

    public LocalDateTime getCheckerDoneAt() {
        return checkerDoneAt;
    }

    public void setCheckerDoneAt(LocalDateTime checkerDoneAt) {
        this.checkerDoneAt = checkerDoneAt;
    }

    public Boolean getReviewed() {
        return reviewed;
    }

    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }
}