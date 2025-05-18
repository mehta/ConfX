package com.abhinavmehta.confx.dto;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigItemResponseDto {
    private Long id;
    private Long projectId;
    private String configKey;
    private ConfigDataType dataType;
    private String description;
    private String notes;
    private Long createdAt;
    private Long updatedAt;
} 