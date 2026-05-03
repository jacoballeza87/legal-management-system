package com.legal.cases.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class RabbitMQConfig {

    private final RabbitAdmin rabbitAdmin;

    public RabbitMQConfig(ConnectionFactory connectionFactory) {
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public TopicExchange legalExchange() {
        return new TopicExchange("legal.exchange");
    }

    @Bean
    public Queue caseCreatedQueue() {
        return new Queue("case.created.queue");
    }

    @Bean
    public Binding caseCreatedBinding(Queue caseCreatedQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(caseCreatedQueue)
                .to(exchange)
                .with("case.created");
    }

    @Bean
    public Queue userDeletedQueue() {
        return new Queue("user.deleted.queue", true);
    }

    @Bean
    public Queue documentUploadedQueue() {
        return new Queue("document.uploaded.queue", true);
    }

    @PostConstruct
    public void declareQueues() {
        rabbitAdmin.declareQueue(new Queue("user.deleted.queue", true));
        rabbitAdmin.declareQueue(new Queue("document.uploaded.queue", true));
    }

}