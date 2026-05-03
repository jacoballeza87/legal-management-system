package com.legal.cases.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig implements RabbitListenerConfigurer {

    private final ConnectionFactory connectionFactory;

    public RabbitMQConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public TopicExchange legalExchange() {
        return new TopicExchange("legal.exchange");
    }

    @Bean
    public Queue caseCreatedQueue() {
        return new Queue("case.created.queue", true);
    }

    @Bean
    public Queue userDeletedQueue() {
        return new Queue("user.deleted.queue", true);
    }

    @Bean
    public Queue documentUploadedQueue() {
        return new Queue("document.uploaded.queue", true);
    }

    @Bean
    public Binding caseCreatedBinding(Queue caseCreatedQueue, TopicExchange legalExchange) {
        return BindingBuilder
                .bind(caseCreatedQueue)
                .to(legalExchange)
                .with("case.created");
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        RabbitAdmin admin = rabbitAdmin();
        admin.declareQueue(caseCreatedQueue());
        admin.declareQueue(userDeletedQueue());
        admin.declareQueue(documentUploadedQueue());
    }
}
