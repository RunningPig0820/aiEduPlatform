package com.ai.edu.domain.shared.event;

import java.time.LocalDateTime;

/**
 * 领域事件基类 - 纯 POJO，不依赖 Spring
 */
public abstract class DomainEvent {

    private final LocalDateTime occurredOn;

    protected DomainEvent() {
        this.occurredOn = LocalDateTime.now();
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}