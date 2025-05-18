package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.ConfigVersionResponseDto;
import com.abhinavmehta.confx.dto.EvaluatedConfigResponseDto;
import com.abhinavmehta.confx.dto.EvaluationContext;
import com.abhinavmehta.confx.dto.RuleDto;
import com.abhinavmehta.confx.entity.ConfigDependency;
import com.abhinavmehta.confx.entity.ConfigItem;
import com.abhinavmehta.confx.entity.Rule;
import com.abhinavmehta.confx.model.enums.ConfigDataType;
import com.abhinavmehta.confx.repository.ConfigDependencyRepository;
import com.abhinavmehta.confx.repository.ConfigItemRepository;
import com.abhinavmehta.confx.repository.EnvironmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigEvaluationService {

    private final ConfigVersionService configVersionService;
    private final RuleService ruleService; 
    private final RuleEvaluationService ruleEvaluationEngine;
    private final ConfigItemRepository configItemRepository;
    private final EnvironmentRepository environmentRepository;
    private final ConfigDependencyRepository configDependencyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Public entry point
    @Transactional(readOnly = true)
    public EvaluatedConfigResponseDto evaluateConfig(Long projectId, Long environmentId, String configKey, EvaluationContext evalContext) {
        return evaluateConfigInternal(projectId, environmentId, configKey, evalContext, new HashSet<>());
    }

    // Internal recursive method with cycle detection stack
    private EvaluatedConfigResponseDto evaluateConfigInternal(
            Long projectId, Long environmentId, String configKey,
            EvaluationContext evalContext, Set<String> evaluationStack) {

        if (evaluationStack.contains(configKey)) {
            log.warn("Cyclic dependency detected during evaluation for configKey: {}. Stack: {}", configKey, evaluationStack);
            ConfigItem cyclicItem = configItemRepository.findByProjectIdAndConfigKey(projectId, configKey)
                .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found during cyclic check: " + configKey)); // Should not happen
            Object offValue = cyclicItem.getDataType() == ConfigDataType.BOOLEAN ? false : null;
            return EvaluatedConfigResponseDto.builder()
                .configKey(configKey)
                .value(offValue)
                .dataType(cyclicItem.getDataType())
                .evaluationSource("CYCLIC_DEPENDENCY_ERROR")
                .build();
        }
        evaluationStack.add(configKey);

        ConfigItem configItem = configItemRepository.findByProjectIdAndConfigKey(projectId, configKey)
            .orElseThrow(() -> {
                evaluationStack.remove(configKey);
                return new EntityNotFoundException(String.format("ConfigItem with key '%s' not found in project %d", configKey, projectId));
            });

        environmentRepository.findByIdAndProjectId(environmentId, projectId)
            .orElseThrow(() -> {
                evaluationStack.remove(configKey);
                return new EntityNotFoundException(String.format("Environment with id %d not found in project %d", environmentId, projectId));
            });

        // --- START DEPENDENCY CHECK ---        
        List<ConfigDependency> dependencies = configDependencyRepository.findByDependentConfigItemId(configItem.getId());
        if (dependencies != null && !dependencies.isEmpty()) {
            for (ConfigDependency dependency : dependencies) {
                ConfigItem prerequisiteItem = dependency.getPrerequisiteConfigItem();
                EvaluatedConfigResponseDto prerequisiteResult = this.evaluateConfigInternal(
                    projectId, environmentId, prerequisiteItem.getConfigKey(),
                    evalContext, new HashSet<>(evaluationStack) // Pass copy of stack for parallel branches
                );

                boolean prerequisiteMet = compareEvaluatedValue(
                    prerequisiteResult.getValue(),
                    dependency.getPrerequisiteExpectedValue(),
                    prerequisiteItem.getDataType()
                );

                if (!prerequisiteMet) {
                    log.info("Prerequisite not met for config '{}' (project {}): Prerequisite '{}' (expected '{}', got '{}').",
                             configKey, projectId, prerequisiteItem.getConfigKey(), 
                             dependency.getPrerequisiteExpectedValue(), prerequisiteResult.getValue());
                    
                    Object offValue = configItem.getDataType() == ConfigDataType.BOOLEAN ? false : null;
                    ConfigVersionResponseDto activeVersionForInfo = safeGetActiveVersionInfo(projectId, environmentId, configItem.getId());

                    evaluationStack.remove(configKey);
                    return EvaluatedConfigResponseDto.builder()
                        .configKey(configKey)
                        .value(offValue)
                        .dataType(configItem.getDataType())
                        .versionId(activeVersionForInfo != null ? activeVersionForInfo.getId() : null)
                        .versionNumber(activeVersionForInfo != null ? activeVersionForInfo.getVersionNumber() : null)
                        .evaluationSource("PREREQUISITE_NOT_MET")
                        .build();
                }
            }
        }
        // --- END DEPENDENCY CHECK ---

        ConfigVersionResponseDto activeVersionDto = configVersionService.getActiveConfigVersion(projectId, environmentId, configItem.getId());
        List<RuleDto> ruleDtos = activeVersionDto.getRules();
        List<Rule> rules = ruleDtos.stream().map(dto -> Rule.builder()
                                                    .id(dto.getId())
                                                    .conditionExpression(dto.getConditionExpression())
                                                    .valueToServe(dto.getValueToServe())
                                                    .priority(dto.getPriority())
                                                    .description(dto.getDescription())
                                                    .build())
                                     .collect(Collectors.toList());

        String resolvedValueString = null;
        Long matchedRuleId = null; // TODO: Enhance RuleEvaluationService to return matched rule ID
        String evaluationSource = "DEFAULT_VALUE";

        String ruleMatchedValue = ruleEvaluationEngine.evaluateRules(rules, evalContext);

        if (ruleMatchedValue != null) {
            resolvedValueString = ruleMatchedValue;
            evaluationSource = "RULE_MATCH";
        } else {
            resolvedValueString = activeVersionDto.getValue();
        }

        Object typedValue = convertValueToDataType(resolvedValueString, configItem.getDataType());
        evaluationStack.remove(configKey);

        return EvaluatedConfigResponseDto.builder()
                .configKey(configKey)
                .value(typedValue)
                .dataType(configItem.getDataType())
                .versionId(activeVersionDto.getId())
                .versionNumber(activeVersionDto.getVersionNumber())
                .matchedRuleId(matchedRuleId)
                .evaluationSource(evaluationSource)
                .build();
    }
    
    // Helper to safely get version info, e.g., if a config is off due to prerequisite, we might still want to log its defined version.
    private ConfigVersionResponseDto safeGetActiveVersionInfo(Long projectId, Long environmentId, Long configItemId) {
        try {
            return configVersionService.getActiveConfigVersion(projectId, environmentId, configItemId);
        } catch (EntityNotFoundException e) {
            log.warn("Could not retrieve active version info for {}/{}/{} during prerequisite failure logging: {}", projectId, environmentId, configItemId, e.getMessage());
            return null;
        }
    }

    private boolean compareEvaluatedValue(Object actualEvaluatedValue, String expectedValueString, ConfigDataType prerequisiteDataType) {
        if (expectedValueString == null) { // If expected is null, actual must also be null.
             return actualEvaluatedValue == null;
        }
        if (actualEvaluatedValue == null) { // If actual is null but expected is not, they don't match.
            return false;
        }

        try {
            switch (prerequisiteDataType) {
                case BOOLEAN:
                    return ((Boolean) actualEvaluatedValue).equals(Boolean.parseBoolean(expectedValueString));
                case INTEGER:
                    return ((Number) actualEvaluatedValue).intValue() == Integer.parseInt(expectedValueString);
                case DOUBLE:
                    // Using Number.doubleValue() for flexibility if actualEvaluatedValue is Integer but expected is Double string e.g. 10 vs "10.0"
                    return ((Number) actualEvaluatedValue).doubleValue() == Double.parseDouble(expectedValueString);
                case STRING:
                    return actualEvaluatedValue.toString().equals(expectedValueString);
                case JSON:
                    JsonNode actualJson = (actualEvaluatedValue instanceof JsonNode) ? (JsonNode) actualEvaluatedValue : objectMapper.valueToTree(actualEvaluatedValue);
                    JsonNode expectedJson = objectMapper.readTree(expectedValueString);
                    return actualJson.equals(expectedJson);
                default:
                    log.warn("Unsupported data type for prerequisite comparison: {}", prerequisiteDataType);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error comparing prerequisite value: actual='{}' (type: {}), expectedString='{}', prerequisiteType='{}': {}",
                      actualEvaluatedValue, actualEvaluatedValue.getClass().getName(), expectedValueString, prerequisiteDataType, e.getMessage());
            return false;
        }
    }

    private Object convertValueToDataType(String stringValue, ConfigDataType dataType) {
        if (stringValue == null) {
             // For BOOLEAN, LaunchDarkly evaluates a null value from rules/default as false for a boolean flag.
             // For other types, null usually means null.
            if (dataType == ConfigDataType.BOOLEAN) return false;
            return null;
        }
        try {
            switch (dataType) {
                case BOOLEAN: return Boolean.parseBoolean(stringValue);
                case INTEGER: return Integer.parseInt(stringValue);
                case DOUBLE:  return Double.parseDouble(stringValue);
                case STRING:  return stringValue;
                case JSON:
                    return objectMapper.readTree(stringValue);
                default:
                    log.warn("Unsupported data type for conversion: {}", dataType);
                    return stringValue;
            }
        } catch (NumberFormatException e) {
            log.error("Failed to convert value '{}' to type {}: {}", stringValue, dataType, e.getMessage());
            throw new IllegalArgumentException("Invalid value format for data type " + dataType + ": " + stringValue);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON value '{}': {}", stringValue, e.getMessage());
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
        }
    }
} 