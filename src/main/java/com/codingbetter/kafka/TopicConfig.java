package com.codingbetter.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração dos tópicos Kafka.
 * Tópicos são particionados por service.id para permitir processamento paralelo.
 */
@Configuration
public class TopicConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.activity-raw.partitions:30}")
    private int activityRawPartitions;

    @Value("${kafka.topics.activity-raw.replication-factor:3}")
    private short activityRawReplicationFactor;

    @Value("${kafka.topics.activity-snapshot.partitions:30}")
    private int activitySnapshotPartitions;

    @Value("${kafka.topics.activity-snapshot.replication-factor:3}")
    private short activitySnapshotReplicationFactor;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic activityRawTopic() {
        return TopicBuilder.name("governance.activity.raw")
                .partitions(activityRawPartitions)
                .replicas(activityRawReplicationFactor)
                .config("retention.ms", String.valueOf(35L * 24 * 60 * 60 * 1000)) // 35 dias
                .config("compression.type", "gzip")
                .build();
    }

    @Bean
    public NewTopic activitySnapshotTopic() {
        return TopicBuilder.name("governance.activity.snapshot")
                .partitions(activitySnapshotPartitions)
                .replicas(activitySnapshotReplicationFactor)
                .config("retention.ms", String.valueOf(90L * 24 * 60 * 60 * 1000)) // 90 dias
                .config("compression.type", "gzip")
                .build();
    }
}

