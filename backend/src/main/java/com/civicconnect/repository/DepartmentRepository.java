package com.civicconnect.repository;

import com.civicconnect.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Department} entities.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByName(String name);

    Optional<Department> findByCode(String code);

    List<Department> findByActiveTrue();

    List<Department> findByActiveTrueOrderByNameAsc();

    boolean existsByCode(String code);
}
