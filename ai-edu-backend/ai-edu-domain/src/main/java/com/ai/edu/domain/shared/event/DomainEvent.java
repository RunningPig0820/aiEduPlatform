package com.ai.edu.domain.shared.event;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

/**
 * 领域事件基类
 */
public abstract class DomainEvent extends ApplicationEvent {

    private final LocalDateTime occurredOn;

    protected DomainEvent(Object source) {
        super(source);
        this.occurredOn = LocalDateTime.now();
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}