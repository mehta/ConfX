package com.abhinavmehta.confx.listeners;

import com.abhinavmehta.confx.dto.sse.ConfigUpdateSseDto;
import com.abhinavmehta.confx.events.ConfigItemDeletedEvent;
import com.abhinavmehta.confx.events.ConfigVersionUpdatedEvent;
import com.abhinavmehta.confx.events.EnvironmentDeletedEvent;
import com.abhinavmehta.confx.events.ProjectDeletedEvent;
import com.abhinavmehta.confx.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigUpdateEventListener {

    private final SseService sseService;

    @Async
    @EventListener
    public void handleConfigVersionUpdated(ConfigVersionUpdatedEvent event) {
        log.info("Event: ConfigVersionUpdated for Project: {}, Env: {}, ConfigKey: {}", 
                 event.getProjectId(), event.getEnvironmentId(), event.getUpdatedConfigVersionData().getConfigItemKey());
        ConfigUpdateSseDto sseDto = new ConfigUpdateSseDto(
            ConfigUpdateSseDto.TYPE_CONFIG_VERSION_UPDATED,
            event.getUpdatedConfigVersionData()
        );
        sseService.sendUpdateToClients(event.getProjectId(), event.getEnvironmentId(), sseDto);
    }

    @Async
    @EventListener
    public void handleConfigItemDeleted(ConfigItemDeletedEvent event) {
        log.info("Event: ConfigItemDeleted for Project: {}, ConfigKey: {}", 
                 event.getProjectId(), event.getConfigKey());
        // This event needs to be broadcast to ALL environments within the project,
        // as the config item is gone from all of them.
        // SseService currently maps emitters by projectId:environmentId.
        // We would need a way to get all environmentIds for a project or SseService to handle broadcasts to a project.
        // For now, this will be a TODO or requires SseService enhancement.
        // As a simplification for now, we assume client SDKs might re-fetch or handle this gracefully.
        // A more robust approach: iterate environments of the project and send individual targeted messages.
        // Or, SseService could have a method like sendToAllEnvironmentsInProject(projectId, payload).
        
        // Simplified payload for deletion:
        Map<String, Object> payload = Map.of("configItemId", event.getConfigItemId(), "configKey", event.getConfigKey());
        ConfigUpdateSseDto sseDto = new ConfigUpdateSseDto(
            ConfigUpdateSseDto.TYPE_CONFIG_ITEM_DELETED,
            payload
        );
        // TODO: Enhance SseService to broadcast to all environments of event.getProjectId()
        log.warn("TODO: Broadcasting ConfigItemDeletedEvent to all environments of project {} is not fully implemented in SseService yet.", event.getProjectId());
    }

    @Async
    @EventListener
    public void handleEnvironmentDeleted(EnvironmentDeletedEvent event) {
        log.info("Event: EnvironmentDeleted for Project: {}, Env: {}", 
                 event.getProjectId(), event.getEnvironmentId());
        // This event is specific to the deleted environment.
        Map<String, Object> payload = Map.of("environmentId", event.getEnvironmentId());
        ConfigUpdateSseDto sseDto = new ConfigUpdateSseDto(
            ConfigUpdateSseDto.TYPE_ENVIRONMENT_DELETED,
            payload
        );
        // Send to the specific environment (clients connected to it will be disconnected or should handle this)
        sseService.sendUpdateToClients(event.getProjectId(), event.getEnvironmentId(), sseDto);
        // Optionally, SseService could also clean up all emitters for this specific environment key upon receiving this.
    }

    @Async
    @EventListener
    public void handleProjectDeleted(ProjectDeletedEvent event) {
        log.info("Event: ProjectDeleted for Project: {}", event.getProjectId());
        // This event means all configs & environments under this project are gone.
        // Similar to ConfigItemDeleted, this should ideally be broadcast to all clients
        // connected to any environment of this project.
        Map<String, Object> payload = Map.of("projectId", event.getProjectId());
         ConfigUpdateSseDto sseDto = new ConfigUpdateSseDto(
            ConfigUpdateSseDto.TYPE_PROJECT_DELETED,
            payload
        );
        // TODO: Enhance SseService to broadcast to all environments of event.getProjectId()
        // and then remove all emitters associated with this project.
        log.warn("TODO: Broadcasting ProjectDeletedEvent and cleaning up all associated emitters for project {} is not fully implemented in SseService yet.", event.getProjectId());
    }
} 