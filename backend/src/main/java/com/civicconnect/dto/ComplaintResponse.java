package com.civicconnect.dto;

import com.civicconnect.enums.ComplaintCategory;
import com.civicconnect.enums.ComplaintStatus;
import com.civicconnect.model.Department;
import com.civicconnect.model.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for complaint data.
 */
public class ComplaintResponse {

    private UUID id;
    private String referenceNumber;
    private String trackingCode;
    private String title;
    private ComplaintCategory category;
    private String description;
    private String locality;
    private String landmark;
    private ComplaintStatus status;
    private String submitterContact;
    private DepartmentResponse assignedDepartment;
    private String remarks;
    private List<ComplaintStatusHistoryResponse> statusHistory;
    private List<ComplaintAttachmentResponse> attachments;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ComplaintResponse() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getSubmitterContact() { return submitterContact; }
    public void setSubmitterContact(String submitterContact) { this.submitterContact = submitterContact; }

    public DepartmentResponse getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(DepartmentResponse assignedDepartment) { this.assignedDepartment = assignedDepartment; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public List<ComplaintStatusHistoryResponse> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<ComplaintStatusHistoryResponse> statusHistory) { this.statusHistory = statusHistory; }

    public List<ComplaintAttachmentResponse> getAttachments() { return attachments; }
    public void setAttachments(List<ComplaintAttachmentResponse> attachments) { this.attachments = attachments; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class DepartmentResponse {
        private UUID id;
        private String name;
        private String code;

        public DepartmentResponse() {}
        public DepartmentResponse(Department dept) {
            this.id = dept.getId();
            this.name = dept.getName();
            this.code = dept.getCode();
        }
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class ComplaintStatusHistoryResponse {
        private UUID id;
        private ComplaintStatus fromStatus;
        private ComplaintStatus toStatus;
        private String note;
        private OffsetDateTime changedAt;
        private UserResponse changedBy;

        public ComplaintStatusHistoryResponse() {}
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public ComplaintStatus getFromStatus() { return fromStatus; }
        public void setFromStatus(ComplaintStatus fromStatus) { this.fromStatus = fromStatus; }
        public ComplaintStatus getToStatus() { return toStatus; }
        public void setToStatus(ComplaintStatus toStatus) { this.toStatus = toStatus; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public OffsetDateTime getChangedAt() { return changedAt; }
        public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
        public UserResponse getChangedBy() { return changedBy; }
        public void setChangedBy(UserResponse changedBy) { this.changedBy = changedBy; }
    }

    public static class ComplaintAttachmentResponse {
        private UUID id;
        private String fileName;
        private String mimeType;
        private Long sizeBytes;
        private OffsetDateTime uploadedAt;
        private String url;

        public ComplaintAttachmentResponse() {}
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public Long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
        public OffsetDateTime getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class UserResponse {
        private UUID id;
        private String fullName;
        private String email;

        public UserResponse() {}
        public UserResponse(User user) {
            this.id = user.getId();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
        }
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
