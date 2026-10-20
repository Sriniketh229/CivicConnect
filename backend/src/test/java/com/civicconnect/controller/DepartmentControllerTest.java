package com.civicconnect.controller;

import com.civicconnect.dto.DepartmentResponse;
import com.civicconnect.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private DepartmentController departmentController;

    @Test
    void listActiveDepartments_returnsDepartmentIdsForAssignment() {
        DepartmentResponse department = new DepartmentResponse();
        UUID id = UUID.randomUUID();
        department.setId(id);
        department.setCode("ROADS");
        department.setName("Roads and Infrastructure");
        when(departmentService.getActiveDepartments()).thenReturn(List.of(department));

        ResponseEntity<List<DepartmentResponse>> response = departmentController.listActiveDepartments();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).singleElement().extracting(DepartmentResponse::getId).isEqualTo(id);
    }
}
