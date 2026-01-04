package com.codingbetter.kafka;

import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka para eventos e snapshots.
 * Suporta processamento paralelo através de múltiplas instâncias.
 */
@Component
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final ObjectMapper objectMapper;

    public KafkaConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Consome eventos de atividade do tópico raw.
     * Cada instância do consumidor processa uma ou mais partições.
     */
    @KafkaListener(
            topics = "governance.activity.raw",
            groupId = "governance-consolidation",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeActivityEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            ServiceActivityEvent event = objectMapper.readValue(message, ServiceActivityEvent.class);
            logger.debug("Evento recebido: serviceId={}, partition={}, offset={}",
                    event.getServiceId(), partition, offset);

            // Processamento será feito pelo ConsolidationWorker
            // Este listener pode ser usado para outros propósitos (ex: métricas)

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Erro ao processar evento de atividade: partition={}, offset={}",
                    partition, offset, e);
            // Em produção, considerar DLQ (Dead Letter Queue)
        }
    }

    /**
     * Consome snapshots do tópico de snapshots.
     * Usado principalmente para integração com Backstage.
     */
    @KafkaListener(
            topics = "governance.activity.snapshot",
            groupId = "governance-backstage",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSnapshot(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            ServiceActivitySnapshot snapshot = objectMapper.readValue(message, ServiceActivitySnapshot.class);
            logger.debug("Snapshot recebido: serviceId={}, classification={}, partition={}, offset={}",
                    snapshot.getServiceId(), snapshot.getClassification(), partition, offset);

            // Processamento será feito pelo BackstageClient
            // Este listener pode ser usado para outros propósitos (ex: dashboards)

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Erro ao processar snapshot: partition={}, offset={}",
                    partition, offset, e);
        }
    }
}

