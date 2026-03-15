package com.legal.cases.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

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

}