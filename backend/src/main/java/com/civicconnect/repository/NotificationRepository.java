package com.civicconnect.repository;

import com.civicconnect.model.Notification;
import com.civicconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link Notification} entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.read = false")
    List<Notification> findUnreadByUserId(@Param("userId") UUID userId);

    List<Notification> findByComplaintIdOrderByCreatedAtDesc(UUID complaintId);

    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.id IN :ids")
    int markAsRead(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId")
    int markAllAsRead(@Param("userId") UUID userId);
}
