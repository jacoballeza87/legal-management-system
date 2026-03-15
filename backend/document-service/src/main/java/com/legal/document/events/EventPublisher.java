package com.legal.document.events;

public interface EventPublisher {

    void publish(String routingKey, Object event);

}