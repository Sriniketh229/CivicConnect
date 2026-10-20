package com.civicconnect.dto;

import com.civicconnect.enums.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a new complaint.
 */
public class CreateComplaintRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotNull(message = "Category is required")
    private ComplaintCategory category;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Locality is required")
    @Size(max = 255, message = "Locality must not exceed 255 characters")
    private String locality;

    @Size(max = 255, message = "Landmark must not exceed 255 characters")
    private String landmark;

    private String submitterContact;

    private UUID submittedById;

    public CreateComplaintRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getSubmitterContact() { return submitterContact; }
    public void setSubmitterContact(String submitterContact) { this.submitterContact = submitterContact; }

    public UUID getSubmittedById() { return submittedById; }
    public void setSubmittedById(UUID submittedById) { this.submittedById = submittedById; }
}