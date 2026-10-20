package com.civicconnect.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Metadata record for a file (photo or document) uploaded alongside a complaint.
 *
 * <p>The actual binary is stored in a private Supabase Storage bucket.
 * This entity stores only the path within that bucket, the original filename,
 * MIME type, and file size. The backend generates short-lived signed URLs
 * on demand via the Supabase Storage REST API.
 *
 * <p>This design means:
 * <ul>
 *   <li>The database never stores binary blobs.</li>
 *   <li>The frontend receives a time-limited signed URL (e.g. 15 minutes)
 *       instead of a permanent public URL, preventing unauthorised access.</li>
 *   <li>Deleting or rotating credentials does not orphan database rows.</li>
 * </ul>
 *
 * <p>Table: {@code complaint_attachments}
 */
@Entity
@Table(name = "complaint_attachments")
public class ComplaintAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /**
     * Path of the file within the Supabase Storage bucket.
     * Example: {@code complaints/CC-2026-0001/photo-uuid.jpg}
     */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    /** Original filename as submitted by the user. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** MIME type (e.g. {@code image/jpeg}, {@code image/png}). */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** File size in bytes. Used for display and validation. */
    @Column(name = "size_bytes")
    private Long sizeBytes;

    /**
     * The user who uploaded this file.
     * Null if uploaded by an anonymous complainant
     * (tracked only by complaint ownership).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    // ── Constructors ─────────────────────────────────────────────────────────

    protected ComplaintAttachment() {
        // Required by JPA
    }

    public ComplaintAttachment(Complaint complaint,
                                String storagePath,
                                String fileName,
                                String mimeType,
                                Long sizeBytes,
                                User uploadedBy) {
        this.complaint   = complaint;
        this.storagePath = storagePath;
        this.fileName    = fileName;
        this.mimeType    = mimeType;
        this.sizeBytes   = sizeBytes;
        this.uploadedBy  = uploadedBy;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public String getStoragePath() { return storagePath; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public User getUploadedBy() { return uploadedBy; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }

    // ── Object methods ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplaintAttachment that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
