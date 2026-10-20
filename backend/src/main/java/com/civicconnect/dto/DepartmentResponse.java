package com.civicconnect.dto;

import com.civicconnect.model.Department;

import java.util.UUID;

/**
 * Department data exposed for complaint assignment choices.
 */
public class DepartmentResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;

    public DepartmentResponse() {
    }

    public DepartmentResponse(Department department) {
        this.id = department.getId();
        this.name = department.getName();
        this.code = department.getCode();
        this.description = department.getDescription();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
