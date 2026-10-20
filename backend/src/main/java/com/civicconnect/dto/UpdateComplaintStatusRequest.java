package com.civicconnect.dto;

import com.civicconnect.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating a complaint's status.
 */
public class UpdateComplaintStatusRequest {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;

    private UUID changedById;

    public UpdateComplaintStatusRequest() {}

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public UUID getChangedById() { return changedById; }
    public void setChangedById(UUID changedById) { this.changedById = changedById; }
}