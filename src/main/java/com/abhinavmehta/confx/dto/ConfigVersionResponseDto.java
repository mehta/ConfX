package com.abhinavmehta.confx.dto;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigVersionResponseDto {
    private Long id; // Version ID
    private Long configItemId;
    private String configItemKey; // For convenience
    private ConfigDataType configItemDataType; // For convenience
    private Long environmentId;
    private String environmentName; // For convenience
    private String value;
    private boolean isActive;
    private Integer versionNumber;
    private String changeDescription;
    private Long createdAt;
    private Long updatedAt;
} 