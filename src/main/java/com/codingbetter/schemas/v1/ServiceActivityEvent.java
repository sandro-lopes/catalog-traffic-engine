package com.codingbetter.schemas.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;

/**
 * Evento de atividade normalizado de um sistema em uma janela de tempo.
 * Representa atividade observada, sem decisão de status.
 */
public class ServiceActivityEvent {

    @NotBlank
    @JsonProperty("service.id")
    private String serviceId;

    @NotNull
    @PositiveOrZero
    @JsonProperty("activity.count")
    private Long activityCount;

    @JsonProperty("dependencies.callers")
    private List<String> callers;

    @NotNull
    @JsonProperty("timestamps.window")
    private TimeWindow window;

    @NotNull
    @JsonProperty("confidence.level")
    private ConfidenceLevel confidenceLevel;

    @NotNull
    @JsonProperty("metadata")
    private Metadata metadata;

    @JsonProperty("repository")
    private RepositoryInfo repository;

    @JsonProperty("discoverySource")
    private DiscoverySource discoverySource;

    public ServiceActivityEvent() {
    }

    public ServiceActivityEvent(String serviceId, Long activityCount, List<String> callers,
                               TimeWindow window, ConfidenceLevel confidenceLevel, Metadata metadata) {
        this.serviceId = serviceId;
        this.activityCount = activityCount;
        this.callers = callers;
        this.window = window;
        this.confidenceLevel = confidenceLevel;
        this.metadata = metadata;
    }

    // Getters and Setters
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Long getActivityCount() {
        return activityCount;
    }

    public void setActivityCount(Long activityCount) {
        this.activityCount = activityCount;
    }

    public List<String> getCallers() {
        return callers;
    }

    public void setCallers(List<String> callers) {
        this.callers = callers;
    }

    public TimeWindow getWindow() {
        return window;
    }

    public void setWindow(TimeWindow window) {
        this.window = window;
    }

    public ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(ConfidenceLevel confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public static class TimeWindow {
        @NotNull
        private Instant start;

        @NotNull
        private Instant end;

        public TimeWindow() {
        }

        public TimeWindow(Instant start, Instant end) {
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

    public static class Metadata {
        @NotBlank
        private String environment;

        @NotBlank
        private String source;

        public Metadata() {
        }

        public Metadata(String environment, String source) {
            this.environment = environment;
            this.source = source;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    public enum ConfidenceLevel {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum DiscoverySource {
        GITHUB,
        DYNATRACE,
        BOTH
    }

    public static class RepositoryInfo {
        private String name;
        private String fullName;
        private String sigla;
        private String type;
        private String serviceName;
        private String url;

        public RepositoryInfo() {
        }

        public RepositoryInfo(String name, String fullName, String sigla, String type, String serviceName, String url) {
            this.name = name;
            this.fullName = fullName;
            this.sigla = sigla;
            this.type = type;
            this.serviceName = serviceName;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getSigla() {
            return sigla;
        }

        public void setSigla(String sigla) {
            this.sigla = sigla;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public RepositoryInfo getRepository() {
        return repository;
    }

    public void setRepository(RepositoryInfo repository) {
        this.repository = repository;
    }

    public DiscoverySource getDiscoverySource() {
        return discoverySource;
    }

    public void setDiscoverySource(DiscoverySource discoverySource) {
        this.discoverySource = discoverySource;
    }
}

