package com.legal.notification.config;

import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final String CASE_EVENTS_QUEUE = "case.events.queue";

    private final RabbitAdmin rabbitAdmin;

    public RabbitMQConfig(ConnectionFactory connectionFactory) {
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
    }

    @Bean
    public TopicExchange legalExchange() {
        return new TopicExchange("legal.exchange");
    }

    @Bean
    public Queue caseEventsQueue() {
        return new Queue(CASE_EVENTS_QUEUE, true);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @PostConstruct
    public void declareQueues() {
        rabbitAdmin.declareQueue(new Queue(CASE_EVENTS_QUEUE, true));
    }

}