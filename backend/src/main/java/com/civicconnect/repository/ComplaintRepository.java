package com.civicconnect.repository;

import com.civicconnect.enums.ComplaintCategory;
import com.civicconnect.enums.ComplaintStatus;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Complaint} entities.
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    Optional<Complaint> findByReferenceNumber(String referenceNumber);

    Optional<Complaint> findByTrackingCode(String trackingCode);

    List<Complaint> findBySubmittedBy(User submittedBy);

    Page<Complaint> findBySubmittedBy(User submittedBy, Pageable pageable);

    List<Complaint> findByAssignedDepartmentId(UUID departmentId);

    Page<Complaint> findByAssignedDepartmentId(UUID departmentId, Pageable pageable);

    List<Complaint> findByStatus(ComplaintStatus status);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    List<Complaint> findByCategory(ComplaintCategory category);

    @Query("SELECT c FROM Complaint c WHERE c.submittedBy IS NULL AND c.trackingCode = :trackingCode")
    Optional<Complaint> findAnonymousByTrackingCode(@Param("trackingCode") String trackingCode);

    @Query("SELECT c FROM Complaint c WHERE c.submittedBy = :user OR (c.submittedBy IS NULL AND c.trackingCode = :trackingCode)")
    Optional<Complaint> findByUserOrTrackingCode(@Param("user") User user, @Param("trackingCode") String trackingCode);

    boolean existsByReferenceNumber(String referenceNumber);

    long countByStatus(ComplaintStatus status);

    long countBySubmittedByAndStatus(User user, ComplaintStatus status);

    @Query("SELECT c FROM Complaint c WHERE c.submittedBy = :user ORDER BY c.createdAt DESC")
    List<Complaint> findBySubmittedByOrderByCreatedAtDesc(@Param("user") User user);
}