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
@RequestMapping("/api/v1/projects/{projectId}/configs/{configItemId}/dependencies")
@RequiredArgsConstructor
public class ConfigDependencyController {

    private final ConfigDependencyService dependencyService;

    @PostMapping
    public ResponseEntity<ConfigDependencyResponseDto> addDependency(
            @PathVariable Long projectId,
            @PathVariable Long configItemId,
            @Valid @RequestBody ConfigDependencyRequestDto requestDto) {
        ConfigDependencyResponseDto response = dependencyService.addDependency(projectId, configItemId, requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ConfigDependencyResponseDto>> getDependenciesForConfigItem(@PathVariable Long configItemId) {
        // projectId is in the path but not strictly needed if configItemId is globally unique for dependencies
        // However, service layer does use projectId for fetching ConfigItems, so it's good for context.
        List<ConfigDependencyResponseDto> response = dependencyService.getDependenciesForConfigItem(configItemId);
        return ResponseEntity.ok(response);
    }

    // Endpoint to get configs that depend ON this configItemId
    @GetMapping("/dependents")
    public ResponseEntity<List<ConfigDependencyResponseDto>> getDependentsOfConfigItem(@PathVariable Long configItemId) {
        List<ConfigDependencyResponseDto> response = dependencyService.getDependentsOfConfigItem(configItemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<Void> removeDependency(@PathVariable Long dependencyId) {
        // projectId and configItemId from path can be used for auth/scoping checks if needed before deleting by direct dependencyId
        dependencyService.removeDependency(dependencyId);
        return ResponseEntity.noContent().build();
    }
} 