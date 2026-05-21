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
@Table(name = "outward_batches")
public class OutwardBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 60)
    private String batchId;

    @Column(name = "batch_index")
    private Short batchIndex;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "clearing_date", nullable = false)
    private LocalDate clearingDate;

    @Column(name = "clearing_session_ref", length = 30)
    private String clearingSessionRef;

    @Column(name = "route", length = 20)
    private String route;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "is_micr_repair_batch", nullable = false)
    private Boolean isMicrRepairBatch;

    @Column(name = "total_cheques", nullable = false)
    private Integer totalCheques;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "iqa_pass", nullable = false)
    private Integer iqaPass;

    @Column(name = "iqa_fail", nullable = false)
    private Integer iqaFail;

    @Column(name = "xml_file_name", length = 150)
    private String xmlFileName;

    @Column(name = "cbx_file", length = 150)
    private String cbxFile;

    @Column(name = "cibf_file", length = 150)
    private String cibfFile;

    @Column(name = "cxf_file", length = 150)
    private String cxfFile;

    @Column(name = "maker_done", nullable = false)
    private Boolean makerDone;

    @Column(name = "checker_done", nullable = false)
    private Boolean checkerDone;

    @Column(name = "cxf_generated", nullable = false)
    private Boolean cxfGenerated;

    @Column(name = "dem_sent", nullable = false)
    private Boolean demSent;

    @Column(name = "supervisor_verified", nullable = false)
    private Boolean supervisorVerified;

    @Column(name = "return_window_deadline")
    private LocalDateTime returnWindowDeadline;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;


    // ── Constructors ──────────────────────────────────────

    public OutwardBatch() {
    }


    // ── Getters and Setters ───────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Short getBatchIndex() {
        return batchIndex;
    }

    public void setBatchIndex(Short batchIndex) {
        this.batchIndex = batchIndex;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public LocalDate getClearingDate() {
        return clearingDate;
    }

    public void setClearingDate(LocalDate clearingDate) {
        this.clearingDate = clearingDate;
    }

    public String getClearingSessionRef() {
        return clearingSessionRef;
    }

    public void setClearingSessionRef(String clearingSessionRef) {
        this.clearingSessionRef = clearingSessionRef;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsMicrRepairBatch() {
        return isMicrRepairBatch;
    }

    public void setIsMicrRepairBatch(Boolean isMicrRepairBatch) {
        this.isMicrRepairBatch = isMicrRepairBatch;
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

    public Integer getIqaPass() {
        return iqaPass;
    }

    public void setIqaPass(Integer iqaPass) {
        this.iqaPass = iqaPass;
    }

    public Integer getIqaFail() {
        return iqaFail;
    }

    public void setIqaFail(Integer iqaFail) {
        this.iqaFail = iqaFail;
    }

    public String getXmlFileName() {
        return xmlFileName;
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }

    public String getCbxFile() {
        return cbxFile;
    }

    public void setCbxFile(String cbxFile) {
        this.cbxFile = cbxFile;
    }

    public String getCibfFile() {
        return cibfFile;
    }

    public void setCibfFile(String cibfFile) {
        this.cibfFile = cibfFile;
    }

    public String getCxfFile() {
        return cxfFile;
    }

    public void setCxfFile(String cxfFile) {
        this.cxfFile = cxfFile;
    }

    public Boolean getMakerDone() {
        return makerDone;
    }

    public void setMakerDone(Boolean makerDone) {
        this.makerDone = makerDone;
    }

    public Boolean getCheckerDone() {
        return checkerDone;
    }

    public void setCheckerDone(Boolean checkerDone) {
        this.checkerDone = checkerDone;
    }

    public Boolean getCxfGenerated() {
        return cxfGenerated;
    }

    public void setCxfGenerated(Boolean cxfGenerated) {
        this.cxfGenerated = cxfGenerated;
    }

    public Boolean getDemSent() {
        return demSent;
    }

    public void setDemSent(Boolean demSent) {
        this.demSent = demSent;
    }

    public Boolean getSupervisorVerified() {
        return supervisorVerified;
    }

    public void setSupervisorVerified(Boolean supervisorVerified) {
        this.supervisorVerified = supervisorVerified;
    }

    public LocalDateTime getReturnWindowDeadline() {
        return returnWindowDeadline;
    }

    public void setReturnWindowDeadline(LocalDateTime returnWindowDeadline) {
        this.returnWindowDeadline = returnWindowDeadline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }
}