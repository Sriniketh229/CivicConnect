package com.civicconnect.repository;

import com.civicconnect.model.AuditLog;
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
 * Repository for {@link AuditLog} entities.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    List<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntity(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.actor.id = :actorId ORDER BY a.createdAt DESC")
    List<AuditLog> findByActor(@Param("actorId") UUID actorId);
}