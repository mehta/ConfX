package com.abhinavmehta.confx.dto;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateConfigItemRequestDto {
    // Key is not updatable, data type might be, depending on rules (e.g., if no values exist yet)
    // For simplicity, let's make dataType updatable for now.
    // If it shouldn't be, this DTO would only contain description and notes.

    @NotNull(message = "Data type cannot be null")
    private ConfigDataType dataType;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
} 