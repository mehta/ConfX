package com.abhinavmehta.confx.controller;

import com.abhinavmehta.confx.dto.CreateEnvironmentRequestDto;
import com.abhinavmehta.confx.dto.EnvironmentResponseDto;
import com.abhinavmehta.confx.service.EnvironmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/environments")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @PostMapping
    public ResponseEntity<EnvironmentResponseDto> createEnvironment(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateEnvironmentRequestDto createDto) {
        EnvironmentResponseDto environment = environmentService.createEnvironment(projectId, createDto);
        return new ResponseEntity<>(environment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EnvironmentResponseDto>> getEnvironmentsByProjectId(
            @PathVariable Long projectId) {
        List<EnvironmentResponseDto> environments = environmentService.getEnvironmentsByProjectId(projectId);
        return ResponseEntity.ok(environments);
    }

    @GetMapping("/{environmentId}")
    public ResponseEntity<EnvironmentResponseDto> getEnvironmentById(
            @PathVariable Long projectId,
            @PathVariable Long environmentId) {
        EnvironmentResponseDto environment = environmentService.getEnvironmentById(projectId, environmentId);
        return ResponseEntity.ok(environment);
    }

    @PutMapping("/{environmentId}")
    public ResponseEntity<EnvironmentResponseDto> updateEnvironment(
            @PathVariable Long projectId,
            @PathVariable Long environmentId,
            @Valid @RequestBody CreateEnvironmentRequestDto updateDto) {
        EnvironmentResponseDto updatedEnvironment = environmentService.updateEnvironment(projectId, environmentId, updateDto);
        return ResponseEntity.ok(updatedEnvironment);
    }

    @DeleteMapping("/{environmentId}")
    public ResponseEntity<Void> deleteEnvironment(
            @PathVariable Long projectId,
            @PathVariable Long environmentId) {
        environmentService.deleteEnvironment(projectId, environmentId);
        return ResponseEntity.noContent().build();
    }
} 