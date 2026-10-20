package com.civicconnect.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * An immutable, system-wide audit trail entry.
 *
 * <p>Every significant state-changing action in CivicConnect writes a row here.
 * Rows are never updated or deleted. This table answers questions like:
 * "Who deactivated user X and when?" or "What was the complaint status before
 * this officer changed it?"
 *
 * <p>Uses a {@code BIGSERIAL} primary key (auto-incrementing long) for fast
 * sequential append without the overhead of UUID generation or indexing for
 * this high-volume, append-only table.
 *
 * <p>The {@link #metadata} column is stored as PostgreSQL {@code JSONB},
 * allowing flexible before/after snapshots, IP addresses, or other context
 * without schema migrations.
 *
 * <p>Table: {@code audit_log}
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_entity", columnList = "entity_type, entity_id")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * The user who triggered this action.
     * Null for system-generated events (e.g. scheduled jobs or Flyway migrations).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /**
     * Machine-readable action identifier.
     * Examples: {@code COMPLAINT_CREATED}, {@code STATUS_CHANGED},
     * {@code USER_DEACTIVATED}, {@code DEPARTMENT_UPDATED}.
     */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * Type of entity affected.
     * Examples: {@code complaint}, {@code user}, {@code department}.
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** Primary key of the affected entity. */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Flexible JSON payload containing before/after state or additional context.
     * Stored as PostgreSQL {@code JSONB} for efficient querying.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ── Constructors ─────────────────────────────────────────────────────────

    protected AuditLog() {
        // Required by JPA
    }

    public AuditLog(User actor,
                    String action,
                    String entityType,
                    UUID entityId,
                    Map<String, Object> metadata) {
        this.actor      = actor;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.metadata   = metadata;
    }

    // ── Getters (no setters — audit log is immutable after creation) ─────────

    public Long getId() { return id; }
    public User getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    // ── Object methods ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
