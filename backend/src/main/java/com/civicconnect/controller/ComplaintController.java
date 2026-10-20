package com.civicconnect.controller;

import com.civicconnect.dto.ComplaintResponse;
import com.civicconnect.dto.CreateComplaintRequest;
import com.civicconnect.dto.UpdateComplaintStatusRequest;
import com.civicconnect.enums.ComplaintStatus;
import com.civicconnect.service.ComplaintService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for complaint management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST   /api/v1/complaints}           — Create a new complaint</li>
 *   <li>{@code GET    /api/v1/complaints}           — List complaints (paginated)</li>
 *   <li>{@code GET    /api/v1/complaints/{id}       — Get complaint by ID (owner only)</li>
 *   <li>{@code GET    /api/v1/complaints/ref/{ref}  — Public tracking by reference number</li>
 *   <li>{@code GET    /api/v1/complaints/track/{code} — Anonymous tracking by tracking code</li>
 *   <li>{@code PATCH  /api/v1/complaints/{id}/status — Update complaint status</li>
 *   <li>{@code GET    /api/v1/complaints/{id}/history — Get complaint status history</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/complaints")
@Validated
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * Creates a new complaint.
     * Can be submitted by a registered user (with userId) or anonymously.
     */
    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(@Valid @RequestBody CreateComplaintRequest request) {
        ComplaintResponse response = complaintService.createComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all complaints (paginated).
     * Query params: page, size, sort (e.g., ?page=0&size=20&sort=createdAt,desc)
     */
    @GetMapping
    public ResponseEntity<Page<ComplaintResponse>> listComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) UUID departmentId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ComplaintResponse> page;
        if (status != null) {
            page = complaintService.getComplaintsByStatus(status, pageable);
        } else if (departmentId != null) {
            page = complaintService.getComplaintsByDepartment(departmentId, pageable);
        } else {
            page = complaintService.getAllComplaints(pageable);
        }
        return ResponseEntity.ok(page);
    }

    /**
     * Lists complaints submitted by the authenticated user.
     * TODO: Replace userId with authenticated principal when JWT is implemented.
     */
    @GetMapping("/my")
    public ResponseEntity<Page<ComplaintResponse>> listMyComplaints(
            @RequestParam UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ComplaintResponse> page = complaintService.getComplaintsByUser(userId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Gets a complaint by ID.
     * TODO: Add ownership check when JWT is implemented.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaint(@PathVariable UUID id) {
        // For now, allow access by ID (will be restricted in Phase C)
        Optional<ComplaintResponse> complaint = complaintService.getByIdForUser(id, null);
        return complaint.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Public tracking by reference number (e.g., CC-2026-0001).
     * Returns limited non-sensitive information.
     */
    @GetMapping("/ref/{referenceNumber}")
    public ResponseEntity<ComplaintResponse> trackByReference(@PathVariable String referenceNumber) {
        Optional<ComplaintResponse> complaint = complaintService.getByReferenceNumber(referenceNumber);
        return complaint.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Anonymous tracking by tracking code.
     * Returns limited non-sensitive information.
     */
    @GetMapping("/track/{trackingCode}")
    public ResponseEntity<ComplaintResponse> trackByCode(@PathVariable String trackingCode) {
        Optional<ComplaintResponse> complaint = complaintService.getByTrackingCode(trackingCode);
        return complaint.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates complaint status.
     * Validates transition rules and creates status history entry.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplaintStatusRequest request) {
        try {
            ComplaintResponse response = complaintService.updateStatus(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gets status history for a complaint.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ComplaintResponse.ComplaintStatusHistoryResponse>> getHistory(@PathVariable UUID id) {
        List<ComplaintResponse.ComplaintStatusHistoryResponse> history = complaintService.getStatusHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Assigns a department to a complaint.
     */
    @PatchMapping("/{id}/department")
    public ResponseEntity<ComplaintResponse> assignDepartment(
            @PathVariable UUID id,
            @RequestParam UUID departmentId) {
        try {
            ComplaintResponse response = complaintService.assignDepartment(id, departmentId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAttachment(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.uploadAttachment(id, file));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        try {
            var metadata = complaintService.getAttachmentMetadata(id, attachmentId);
            var object = complaintService.getAttachment(id, attachmentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                    .header("Content-Disposition", "inline; filename=\"" + metadata.getFileName().replace("\"", "") + "\"")
                    .body(object.bytes());
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
