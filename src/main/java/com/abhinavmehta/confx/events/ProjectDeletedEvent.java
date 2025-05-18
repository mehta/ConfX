package com.abhinavmehta.confx.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProjectDeletedEvent extends ApplicationEvent {
    private final Long projectId;

    public ProjectDeletedEvent(Object source, Long projectId) {
        super(source);
        this.projectId = projectId;
    }
} 