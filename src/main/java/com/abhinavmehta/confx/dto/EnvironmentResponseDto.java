package com.abhinavmehta.confx.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvironmentResponseDto {
    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String colorTag;
    private Long createdAt;
    private Long updatedAt;
} 