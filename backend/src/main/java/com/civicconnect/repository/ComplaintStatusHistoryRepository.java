package com.civicconnect.repository;

import com.civicconnect.enums.ComplaintStatus;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.ComplaintStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ComplaintStatusHistory} entities.
 */
@Repository
public interface ComplaintStatusHistoryRepository extends JpaRepository<ComplaintStatusHistory, UUID> {

    List<ComplaintStatusHistory> findByComplaintIdOrderByChangedAtAsc(UUID complaintId);

    @Query("SELECT csh FROM ComplaintStatusHistory csh WHERE csh.complaint.id = :complaintId ORDER BY csh.changedAt ASC")
    List<ComplaintStatusHistory> findByComplaintId(@Param("complaintId") UUID complaintId);

    List<ComplaintStatusHistory> findByComplaintIdAndToStatus(UUID complaintId, ComplaintStatus status);

    List<ComplaintStatusHistory> findByChangedByIdOrderByChangedAtDesc(UUID changedById);

    @Query("SELECT COUNT(csh) FROM ComplaintStatusHistory csh WHERE csh.complaint.id = :complaintId AND csh.toStatus = :status")
    long countByComplaintIdAndToStatus(@Param("complaintId") UUID complaintId, @Param("status") ComplaintStatus status);
}
