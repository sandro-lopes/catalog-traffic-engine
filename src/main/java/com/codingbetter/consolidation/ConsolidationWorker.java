package com.codingbetter.consolidation;

import com.codingbetter.kafka.KafkaProducer;
import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Worker de consolidação que processa uma partição específica do Kafka.
 * Cada worker processa eventos dos últimos 30 dias de sua partição atribuída.
 */
@Component
public class ConsolidationWorker {

    private static final Logger logger = LoggerFactory.getLogger(ConsolidationWorker.class);

    private final ConsumerFactory<String, String> consumerFactory;
    private final ObjectMapper objectMapper;
    private final SnapshotGenerator snapshotGenerator;
    private final KafkaProducer kafkaProducer;

    public ConsolidationWorker(
            ConsumerFactory<String, String> consumerFactory,
            ObjectMapper objectMapper,
            SnapshotGenerator snapshotGenerator,
            KafkaProducer kafkaProducer) {
        this.consumerFactory = consumerFactory;
        this.objectMapper = objectMapper;
        this.snapshotGenerator = snapshotGenerator;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * Processa uma partição específica do tópico raw.
     * @param partitionId ID da partição a processar
     * @param windowDays Número de dias para considerar (padrão: 30)
     * @return Lista de snapshots gerados
     */
    public List<ServiceActivitySnapshot> processPartition(int partitionId, int windowDays) {
        logger.info("Iniciando processamento da partição {} (janela: {} dias)", partitionId, windowDays);

        try (org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer = createPartitionConsumer(partitionId)) {
            // Calcula timestamp de início (30 dias atrás)
            Instant cutoffTime = Instant.now().minus(windowDays, ChronoUnit.DAYS);
            long cutoffTimestamp = cutoffTime.toEpochMilli();

            // Lê eventos da partição
            List<ServiceActivityEvent> events = readEventsFromPartition(consumer, cutoffTimestamp);

            logger.info("Lidos {} eventos da partição {}", events.size(), partitionId);

            // Gera snapshots
            List<ServiceActivitySnapshot> snapshots = snapshotGenerator.generateSnapshots(events);

            // Publica snapshots
            snapshots.forEach(snapshot -> {
                kafkaProducer.publishSnapshot(snapshot);
                logger.debug("Snapshot publicado: serviceId={}, classification={}",
                        snapshot.getServiceId(), snapshot.getClassification());
            });

            logger.info("Partição {} processada: {} snapshots gerados", partitionId, snapshots.size());

            return snapshots;
        } catch (Exception e) {
            logger.error("Erro ao processar partição {}", partitionId, e);
            throw new RuntimeException("Erro ao processar partição", e);
        }
    }

    @SuppressWarnings("unchecked")
    private org.apache.kafka.clients.consumer.KafkaConsumer<String, String> createPartitionConsumer(int partitionId) {
        org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer = 
                (org.apache.kafka.clients.consumer.KafkaConsumer<String, String>) consumerFactory.createConsumer();
        TopicPartition partition = new TopicPartition("governance.activity.raw", partitionId);
        consumer.assign(Collections.singletonList(partition));
        consumer.seekToBeginning(Collections.singletonList(partition));
        return consumer;
    }

    private List<ServiceActivityEvent> readEventsFromPartition(
            org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer,
            long cutoffTimestamp) {

        List<ServiceActivityEvent> events = new ArrayList<>();
        Duration timeout = Duration.ofSeconds(5);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(timeout);

            if (records.isEmpty()) {
                break;
            }

            for (ConsumerRecord<String, String> record : records) {
                // Filtra eventos dentro da janela de tempo
                if (record.timestamp() >= cutoffTimestamp) {
                    try {
                        ServiceActivityEvent event = objectMapper.readValue(
                                record.value(), ServiceActivityEvent.class);
                        events.add(event);
                    } catch (Exception e) {
                        logger.warn("Erro ao deserializar evento: partition={}, offset={}",
                                record.partition(), record.offset(), e);
                    }
                } else {
                    // Eventos mais antigos que a janela podem ser ignorados
                    // (otimização: poderia usar seek para pular direto)
                }
            }
        }

        return events;
    }
}

