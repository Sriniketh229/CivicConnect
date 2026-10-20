package com.civicconnect.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A municipal department to which complaints can be assigned.
 *
 * <p>Departments are seeded via Flyway migration V2 and managed only by
 * ADMIN users. Officers and department heads are linked to exactly one
 * department via {@link User#getDepartment()}.
 *
 * <p>Table: {@code departments}
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Human-readable department name shown to citizens and officers. */
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    /** Short uppercase code used in logs and reference generation. */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Soft-delete flag. Inactive departments cannot receive new assignments. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    protected Department() {
        // Required by JPA
    }

    public Department(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active) { this.active = active; }

    // ── Object methods ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Department{id=" + id + ", code='" + code + "', name='" + name + "'}";
    }
}
