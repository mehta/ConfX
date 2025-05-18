package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.ConfigItemResponseDto;
import com.abhinavmehta.confx.dto.CreateConfigItemRequestDto;
import com.abhinavmehta.confx.dto.UpdateConfigItemRequestDto;
import com.abhinavmehta.confx.entity.ConfigItem;
import com.abhinavmehta.confx.entity.Project;
import com.abhinavmehta.confx.repository.ConfigItemRepository;
import com.abhinavmehta.confx.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigItemService {

    private final ConfigItemRepository configItemRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public ConfigItemResponseDto createConfigItem(Long projectId, CreateConfigItemRequestDto createDto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

        configItemRepository.findByProjectIdAndConfigKey(projectId, createDto.getConfigKey()).ifPresent(ci -> {
            throw new IllegalArgumentException("ConfigItem with key '" + createDto.getConfigKey() + "' already exists in this project.");
        });

        ConfigItem configItem = ConfigItem.builder()
                .project(project)
                .configKey(createDto.getConfigKey())
                .dataType(createDto.getDataType())
                .description(createDto.getDescription())
                .notes(createDto.getNotes())
                .build();

        configItem = configItemRepository.save(configItem);
        return mapToDto(configItem);
    }

    @Transactional(readOnly = true)
    public List<ConfigItemResponseDto> getConfigItemsByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Project not found with id: " + projectId);
        }
        return configItemRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConfigItemResponseDto getConfigItemById(Long projectId, Long configItemId) {
        ConfigItem configItem = configItemRepository.findByIdAndProjectId(configItemId, projectId)
                .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found with id: " + configItemId + " for project: " + projectId));
        return mapToDto(configItem);
    }
    
    @Transactional(readOnly = true)
    public ConfigItemResponseDto getConfigItemByKey(Long projectId, String configKey) {
        ConfigItem configItem = configItemRepository.findByProjectIdAndConfigKey(projectId, configKey)
                .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found with key: " + configKey + " for project: " + projectId));
        return mapToDto(configItem);
    }

    @Transactional
    public ConfigItemResponseDto updateConfigItem(Long projectId, Long configItemId, UpdateConfigItemRequestDto updateDto) {
        projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

        ConfigItem configItem = configItemRepository.findById(configItemId)
                .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found with id: " + configItemId));

        if (!configItem.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("ConfigItem with id " + configItemId + " does not belong to project " + projectId);
        }

        // configKey is not updatable. If it were, unique constraint checks would be needed.
        configItem.setDataType(updateDto.getDataType());
        configItem.setDescription(updateDto.getDescription());
        configItem.setNotes(updateDto.getNotes());
        // TODO: Consider implications of changing dataType if ConfigVersions with values exist.
        // For now, allowing it. A more robust solution might restrict this or handle data conversion.

        configItem = configItemRepository.save(configItem);
        return mapToDto(configItem);
    }

    @Transactional
    public void deleteConfigItem(Long projectId, Long configItemId) {
        projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
            
        ConfigItem configItem = configItemRepository.findById(configItemId)
                .orElseThrow(() -> new EntityNotFoundException("ConfigItem not found with id: " + configItemId));

        if (!configItem.getProject().getId().equals(projectId)) {
             throw new IllegalArgumentException("ConfigItem with id " + configItemId + " does not belong to project " + projectId);
        }
        // Deleting a ConfigItem should also delete its associated ConfigVersions and Rules.
        // This will be handled by ON DELETE CASCADE in the DB for ConfigVersions.
        // Rules associated with this ConfigItem might need explicit cleanup if not cascaded.
        configItemRepository.deleteById(configItemId);
    }

    private ConfigItemResponseDto mapToDto(ConfigItem configItem) {
        return ConfigItemResponseDto.builder()
                .id(configItem.getId())
                .projectId(configItem.getProject().getId())
                .configKey(configItem.getConfigKey())
                .dataType(configItem.getDataType())
                .description(configItem.getDescription())
                .notes(configItem.getNotes())
                .createdAt(configItem.getCreatedAt())
                .updatedAt(configItem.getUpdatedAt())
                .build();
    }
} 