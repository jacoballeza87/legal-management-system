package com.legal.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CASE_EVENTS_QUEUE = "case.events.queue";

    @Bean
    public Queue caseEventsQueue() {
        return new Queue(CASE_EVENTS_QUEUE, true);
    }

    @Bean
    public TopicExchange legalExchange() {
        return new TopicExchange("legal.exchange");
    }

}