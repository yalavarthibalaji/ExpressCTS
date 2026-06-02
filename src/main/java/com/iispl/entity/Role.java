package com.iispl.entity;

import java.util.ArrayList;
import java.util.List;

import com.iispl.enums.Status;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "can_create_batch")
    private boolean canCreateBatch;

    @Column(name = "can_approve_outward")
    private boolean canApproveOutward;

    @Column(name = "can_approve_inward")
    private boolean canApproveInward;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<User> users = new ArrayList<>();

    public Role() {
    }

    public Role(String id, String roleName, String description, boolean canCreateBatch, boolean canApproveOutward,
            boolean canApproveInward, Status status) {
        this.id = id;
        this.roleName = roleName;
        this.description = description;
        this.canCreateBatch = canCreateBatch;
        this.canApproveOutward = canApproveOutward;
        this.canApproveInward = canApproveInward;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String n) {
        this.roleName = n;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public boolean isCanCreateBatch() {
        return canCreateBatch;
    }

    public void setCanCreateBatch(boolean b) {
        this.canCreateBatch = b;
    }

    public boolean isCanApproveOutward() {
        return canApproveOutward;
    }

    public void setCanApproveOutward(boolean b) {
        this.canApproveOutward = b;
    }

    public boolean isCanApproveInward() {
        return canApproveInward;
    }

    public void setCanApproveInward(boolean b) {
        this.canApproveInward = b;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status s) {
        this.status = s;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> u) {
        this.users = u;
    }
}

