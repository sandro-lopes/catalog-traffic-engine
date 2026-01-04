package com.codingbetter.service;

import com.codingbetter.backstage.BackstageClient;
import com.codingbetter.backstage.BackstageMapper;
import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de integração com Backstage.
 * Consome snapshots do Kafka e atualiza Backstage em batch.
 */
@Service
public class BackstageIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(BackstageIntegrationService.class);

    private final BackstageClient backstageClient;
    private final ObjectMapper objectMapper;

    // Buffer para acumular snapshots antes de enviar em batch
    private final Map<String, ServiceActivitySnapshot> snapshotBuffer = new HashMap<>();
    
    // Buffer para eventos de atividade (Fase 2)
    private final Map<String, ServiceActivityEvent> activityEventBuffer = new HashMap<>();

    @Value("${backstage.integration.phase:1}")
    private int currentPhase;

    public BackstageIntegrationService(
            BackstageClient backstageClient,
            ObjectMapper objectMapper) {
        this.backstageClient = backstageClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Consome snapshots do tópico e acumula no buffer.
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

            // Adiciona ao buffer (substitui snapshot anterior do mesmo serviço)
            snapshotBuffer.put(snapshot.getServiceId(), snapshot);

            logger.debug("Snapshot acumulado no buffer: serviceId={}, buffer size={}",
                    snapshot.getServiceId(), snapshotBuffer.size());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Erro ao processar snapshot para Backstage: partition={}, offset={}",
                    partition, offset, e);
        }
    }

    /**
     * Consome eventos de atividade (Fase 2).
     */
    @KafkaListener(
            topics = "governance.activity.raw",
            groupId = "governance-backstage-activity",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeActivityEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        if (currentPhase < 2) {
            acknowledgment.acknowledge();
            return; // Fase 2 ainda não ativa
        }

        try {
            ServiceActivityEvent event = objectMapper.readValue(message, ServiceActivityEvent.class);
            activityEventBuffer.put(event.getServiceId(), event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Erro ao processar evento de atividade para Backstage: partition={}, offset={}",
                    partition, offset, e);
        }
    }

    /**
     * Envia snapshots acumulados para Backstage em batch.
     * Executa periodicamente conforme configurado.
     * Atualiza conforme fase atual.
     */
    @Scheduled(fixedRateString = "${backstage.integration.update-interval-seconds:60}000")
    public void syncToBackstage() {
        if (snapshotBuffer.isEmpty() && activityEventBuffer.isEmpty()) {
            logger.debug("Buffer vazio, nada para sincronizar com Backstage");
            return;
        }

        logger.info("Sincronizando dados com Backstage (Fase {})", currentPhase);

        try {
            Map<String, Map<String, Object>> entitiesData = new HashMap<>();

            // Fase 1: Dados básicos (já devem estar no Backstage via catalog-info.yaml)
            // Fase 2: Adiciona dados de atividade
            if (currentPhase >= 2 && !activityEventBuffer.isEmpty()) {
                activityEventBuffer.forEach((serviceId, event) -> {
                    ObjectNode phase2Data = BackstageMapper.toBackstageFormatPhase2(event, objectMapper);
                    entitiesData.put(serviceId, objectMapper.convertValue(phase2Data, Map.class));
                });
            }

            // Fase 3: Adiciona classificação
            if (currentPhase >= 3 && !snapshotBuffer.isEmpty()) {
                snapshotBuffer.forEach((serviceId, snapshot) -> {
                    Map<String, Object> existingData = entitiesData.getOrDefault(serviceId, new HashMap<>());
                    ObjectNode phase3Data = BackstageMapper.toBackstageFormatPhase3(snapshot, objectMapper);
                    Map<String, Object> phase3Map = objectMapper.convertValue(phase3Data, Map.class);
                    
                    // Merge com dados existentes
                    if (existingData.containsKey("metadata") && phase3Map.containsKey("metadata")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> existingMetadata = (Map<String, Object>) existingData.get("metadata");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> phase3Metadata = (Map<String, Object>) phase3Map.get("metadata");
                        
                        if (existingMetadata.containsKey("annotations") && phase3Metadata.containsKey("annotations")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> existingAnnotations = (Map<String, Object>) existingMetadata.get("annotations");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> phase3Annotations = (Map<String, Object>) phase3Metadata.get("annotations");
                            existingAnnotations.putAll(phase3Annotations);
                        }
                    } else {
                        existingData.putAll(phase3Map);
                    }
                    
                    entitiesData.put(serviceId, existingData);
                });
            }

            // Fase 4: Dados completos
            if (currentPhase >= 4 && !snapshotBuffer.isEmpty()) {
                snapshotBuffer.forEach((serviceId, snapshot) -> {
                    ObjectNode phase4Data = BackstageMapper.toBackstageFormatPhase4(snapshot, objectMapper);
                    entitiesData.put(serviceId, objectMapper.convertValue(phase4Data, Map.class));
                });
            }

            if (entitiesData.isEmpty()) {
                logger.debug("Nenhum dado para sincronizar na fase atual");
                return;
            }

            // Envia em batch
            backstageClient.updateEntitiesBatch(entitiesData)
                    .doOnSuccess(v -> {
                        logger.info("{} entidades sincronizadas com Backstage com sucesso (Fase {})", 
                                entitiesData.size(), currentPhase);
                        snapshotBuffer.clear();
                        activityEventBuffer.clear();
                    })
                    .doOnError(error -> logger.error("Erro ao sincronizar com Backstage", error))
                    .block();

        } catch (Exception e) {
            logger.error("Erro crítico ao sincronizar com Backstage", e);
        }
    }

    /**
     * Retorna tamanho atual do buffer (útil para monitoramento).
     */
    public int getBufferSize() {
        return snapshotBuffer.size();
    }
}

