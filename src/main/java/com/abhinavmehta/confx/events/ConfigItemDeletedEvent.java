package com.abhinavmehta.confx.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConfigItemDeletedEvent extends ApplicationEvent {
    private final Long projectId;
    private final Long configItemId;
    private final String configKey;

    public ConfigItemDeletedEvent(Object source, Long projectId, Long configItemId, String configKey) {
        super(source);
        this.projectId = projectId;
        this.configItemId = configItemId;
        this.configKey = configKey;
    }
} 