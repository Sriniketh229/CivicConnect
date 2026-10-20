package com.civicconnect.controller;

import com.civicconnect.dto.ComplaintResponse;
import com.civicconnect.dto.CreateComplaintRequest;
import com.civicconnect.dto.UpdateComplaintStatusRequest;
import com.civicconnect.enums.ComplaintCategory;
import com.civicconnect.enums.ComplaintStatus;
import com.civicconnect.service.ComplaintService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ComplaintController}.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintControllerTest {

    @Mock
    private ComplaintService complaintService;

    @InjectMocks
    private ComplaintController complaintController;

    @Test
    void createComplaint_returnsCreatedResponse() {
        // Given
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Test complaint");
        request.setCategory(ComplaintCategory.ROADS_AND_POTHOLES);
        request.setDescription("Test description");
        request.setLocality("Test locality");

        ComplaintResponse response = new ComplaintResponse();
        response.setId(UUID.randomUUID());
        response.setReferenceNumber("CC-2026-0001");
        response.setTitle("Test complaint");
        response.setStatus(ComplaintStatus.SUBMITTED);

        when(complaintService.createComplaint(any(CreateComplaintRequest.class))).thenReturn(response);

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.createComplaint(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getReferenceNumber()).isEqualTo("CC-2026-0001");
    }

    @Test
    void listComplaints_returnsPage() {
        // Given
        ComplaintResponse response = new ComplaintResponse();
        response.setId(UUID.randomUUID());
        response.setReferenceNumber("CC-2026-0001");
        response.setStatus(ComplaintStatus.SUBMITTED);

        Page<ComplaintResponse> page = new PageImpl<>(List.of(response));
        when(complaintService.getAllComplaints(any(Pageable.class))).thenReturn(page);

        // When
        ResponseEntity<Page<ComplaintResponse>> result = complaintController.listComplaints(null, null, Pageable.unpaged());

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
    }

    @Test
    void listComplaints_withStatusFilter_returnsFilteredPage() {
        // Given
        ComplaintResponse response = new ComplaintResponse();
        response.setId(UUID.randomUUID());
        response.setReferenceNumber("CC-2026-0001");
        response.setStatus(ComplaintStatus.IN_PROGRESS);

        Page<ComplaintResponse> page = new PageImpl<>(List.of(response));
        when(complaintService.getComplaintsByStatus(eq(ComplaintStatus.IN_PROGRESS), any(Pageable.class))).thenReturn(page);

        // When
        ResponseEntity<Page<ComplaintResponse>> result = complaintController.listComplaints(ComplaintStatus.IN_PROGRESS, null, Pageable.unpaged());

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().getContent().get(0).getStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);
    }

    @Test
    void trackByReference_found_returnsComplaint() {
        // Given
        String ref = "CC-2026-0001";
        ComplaintResponse response = new ComplaintResponse();
        response.setId(UUID.randomUUID());
        response.setReferenceNumber(ref);
        response.setStatus(ComplaintStatus.SUBMITTED);

        when(complaintService.getByReferenceNumber(ref)).thenReturn(Optional.of(response));

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.trackByReference(ref);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getReferenceNumber()).isEqualTo(ref);
    }

    @Test
    void trackByReference_notFound_returns404() {
        // Given
        when(complaintService.getByReferenceNumber("CC-2026-9999")).thenReturn(Optional.empty());

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.trackByReference("CC-2026-9999");

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void trackByCode_found_returnsComplaint() {
        // Given
        String code = "TRACK123";
        ComplaintResponse response = new ComplaintResponse();
        response.setId(UUID.randomUUID());
        response.setTrackingCode(code);
        response.setStatus(ComplaintStatus.SUBMITTED);

        when(complaintService.getByTrackingCode(code)).thenReturn(Optional.of(response));

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.trackByCode(code);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTrackingCode()).isEqualTo(code);
    }

    @Test
    void updateStatus_validTransition_returnsUpdatedComplaint() {
        // Given
        UUID id = UUID.randomUUID();
        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(ComplaintStatus.UNDER_REVIEW);
        request.setNote("Under review");

        ComplaintResponse response = new ComplaintResponse();
        response.setId(id);
        response.setReferenceNumber("CC-2026-0001");
        response.setStatus(ComplaintStatus.UNDER_REVIEW);
        response.setRemarks("Under review");

        when(complaintService.updateStatus(eq(id), any(UpdateComplaintStatusRequest.class))).thenReturn(response);

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.updateStatus(id, request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(ComplaintStatus.UNDER_REVIEW);
    }

    @Test
    void updateStatus_invalidTransition_returns400() {
        // Given
        UUID id = UUID.randomUUID();
        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(ComplaintStatus.SUBMITTED); // Invalid from RESOLVED

        when(complaintService.updateStatus(eq(id), any(UpdateComplaintStatusRequest.class)))
                .thenThrow(new IllegalStateException("Invalid status transition"));

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.updateStatus(id, request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void updateStatus_notFound_returns404() {
        // Given
        UUID id = UUID.randomUUID();
        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(ComplaintStatus.UNDER_REVIEW);

        when(complaintService.updateStatus(eq(id), any(UpdateComplaintStatusRequest.class)))
                .thenThrow(new NoSuchElementException("Complaint not found"));

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.updateStatus(id, request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getHistory_returnsHistoryList() {
        // Given
        UUID id = UUID.randomUUID();
        ComplaintResponse.ComplaintStatusHistoryResponse h1 = new ComplaintResponse.ComplaintStatusHistoryResponse();
        h1.setId(UUID.randomUUID());
        h1.setToStatus(ComplaintStatus.SUBMITTED);

        ComplaintResponse.ComplaintStatusHistoryResponse h2 = new ComplaintResponse.ComplaintStatusHistoryResponse();
        h2.setId(UUID.randomUUID());
        h2.setFromStatus(ComplaintStatus.SUBMITTED);
        h2.setToStatus(ComplaintStatus.UNDER_REVIEW);

        when(complaintService.getStatusHistory(id)).thenReturn(List.of(h1, h2));

        // When
        ResponseEntity<List<ComplaintResponse.ComplaintStatusHistoryResponse>> result = complaintController.getHistory(id);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(2);
    }

    @Test
    void assignDepartment_valid_returnsUpdatedComplaint() {
        // Given
        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        ComplaintResponse response = new ComplaintResponse();
        response.setId(id);
        response.setAssignedDepartment(new ComplaintResponse.DepartmentResponse());
        response.getAssignedDepartment().setCode("ROADS");

        when(complaintService.assignDepartment(id, deptId)).thenReturn(response);

        // When
        ResponseEntity<ComplaintResponse> result = complaintController.assignDepartment(id, deptId);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().getAssignedDepartment().getCode()).isEqualTo("ROADS");
    }
}