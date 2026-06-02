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
@Table(name = "inward_micr_repair")
public class InwardMicrRepair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many Repair Entries -> One Cheque  (two-way: InwardCheque.micrRepairs)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inward_cheque_id", nullable = false)
    private InwardCheque inwardCheque;

    // cheque_no | bank_code | city_code | branch_code | transaction_code
    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "old_value", length = 100)
    private String oldValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    // MICR_ERROR | IQA_FAIL
    @Column(name = "repair_type", nullable = false, length = 20)
    private String repairType;

    // Unidirectional — maker who did the repair
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repaired_by", nullable = false)
    private User repairedBy;

    @Column(name = "repaired_at", nullable = false)
    private LocalDateTime repairedAt;

    @PrePersist
    public void prePersist() {
        this.repairedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InwardCheque getInwardCheque() { return inwardCheque; }
    public void setInwardCheque(InwardCheque inwardCheque) { this.inwardCheque = inwardCheque; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getRepairType() { return repairType; }
    public void setRepairType(String repairType) { this.repairType = repairType; }

    public User getRepairedBy() { return repairedBy; }
    public void setRepairedBy(User repairedBy) { this.repairedBy = repairedBy; }

    public LocalDateTime getRepairedAt() { return repairedAt; }
    public void setRepairedAt(LocalDateTime repairedAt) { this.repairedAt = repairedAt; }
}
