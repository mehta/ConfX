package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.ConfigDependencyRequestDto;
import com.abhinavmehta.confx.dto.ConfigDependencyResponseDto;
import com.abhinavmehta.confx.entity.ConfigDependency;
import com.abhinavmehta.confx.entity.ConfigItem;
import com.abhinavmehta.confx.repository.ConfigDependencyRepository;
import com.abhinavmehta.confx.repository.ConfigItemRepository;
import com.abhinavmehta.confx.service.helpers.ConfigValueValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigDependencyService {

    private final ConfigDependencyRepository dependencyRepository;
    private final ConfigItemRepository configItemRepository;
    private final ConfigValueValidator configValueValidator; // To validate prerequisiteExpectedValue against prerequisite's data type

    @Transactional
    public ConfigDependencyResponseDto addDependency(Long projectId, Long dependentConfigItemId, ConfigDependencyRequestDto requestDto) {
        ConfigItem dependentItem = configItemRepository.findByIdAndProjectId(dependentConfigItemId, projectId)
            .orElseThrow(() -> new EntityNotFoundException("Dependent ConfigItem not found with id: " + dependentConfigItemId + " in project: " + projectId));

        ConfigItem prerequisiteItem = configItemRepository.findByIdAndProjectId(requestDto.getPrerequisiteConfigItemId(), projectId)
            .orElseThrow(() -> new EntityNotFoundException("Prerequisite ConfigItem not found with id: " + requestDto.getPrerequisiteConfigItemId() + " in project: " + projectId));

        if (dependentConfigItemId.equals(requestDto.getPrerequisiteConfigItemId())) {
            throw new IllegalArgumentException("A ConfigItem cannot depend on itself.");
        }

        // Validate prerequisiteExpectedValue against the prerequisiteItem's dataType
        if (!configValueValidator.isValid(requestDto.getPrerequisiteExpectedValue(), prerequisiteItem.getDataType())) {
            throw new IllegalArgumentException(
                String.format("Invalid prerequisiteExpectedValue ('%s') for prerequisite '%s'. Expected type: %s.",
                              requestDto.getPrerequisiteExpectedValue(), prerequisiteItem.getConfigKey(), prerequisiteItem.getDataType()));
        }

        dependencyRepository.findByDependentConfigItemIdAndPrerequisiteConfigItemId(dependentConfigItemId, requestDto.getPrerequisiteConfigItemId())
            .ifPresent(d -> {
                throw new IllegalArgumentException("Dependency already exists.");
            });

        // Circular dependency check: If A -> B is being added, check if B already depends on A (directly or indirectly)
        // This means checking if `dependentConfigItemId` is a prerequisite of `prerequisiteConfigItemId`
        if (isCircularDependency(requestDto.getPrerequisiteConfigItemId(), dependentConfigItemId, new HashSet<>())) {
            throw new IllegalArgumentException("Adding this dependency would create a circular dependency.");
        }

        ConfigDependency dependency = ConfigDependency.builder()
                .dependentConfigItem(dependentItem)
                .prerequisiteConfigItem(prerequisiteItem)
                .prerequisiteExpectedValue(requestDto.getPrerequisiteExpectedValue())
                .description(requestDto.getDescription())
                .build();

        dependency = dependencyRepository.save(dependency);
        return mapToDto(dependency);
    }

    /**
     * Checks for circular dependencies using DFS.
     * Returns true if adding a dependency from potentialDependent to potentialPrerequisite would create a cycle.
     * This means checking if potentialPrerequisite already has potentialDependent as one of its prerequisites (directly or indirectly).
     */
    private boolean isCircularDependency(Long currentItemId, Long targetItemId, Set<Long> visited) {
        if (currentItem.equals(targetItemId)) {
            return true; // Found the target, cycle detected
        }
        if (visited.contains(currentItem)) {
            return false; // Already visited this path, no cycle found through here
        }
        visited.add(currentItem);

        List<ConfigDependency> prerequisites = dependencyRepository.findByDependentConfigItemId(currentItem);
        for (ConfigDependency prereq : prerequisites) {
            if (isCircularDependency(prereq.getPrerequisiteConfigItem().getId(), targetItemId, new HashSet<>(visited))) {
                return true;
            }
        }
        return false;
    }


    @Transactional(readOnly = true)
    public List<ConfigDependencyResponseDto> getDependenciesForConfigItem(Long configItemId) {
        return dependencyRepository.findByDependentConfigItemId(configItemId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConfigDependencyResponseDto> getDependentsOfConfigItem(Long configItemId) {
        return dependencyRepository.findByPrerequisiteConfigItemId(configItemId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeDependency(Long dependencyId) {
        if (!dependencyRepository.existsById(dependencyId)) {
            throw new EntityNotFoundException("Dependency not found with id: " + dependencyId);
        }
        dependencyRepository.deleteById(dependencyId);
    }
    
    // Helper to map to DTO
    private ConfigDependencyResponseDto mapToDto(ConfigDependency dependency) {
        return ConfigDependencyResponseDto.builder()
                .id(dependency.getId())
                .dependentConfigItemId(dependency.getDependentConfigItem().getId())
                .dependentConfigKey(dependency.getDependentConfigItem().getConfigKey())
                .prerequisiteConfigItemId(dependency.getPrerequisiteConfigItem().getId())
                .prerequisiteConfigKey(dependency.getPrerequisiteConfigItem().getConfigKey())
                .prerequisiteDataType(dependency.getPrerequisiteConfigItem().getDataType())
                .prerequisiteExpectedValue(dependency.getPrerequisiteExpectedValue())
                .description(dependency.getDescription())
                .createdAt(dependency.getCreatedAt())
                .updatedAt(dependency.getUpdatedAt())
                .build();
    }
} 