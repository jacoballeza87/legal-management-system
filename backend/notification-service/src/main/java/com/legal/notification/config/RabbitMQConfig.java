package com.legal.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig implements RabbitListenerConfigurer {

    private static final String CASE_EVENTS_QUEUE = "case.events.queue";

    private final ConnectionFactory connectionFactory;

    public RabbitMQConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
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
    public RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Implements RabbitListenerConfigurer to guarantee queues are declared in
     * RabbitMQ via RabbitAdmin BEFORE any listener container is started.
     * This fixes the timing issue where @RabbitListener tries to bind to a
     * queue that does not yet exist.
     */
    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        RabbitAdmin admin = rabbitAdmin();
        admin.declareQueue(caseEventsQueue());

        registrar.setContainerFactory(
                new SimpleRabbitListenerContainerFactory() {{
                    setConnectionFactory(connectionFactory);
                }}
        );
    }
}