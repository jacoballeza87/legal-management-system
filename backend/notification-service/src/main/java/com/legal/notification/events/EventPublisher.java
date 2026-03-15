package com.legal.notification.events;

public interface EventPublisher {

    void publish(String routingKey, Object event);

}