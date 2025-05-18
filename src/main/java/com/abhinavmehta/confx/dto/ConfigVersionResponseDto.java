package com.abhinavmehta.confx.dto;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConfigVersionResponseDto {
    private Long id; // Version ID
    private Long configItemId;
    private String configItemKey; // For convenience
    private ConfigDataType configItemDataType; // For convenience
    private Long environmentId;
    private String environmentName; // For convenience
    private String value; // Default value for this version
    private boolean isActive;
    private Integer versionNumber;
    private String changeDescription;
    private List<RuleDto> rules; // Rules associated with this version
    private Long createdAt;
    private Long updatedAt;
} 