package com.civicconnect.service;

import com.civicconnect.dto.DepartmentResponse;
import com.civicconnect.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read operations for departments that can receive complaints.
 */
@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentResponse> getActiveDepartments() {
        return departmentRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(DepartmentResponse::new)
                .toList();
    }
}
