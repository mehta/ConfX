package com.abhinavmehta.confx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequestDto {
    @NotBlank(message = "Project name cannot be blank")
    @Size(max = 255, message = "Project name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Project description cannot exceed 1000 characters") // Assuming description can be longer
    private String description;
} 