package com.abhinavmehta.confx.dto.sse;

import com.abhinavmehta.confx.dto.ConfigVersionResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigUpdateSseDto {
    private String type; // e.g., "CONFIG_VERSION_UPDATED", "CONFIG_ITEM_DELETED"
    private Object payload; // Can be ConfigVersionResponseDto or just a key/ID

    public static final String TYPE_CONFIG_VERSION_UPDATED = "CONFIG_VERSION_UPDATED";
    public static final String TYPE_CONFIG_ITEM_DELETED = "CONFIG_ITEM_DELETED";
    public static final String TYPE_ENVIRONMENT_DELETED = "ENVIRONMENT_DELETED";
    public static final String TYPE_PROJECT_DELETED = "PROJECT_DELETED";
    // Add other types as needed
} 