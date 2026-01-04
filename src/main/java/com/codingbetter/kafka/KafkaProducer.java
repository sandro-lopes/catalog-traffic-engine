package com.codingbetter.kafka;

import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Produtor Kafka para eventos de atividade e snapshots.
 * Particionamento por service.id para distribuição uniforme.
 */
@Component
public class KafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica evento de atividade no tópico raw.
     * Particionamento por service.id garante que eventos do mesmo serviço
     * vão para a mesma partição, permitindo processamento ordenado.
     */
    public CompletableFuture<SendResult<String, String>> publishActivityEvent(ServiceActivityEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    "governance.activity.raw",
                    event.getServiceId(), // Key para particionamento
                    json
            );

            logger.debug("Publicando evento de atividade: serviceId={}, partition={}",
                    event.getServiceId(), record.partition());

            return kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Erro ao publicar evento de atividade: serviceId={}",
                                    event.getServiceId(), ex);
                        } else {
                            logger.debug("Evento publicado com sucesso: serviceId={}, offset={}",
                                    event.getServiceId(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar evento de atividade: serviceId={}",
                    event.getServiceId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Publica snapshot consolidado no tópico de snapshots.
     */
    public CompletableFuture<SendResult<String, String>> publishSnapshot(ServiceActivitySnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    "governance.activity.snapshot",
                    snapshot.getServiceId(), // Key para particionamento
                    json
            );

            logger.debug("Publicando snapshot: serviceId={}, classification={}",
                    snapshot.getServiceId(), snapshot.getClassification());

            return kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Erro ao publicar snapshot: serviceId={}",
                                    snapshot.getServiceId(), ex);
                        } else {
                            logger.debug("Snapshot publicado com sucesso: serviceId={}, offset={}",
                                    snapshot.getServiceId(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar snapshot: serviceId={}",
                    snapshot.getServiceId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Publica mensagem genérica em um tópico.
     */
    public CompletableFuture<SendResult<String, String>> send(String topic, String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        return kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Erro ao publicar mensagem no tópico {}: key={}", topic, key, ex);
                    } else {
                        logger.debug("Mensagem publicada com sucesso: topic={}, key={}, offset={}",
                                topic, key, result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Retorna o ObjectMapper para serialização.
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

