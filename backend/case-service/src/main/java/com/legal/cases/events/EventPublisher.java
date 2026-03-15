package com.legal.cases.events;

public interface EventPublisher {

    void publish(String routingKey, Object event);

}