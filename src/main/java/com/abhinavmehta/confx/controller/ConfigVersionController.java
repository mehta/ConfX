package com.abhinavmehta.confx.controller;

import com.abhinavmehta.confx.dto.ConfigVersionResponseDto;
import com.abhinavmehta.confx.dto.PublishConfigRequestDto;
import com.abhinavmehta.confx.service.ConfigVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/environments/{environmentId}/configs/{configItemId}/versions")
@RequiredArgsConstructor
public class ConfigVersionController {

    private final ConfigVersionService configVersionService;

    @PostMapping
    public ResponseEntity<ConfigVersionResponseDto> publishNewVersion(
            @PathVariable Long projectId,
            @PathVariable Long environmentId,
            @PathVariable Long configItemId,
            @Valid @RequestBody PublishConfigRequestDto publishDto) {
        ConfigVersionResponseDto newVersion = configVersionService.publishNewVersion(projectId, environmentId, configItemId, publishDto);
        return new ResponseEntity<>(newVersion, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ConfigVersionResponseDto>> getConfigVersionHistory(
            @PathVariable Long projectId,
            @PathVariable Long environmentId,
            @PathVariable Long configItemId) {
        List<ConfigVersionResponseDto> history = configVersionService.getConfigVersionHistory(projectId, environmentId, configItemId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/active")
    public ResponseEntity<ConfigVersionResponseDto> getActiveConfigVersion(
            @PathVariable Long projectId,
            @PathVariable Long environmentId,
            @PathVariable Long configItemId) {
        ConfigVersionResponseDto activeVersion = configVersionService.getActiveConfigVersion(projectId, environmentId, configItemId);
        return ResponseEntity.ok(activeVersion);
    }
    
    @GetMapping("/number/{versionNumber}")
    public ResponseEntity<ConfigVersionResponseDto> getConfigVersionByNumber(
            @PathVariable Long projectId,
            @PathVariable Long environmentId,
            @PathVariable Long configItemId,
            @PathVariable Integer versionNumber) {
        ConfigVersionResponseDto version = configVersionService.getConfigVersionByNumber(projectId, environmentId, configItemId, versionNumber);
        return ResponseEntity.ok(version);
    }

    @PostMapping("/{versionIdToRollbackTo}/rollback")
    public ResponseEntity<ConfigVersionResponseDto> rollbackToVersion(
            @PathVariable Long projectId,
            @PathVariable Long environmentId,
            @PathVariable Long configItemId,
            @PathVariable Long versionIdToRollbackTo) {
        ConfigVersionResponseDto rolledBackVersion = configVersionService.rollbackToVersion(projectId, environmentId, configItemId, versionIdToRollbackTo);
        return ResponseEntity.ok(rolledBackVersion);
    }
} 