package com.abhinavmehta.confx.controller;

import com.abhinavmehta.confx.dto.ConfigVersionResponseDto;
import com.abhinavmehta.confx.service.ConfigVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/environments/{environmentId}/all-active-configs")
@RequiredArgsConstructor
public class EnvironmentConfigController {

    private final ConfigVersionService configVersionService;

    @GetMapping
    public ResponseEntity<List<ConfigVersionResponseDto>> getAllActiveConfigsForEnvironment(
            @PathVariable Long projectId,
            @PathVariable Long environmentId) {
        List<ConfigVersionResponseDto> allActiveConfigs = configVersionService.getAllActiveConfigsForEnvironment(projectId, environmentId);
        return ResponseEntity.ok(allActiveConfigs);
    }
} 