package com.iispl.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.iispl.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "branch")
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "created_date")
    private LocalDate createdDate;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public User() {}

    // Only plain scalar fields in constructor
    public User(String username, String passwordHash, String fullName,
                String branch, Status status, LocalDate createdDate,
                LocalDateTime lastLoginAt) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.branch       = branch;
        this.status       = status;
        this.createdDate  = createdDate;
        this.lastLoginAt  = lastLoginAt;
    }

    public String        getId()                         { return id; }

    public String        getUsername()                   { return username; }
    public void          setUsername(String u)           { this.username = u; }

    public String        getPasswordHash()               { return passwordHash; }
    public void          setPasswordHash(String p)       { this.passwordHash = p; }

    public String        getFullName()                   { return fullName; }
    public void          setFullName(String n)           { this.fullName = n; }

    public String        getBranch()                     { return branch; }
    public void          setBranch(String b)             { this.branch = b; }

    public Status        getStatus()                     { return status; }
    public void          setStatus(Status s)             { this.status = s; }

    public LocalDate     getCreatedDate()                { return createdDate; }
    public void          setCreatedDate(LocalDate d)     { this.createdDate = d; }

    public LocalDateTime getLastLoginAt()                { return lastLoginAt; }
    public void          setLastLoginAt(LocalDateTime t) { this.lastLoginAt = t; }

    public Role          getRole()                       { return role; }
    public void          setRole(Role r)                 { this.role = r; }
}