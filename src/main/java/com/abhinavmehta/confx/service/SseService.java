package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.sse.ConfigUpdateSseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {
    // Key: "projectId:environmentId"
    private final Map<String, List<SseEmitter>> projectEnvEmitters = new ConcurrentHashMap<>();
    private static final Long SSE_EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    public SseEmitter createEmitter(Long projectId, Long environmentId) {
        SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
        String emitterKey = getEmitterKey(projectId, environmentId);

        emitter.onCompletion(() -> {
            log.info("SseEmitter completed for {}/{}", projectId, environmentId);
            removeEmitter(projectId, environmentId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("SseEmitter timed out for {}/{}", projectId, environmentId);
            emitter.complete(); // Triggers onCompletion
        });
        emitter.onError(e -> {
            log.error("SseEmitter error for {}/{}: {}", projectId, environmentId, e.getMessage());
            emitter.completeWithError(e); // Triggers onCompletion
        });

        projectEnvEmitters.computeIfAbsent(emitterKey, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SseEmitter created and registered for Project: {}, Environment: {}. Current active for key: {}", 
                 projectId, environmentId, projectEnvEmitters.get(emitterKey).size());
        
        // Send a confirmation event to the client
        try {
            emitter.send(SseEmitter.event().name("connection_established").data("SSE connection established for " + emitterKey));
        } catch (IOException e) {
            log.warn("Failed to send connection confirmation for emitter {}: {}", emitterKey, e.getMessage());
            removeEmitter(projectId, environmentId, emitter);
        }
        return emitter;
    }

    public void removeEmitter(Long projectId, Long environmentId, SseEmitter emitter) {
        String emitterKey = getEmitterKey(projectId, environmentId);
        List<SseEmitter> emitters = projectEnvEmitters.get(emitterKey);
        if (emitters != null) {
            boolean removed = emitters.remove(emitter);
            if (removed) {
                log.info("SseEmitter removed for Project: {}, Environment: {}. Remaining for key: {}", 
                         projectId, environmentId, emitters.size());
            }
            if (emitters.isEmpty()) {
                projectEnvEmitters.remove(emitterKey);
                log.info("No more emitters for key {}, removing from map.", emitterKey);
            }
        }
    }

    public void sendUpdateToClients(Long projectId, Long environmentId, ConfigUpdateSseDto ssePayload) {
        String emitterKey = getEmitterKey(projectId, environmentId);
        List<SseEmitter> emitters = projectEnvEmitters.get(emitterKey);

        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE clients for Project: {}, Environment: {} to send update.", projectId, environmentId);
            return;
        }

        log.info("Sending SSE update to {} clients for Project: {}, Environment: {}. Payload type: {}", 
                 emitters.size(), projectId, environmentId, ssePayload.getType());

        List<SseEmitter> emittersToRemove = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .data(ssePayload, MediaType.APPLICATION_JSON)
                        .name(ssePayload.getType()) // Event name for client-side handling
                        .id(String.valueOf(System.currentTimeMillis())); // Unique event ID
                emitter.send(event);
                log.debug("Successfully sent SSE event to an emitter for key {}", emitterKey);
            } catch (IOException e) {
                log.warn("Failed to send SSE event to an emitter for key {}: {}. Marking for removal.", emitterKey, e.getMessage());
                emittersToRemove.add(emitter); 
            }
        }
        // Clean up failed emitters
        if (!emittersToRemove.isEmpty()) {
            emittersToRemove.forEach(emitter -> removeEmitter(projectId, environmentId, emitter));
        }
    }

    private String getEmitterKey(Long projectId, Long environmentId) {
        return projectId + ":" + environmentId;
    }
} 