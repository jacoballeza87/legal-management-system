package com.legal.cases.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LEGAL_EXCHANGE = "legal.exchange";
    
    // Colas principales del dominio de casos
    public static final String CASE_CREATED_QUEUE = "case.created.queue";
    public static final String CASE_UPDATED_QUEUE = "case.updated.queue";
    public static final String CASE_STATUS_CHANGED_QUEUE = "case.status.changed.queue";
    public static final String CASE_DELETED_QUEUE = "case.deleted.queue";
    
    // Routing Keys
    public static final String ROUTING_KEY_CREATED = "case.created";
    public static final String ROUTING_KEY_UPDATED = "case.updated";
    public static final String ROUTING_KEY_STATUS_CHANGED = "case.status.changed";
    public static final String ROUTING_KEY_DELETED = "case.deleted";

    @Bean
    public TopicExchange legalExchange() {
        return new TopicExchange(LEGAL_EXCHANGE, true, false);
    }

    @Bean
    public Queue caseCreatedQueue() {
        return new Queue(CASE_CREATED_QUEUE, true);
    }

    @Bean
    public Queue caseUpdatedQueue() {
        return new Queue(CASE_UPDATED_QUEUE, true);
    }

    @Bean
    public Queue caseStatusChangedQueue() {
        return new Queue(CASE_STATUS_CHANGED_QUEUE, true);
    }

    @Bean
    public Queue caseDeletedQueue() {
        return new Queue(CASE_DELETED_QUEUE, true);
    }

    @Bean
    public Binding caseCreatedBinding(Queue caseCreatedQueue, TopicExchange legalExchange) {
        return BindingBuilder.bind(caseCreatedQueue).to(legalExchange).with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Binding caseUpdatedBinding(Queue caseUpdatedQueue, TopicExchange legalExchange) {
        return BindingBuilder.bind(caseUpdatedQueue).to(legalExchange).with(ROUTING_KEY_UPDATED);
    }

    @Bean
    public Binding caseStatusChangedBinding(Queue caseStatusChangedQueue, TopicExchange legalExchange) {
        return BindingBuilder.bind(caseStatusChangedQueue).to(legalExchange).with(ROUTING_KEY_STATUS_CHANGED);
    }

    @Bean
    public Binding caseDeletedBinding(Queue caseDeletedQueue, TopicExchange legalExchange) {
        return BindingBuilder.bind(caseDeletedQueue).to(legalExchange).with(ROUTING_KEY_DELETED);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
