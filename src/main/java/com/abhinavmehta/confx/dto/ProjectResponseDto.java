package com.abhinavmehta.confx.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectResponseDto {
    private Long id;
    private String name;
    private String description;
    private Long createdAt;
    private Long updatedAt;
} 