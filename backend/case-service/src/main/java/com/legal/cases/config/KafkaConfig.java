package com.legal.cases.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.case-created:case.created}")       private String caseCreated;
    @Value("${kafka.topics.case-updated:case.updated}")       private String caseUpdated;
    @Value("${kafka.topics.case-deleted:case.deleted}")       private String caseDeleted;
    @Value("${kafka.topics.case-status-changed:case.status.changed}") private String caseStatusChanged;
    @Value("${kafka.topics.collaborator-added:case.collaborator.added}") private String collaboratorAdded;

    @Bean public NewTopic caseCreatedTopic()       { return TopicBuilder.name(caseCreated).partitions(3).replicas(1).build(); }
    @Bean public NewTopic caseUpdatedTopic()       { return TopicBuilder.name(caseUpdated).partitions(3).replicas(1).build(); }
    @Bean public NewTopic caseDeletedTopic()       { return TopicBuilder.name(caseDeleted).partitions(1).replicas(1).build(); }
    @Bean public NewTopic caseStatusChangedTopic() { return TopicBuilder.name(caseStatusChanged).partitions(3).replicas(1).build(); }
    @Bean public NewTopic collaboratorAddedTopic() { return TopicBuilder.name(collaboratorAdded).partitions(1).replicas(1).build(); }
}
