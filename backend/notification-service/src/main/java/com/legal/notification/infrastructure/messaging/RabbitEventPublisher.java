package com.legal.notification.infrastructure.messaging;

import com.legal.notification.events.EventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String routingKey, Object event) {

        rabbitTemplate.convertAndSend(
                "legal.exchange",
                routingKey,
                event
        );

    }
}