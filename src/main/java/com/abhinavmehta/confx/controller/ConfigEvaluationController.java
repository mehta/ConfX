package com.abhinavmehta.confx.controller;

import com.abhinavmehta.confx.dto.EvaluatedConfigResponseDto;
import com.abhinavmehta.confx.dto.EvaluationContext;
import com.abhinavmehta.confx.service.ConfigEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluate")
@RequiredArgsConstructor
public class ConfigEvaluationController {

    private final ConfigEvaluationService configEvaluationService;

    @PostMapping("/projects/{projectId}/environments/{environmentId}/configs/{configKey}")
    public ResponseEntity<EvaluatedConfigResponseDto> evaluateConfig(
            @PathVariable Long projectId,
            @PathVariable Long environmentId,
            @PathVariable String configKey,
            @Valid @RequestBody(required = false) EvaluationContext evaluationContext) {
        
        EvaluationContext context = (evaluationContext == null) ? new EvaluationContext() : evaluationContext;
        if (context.getAttributes() == null) {
            context.setAttributes(new java.util.HashMap<>()); // Ensure attributes map is not null for SpEL
        }

        EvaluatedConfigResponseDto response = configEvaluationService.evaluateConfig(projectId, environmentId, configKey, context);
        return ResponseEntity.ok(response);
    }
} 