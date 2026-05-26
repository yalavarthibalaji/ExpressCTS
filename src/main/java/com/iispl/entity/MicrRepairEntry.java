// Balaji - 25/06/25

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
@Table(name = "micr_repair_entries")
public class MicrRepairEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // HAS-A relationship with OutwardCheque
    @ManyToOne
    @JoinColumn(name = "outward_cheque_id", nullable = false)
    private OutwardCheque outwardCheque;

    @Column(name = "cheque_number", length = 20)
    private String chequeNumber;

    // PENDING / REPAIRED / FLAGGED
    @Column(name = "repair_status", nullable = false, length = 20)
    private String repairStatus;

    // original values from XML (bad data)
    @Column(name = "original_micr_code", length = 20)
    private String originalMicrCode;

    @Column(name = "original_ifsc_code", length = 20)
    private String originalIfscCode;

    @Column(name = "original_account_number", length = 30)
    private String originalAccountNumber;

    // corrected values entered by MICR repair operator
    @Column(name = "corrected_micr_code", length = 20)
    private String correctedMicrCode;

    @Column(name = "corrected_ifsc_code", length = 20)
    private String correctedIfscCode;

    @Column(name = "corrected_account_number", length = 30)
    private String correctedAccountNumber;

    @Column(name = "corrected_amount", length = 30)
    private String correctedAmount;

    @Column(name = "corrected_cheque_date", length = 20)
    private String correctedChequeDate;

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;

    @Column(name = "repair_remarks", columnDefinition = "TEXT")
    private String repairRemarks;

    @Column(name = "repaired_by", length = 50)
    private String repairedBy;

    @Column(name = "repaired_at")
    private LocalDateTime repairedAt;

    // checker verification fields
    @Column(name = "checker_verified", nullable = false)
    private Boolean checkerVerified;

    @Column(name = "checker_verified_by", length = 50)
    private String checkerVerifiedBy;

    @Column(name = "checker_verified_at")
    private LocalDateTime checkerVerifiedAt;

    @Column(name = "checker_remarks", columnDefinition = "TEXT")
    private String checkerRemarks;


    // ── Constructors ──────────────────────────────────────

    public MicrRepairEntry() {
    }


    // ── Getters and Setters ───────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OutwardCheque getOutwardCheque() {
        return outwardCheque;
    }

    public void setOutwardCheque(OutwardCheque outwardCheque) {
        this.outwardCheque = outwardCheque;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public String getRepairStatus() {
        return repairStatus;
    }

    public void setRepairStatus(String repairStatus) {
        this.repairStatus = repairStatus;
    }

    public String getOriginalMicrCode() {
        return originalMicrCode;
    }

    public void setOriginalMicrCode(String originalMicrCode) {
        this.originalMicrCode = originalMicrCode;
    }

    public String getOriginalIfscCode() {
        return originalIfscCode;
    }

    public void setOriginalIfscCode(String originalIfscCode) {
        this.originalIfscCode = originalIfscCode;
    }

    public String getOriginalAccountNumber() {
        return originalAccountNumber;
    }

    public void setOriginalAccountNumber(String originalAccountNumber) {
        this.originalAccountNumber = originalAccountNumber;
    }

    public String getCorrectedMicrCode() {
        return correctedMicrCode;
    }

    public void setCorrectedMicrCode(String correctedMicrCode) {
        this.correctedMicrCode = correctedMicrCode;
    }

    public String getCorrectedIfscCode() {
        return correctedIfscCode;
    }

    public void setCorrectedIfscCode(String correctedIfscCode) {
        this.correctedIfscCode = correctedIfscCode;
    }

    public String getCorrectedAccountNumber() {
        return correctedAccountNumber;
    }

    public void setCorrectedAccountNumber(String correctedAccountNumber) {
        this.correctedAccountNumber = correctedAccountNumber;
    }

    public String getCorrectedAmount() {
        return correctedAmount;
    }

    public void setCorrectedAmount(String correctedAmount) {
        this.correctedAmount = correctedAmount;
    }

    public String getCorrectedChequeDate() {
        return correctedChequeDate;
    }

    public void setCorrectedChequeDate(String correctedChequeDate) {
        this.correctedChequeDate = correctedChequeDate;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public String getRepairRemarks() {
        return repairRemarks;
    }

    public void setRepairRemarks(String repairRemarks) {
        this.repairRemarks = repairRemarks;
    }

    public String getRepairedBy() {
        return repairedBy;
    }

    public void setRepairedBy(String repairedBy) {
        this.repairedBy = repairedBy;
    }

    public LocalDateTime getRepairedAt() {
        return repairedAt;
    }

    public void setRepairedAt(LocalDateTime repairedAt) {
        this.repairedAt = repairedAt;
    }

    public Boolean getCheckerVerified() {
        return checkerVerified;
    }

    public void setCheckerVerified(Boolean checkerVerified) {
        this.checkerVerified = checkerVerified;
    }

    public String getCheckerVerifiedBy() {
        return checkerVerifiedBy;
    }

    public void setCheckerVerifiedBy(String checkerVerifiedBy) {
        this.checkerVerifiedBy = checkerVerifiedBy;
    }

    public LocalDateTime getCheckerVerifiedAt() {
        return checkerVerifiedAt;
    }

    public void setCheckerVerifiedAt(LocalDateTime checkerVerifiedAt) {
        this.checkerVerifiedAt = checkerVerifiedAt;
    }

    public String getCheckerRemarks() {
        return checkerRemarks;
    }

    public void setCheckerRemarks(String checkerRemarks) {
        this.checkerRemarks = checkerRemarks;
    }
}