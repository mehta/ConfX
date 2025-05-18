package com.abhinavmehta.confx.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EnvironmentDeletedEvent extends ApplicationEvent {
    private final Long projectId;
    private final Long environmentId;

    public EnvironmentDeletedEvent(Object source, Long projectId, Long environmentId) {
        super(source);
        this.projectId = projectId;
        this.environmentId = environmentId;
    }
} 