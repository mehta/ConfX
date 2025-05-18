package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.sse.ConfigUpdateSseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
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
    private static final long HEARTBEAT_INTERVAL_MS = 25 * 1000L; // 25 seconds

    public SseEmitter createEmitter(Long projectId, Long environmentId) {
        SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
        String emitterKey = getEmitterKey(projectId, environmentId);

        emitter.onCompletion(() -> {
            log.info("SseEmitter completed for {}/{}", projectId, environmentId);
            removeEmitter(projectId, environmentId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("SseEmitter timed out for {}/{}", projectId, environmentId);
            removeEmitter(projectId, environmentId, emitter);
        });
        emitter.onError(e -> {
            log.error("SseEmitter error for {}/{}: {}", projectId, environmentId, e.getMessage());
            removeEmitter(projectId, environmentId, emitter);
        });

        projectEnvEmitters.computeIfAbsent(emitterKey, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SseEmitter created and registered for Project: {}, Environment: {}. Current active for key: {}", 
                 projectId, environmentId, projectEnvEmitters.get(emitterKey).size());
        
        try {
            emitter.send(SseEmitter.event().name("connection_established").data("SSE connection established for " + emitterKey).id(String.valueOf(System.currentTimeMillis())));
            emitter.send(SseEmitter.comment("ping"));
        } catch (IOException e) {
            log.warn("Failed to send initial messages for emitter {}: {}. Removing.", emitterKey, e.getMessage());
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
                        .name(ssePayload.getType())
                        .id(String.valueOf(System.currentTimeMillis()));
                emitter.send(event);
                log.debug("Successfully sent SSE event to an emitter for key {}", emitterKey);
            } catch (IOException e) {
                log.warn("Failed to send SSE event to an emitter for key {}: {}. Marking for removal.", emitterKey, e.getMessage());
                emittersToRemove.add(emitter); 
            }
        }
        
        emittersToRemove.forEach(e -> {
            List<SseEmitter> currentEmitters = projectEnvEmitters.get(emitterKey);
            if(currentEmitters != null) currentEmitters.remove(e);
        });
    }

    private String getEmitterKey(Long projectId, Long environmentId) {
        return projectId + ":" + environmentId;
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeats() {
        if (projectEnvEmitters.isEmpty()) {
            return;
        }
        log.trace("Sending heartbeats to SSE clients. Number of emitter keys: {}", projectEnvEmitters.size());
        projectEnvEmitters.forEach((key, emitters) -> {
            if (emitters.isEmpty()) {
                return;
            }
            List<SseEmitter> emittersToRemoveOnHeartbeat = new ArrayList<>();
            log.trace("Sending heartbeat to {} emitters for key: {}", emitters.size(), key);
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.comment("ping"));
                } catch (IOException e) {
                    log.warn("Failed to send heartbeat to emitter for key {}: {}. Marking for removal.", key, e.getMessage());
                    emittersToRemoveOnHeartbeat.add(emitter);
                }
            }
            emittersToRemoveOnHeartbeat.forEach(e -> {
                 emitters.remove(e);
                 String[] parts = key.split(":");
                 if (parts.length == 2) {
                     try {
                         Long pId = Long.parseLong(parts[0]);
                         Long eId = Long.parseLong(parts[1]);
                         removeEmitter(pId, eId, e);
                     } catch (NumberFormatException nfe) {
                         log.error("Could not parse project/environment ID from emitter key: {}", key, nfe);
                     }
                 }
            });
        });
    }
} 