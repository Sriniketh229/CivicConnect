package com.civicconnect.repository;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.ComplaintAttachment;
import com.civicconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ComplaintAttachment} entities.
 */
@Repository
public interface ComplaintAttachmentRepository extends JpaRepository<ComplaintAttachment, UUID> {

    List<ComplaintAttachment> findByComplaintId(UUID complaintId);

    List<ComplaintAttachment> findByComplaintIdOrderByUploadedAtDesc(UUID complaintId);

    List<ComplaintAttachment> findByUploadedById(UUID userId);

    boolean existsByComplaintIdAndStoragePath(UUID complaintId, String storagePath);
}
