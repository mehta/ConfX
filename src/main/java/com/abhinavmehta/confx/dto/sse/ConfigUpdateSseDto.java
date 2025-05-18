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
    // Add other types as needed, e.g., for config item deletion
} 