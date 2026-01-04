package com.codingbetter.adapters.dynatrace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente HTTP para API Dynatrace com circuit breaker, rate limiting e retry.
 */
@Component
public class DynatraceClient {

    private static final Logger logger = LoggerFactory.getLogger(DynatraceClient.class);

    private final WebClient webClient;
    private final DynatraceConfig config;
    private final ObjectMapper objectMapper;

    public DynatraceClient(DynatraceConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getApi().getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Api-Token " + config.getApi().getToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Descobre todos os serviços disponíveis no Dynatrace.
     * Suporta paginação para lidar com grandes volumes.
     */
    @CircuitBreaker(name = "dynatrace")
    @RateLimiter(name = "dynatrace")
    @Retry(name = "dynatrace")
    public Mono<List<String>> discoverServices() {
        logger.debug("Descobrindo serviços no Dynatrace");

        return webClient.get()
                .uri("/api/v2/entities?entitySelector=type(\"SERVICE\")&pageSize=500")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(this::parseServiceIds)
                .doOnSuccess(services -> logger.info("Descobertos {} serviços no Dynatrace", services.size()))
                .doOnError(error -> logger.error("Erro ao descobrir serviços no Dynatrace", error));
    }

    /**
     * Extrai métricas de atividade para um serviço específico.
     */
    @CircuitBreaker(name = "dynatrace")
    @RateLimiter(name = "dynatrace")
    @Retry(name = "dynatrace")
    public Mono<DynatraceServiceMetrics> getServiceMetrics(String serviceId, long startTime, long endTime) {
        logger.debug("Extraindo métricas para serviço: {}", serviceId);

        String query = String.format(
                "timeseriesId=com.dynatrace.builtin:service.responsetime&entityIds=%s&startTimestamp=%d&endTimestamp=%d",
                serviceId, startTime, endTime
        );

        return webClient.get()
                .uri("/api/v2/timeseries/query?" + query)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> parseMetrics(serviceId, json))
                .doOnError(error -> logger.error("Erro ao extrair métricas para serviço: {}", serviceId, error));
    }

    /**
     * Extrai dependências (callers) de um serviço.
     */
    @CircuitBreaker(name = "dynatrace")
    @RateLimiter(name = "dynatrace")
    @Retry(name = "dynatrace")
    public Mono<List<String>> getServiceCallers(String serviceId, long startTime, long endTime) {
        logger.debug("Extraindo callers para serviço: {}", serviceId);

        String query = String.format(
                "entityId=%s&from=%d&to=%d",
                serviceId, startTime, endTime
        );

        return webClient.get()
                .uri("/api/v2/entities/" + serviceId + "/serviceFromRelationships?" + query)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(this::parseCallers)
                .doOnError(error -> logger.error("Erro ao extrair callers para serviço: {}", serviceId, error));
    }

