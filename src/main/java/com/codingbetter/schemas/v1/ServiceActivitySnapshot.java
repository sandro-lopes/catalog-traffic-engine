package com.codingbetter.schemas.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Snapshot consolidado representando o estado de um sistema.
 * Resultado da consolidação de eventos de atividade.
 */
public class ServiceActivitySnapshot {

    @NotBlank
    @JsonProperty("service.id")
    private String serviceId;

    @NotNull
    @JsonProperty("receivesTraffic")
    private Boolean receivesTraffic;

    @NotNull
    @PositiveOrZero
    @JsonProperty("trafficVolume")
    private Long trafficVolume;

    @NotNull
    @JsonProperty("lastSeen")
    private Instant lastSeen;

    @JsonProperty("activeCallers")
    private List<String> activeCallers;

    @NotNull
    @JsonProperty("confidenceLevel")
    private ServiceActivityEvent.ConfidenceLevel confidenceLevel;

    @NotNull
    @JsonProperty("classification")
    private Classification classification;

    @NotNull
    @JsonProperty("snapshotDate")
    private LocalDate snapshotDate;

    @JsonProperty("finOpsMetrics")
    private ServiceFinOpsMetrics finOpsMetrics;

    @JsonProperty("costOptimization")
    private CostOptimizationRecommendation costOptimization;

    public ServiceActivitySnapshot() {
    }

    public ServiceActivitySnapshot(String serviceId, Boolean receivesTraffic, Long trafficVolume,
                                  Instant lastSeen, List<String> activeCallers,
                                  ServiceActivityEvent.ConfidenceLevel confidenceLevel,
                                  Classification classification, LocalDate snapshotDate) {
        this.serviceId = serviceId;
        this.receivesTraffic = receivesTraffic;
        this.trafficVolume = trafficVolume;
        this.lastSeen = lastSeen;
        this.activeCallers = activeCallers;
        this.confidenceLevel = confidenceLevel;
        this.classification = classification;
        this.snapshotDate = snapshotDate;
    }

    // Getters and Setters
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Boolean getReceivesTraffic() {
        return receivesTraffic;
    }

    public void setReceivesTraffic(Boolean receivesTraffic) {
        this.receivesTraffic = receivesTraffic;
    }

    public Long getTrafficVolume() {
        return trafficVolume;
    }

    public void setTrafficVolume(Long trafficVolume) {
        this.trafficVolume = trafficVolume;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public List<String> getActiveCallers() {
        return activeCallers;
    }

    public void setActiveCallers(List<String> activeCallers) {
        this.activeCallers = activeCallers;
    }

    public ServiceActivityEvent.ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(ServiceActivityEvent.ConfidenceLevel confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public ServiceFinOpsMetrics getFinOpsMetrics() {
        return finOpsMetrics;
    }

    public void setFinOpsMetrics(ServiceFinOpsMetrics finOpsMetrics) {
        this.finOpsMetrics = finOpsMetrics;
    }

    public CostOptimizationRecommendation getCostOptimization() {
        return costOptimization;
    }

    public void setCostOptimization(CostOptimizationRecommendation costOptimization) {
        this.costOptimization = costOptimization;
    }

    public enum Classification {
        ACTIVE,      // lastSeen <= 7 dias
        LOW_USAGE,   // 8 <= lastSeen <= 30 dias
        NO_TRAFFIC   // lastSeen > 30 dias
    }
}

