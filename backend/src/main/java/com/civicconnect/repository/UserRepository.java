package com.civicconnect.repository;

import com.civicconnect.model.User;
import com.civicconnect.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByDepartmentIdAndRoleIn(UUID departmentId, List<UserRole> roles);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.role IN :roles")
    List<User> findActiveStaffByRoles(@Param("roles") List<UserRole> roles);

    Optional<User> findByIdAndActiveTrue(UUID id);
}
