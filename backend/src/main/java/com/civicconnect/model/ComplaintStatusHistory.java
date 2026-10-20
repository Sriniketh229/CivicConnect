package com.civicconnect.model;

import com.civicconnect.enums.ComplaintStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An immutable record of one status transition on a {@link Complaint}.
 *
 * <p>This table is append-only: rows are inserted when a status changes
 * and are never updated or deleted. The full ordered collection represents
 * the authoritative complaint timeline shown on the citizen tracking page.
 *
 * <p>Table: {@code complaint_status_history}
 */
@Entity
@Table(name = "complaint_status_history", indexes = {
        @Index(name = "idx_csh_complaint_id", columnList = "complaint_id")
})
public class ComplaintStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The complaint whose status changed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /**
     * Status before this transition.
     * Null only for the initial SUBMITTED entry (no prior status).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ComplaintStatus fromStatus;

    /** Status after this transition. */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ComplaintStatus toStatus;

    /**
     * The staff member who triggered this transition.
     * Null when the initial submission entry is created by the system
     * or when an anonymous citizen reopens a complaint via tracking code.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    /** Officer note displayed to the citizen on the tracking page. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Exact timestamp of the status change. Set by the service before persist. */
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();

    // ── Constructors ─────────────────────────────────────────────────────────

    protected ComplaintStatusHistory() {
        // Required by JPA
    }

    public ComplaintStatusHistory(Complaint complaint,
                                   ComplaintStatus fromStatus,
                                   ComplaintStatus toStatus,
                                   User changedBy,
                                   String note) {
        this.complaint  = complaint;
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
        this.changedBy  = changedBy;
        this.note       = note;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public ComplaintStatus getFromStatus() { return fromStatus; }
    public ComplaintStatus getToStatus() { return toStatus; }
    public User getChangedBy() { return changedBy; }
    public String getNote() { return note; }
    public OffsetDateTime getChangedAt() { return changedAt; }

    // ── Object methods ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplaintStatusHistory that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
