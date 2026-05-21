package com.iispl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "outward_cheque_staging")
public class OutwardChequeStaging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "source_file_name", length = 150)
    private String sourceFileName;

    @Column(name = "uploaded_by", length = 50)
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "batch_id", length = 60)
    private String batchId;

    @Column(name = "cheque_number", length = 20)
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

    @Column(name = "depositor_account_number", length = 30)
    private String depositorAccountNumber;

    @Column(name = "image_front_path", length = 255)
    private String imageFrontPath;

    @Column(name = "image_back_path", length = 255)
    private String imageBackPath;

    @Column(name = "iqa_status", length = 10)
    private String iqaStatus;

    @Column(name = "staging_status", nullable = false, length = 20)
    private String stagingStatus;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;


    // ── Constructors ──────────────────────────────────────

    public OutwardChequeStaging() {
    }


    // ── Getters and Setters ───────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
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

    public String getStagingStatus() {
        return stagingStatus;
    }

    public void setStagingStatus(String stagingStatus) {
        this.stagingStatus = stagingStatus;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}