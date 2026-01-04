package com.codingbetter.backstage;

import com.codingbetter.adapters.github.RepositoryMetadata;
import com.codingbetter.ownership.CommitterInfo;
import com.codingbetter.ownership.InferredOwner;
import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapeador que converte snapshots para formato Backstage.
 * Mapeia campos para:
 * - governance.traffic → receivesTraffic, trafficVolume
 * - governance.risk → classification, confidenceLevel
 * - governance.integration → activeCallers, lastSeen
 */
public class BackstageMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Converte snapshot para formato de atualização do Backstage.
     */
    public static ObjectNode toBackstageFormat(ServiceActivitySnapshot snapshot, ObjectMapper objectMapper) {
        ObjectNode root = objectMapper.createObjectNode();

        // governance.traffic
        ObjectNode traffic = objectMapper.createObjectNode();
        traffic.put("receivesTraffic", snapshot.getReceivesTraffic());
        traffic.put("trafficVolume", snapshot.getTrafficVolume());
        root.set("governance.traffic", traffic);

        // governance.risk
        ObjectNode risk = objectMapper.createObjectNode();
        risk.put("classification", snapshot.getClassification().name());
        risk.put("confidenceLevel", snapshot.getConfidenceLevel().name());
        root.set("governance.risk", risk);

        // governance.integration
        ObjectNode integration = objectMapper.createObjectNode();
        integration.put("lastSeen", snapshot.getLastSeen().toString());
        integration.set("activeCallers", objectMapper.valueToTree(snapshot.getActiveCallers()));
        root.set("governance.integration", integration);

        return root;
    }

    /**
     * Cria payload de atualização em batch para Backstage.
     */
    public static ObjectNode createBatchUpdate(
            Map<String, ServiceActivitySnapshot> snapshots,
            ObjectMapper objectMapper) {

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode entities = objectMapper.createObjectNode();

        snapshots.forEach((serviceId, snapshot) -> {
            ObjectNode entity = toBackstageFormat(snapshot, objectMapper);
            entities.set(serviceId, entity);
        });

        root.set("entities", entities);
        return root;
    }

    /**
     * Fase 1: Dados básicos de descoberta e ownership.
     */
    public static ObjectNode toBackstageFormatPhase1(RepositoryMetadata repo, InferredOwner owner, ObjectMapper objectMapper) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode metadata = objectMapper.createObjectNode();
        ObjectNode annotations = objectMapper.createObjectNode();

        // Anotações de descoberta
        if (repo.getSigla() != null) {
            annotations.put("governance.discovery.sigla", repo.getSigla());
        }
        if (repo.getType() != null) {
            annotations.put("governance.discovery.type", repo.getType());
        }
        if (repo.getServiceName() != null) {
            annotations.put("governance.discovery.serviceName", repo.getServiceName());
        }
        annotations.put("governance.discovery.discoveredAt", DATE_TIME_FORMATTER.format(Instant.now()));

        // Anotações de ownership (se disponível)
        if (owner != null && !owner.isUnknown()) {
            annotations.put("governance.ownership.primaryOwner", owner.getOwner());
            annotations.put("governance.ownership.inferredFrom", owner.getStrategy());
            annotations.put("governance.ownership.confidence", String.format("%.2f", owner.getConfidence()));
        }

        // Top committers (lista dos top 10)
        if (repo.getTopCommitters() != null && !repo.getTopCommitters().isEmpty()) {
            ArrayNode committersArray = objectMapper.createArrayNode();
            for (CommitterInfo committer : repo.getTopCommitters()) {
                ObjectNode committerNode = objectMapper.createObjectNode();
                committerNode.put("id", committer.getId());
                committerNode.put("email", committer.getEmail());
                committerNode.put("name", committer.getName());
                committerNode.put("commitCount", committer.getCommitCount());
                committerNode.put("weightedScore", committer.getWeightedScore());
                committersArray.add(committerNode);
            }
            annotations.set("governance.contributors.topCommitters", committersArray);
        }

        metadata.set("annotations", annotations);
        root.set("metadata", metadata);

        // Spec básico
        ObjectNode spec = objectMapper.createObjectNode();
        spec.put("type", "service");
        spec.put("lifecycle", "production");
        if (owner != null && !owner.isUnknown()) {
            spec.put("owner", owner.getOwner());
        } else {
            spec.put("owner", "team-unknown");
        }
        root.set("spec", spec);

        return root;
    }

    /**
     * Fase 2: Adicionar dados de atividade.
     */
    public static ObjectNode toBackstageFormatPhase2(ServiceActivityEvent event, ObjectMapper objectMapper) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode metadata = objectMapper.createObjectNode();
        ObjectNode annotations = objectMapper.createObjectNode();

        // Anotações de atividade
        annotations.put("governance.activity.receivesTraffic", event.getActivityCount() > 0);
        if (event.getWindow() != null && event.getWindow().getEnd() != null) {
            annotations.put("governance.activity.lastSeen", DATE_TIME_FORMATTER.format(event.getWindow().getEnd()));
        }
        annotations.put("governance.activity.trafficVolume", event.getActivityCount());
        if (event.getCallers() != null && !event.getCallers().isEmpty()) {
            annotations.set("governance.activity.activeCallers", objectMapper.valueToTree(event.getCallers()));
        }

        metadata.set("annotations", annotations);
        root.set("metadata", metadata);

        return root;
    }

    /**
     * Fase 3: Adicionar classificação.
     */
    public static ObjectNode toBackstageFormatPhase3(ServiceActivitySnapshot snapshot, ObjectMapper objectMapper) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode metadata = objectMapper.createObjectNode();
        ObjectNode annotations = objectMapper.createObjectNode();

        // Anotações de classificação
        if (snapshot.getClassification() != null) {
            annotations.put("governance.classification.status", snapshot.getClassification().name());
        }
        if (snapshot.getConfidenceLevel() != null) {
            annotations.put("governance.classification.confidenceLevel", snapshot.getConfidenceLevel().name());
        }
        if (snapshot.getSnapshotDate() != null) {
            annotations.put("governance.classification.snapshotDate", snapshot.getSnapshotDate().toString());
        }

        metadata.set("annotations", annotations);
        root.set("metadata", metadata);

        return root;
    }

    /**
     * Fase 4: Dados completos (consolida Fase 1 + 2 + 3).
     */
    public static ObjectNode toBackstageFormatPhase4(ServiceActivitySnapshot snapshot, ObjectMapper objectMapper) {
        // Usa o método original que já tem todos os dados
        return toBackstageFormat(snapshot, objectMapper);
    }
}

