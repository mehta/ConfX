package com.abhinavmehta.confx.dto;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConfigItemRequestDto {
    @NotBlank(message = "Config key cannot be blank")
    @Size(min = 1, max = 255, message = "Config key must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Config key can only contain alphanumeric characters, underscores, dots, and hyphens")
    private String configKey;

    @NotNull(message = "Data type cannot be null")
    private ConfigDataType dataType;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
} 