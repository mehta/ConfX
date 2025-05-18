package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.ConfigVersionResponseDto;
import com.abhinavmehta.confx.dto.EvaluatedConfigResponseDto;
import com.abhinavmehta.confx.dto.EvaluationContext;
import com.abhinavmehta.confx.dto.RuleDto;
import com.abhinavmehta.confx.entity.ConfigItem;
import com.abhinavmehta.confx.entity.Rule;
import com.abhinavmehta.confx.model.enums.ConfigDataType;
import com.abhinavmehta.confx.repository.ConfigItemRepository;
import com.abhinavmehta.confx.repository.EnvironmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigEvaluationService {

    private final ConfigVersionService configVersionService;
    private final RuleService ruleService; // Already provides DTOs, but we might need entities for RuleEvaluationService
    private final RuleEvaluationService ruleEvaluationEngine; // Self-injection or separate service
    private final ConfigItemRepository configItemRepository;
    private final EnvironmentRepository environmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For JSON parsing

    @Transactional(readOnly = true)
    public EvaluatedConfigResponseDto evaluateConfig(Long projectId, Long environmentId, String configKey, EvaluationContext evalContext) {
        ConfigItem configItem = configItemRepository.findByProjectIdAndConfigKey(projectId, configKey)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("ConfigItem with key '%s' not found in project %d", configKey, projectId)));

        environmentRepository.findByIdAndProjectId(environmentId, projectId)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("Environment with id %d not found in project %d", environmentId, projectId)));

        ConfigVersionResponseDto activeVersionDto = configVersionService.getActiveConfigVersion(projectId, environmentId, configItem.getId());
        List<RuleDto> ruleDtos = activeVersionDto.getRules();
        
        // Convert RuleDtos to Rule entities if RuleEvaluationService expects entities.
        // For now, let's assume RuleEvaluationService can work with DTOs or we adapt it.
        // Or, more cleanly, RuleService could return List<Rule> for internal use.
        // Let's simulate fetching Rule entities for the RuleEvaluationService:
        List<Rule> rules = ruleDtos.stream().map(dto -> Rule.builder()
                                                    .id(dto.getId())
                                                    .conditionExpression(dto.getConditionExpression())
                                                    .valueToServe(dto.getValueToServe())
                                                    .priority(dto.getPriority())
                                                    .description(dto.getDescription())
                                                    .build())
                                     .collect(Collectors.toList());

        String resolvedValueString = null;
        Long matchedRuleId = null;
        String evaluationSource = "DEFAULT_VALUE";

        String ruleMatchedValue = ruleEvaluationEngine.evaluateRules(rules, evalContext);

        if (ruleMatchedValue != null) {
            resolvedValueString = ruleMatchedValue;
            evaluationSource = "RULE_MATCH";
            // Find which rule matched to get its ID (requires RuleEvaluationService to indicate this or re-iterate)
            // For simplicity, this detail is omitted for now but important for audit/debugging UI.
            // A more robust evaluateRules could return a wrapper object with matched rule details.
        } else {
            resolvedValueString = activeVersionDto.getValue();
        }

        Object typedValue = convertValueToDataType(resolvedValueString, configItem.getDataType());

        return EvaluatedConfigResponseDto.builder()
                .configKey(configKey)
                .value(typedValue)
                .dataType(configItem.getDataType())
                .versionId(activeVersionDto.getId())
                .versionNumber(activeVersionDto.getVersionNumber())
                .matchedRuleId(matchedRuleId) // Placeholder
                .evaluationSource(evaluationSource)
                .build();
    }

    private Object convertValueToDataType(String stringValue, ConfigDataType dataType) {
        if (stringValue == null) return null;

        try {
            switch (dataType) {
                case BOOLEAN: return Boolean.parseBoolean(stringValue);
                case INTEGER: return Integer.parseInt(stringValue);
                case DOUBLE:  return Double.parseDouble(stringValue);
                case STRING:  return stringValue;
                case JSON:
                    return objectMapper.readTree(stringValue); // Return as JsonNode
                default:
                    log.warn("Unsupported data type for conversion: {}", dataType);
                    return stringValue; // Or throw error
            }
        } catch (NumberFormatException e) {
            log.error("Failed to convert value '{}' to type {}: {}", stringValue, dataType, e.getMessage());
            throw new IllegalArgumentException("Invalid value format for data type " + dataType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON value '{}': {}", stringValue, e.getMessage());
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
        }
    }
} 