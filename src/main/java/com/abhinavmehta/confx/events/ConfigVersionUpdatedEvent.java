package com.abhinavmehta.confx.events;

import com.abhinavmehta.confx.dto.ConfigVersionResponseDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConfigVersionUpdatedEvent extends ApplicationEvent {
    private final Long projectId;
    private final Long environmentId;
    private final ConfigVersionResponseDto updatedConfigVersionData;

    public ConfigVersionUpdatedEvent(Object source, Long projectId, Long environmentId, ConfigVersionResponseDto updatedConfigVersionData) {
        super(source);
        this.projectId = projectId;
        this.environmentId = environmentId;
        this.updatedConfigVersionData = updatedConfigVersionData;
    }
} 