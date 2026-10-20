package com.civicconnect.service;

import com.civicconnect.dto.CreateComplaintRequest;
import com.civicconnect.dto.ComplaintResponse;
import com.civicconnect.dto.UpdateComplaintStatusRequest;
import com.civicconnect.enums.ComplaintStatus;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.ComplaintAttachment;
import com.civicconnect.model.ComplaintStatusHistory;
import com.civicconnect.model.Department;
import com.civicconnect.model.User;
import com.civicconnect.repository.ComplaintRepository;
import com.civicconnect.repository.ComplaintAttachmentRepository;
import com.civicconnect.repository.ComplaintStatusHistoryRepository;
import com.civicconnect.repository.DepartmentRepository;
import com.civicconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for complaint management.
 */
@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintStatusHistoryRepository statusHistoryRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageService storageService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintStatusHistoryRepository statusHistoryRepository,
                            ComplaintAttachmentRepository attachmentRepository,
                            DepartmentRepository departmentRepository,
                            UserRepository userRepository,
                            SupabaseStorageService storageService) {
        this.complaintRepository = complaintRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.attachmentRepository = attachmentRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Creates a new complaint.
     */
    public ComplaintResponse createComplaint(CreateComplaintRequest request) {
        String referenceNumber = generateReferenceNumber();
        String trackingCode = UUID.randomUUID().toString().replace("-", "").toUpperCase();

        User submittedBy = null;
        if (request.getSubmittedById() != null) {
            submittedBy = userRepository.findById(request.getSubmittedById())
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + request.getSubmittedById()));
        }

        Department assignedDepartment = null;
        if (request.getCategory() != null) {
            // Auto-assign department based on category
            assignedDepartment = assignDepartmentByCategory(request.getCategory());
        }

        Complaint complaint = new Complaint(
                referenceNumber,
                trackingCode,
                request.getTitle(),
                request.getCategory(),
                request.getDescription(),
                request.getLocality()
        );

        if (request.getLandmark() != null) {
            complaint.setLandmark(request.getLandmark());
        }
        complaint.setSubmitterContact(request.getSubmitterContact());
        complaint.setSubmittedBy(submittedBy);
        complaint.setAssignedDepartment(assignedDepartment);

        complaint = complaintRepository.save(complaint);

        // Create initial status history entry
        ComplaintStatusHistory initialHistory = new ComplaintStatusHistory(
                complaint,
                null,
                ComplaintStatus.SUBMITTED,
                submittedBy,
                "Complaint submitted"
        );
        statusHistoryRepository.save(initialHistory);

        return toResponse(complaint);
    }

    /**
     * Gets a complaint by reference number (public tracking).
     */
    @Transactional(readOnly = true)
    public Optional<ComplaintResponse> getByReferenceNumber(String referenceNumber) {
        return complaintRepository.findByReferenceNumber(referenceNumber)
                .map(this::toResponse);
    }

    /**
     * Gets a complaint by tracking code (anonymous tracking).
     */
    @Transactional(readOnly = true)
    public Optional<ComplaintResponse> getByTrackingCode(String trackingCode) {
        return complaintRepository.findAnonymousByTrackingCode(trackingCode)
                .map(this::toResponse);
    }

    /**
     * Gets a complaint by ID with ownership check.
     */
    @Transactional(readOnly = true)
    public Optional<ComplaintResponse> getByIdForUser(UUID complaintId, UUID userId) {
        return complaintRepository.findById(complaintId)
                .filter(c -> c.getSubmittedBy() != null && c.getSubmittedBy().getId().equals(userId))
                .map(this::toResponse);
    }

    /**
     * Lists complaints submitted by a user.
     */
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getComplaintsByUser(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        return complaintRepository.findBySubmittedBy(user, pageable)
                .map(this::toResponse);
    }

    /**
     * Lists all complaints (admin/staff view).
     */
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getAllComplaints(Pageable pageable) {
        return complaintRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Lists complaints by status.
     */
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status, Pageable pageable) {
        return complaintRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    /**
     * Lists complaints assigned to a department.
     */
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getComplaintsByDepartment(UUID departmentId, Pageable pageable) {
        return complaintRepository.findByAssignedDepartmentId(departmentId, pageable).map(this::toResponse);
    }

    /**
     * Updates complaint status with validation.
     */
    public ComplaintResponse updateStatus(UUID complaintId, UpdateComplaintStatusRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));

        ComplaintStatus currentStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s -> %s", currentStatus, newStatus));
        }

        User changedBy = null;
        if (request.getChangedById() != null) {
            changedBy = userRepository.findById(request.getChangedById())
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + request.getChangedById()));
        }

        complaint.setStatus(newStatus);
        if (request.getNote() != null) {
            complaint.setRemarks(request.getNote());
        }

        complaint = complaintRepository.save(complaint);

        // Create status history entry
        ComplaintStatusHistory history = new ComplaintStatusHistory(
                complaint,
                currentStatus,
                newStatus,
                changedBy,
                request.getNote()
        );
        statusHistoryRepository.save(history);

        return toResponse(complaint);
    }

    /**
     * Assigns a department to a complaint.
     */
    public ComplaintResponse assignDepartment(UUID complaintId, UUID departmentId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NoSuchElementException("Department not found: " + departmentId));

        if (!department.isActive()) {
            throw new IllegalStateException("Cannot assign to inactive department: " + departmentId);
        }

        complaint.setAssignedDepartment(department);
        complaint = complaintRepository.save(complaint);

        return toResponse(complaint);
    }

    /**
     * Gets status history for a complaint.
     */
    @Transactional(readOnly = true)
    public List<ComplaintResponse.ComplaintStatusHistoryResponse> getStatusHistory(UUID complaintId) {
        return statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(complaintId)
                .stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    /** Uploads one validated image and persists only its storage metadata. */
    public ComplaintResponse.ComplaintAttachmentResponse uploadAttachment(UUID complaintId, MultipartFile file) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an image to upload.");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("Each image must be 5 MB or smaller.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("The selected image could not be read.", e);
        }
        String mimeType = detectImageType(bytes);
        String extension = switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> "webp";
        };
        String fileName = safeFileName(file.getOriginalFilename(), extension);
        String storagePath = "complaints/" + complaint.getId() + "/" + UUID.randomUUID() + "." + extension;
        storageService.upload(storagePath, bytes, mimeType);
        try {
            ComplaintAttachment attachment = attachmentRepository.save(
                    new ComplaintAttachment(complaint, storagePath, fileName, mimeType, (long) bytes.length, null));
            return toAttachmentResponse(attachment);
        } catch (RuntimeException e) {
            storageService.deleteQuietly(storagePath);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public SupabaseStorageService.StoredObject getAttachment(UUID complaintId, UUID attachmentId) {
        ComplaintAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(item -> item.getComplaint().getId().equals(complaintId))
                .orElseThrow(() -> new NoSuchElementException("Attachment not found."));
        return storageService.download(attachment.getStoragePath());
    }

    @Transactional(readOnly = true)
    public ComplaintAttachment getAttachmentMetadata(UUID complaintId, UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .filter(item -> item.getComplaint().getId().equals(complaintId))
                .orElseThrow(() -> new NoSuchElementException("Attachment not found."));
    }

    private String generateReferenceNumber() {
        int year = OffsetDateTime.now().getYear();
        long count = complaintRepository.count() + 1;
        return String.format("CC-%d-%04d", year, count);
    }

    private Department assignDepartmentByCategory(com.civicconnect.enums.ComplaintCategory category) {
        String deptCode = switch (category) {
            case GARBAGE_COLLECTION -> "SWM";
            case ROADS_AND_POTHOLES -> "ROADS";
            case DRAINAGE_PROBLEMS -> "DRAIN";
            case STREETLIGHT_ISSUES -> "ELEC";
            case WATER_SUPPLY -> "WATER";
        };
        return departmentRepository.findByCode(deptCode).orElse(null);
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        ComplaintResponse response = new ComplaintResponse();
        response.setId(complaint.getId());
        response.setReferenceNumber(complaint.getReferenceNumber());
        response.setTrackingCode(complaint.getTrackingCode());
        response.setTitle(complaint.getTitle());
        response.setCategory(complaint.getCategory());
        response.setDescription(complaint.getDescription());
        response.setLocality(complaint.getLocality());
        response.setLandmark(complaint.getLandmark());
        response.setStatus(complaint.getStatus());
        response.setSubmitterContact(complaint.getSubmitterContact());
        response.setRemarks(complaint.getRemarks());
        response.setCreatedAt(complaint.getCreatedAt());
        response.setUpdatedAt(complaint.getUpdatedAt());

        if (complaint.getAssignedDepartment() != null) {
            response.setAssignedDepartment(new ComplaintResponse.DepartmentResponse(complaint.getAssignedDepartment()));
        }

        // Load status history
        List<ComplaintResponse.ComplaintStatusHistoryResponse> history = statusHistoryRepository
                .findByComplaintIdOrderByChangedAtAsc(complaint.getId())
                .stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
        response.setStatusHistory(history);

        // Load attachments
        List<ComplaintResponse.ComplaintAttachmentResponse> attachments = attachmentRepository
                .findByComplaintIdOrderByUploadedAtDesc(complaint.getId())
                .stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());
        response.setAttachments(attachments);

        return response;
    }

    private ComplaintResponse.ComplaintStatusHistoryResponse toHistoryResponse(ComplaintStatusHistory history) {
        ComplaintResponse.ComplaintStatusHistoryResponse resp = new ComplaintResponse.ComplaintStatusHistoryResponse();
        resp.setId(history.getId());
        resp.setFromStatus(history.getFromStatus());
        resp.setToStatus(history.getToStatus());
        resp.setNote(history.getNote());
        resp.setChangedAt(history.getChangedAt());
        if (history.getChangedBy() != null) {
            resp.setChangedBy(new ComplaintResponse.UserResponse(history.getChangedBy()));
        }
        return resp;
    }

    private ComplaintResponse.ComplaintAttachmentResponse toAttachmentResponse(ComplaintAttachment attachment) {
        ComplaintResponse.ComplaintAttachmentResponse resp = new ComplaintResponse.ComplaintAttachmentResponse();
        resp.setId(attachment.getId());
        resp.setFileName(attachment.getFileName());
        resp.setMimeType(attachment.getMimeType());
        resp.setSizeBytes(attachment.getSizeBytes());
        resp.setUploadedAt(attachment.getUploadedAt());
        resp.setUrl("/api/v1/complaints/" + attachment.getComplaint().getId() + "/attachments/" + attachment.getId());
        return resp;
    }

    private String detectImageType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        throw new IllegalArgumentException("Only valid JPEG, PNG, or WebP images can be uploaded.");
    }

    private String safeFileName(String originalName, String extension) {
        String rawName = originalName == null ? "photo" : originalName.replaceAll(".*[\\\\/]", "");
        String baseName = rawName.replaceFirst("\\.[^.]*$", "").replaceAll("[^A-Za-z0-9._-]", "_");
        if (baseName.isBlank()) baseName = "photo";
        return baseName.substring(0, Math.min(baseName.length(), 240)) + "." + extension;
    }
}
