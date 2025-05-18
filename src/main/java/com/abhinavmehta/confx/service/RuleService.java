package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.RuleDto;
import com.abhinavmehta.confx.entity.ConfigItem;
import com.abhinavmehta.confx.entity.ConfigVersion;
import com.abhinavmehta.confx.entity.Rule;
import com.abhinavmehta.confx.repository.RuleRepository;
import com.abhinavmehta.confx.service.helpers.ConfigValueValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final ConfigValueValidator configValueValidator;

    @Transactional
    public List<Rule> setRulesForConfigVersion(ConfigVersion configVersion, List<RuleDto> ruleDtos, ConfigItem configItem) {
        // Delete existing rules for this config version first
        ruleRepository.deleteByConfigVersionId(configVersion.getId());

        if (ruleDtos == null || ruleDtos.isEmpty()) {
            return new ArrayList<>();
        }

        // Validate priorities are unique for this version
        Set<Integer> priorities = new HashSet<>();
        for (RuleDto dto : ruleDtos) {
            if (!priorities.add(dto.getPriority())) {
                throw new IllegalArgumentException("Duplicate priority found in rules: " + dto.getPriority());
            }
            // Validate valueToServe against ConfigItem's dataType
            if (!configValueValidator.isValid(dto.getValueToServe(), configItem.getDataType())) {
                throw new IllegalArgumentException(
                    String.format("Invalid valueToServe ('%s') for rule with priority %d. Expected type: %s.", 
                                  dto.getValueToServe(), dto.getPriority(), configItem.getDataType()));
            }
        }

        List<Rule> rules = ruleDtos.stream()
                .map(dto -> Rule.builder()
                        .configVersion(configVersion)
                        .priority(dto.getPriority())
                        .conditionExpression(dto.getConditionExpression())
                        .valueToServe(dto.getValueToServe())
                        .description(dto.getDescription())
                        .build())
                .collect(Collectors.toList());

        return ruleRepository.saveAll(rules);
    }

    public List<RuleDto> getRulesForConfigVersion(Long configVersionId) {
        return ruleRepository.findByConfigVersionIdOrderByPriorityAsc(configVersionId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private RuleDto mapToDto(Rule rule) {
        return RuleDto.builder()
                .id(rule.getId())
                .priority(rule.getPriority())
                .conditionExpression(rule.getConditionExpression())
                .valueToServe(rule.getValueToServe())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    // Individual rule CRUD might not be directly exposed via controller, 
    // but managed as a whole list under ConfigVersion.
    // If needed, methods like addRuleToVersion, updateRule, deleteRule can be added.
} 