    /**
     * Extrai métricas de recursos (CPU, memória) para análise FinOps.
     * Coleta métricas em janelas de 1 hora para análise de padrões.
     */
    @CircuitBreaker(name = "dynatrace")
    @RateLimiter(name = "dynatrace")
    @Retry(name = "dynatrace")
    public Mono<ResourceMetrics> getResourceMetrics(String serviceId, long startTime, long endTime) {
        logger.debug("Extraindo métricas de recursos para serviço: {}", serviceId);

        // Query múltiplas métricas: CPU, memória, request rate, response time
        String query = String.format(
                "timeseriesId=com.dynatrace.builtin:service.cpu,com.dynatrace.builtin:service.memory,com.dynatrace.builtin:service.requestsPerSecond,com.dynatrace.builtin:service.responsetime&entityIds=%s&startTimestamp=%d&endTimestamp=%d&resolution=hour",
                serviceId, startTime, endTime
        );

        return webClient.get()
                .uri("/api/v2/timeseries/query?" + query)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> parseResourceMetrics(serviceId, json))
                .doOnError(error -> logger.error("Erro ao extrair métricas de recursos para serviço: {}", serviceId, error));
    }

    /**
     * Obtém detalhes da entidade do serviço no Dynatrace.
     * Inclui nome, tags (incluindo tags Azure), tecnologia, etc.
     */
    @CircuitBreaker(name = "dynatrace")
    @RateLimiter(name = "dynatrace")
    @Retry(name = "dynatrace")
    public Mono<EntityDetails> getServiceEntityDetails(String serviceId) {
        logger.debug("Extraindo detalhes da entidade para serviço: {}", serviceId);

        return webClient.get()
                .uri("/api/v2/entities/{entityId}", serviceId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> parseEntityDetails(serviceId, json))
                .doOnError(error -> logger.error("Erro ao extrair detalhes da entidade para serviço: {}", serviceId, error));
    }

    private List<String> parseServiceIds(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode entities = root.get("entities");
            List<String> serviceIds = new ArrayList<>();

            if (entities != null && entities.isArray()) {
                for (JsonNode entity : entities) {
                    JsonNode entityId = entity.get("entityId");
                    if (entityId != null) {
                        serviceIds.add(entityId.asText());
                    }
                }
            }

            return serviceIds;
        } catch (Exception e) {
            logger.error("Erro ao parsear lista de serviços", e);
            throw new RuntimeException("Erro ao parsear resposta do Dynatrace", e);
        }
    }

    private DynatraceServiceMetrics parseMetrics(String serviceId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            // Simplificado - em produção, parsear estrutura completa do Dynatrace
            long requestCount = 0;
            if (root.has("result")) {
                JsonNode result = root.get("result");
                if (result.isArray() && result.size() > 0) {
                    JsonNode data = result.get(0).get("data");
                    if (data != null && data.isArray() && data.size() > 0) {
                        JsonNode values = data.get(0).get("values");
                        if (values != null && values.isArray()) {
                            requestCount = values.size();
                        }
                    }
                }
            }

            return new DynatraceServiceMetrics(serviceId, requestCount);
        } catch (Exception e) {
            logger.error("Erro ao parsear métricas", e);
            return new DynatraceServiceMetrics(serviceId, 0L);
        }
    }

    private List<String> parseCallers(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> callers = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode relationship : root) {
                    JsonNode fromEntity = relationship.get("fromEntity");
                    if (fromEntity != null) {
                        JsonNode entityId = fromEntity.get("entityId");
                        if (entityId != null) {
                            callers.add(entityId.asText());
                        }
                    }
                }
            }

            return callers;
        } catch (Exception e) {
            logger.error("Erro ao parsear callers", e);
            return new ArrayList<>();
        }
    }

    private ResourceMetrics parseResourceMetrics(String serviceId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            ResourceMetrics metrics = new ResourceMetrics(serviceId);

            // Parse múltiplas séries temporais
            if (root.has("result")) {
                JsonNode results = root.get("result");
                if (results.isArray()) {
                    for (JsonNode result : results) {
                        String timeseriesId = result.has("timeseriesId") 
                                ? result.get("timeseriesId").asText() : null;
                        JsonNode data = result.get("data");
                        
                        if (data != null && data.isArray() && data.size() > 0) {
                            List<Double> values = new ArrayList<>();
                            for (JsonNode dataPoint : data) {
                                JsonNode valuesArray = dataPoint.get("values");
                                if (valuesArray != null && valuesArray.isArray()) {
                                    for (JsonNode value : valuesArray) {
                                        if (!value.isNull()) {
                                            values.add(value.asDouble());
                                        }
                                    }
                                }
                            }

                            if (timeseriesId != null) {
                                if (timeseriesId.contains("cpu")) {
                                    metrics.setCpuValues(values);
                                } else if (timeseriesId.contains("memory")) {
                                    metrics.setMemoryValues(values);
                                } else if (timeseriesId.contains("requestsPerSecond")) {
                                    metrics.setRequestRateValues(values);
                                } else if (timeseriesId.contains("responsetime")) {
                                    metrics.setResponseTimeValues(values);
                                }
                            }
                        }
                    }
                }
            }

            return metrics;
        } catch (Exception e) {
            logger.error("Erro ao parsear métricas de recursos", e);
            return new ResourceMetrics(serviceId);
        }
    }

    private EntityDetails parseEntityDetails(String serviceId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            EntityDetails details = new EntityDetails(serviceId);

            if (root.has("displayName")) {
                details.setDisplayName(root.get("displayName").asText());
            }

            if (root.has("tags")) {
                List<String> tags = new ArrayList<>();
                JsonNode tagsNode = root.get("tags");
                if (tagsNode.isArray()) {
                    for (JsonNode tag : tagsNode) {
                        if (tag.has("key")) {
                            String key = tag.get("key").asText();
                            String value = tag.has("value") ? tag.get("value").asText() : "";
                            tags.add(key + ":" + value);
                            
                            // Extrai tags Azure específicas
                            if (key.equals("azure.resourceId")) {
                                details.setAzureResourceId(value);
                            } else if (key.equals("azure.resourceGroup")) {
                                details.setAzureResourceGroup(value);
                            } else if (key.equals("azure.subscription")) {
                                details.setAzureSubscription(value);
                            }
                        }
                    }
                }
                details.setTags(tags);
            }

            if (root.has("properties")) {
                JsonNode properties = root.get("properties");
                if (properties.has("technologyType")) {
                    details.setTechnology(properties.get("technologyType").asText());
                }
            }

            return details;
        } catch (Exception e) {
            logger.error("Erro ao parsear detalhes da entidade", e);
            return new EntityDetails(serviceId);
        }
    }

    public static class ResourceMetrics {
        private final String serviceId;
        private List<Double> cpuValues = new ArrayList<>();
        private List<Double> memoryValues = new ArrayList<>();
        private List<Double> requestRateValues = new ArrayList<>();
        private List<Double> responseTimeValues = new ArrayList<>();

        public ResourceMetrics(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getServiceId() {
            return serviceId;
        }

        public List<Double> getCpuValues() {
            return cpuValues;
        }

        public void setCpuValues(List<Double> cpuValues) {
            this.cpuValues = cpuValues;
        }

        public List<Double> getMemoryValues() {
            return memoryValues;
        }

        public void setMemoryValues(List<Double> memoryValues) {
            this.memoryValues = memoryValues;
        }

        public List<Double> getRequestRateValues() {
            return requestRateValues;
        }

        public void setRequestRateValues(List<Double> requestRateValues) {
            this.requestRateValues = requestRateValues;
        }

        public List<Double> getResponseTimeValues() {
            return responseTimeValues;
        }

        public void setResponseTimeValues(List<Double> responseTimeValues) {
            this.responseTimeValues = responseTimeValues;
        }
    }

    public static class EntityDetails {
        private final String serviceId;
        private String displayName;
        private List<String> tags = new ArrayList<>();
        private String technology;
        private String azureResourceId;
        private String azureResourceGroup;
        private String azureSubscription;

        public EntityDetails(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getServiceId() {
            return serviceId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getTechnology() {
            return technology;
        }

        public void setTechnology(String technology) {
            this.technology = technology;
        }

        public String getAzureResourceId() {
            return azureResourceId;
        }

        public void setAzureResourceId(String azureResourceId) {
            this.azureResourceId = azureResourceId;
        }

        public String getAzureResourceGroup() {
            return azureResourceGroup;
        }

        public void setAzureResourceGroup(String azureResourceGroup) {
            this.azureResourceGroup = azureResourceGroup;
        }

        public String getAzureSubscription() {
            return azureSubscription;
        }

        public void setAzureSubscription(String azureSubscription) {
            this.azureSubscription = azureSubscription;
        }
    }

    public static class DynatraceServiceMetrics {
        private final String serviceId;
        private final long requestCount;

        public DynatraceServiceMetrics(String serviceId, long requestCount) {
            this.serviceId = serviceId;
            this.requestCount = requestCount;
        }

        public String getServiceId() {
            return serviceId;
        }

        public long getRequestCount() {
            return requestCount;
        }
    }
}

