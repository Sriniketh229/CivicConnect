package com.civicconnect.model;

import com.civicconnect.enums.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A registered user of CivicConnect (citizen, officer, department head, or admin).
 *
 * <p>All user types share this single table. The {@link UserRole} discriminates
 * between them at the application layer. Spring Security uses this entity as
 * the principal via a {@code UserDetails} adapter (implemented in Phase 2).
 *
 * <p>Anonymous complainants are not represented by a {@code User} record. Their
 * optional contact information is stored directly on the {@link Complaint} entity.
 *
 * <p>Table: {@code users}
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Login identifier. Must be unique across all users. */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt-hashed password.
     * Null only for users provisioned via Supabase Auth (Option B) —
     * not applicable under the chosen Option A (Spring JWT) approach.
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** Display name shown in officer dashboards and notifications. */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** Optional contact number for SMS notifications (future phase). */
    @Column(name = "phone", length = 20)
    private String phone;

    /** Role governing what this user can do in the system. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role = UserRole.CITIZEN;

    /**
     * Department that this user belongs to.
     * Null for CITIZEN and ADMIN roles.
     * Required for OFFICER and DEPARTMENT_HEAD roles.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Soft-delete flag. Deactivated users cannot log in or submit complaints. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    protected User() {
        // Required by JPA
    }

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public Department getDepartment() { return department; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(UserRole role) { this.role = role; }
    public void setDepartment(Department department) { this.department = department; }
    public void setActive(boolean active) { this.active = active; }

    // ── Convenience ──────────────────────────────────────────────────────────

    /** Returns true if this user has officer-level or higher privileges. */
    public boolean isStaff() {
        return role == UserRole.OFFICER
                || role == UserRole.DEPARTMENT_HEAD
                || role == UserRole.ADMIN;
    }

    // ── Object methods ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', role=" + role + "}";
    }
}
