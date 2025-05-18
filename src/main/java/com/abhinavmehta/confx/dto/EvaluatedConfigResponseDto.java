package com.abhinavmehta.confx.dto;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluatedConfigResponseDto {
    private String configKey;
    private Object value; // The actual typed value
    private ConfigDataType dataType;
    private Long versionId; // ID of the ConfigVersion that was evaluated
    private Integer versionNumber;
    private Long matchedRuleId; // ID of the rule that matched, if any
    private String evaluationSource; // e.g., "DEFAULT_VALUE" or "RULE_MATCH"
} 