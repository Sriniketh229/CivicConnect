package com.civicconnect.repository;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.ComplaintAssignment;
import com.civicconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ComplaintAssignment} entities.
 */
@Repository
public interface ComplaintAssignmentRepository extends JpaRepository<ComplaintAssignment, UUID> {

    Optional<ComplaintAssignment> findByComplaintIdAndActiveTrue(UUID complaintId);

    List<ComplaintAssignment> findByComplaintIdOrderByAssignedAtDesc(UUID complaintId);

    List<ComplaintAssignment> findByOfficerIdAndActiveTrue(UUID officerId);

    @Query("SELECT ca FROM ComplaintAssignment ca WHERE ca.complaint.id = :complaintId AND ca.active = true")
    Optional<ComplaintAssignment> findActiveByComplaintId(@Param("complaintId") UUID complaintId);

    @Query("SELECT ca FROM ComplaintAssignment ca WHERE ca.officer.id = :officerId AND ca.active = true")
    List<ComplaintAssignment> findActiveByOfficerId(@Param("officerId") UUID officerId);

    @Query("SELECT ca FROM ComplaintAssignment ca WHERE ca.assignedBy.id = :assignedById ORDER BY ca.assignedAt DESC")
    List<ComplaintAssignment> findByAssignedByOrderByAssignedAtDesc(@Param("assignedById") UUID assignedById);

    boolean existsByComplaintIdAndOfficerIdAndActiveTrue(UUID complaintId, UUID officerId);
}
