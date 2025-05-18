package com.abhinavmehta.confx.controller;

import com.abhinavmehta.confx.dto.ConfigItemResponseDto;
import com.abhinavmehta.confx.dto.CreateConfigItemRequestDto;
import com.abhinavmehta.confx.dto.UpdateConfigItemRequestDto;
import com.abhinavmehta.confx.service.ConfigItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/configs")
@RequiredArgsConstructor
public class ConfigItemController {

    private final ConfigItemService configItemService;

    @PostMapping
    public ResponseEntity<ConfigItemResponseDto> createConfigItem(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateConfigItemRequestDto createDto) {
        ConfigItemResponseDto configItem = configItemService.createConfigItem(projectId, createDto);
        return new ResponseEntity<>(configItem, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ConfigItemResponseDto>> getConfigItemsByProjectId(@PathVariable Long projectId) {
        List<ConfigItemResponseDto> configItems = configItemService.getConfigItemsByProjectId(projectId);
        return ResponseEntity.ok(configItems);
    }

    @GetMapping("/{configItemId}")
    public ResponseEntity<ConfigItemResponseDto> getConfigItemById(
            @PathVariable Long projectId,
            @PathVariable Long configItemId) {
        ConfigItemResponseDto configItem = configItemService.getConfigItemById(projectId, configItemId);
        return ResponseEntity.ok(configItem);
    }
    
    @GetMapping("/key/{configKey}")
    public ResponseEntity<ConfigItemResponseDto> getConfigItemByKey(
            @PathVariable Long projectId,
            @PathVariable String configKey) {
        ConfigItemResponseDto configItem = configItemService.getConfigItemByKey(projectId, configKey);
        return ResponseEntity.ok(configItem);
    }

    @PutMapping("/{configItemId}")
    public ResponseEntity<ConfigItemResponseDto> updateConfigItem(
            @PathVariable Long projectId,
            @PathVariable Long configItemId,
            @Valid @RequestBody UpdateConfigItemRequestDto updateDto) {
        ConfigItemResponseDto updatedConfigItem = configItemService.updateConfigItem(projectId, configItemId, updateDto);
        return ResponseEntity.ok(updatedConfigItem);
    }

    @DeleteMapping("/{configItemId}")
    public ResponseEntity<Void> deleteConfigItem(
            @PathVariable Long projectId,
            @PathVariable Long configItemId) {
        configItemService.deleteConfigItem(projectId, configItemId);
        return ResponseEntity.noContent().build();
    }
} 