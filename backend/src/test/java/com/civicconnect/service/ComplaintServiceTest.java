package com.civicconnect.service;

import com.civicconnect.dto.CreateComplaintRequest;
import com.civicconnect.dto.ComplaintResponse;
import com.civicconnect.dto.UpdateComplaintStatusRequest;
import com.civicconnect.enums.ComplaintCategory;
import com.civicconnect.enums.ComplaintStatus;
import com.civicconnect.enums.UserRole;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.ComplaintStatusHistory;
import com.civicconnect.model.Department;
import com.civicconnect.model.User;
import com.civicconnect.repository.ComplaintRepository;
import com.civicconnect.repository.ComplaintStatusHistoryRepository;
import com.civicconnect.repository.ComplaintAttachmentRepository;
import com.civicconnect.repository.DepartmentRepository;
import com.civicconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ComplaintService}.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintStatusHistoryRepository statusHistoryRepository;

    @Mock
    private ComplaintAttachmentRepository attachmentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private User testUser;
    private Department testDepartment;
    private UUID userId;
    private UUID departmentId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        departmentId = UUID.randomUUID();

        testUser = new User("test@example.com", "hashedPassword", "Test User");
        setId(testUser, userId);
        testUser.setRole(UserRole.CITIZEN);

        testDepartment = new Department("Roads Department", "ROADS", "Handles road issues");
        setId(testDepartment, departmentId);
        testDepartment.setActive(true);
    }

    private void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    void createComplaint_withRegisteredUser_returnsComplaintResponse() {
        // Given
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Pothole on Main Street");
        request.setCategory(ComplaintCategory.ROADS_AND_POTHOLES);
        request.setDescription("Large pothole causing traffic issues");
        request.setLocality("Main Street, Ward 5");
        request.setLandmark("Near City Mall");
        request.setSubmittedById(userId);

        List<ComplaintStatusHistory> savedHistories = new ArrayList<>();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(departmentRepository.findByCode("ROADS")).thenReturn(Optional.of(testDepartment));
        when(complaintRepository.count()).thenReturn(0L);
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            try {
                setId(c, UUID.randomUUID());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return c;
        });
        when(statusHistoryRepository.save(any(ComplaintStatusHistory.class))).thenAnswer(inv -> {
            ComplaintStatusHistory h = inv.getArgument(0);
            savedHistories.add(h);
            return h;
        });
        when(statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(any())).thenReturn(savedHistories);
        when(attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(any())).thenReturn(Collections.emptyList());

        // When
        ComplaintResponse response = complaintService.createComplaint(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Pothole on Main Street");
        assertThat(response.getCategory()).isEqualTo(ComplaintCategory.ROADS_AND_POTHOLES);
        assertThat(response.getStatus()).isEqualTo(ComplaintStatus.SUBMITTED);
        assertThat(response.getReferenceNumber()).startsWith("CC-");
        assertThat(response.getTrackingCode()).isNotNull();
        assertThat(response.getAssignedDepartment()).isNotNull();
        assertThat(response.getAssignedDepartment().getCode()).isEqualTo("ROADS");
        assertThat(response.getStatusHistory()).hasSize(1);
        assertThat(response.getStatusHistory().get(0).getToStatus()).isEqualTo(ComplaintStatus.SUBMITTED);

        verify(complaintRepository).save(any());
        verify(statusHistoryRepository).save(any());
    }

    @Test
    void createComplaint_anonymous_returnsComplaintResponse() {
        // Given
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Broken streetlight");
        request.setCategory(ComplaintCategory.STREETLIGHT_ISSUES);
        request.setDescription("Streetlight not working for 3 days");
        request.setLocality("Oak Avenue");
        request.setSubmitterContact("anonymous@example.com");

        Department elecDept = new Department("Electrical Department", "ELEC", "Handles electrical issues");
        try {
            setId(elecDept, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        elecDept.setActive(true);

        when(departmentRepository.findByCode("ELEC")).thenReturn(Optional.of(elecDept));
        when(complaintRepository.count()).thenReturn(0L);
        when(complaintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(any())).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(any())).thenReturn(Collections.emptyList());

        // When
        ComplaintResponse response = complaintService.createComplaint(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Broken streetlight");
        assertThat(response.getCategory()).isEqualTo(ComplaintCategory.STREETLIGHT_ISSUES);
        assertThat(response.getStatus()).isEqualTo(ComplaintStatus.SUBMITTED);
        assertThat(response.getSubmitterContact()).isEqualTo("anonymous@example.com");
        assertThat(response.getAssignedDepartment().getCode()).isEqualTo("ELEC");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void createComplaint_invalidUser_throwsException() {
        // Given
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Test");
        request.setCategory(ComplaintCategory.GARBAGE_COLLECTION);
        request.setDescription("Test description");
        request.setLocality("Test locality");
        request.setSubmittedById(UUID.randomUUID());

        when(userRepository.findById(any())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> complaintService.createComplaint(request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateStatus_validTransition_succeeds() throws Exception {
        // Given
        UUID complaintId = UUID.randomUUID();
        User officer = new User("officer@example.com", "hash", "Officer");
        setId(officer, UUID.randomUUID());
        officer.setRole(UserRole.OFFICER);

        Complaint complaint = new Complaint(
                "CC-2026-0001", "TRACK123", "Test", ComplaintCategory.ROADS_AND_POTHOLES,
                "Description", "Locality");
        setId(complaint, complaintId);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setSubmittedBy(testUser);

        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(officer.getId())).thenReturn(Optional.of(officer));
        when(complaintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(any())).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(any())).thenReturn(Collections.emptyList());

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(ComplaintStatus.UNDER_REVIEW);
        request.setNote("Under review by officer");
        request.setChangedById(officer.getId());

        // When
        ComplaintResponse response = complaintService.updateStatus(complaintId, request);

        // Then
        assertThat(response.getStatus()).isEqualTo(ComplaintStatus.UNDER_REVIEW);
        assertThat(response.getRemarks()).isEqualTo("Under review by officer");
        verify(statusHistoryRepository).save(argThat(h ->
                h.getFromStatus() == ComplaintStatus.SUBMITTED &&
                h.getToStatus() == ComplaintStatus.UNDER_REVIEW &&
                h.getNote().equals("Under review by officer")
        ));
    }

    @Test
    void updateStatus_invalidTransition_throwsException() throws Exception {
        // Given
        UUID complaintId = UUID.randomUUID();
        User user = new User("user@example.com", "hash", "User");
        setId(user, UUID.randomUUID());
        user.setRole(UserRole.CITIZEN);

        Complaint complaint = new Complaint(
                "CC-2026-0001", "TRACK123", "Test", ComplaintCategory.ROADS_AND_POTHOLES,
                "Description", "Locality");
        setId(complaint, complaintId);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setSubmittedBy(testUser);

        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(ComplaintStatus.SUBMITTED); // Invalid: RESOLVED -> SUBMITTED
        request.setChangedById(user.getId());

        // When/Then
        assertThatThrownBy(() -> complaintService.updateStatus(complaintId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatus_complaintNotFound_throwsException() {
        // Given
        UUID complaintId = UUID.randomUUID();
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.empty());

        UpdateComplaintStatusRequest request = new UpdateComplaintStatusRequest();
        request.setStatus(ComplaintStatus.UNDER_REVIEW);
        request.setChangedById(UUID.randomUUID());

        // When/Then
        assertThatThrownBy(() -> complaintService.updateStatus(complaintId, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Complaint not found");
    }

    @Test
    void assignDepartment_validDepartment_succeeds() throws Exception {
        // Given
        UUID complaintId = UUID.randomUUID();
        Complaint complaint = new Complaint(
                "CC-2026-0001", "TRACK123", "Test", ComplaintCategory.ROADS_AND_POTHOLES,
                "Description", "Locality");
        setId(complaint, complaintId);
        complaint.setStatus(ComplaintStatus.SUBMITTED);

        Department newDept = new Department("New Dept", "NEW", "New");
        setId(newDept, UUID.randomUUID());
        newDept.setActive(true);

        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(departmentRepository.findById(newDept.getId())).thenReturn(Optional.of(newDept));
        when(complaintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(any())).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(any())).thenReturn(Collections.emptyList());

        // When
        ComplaintResponse response = complaintService.assignDepartment(complaintId, newDept.getId());

        // Then
        assertThat(response.getAssignedDepartment()).isNotNull();
        assertThat(response.getAssignedDepartment().getCode()).isEqualTo("NEW");
        verify(complaintRepository).save(any());
    }

    @Test
    void assignDepartment_inactiveDepartment_throwsException() throws Exception {
        // Given
        UUID complaintId = UUID.randomUUID();
        Complaint complaint = new Complaint(
                "CC-2026-0001", "TRACK123", "Test", ComplaintCategory.ROADS_AND_POTHOLES,
                "Description", "Locality");
        setId(complaint, complaintId);

        Department inactiveDept = new Department("Inactive", "INACT", "Inactive");
        setId(inactiveDept, UUID.randomUUID());
        inactiveDept.setActive(false);

        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(departmentRepository.findById(inactiveDept.getId())).thenReturn(Optional.of(inactiveDept));

        // When/Then
        assertThatThrownBy(() -> complaintService.assignDepartment(complaintId, inactiveDept.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive department");
    }

    @Test
    void getStatusHistory_returnsHistory() {
        // Given
        UUID complaintId = UUID.randomUUID();
        ComplaintStatusHistory history1 = new ComplaintStatusHistory(
                null, null, ComplaintStatus.SUBMITTED, testUser, "Submitted");
        ComplaintStatusHistory history2 = new ComplaintStatusHistory(
                null, ComplaintStatus.SUBMITTED, ComplaintStatus.UNDER_REVIEW, testUser, "Under review");

        when(statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(complaintId))
                .thenReturn(List.of(history1, history2));

        // When
        List<ComplaintResponse.ComplaintStatusHistoryResponse> history = complaintService.getStatusHistory(complaintId);

        // Then
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getToStatus()).isEqualTo(ComplaintStatus.SUBMITTED);
        assertThat(history.get(1).getFromStatus()).isEqualTo(ComplaintStatus.SUBMITTED);
        assertThat(history.get(1).getToStatus()).isEqualTo(ComplaintStatus.UNDER_REVIEW);
    }

    @Test
    void getByReferenceNumber_returnsComplaint() throws Exception {
        // Given
        String ref = "CC-2026-0001";
        Complaint complaint = new Complaint(
                ref, "TRACK123", "Test", ComplaintCategory.ROADS_AND_POTHOLES,
                "Description", "Locality");
        setId(complaint, UUID.randomUUID());
        complaint.setStatus(ComplaintStatus.SUBMITTED);

        when(complaintRepository.findByReferenceNumber(ref)).thenReturn(Optional.of(complaint));
        when(statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(any())).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(any())).thenReturn(Collections.emptyList());

        // When
        Optional<ComplaintResponse> response = complaintService.getByReferenceNumber(ref);

        // Then
        assertThat(response).isPresent();
        assertThat(response.get().getReferenceNumber()).isEqualTo(ref);
    }

    @Test
    void getByTrackingCode_returnsAnonymousComplaint() throws Exception {
        // Given
        String code = "TRACK123";
        Complaint complaint = new Complaint(
                "CC-2026-0001", code, "Test", ComplaintCategory.ROADS_AND_POTHOLES,
                "Description", "Locality");
        setId(complaint, UUID.randomUUID());
        complaint.setSubmittedBy(null); // Anonymous
        complaint.setStatus(ComplaintStatus.SUBMITTED);

        when(complaintRepository.findAnonymousByTrackingCode(code)).thenReturn(Optional.of(complaint));
        when(statusHistoryRepository.findByComplaintIdOrderByChangedAtAsc(any())).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(any())).thenReturn(Collections.emptyList());

        // When
        Optional<ComplaintResponse> response = complaintService.getByTrackingCode(code);

        // Then
        assertThat(response).isPresent();
        assertThat(response.get().getTrackingCode()).isEqualTo(code);
    }
}