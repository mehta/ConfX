package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.CreateProjectRequestDto;
import com.abhinavmehta.confx.dto.ProjectResponseDto;
import com.abhinavmehta.confx.entity.Project;
import com.abhinavmehta.confx.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponseDto createProject(CreateProjectRequestDto createProjectRequestDto) {
        projectRepository.findByName(createProjectRequestDto.getName()).ifPresent(p -> {
            throw new IllegalArgumentException("Project with name '" + createProjectRequestDto.getName() + "' already exists.");
        });

        Project project = Project.builder()
                .name(createProjectRequestDto.getName())
                .description(createProjectRequestDto.getDescription())
                .build();
        // createdAt and updatedAt will be set by BaseEntity/DB
        project = projectRepository.save(project);
        return mapToDto(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
        return mapToDto(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto getProjectByName(String name) {
        Project project = projectRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with name: " + name));
        return mapToDto(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponseDto updateProject(Long projectId, CreateProjectRequestDto projectDetails) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

        // Check if new name conflicts with an existing project (excluding itself)
        projectRepository.findByName(projectDetails.getName()).ifPresent(existingProject -> {
            if (!existingProject.getId().equals(projectId)) {
                throw new IllegalArgumentException("Another project with name '" + projectDetails.getName() + "' already exists.");
            }
        });

        project.setName(projectDetails.getName());
        project.setDescription(projectDetails.getDescription());
        // updatedAt will be updated by BaseEntity/DB trigger
        project = projectRepository.save(project);
        return mapToDto(project);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Project not found with id: " + projectId);
        }
        // Consider implications: what happens to environments, configs under this project?
        // For now, simple delete. Cascade or logical delete might be needed later.
        projectRepository.deleteById(projectId);
    }

    private ProjectResponseDto mapToDto(Project project) {
        return ProjectResponseDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
} 