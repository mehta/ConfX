package com.abhinavmehta.confx.controller;

import com.abhinavmehta.confx.dto.ConfigDependencyRequestDto;
import com.abhinavmehta.confx.dto.ConfigDependencyResponseDto;
import com.abhinavmehta.confx.service.ConfigDependencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/dependencies")
@RequiredArgsConstructor
public class ConfigDependencyController {

    private final ConfigDependencyService dependencyService;

    @PostMapping("/for/{configItemId}")
    public ResponseEntity<ConfigDependencyResponseDto> addDependency(
            @PathVariable Long projectId,
            @PathVariable Long configItemId,
            @Valid @RequestBody ConfigDependencyRequestDto requestDto) {
        ConfigDependencyResponseDto response = dependencyService.addDependency(projectId, configItemId, requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/for/{configItemId}")
    public ResponseEntity<List<ConfigDependencyResponseDto>> getDependenciesForConfigItem(
            @PathVariable Long projectId,
            @PathVariable Long configItemId) {
        List<ConfigDependencyResponseDto> response = dependencyService.getDependenciesForConfigItem(configItemId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/of/{configItemId}")
    public ResponseEntity<List<ConfigDependencyResponseDto>> getDependentsOfConfigItem(
            @PathVariable Long projectId,
            @PathVariable Long configItemId) {
        List<ConfigDependencyResponseDto> response = dependencyService.getDependentsOfConfigItem(configItemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable Long projectId,
            @PathVariable Long dependencyId) {
        dependencyService.removeDependency(dependencyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<ConfigDependencyResponseDto>> getAllDependenciesForProject(@PathVariable Long projectId) {
        List<ConfigDependencyResponseDto> response = dependencyService.getAllDependenciesForProject(projectId);
        return ResponseEntity.ok(response);
    }
} 