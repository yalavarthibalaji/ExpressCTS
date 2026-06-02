package com.iispl.entity.inward;

import java.time.LocalDateTime;

import com.iispl.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "inward_checker_actions")
public class InwardCheckerAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many Actions -> One Cheque  (two-way: InwardCheque.checkerActions)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inward_cheque_id", nullable = false)
    private InwardCheque inwardCheque;

    // Many Actions -> One Batch  (two-way: InwardBatch.checkerActions)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inward_batch_id", nullable = false)
    private InwardBatch inwardBatch;

    // Unidirectional — checker who took the action
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checker_id", nullable = false)
    private User checker;

    // ACCEPTED | RETURNED | SEND_BACK
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "reason_code", length = 5)
    private String reasonCode;

    @Column(name = "reason_text", length = 255)
    private String reasonText;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "actioned_at", nullable = false)
    private LocalDateTime actionedAt;

    @PrePersist
    public void prePersist() {
        this.actionedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InwardCheque getInwardCheque() { return inwardCheque; }
    public void setInwardCheque(InwardCheque inwardCheque) { this.inwardCheque = inwardCheque; }

    public InwardBatch getInwardBatch() { return inwardBatch; }
    public void setInwardBatch(InwardBatch inwardBatch) { this.inwardBatch = inwardBatch; }

    public User getChecker() { return checker; }
    public void setChecker(User checker) { this.checker = checker; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getActionedAt() { return actionedAt; }
    public void setActionedAt(LocalDateTime actionedAt) { this.actionedAt = actionedAt; }
}
