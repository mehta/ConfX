package com.abhinavmehta.confx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEnvironmentRequestDto {
    @NotBlank(message = "Environment name cannot be blank")
    @Size(max = 255, message = "Environment name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Environment description cannot exceed 1000 characters")
    private String description;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color tag must be a valid hex color code, e.g., #RRGGBB or #RGB")
    private String colorTag;
} 