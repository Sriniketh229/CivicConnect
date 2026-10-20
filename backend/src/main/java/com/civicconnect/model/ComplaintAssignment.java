package com.civicconnect.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks the assignment of a specific officer to a complaint.
 *
 * <p>A department head or admin assigns an officer by creating a row here.
 * When a complaint is reassigned, the previous active row is deactivated
 * ({@link #active} = false) and a new row is inserted, preserving the
 * full assignment history.
 *
 * <p>Table: {@code complaint_assignments}
 */
@Entity
@Table(name = "complaint_assignments")
public class ComplaintAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /** The officer who is assigned to work on this complaint. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;

    /** The department head or admin who created this assignment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    /**
     * False when this assignment has been superseded by a newer one.
     * Only one assignment per complaint should have {@code active = true}
     * at any given time.
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // ── Constructors ─────────────────────────────────────────────────────────

    protected ComplaintAssignment() {
        // Required by JPA
    }

    public ComplaintAssignment(Complaint complaint, User officer, User assignedBy) {
        this.complaint  = complaint;
        this.officer    = officer;
        this.assignedBy = assignedBy;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public User getOfficer() { return officer; }
    public User getAssignedBy() { return assignedBy; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public boolean isActive() { return active; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setActive(boolean active) { this.active = active; }

    // ── Object methods ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplaintAssignment that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
