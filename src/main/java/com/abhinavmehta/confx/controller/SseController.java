package com.abhinavmehta.confx.controller;

import com.abhinavmehta.confx.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping(path = "/projects/{projectId}/environments/{environmentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConfigUpdates(
            @PathVariable Long projectId,
            @PathVariable Long environmentId) {
        return sseService.createEmitter(projectId, environmentId);
    }
} 