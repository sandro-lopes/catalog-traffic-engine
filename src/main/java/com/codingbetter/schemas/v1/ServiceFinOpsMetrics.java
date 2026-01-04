package com.codingbetter.schemas.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * Métricas de FinOps para um serviço.
 * Inclui utilização de recursos e recomendações de otimização.
 */
public class ServiceFinOpsMetrics {

    @NotBlank
    @JsonProperty("service.id")
    private String serviceId;

    @NotNull
    @JsonProperty("analysisPeriod")
    private AnalysisPeriod analysisPeriod;

    @NotNull
    @JsonProperty("cpuUtilization")
    private UtilizationMetrics cpuUtilization;

    @NotNull
    @JsonProperty("memoryUtilization")
    private UtilizationMetrics memoryUtilization;

    @JsonProperty("currentResources")
    private ResourceSpec currentResources;

    @JsonProperty("utilizationPattern")
    private UtilizationPattern utilizationPattern;

    @JsonProperty("recommendation")
    private OptimizationRecommendation recommendation;

    public ServiceFinOpsMetrics() {
    }

    // Getters and Setters
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public AnalysisPeriod getAnalysisPeriod() {
        return analysisPeriod;
    }

    public void setAnalysisPeriod(AnalysisPeriod analysisPeriod) {
        this.analysisPeriod = analysisPeriod;
    }

    public UtilizationMetrics getCpuUtilization() {
        return cpuUtilization;
    }

    public void setCpuUtilization(UtilizationMetrics cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public UtilizationMetrics getMemoryUtilization() {
        return memoryUtilization;
    }

    public void setMemoryUtilization(UtilizationMetrics memoryUtilization) {
        this.memoryUtilization = memoryUtilization;
    }

    public ResourceSpec getCurrentResources() {
        return currentResources;
    }

    public void setCurrentResources(ResourceSpec currentResources) {
        this.currentResources = currentResources;
    }

    public UtilizationPattern getUtilizationPattern() {
        return utilizationPattern;
    }

    public void setUtilizationPattern(UtilizationPattern utilizationPattern) {
        this.utilizationPattern = utilizationPattern;
    }

    public OptimizationRecommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(OptimizationRecommendation recommendation) {
        this.recommendation = recommendation;
    }

    public static class AnalysisPeriod {
        @NotNull
        private Instant start;
        @NotNull
        private Instant end;

        public AnalysisPeriod() {
        }

        public AnalysisPeriod(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public Instant getStart() {
            return start;
        }

        public void setStart(Instant start) {
            this.start = start;
        }

        public Instant getEnd() {
            return end;
        }

        public void setEnd(Instant end) {
            this.end = end;
        }
    }

    public static class UtilizationMetrics {
        private Double average;
        private Double p50;
        private Double p95;
        private Double p99;
        private Double peakUtilization;
        private Double lowUtilizationPercent; // Percentual do tempo < 20%

        public UtilizationMetrics() {
        }

        public Double getAverage() {
            return average;
        }

        public void setAverage(Double average) {
            this.average = average;
        }

        public Double getP50() {
            return p50;
        }

        public void setP50(Double p50) {
            this.p50 = p50;
        }

        public Double getP95() {
            return p95;
        }

        public void setP95(Double p95) {
            this.p95 = p95;
        }

        public Double getP99() {
            return p99;
        }

        public void setP99(Double p99) {
            this.p99 = p99;
        }

        public Double getPeakUtilization() {
            return peakUtilization;
        }

        public void setPeakUtilization(Double peakUtilization) {
            this.peakUtilization = peakUtilization;
        }

        public Double getLowUtilizationPercent() {
            return lowUtilizationPercent;
        }

        public void setLowUtilizationPercent(Double lowUtilizationPercent) {
            this.lowUtilizationPercent = lowUtilizationPercent;
        }
    }

    public static class ResourceSpec {
        private Double cpuCores;
        private Double memoryGB;
        private Integer instanceCount;
        private String resourceType; // App Service, Container Instances, etc

        public ResourceSpec() {
        }

        public Double getCpuCores() {
            return cpuCores;
        }

        public void setCpuCores(Double cpuCores) {
            this.cpuCores = cpuCores;
        }

        public Double getMemoryGB() {
            return memoryGB;
        }

        public void setMemoryGB(Double memoryGB) {
            this.memoryGB = memoryGB;
        }

        public Integer getInstanceCount() {
            return instanceCount;
        }

        public void setInstanceCount(Integer instanceCount) {
            this.instanceCount = instanceCount;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }
    }

    public static class UtilizationPattern {
        private Double avgUtilizationPercent; // Média ponderada CPU+Memory
        private List<Integer> peakHours; // Horários de pico (0-23)
        private List<Integer> lowUsageHours; // Horários de baixo uso

        public UtilizationPattern() {
        }

        public Double getAvgUtilizationPercent() {
            return avgUtilizationPercent;
        }

        public void setAvgUtilizationPercent(Double avgUtilizationPercent) {
            this.avgUtilizationPercent = avgUtilizationPercent;
        }

        public List<Integer> getPeakHours() {
            return peakHours;
        }

        public void setPeakHours(List<Integer> peakHours) {
            this.peakHours = peakHours;
        }

        public List<Integer> getLowUsageHours() {
            return lowUsageHours;
        }

        public void setLowUsageHours(List<Integer> lowUsageHours) {
            this.lowUsageHours = lowUsageHours;
        }
    }

    public static class OptimizationRecommendation {
        private RecommendationType type;
        private ResourceSpec suggestedResources;
        private ConfidenceLevel confidence;

        public OptimizationRecommendation() {
        }

        public RecommendationType getType() {
            return type;
        }

        public void setType(RecommendationType type) {
            this.type = type;
        }

        public ResourceSpec getSuggestedResources() {
            return suggestedResources;
        }

        public void setSuggestedResources(ResourceSpec suggestedResources) {
            this.suggestedResources = suggestedResources;
        }

        public ConfidenceLevel getConfidence() {
            return confidence;
        }

        public void setConfidence(ConfidenceLevel confidence) {
            this.confidence = confidence;
        }

        public enum RecommendationType {
            DOWNSCALE,    // Reduzir recursos (ex: menos instâncias)
            RIGHTSIZE,    // Reduzir tamanho do SKU (ex: Premium -> Standard)
            DECOMMISSION  // Descomissionar completamente
        }

        public enum ConfidenceLevel {
            HIGH,
            MEDIUM,
            LOW
        }
    }
}

