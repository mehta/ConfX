package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.CreateEnvironmentRequestDto;
import com.abhinavmehta.confx.dto.EnvironmentResponseDto;
import com.abhinavmehta.confx.entity.Environment;
import com.abhinavmehta.confx.entity.Project;
import com.abhinavmehta.confx.repository.EnvironmentRepository;
import com.abhinavmehta.confx.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ProjectRepository projectRepository; // To verify project existence

    @Transactional
    public EnvironmentResponseDto createEnvironment(Long projectId, CreateEnvironmentRequestDto createDto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

        environmentRepository.findByProjectIdAndName(projectId, createDto.getName()).ifPresent(e -> {
            throw new IllegalArgumentException("Environment with name '" + createDto.getName() + "' already exists in this project.");
        });

        Environment environment = Environment.builder()
                .project(project)
                .name(createDto.getName())
                .description(createDto.getDescription())
                .colorTag(createDto.getColorTag())
                .build();

        environment = environmentRepository.save(environment);
        return mapToDto(environment);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponseDto> getEnvironmentsByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            // This check is important to give a clear error if the project itself doesn't exist
            throw new EntityNotFoundException("Project not found with id: " + projectId);
        }
        return environmentRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDto getEnvironmentById(Long projectId, Long environmentId) {
        // Ensure project exists first
        if (!projectRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Project not found with id: " + projectId);
        }
        Environment environment = environmentRepository.findByIdAndProjectId(environmentId, projectId)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + environmentId + " for project: " + projectId));
        return mapToDto(environment);
    }

    @Transactional
    public EnvironmentResponseDto updateEnvironment(Long projectId, Long environmentId, CreateEnvironmentRequestDto updateDto) {
        // Ensure project exists
        projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

        Environment environment = environmentRepository.findById(environmentId)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + environmentId));

        if (!environment.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Environment with id " + environmentId + " does not belong to project " + projectId + ". Update operation denied.");
        }

        // Check if new name conflicts within the same project (excluding itself)
        environmentRepository.findByProjectIdAndName(projectId, updateDto.getName()).ifPresent(existingEnv -> {
            if (!existingEnv.getId().equals(environmentId)) {
                throw new IllegalArgumentException("Another environment with name '" + updateDto.getName() + "' already exists in this project.");
            }
        });

        environment.setName(updateDto.getName());
        environment.setDescription(updateDto.getDescription());
        environment.setColorTag(updateDto.getColorTag());
        environment = environmentRepository.save(environment);
        return mapToDto(environment);
    }

    @Transactional
    public void deleteEnvironment(Long projectId, Long environmentId) {
        // Ensure project exists
        projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
            
        Environment environment = environmentRepository.findById(environmentId)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + environmentId));

        if (!environment.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Environment with id " + environmentId + " does not belong to project " + projectId + ". Delete operation denied.");
        }
        
        environmentRepository.deleteById(environmentId); // ON DELETE CASCADE will handle related data in DB if any
    }

    private EnvironmentResponseDto mapToDto(Environment environment) {
        return EnvironmentResponseDto.builder()
                .id(environment.getId())
                .projectId(environment.getProject().getId())
                .name(environment.getName())
                .description(environment.getDescription())
                .colorTag(environment.getColorTag())
                .createdAt(environment.getCreatedAt())
                .updatedAt(environment.getUpdatedAt())
                .build();
    }
} 