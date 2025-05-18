package com.abhinavmehta.confx.listeners;

import com.abhinavmehta.confx.dto.sse.ConfigUpdateSseDto;
import com.abhinavmehta.confx.events.ConfigVersionUpdatedEvent;
import com.abhinavmehta.confx.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigUpdateEventListener {

    private final SseService sseService;

    @Async // Process event asynchronously to avoid blocking the main thread
    @EventListener
    public void handleConfigVersionUpdated(ConfigVersionUpdatedEvent event) {
        log.info("Received ConfigVersionUpdatedEvent for Project: {}, Env: {}, ConfigKey: {}", 
                 event.getProjectId(), event.getEnvironmentId(), event.getUpdatedConfigVersionData().getConfigItemKey());

        ConfigUpdateSseDto sseDto = new ConfigUpdateSseDto(
            ConfigUpdateSseDto.TYPE_CONFIG_VERSION_UPDATED,
            event.getUpdatedConfigVersionData()
        );

        sseService.sendUpdateToClients(event.getProjectId(), event.getEnvironmentId(), sseDto);
    }
    
    // TODO: Add listeners for other events like ConfigItem deletion or Environment changes if needed.
} 