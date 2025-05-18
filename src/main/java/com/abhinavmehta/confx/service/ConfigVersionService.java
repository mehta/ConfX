package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.ConfigVersionResponseDto;
import com.abhinavmehta.confx.dto.PublishConfigRequestDto;
import com.abhinavmehta.confx.entity.ConfigItem;
import com.abhinavmehta.confx.entity.ConfigVersion;
import com.abhinavmehta.confx.entity.Environment;
import com.abhinavmehta.confx.repository.ConfigItemRepository;
import com.abhinavmehta.confx.repository.ConfigVersionRepository;
import com.abhinavmehta.confx.repository.EnvironmentRepository;
import com.abhinavmehta.confx.service.helpers.ConfigValueValidator;
import com.abhinavmehta.confx.service.RuleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
// import org.springframework.context.ApplicationEventPublisher; // For SSE/WebSocket later
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigVersionService {

    private final ConfigVersionRepository configVersionRepository;
    private final ConfigItemRepository configItemRepository;
    private final EnvironmentRepository environmentRepository;
    private final ConfigValueValidator configValueValidator;
    private final RuleService ruleService;
    // private final ApplicationEventPublisher eventPublisher; // For SSE/WebSocket later

    @Transactional
    public ConfigVersionResponseDto publishNewVersion(Long projectId, Long environmentId, Long configItemId, PublishConfigRequestDto publishDto) {
        ConfigItem configItem = configItemRepository.findByIdAndProjectId(configItemId, projectId)
                .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found with id: " + configItemId + " in project: " + projectId));

        Environment environment = environmentRepository.findByIdAndProjectId(environmentId, projectId)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + environmentId + " in project: " + projectId));

        if (!configValueValidator.isValid(publishDto.getValue(), configItem.getDataType())) {
            throw new IllegalArgumentException("Invalid default value for data type " + configItem.getDataType() +
                                             ". Provided value: '" + publishDto.getValue() + "'");
        }

        configVersionRepository.deactivateActiveVersions(configItemId, environmentId);
        Integer nextVersionNumber = configVersionRepository.findMaxVersionNumberByConfigItemAndEnvironment(configItemId, environmentId) + 1;

        ConfigVersion newVersion = ConfigVersion.builder()
                .configItem(configItem)
                .environment(environment)
                .value(publishDto.getValue())
                .isActive(true)
                .versionNumber(nextVersionNumber)
                .changeDescription(publishDto.getChangeDescription())
                .build();
        newVersion = configVersionRepository.save(newVersion); // Save version first to get its ID

        // Set rules for the new version
        if (publishDto.getRules() != null) {
            ruleService.setRulesForConfigVersion(newVersion, publishDto.getRules(), configItem);
        }
        
        // TODO: Trigger event for SSE/WebSocket update here

        return mapToDto(newVersion); // mapToDto will need to fetch rules
    }

    @Transactional(readOnly = true)
    public List<ConfigVersionResponseDto> getConfigVersionHistory(Long projectId, Long environmentId, Long configItemId) {
        if (!configItemRepository.existsById(configItemId) || !environmentRepository.existsById(environmentId)) {
            throw new EntityNotFoundException("ConfigItem or Environment not found.");
        }
        // Basic check to ensure configItem belongs to project (environment check implicitly done by service structure)
        configItemRepository.findByIdAndProjectId(configItemId, projectId).orElseThrow(() -> new EntityNotFoundException("ConfigItem not found in project"));
        environmentRepository.findByIdAndProjectId(environmentId, projectId).orElseThrow(() -> new EntityNotFoundException("Environment not found in project"));

        return configVersionRepository.findByConfigItemIdAndEnvironmentIdOrderByVersionNumberDesc(configItemId, environmentId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConfigVersionResponseDto getActiveConfigVersion(Long projectId, Long environmentId, Long configItemId) {
        // Ensure entities belong to the project
        ConfigItem configItem = configItemRepository.findByIdAndProjectId(configItemId, projectId)
            .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found with id: " + configItemId + " in project: " + projectId));
        environmentRepository.findByIdAndProjectId(environmentId, projectId)
            .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + environmentId + " in project: " + projectId));

        return configVersionRepository.findByConfigItemIdAndEnvironmentIdAndIsActiveTrue(configItemId, environmentId)
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("No active configuration found for item " + configItemId + " in environment " + environmentId));
    }
    
    @Transactional(readOnly = true)
    public ConfigVersionResponseDto getConfigVersionByNumber(Long projectId, Long environmentId, Long configItemId, Integer versionNumber) {
        ConfigItem configItem = configItemRepository.findByIdAndProjectId(configItemId, projectId)
            .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found with id: " + configItemId + " in project: " + projectId));
        Environment environment = environmentRepository.findByIdAndProjectId(environmentId, projectId)
            .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + environmentId + " in project: " + projectId));

        return configVersionRepository.findByConfigItemIdAndEnvironmentIdAndVersionNumber(configItemId, environmentId, versionNumber)
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("Version " + versionNumber + " not found for config item " + configItemId + " in environment " + environmentId));
    }

    @Transactional
    public ConfigVersionResponseDto rollbackToVersion(Long projectId, Long environmentId, Long configItemId, Long versionIdToRollbackTo) {
        ConfigVersion versionToRestore = configVersionRepository.findById(versionIdToRollbackTo)
                .orElseThrow(() -> new EntityNotFoundException("Version to rollback to (id: " + versionIdToRollbackTo + ") not found."));

        if (!versionToRestore.getConfigItem().getId().equals(configItemId) || 
            !versionToRestore.getEnvironment().getId().equals(environmentId) ||
            !versionToRestore.getConfigItem().getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Rollback version does not match the specified project, environment, or config item.");
        }

        // Fetch rules from the version being rolled back to
        List<RuleDto> rulesToRestore = ruleService.getRulesForConfigVersion(versionToRestore.getId());

        PublishConfigRequestDto publishDto = new PublishConfigRequestDto();
        publishDto.setValue(versionToRestore.getValue());
        publishDto.setChangeDescription("Rolled back to version #" + versionToRestore.getVersionNumber() + " (ID: " + versionToRestore.getId() + ")");
        publishDto.setRules(rulesToRestore);

        return publishNewVersion(projectId, environmentId, configItemId, publishDto);
    }

    private ConfigVersionResponseDto mapToDto(ConfigVersion configVersion) {
        List<RuleDto> ruleDtos = ruleService.getRulesForConfigVersion(configVersion.getId());
        return ConfigVersionResponseDto.builder()
                .id(configVersion.getId())
                .configItemId(configVersion.getConfigItem().getId())
                .configItemKey(configVersion.getConfigItem().getConfigKey())
                .configItemDataType(configVersion.getConfigItem().getDataType())
                .environmentId(configVersion.getEnvironment().getId())
                .environmentName(configVersion.getEnvironment().getName())
                .value(configVersion.getValue())
                .isActive(configVersion.isActive())
                .versionNumber(configVersion.getVersionNumber())
                .changeDescription(configVersion.getChangeDescription())
                .rules(ruleDtos) // Added rules here
                .createdAt(configVersion.getCreatedAt())
                .updatedAt(configVersion.getUpdatedAt())
                .build();
    }
} 