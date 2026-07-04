package com.example.purchasebackend.domain;

import com.example.purchasebackend.domain.enums.DeveloperUserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A portal user (developer). Authenticates with email/password (JWT). Owns {@link DeveloperApp}s.
 *
 * <p>// TODO: Add email verification.
 * <p>// TODO: Add password reset.
 * <p>// TODO: Add OAuth login later.
 */
@Entity
@Table(name = "developer_user", indexes = @Index(name = "uq_user_email", columnList = "email", unique = true))
public class DeveloperUser {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    /** PBKDF2 hash (format: iterations:saltB64:hashB64). Raw password is never stored. */
    @Column(nullable = false)
    private String passwordHash;

    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeveloperUserRole role = DeveloperUserRole.OWNER;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public DeveloperUserRole getRole() {
        return role;
    }

    public void setRole(DeveloperUserRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
