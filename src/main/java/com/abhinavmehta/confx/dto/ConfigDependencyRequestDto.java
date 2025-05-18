package com.abhinavmehta.confx.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigDependencyRequestDto {
    @NotNull(message = "Prerequisite ConfigItem ID cannot be null")
    private Long prerequisiteConfigItemId;

    @NotNull(message = "Prerequisite expected value cannot be null")
    @Size(min = 1, max = 4000, message = "Prerequisite expected value must be between 1 and 4000 characters")
    private String prerequisiteExpectedValue;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
} 