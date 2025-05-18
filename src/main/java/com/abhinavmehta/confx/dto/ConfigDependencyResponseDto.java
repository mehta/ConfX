package com.abhinavmehta.confx.dto;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigDependencyResponseDto {
    private Long id;
    private Long dependentConfigItemId;
    private String dependentConfigKey; // For convenience

    private Long prerequisiteConfigItemId;
    private String prerequisiteConfigKey; // For convenience
    private ConfigDataType prerequisiteDataType; // For convenience
    private String prerequisiteExpectedValue;
    private String description;

    private Long createdAt;
    private Long updatedAt;
} 