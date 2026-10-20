package com.civicconnect.controller;

import com.civicconnect.dto.DepartmentResponse;
import com.civicconnect.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only department endpoints used to populate assignment controls.
 */
@RestController
@RequestMapping("/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> listActiveDepartments() {
        return ResponseEntity.ok(departmentService.getActiveDepartments());
    }
}
