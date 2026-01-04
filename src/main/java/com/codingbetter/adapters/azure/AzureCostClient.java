package com.codingbetter.adapters.azure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente para Azure Cost Management API.
 * Obtém custos reais de recursos Azure.
 */
@Component
public class AzureCostClient {

    private static final Logger logger = LoggerFactory.getLogger(AzureCostClient.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WebClient webClient;
    private final AzureCostConfig config;
    private final ObjectMapper objectMapper;
    private final Cache<String, ResourceCost> costCache;

    public AzureCostClient(AzureCostConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getApi().getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApi().getToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.costCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24)) // Cache de 24 horas
                .maximumSize(10000)
                .build();
    }

    /**
     * Obtém custo de um recurso Azure para um período.
     */
    @CircuitBreaker(name = "azure")
    @Retry(name = "azure")
    public Mono<ResourceCost> getResourceCost(String resourceId, LocalDate startDate, LocalDate endDate) {
        String cacheKey = String.format("%s:%s:%s", resourceId, startDate, endDate);

        ResourceCost cached = costCache.getIfPresent(cacheKey);
        if (cached != null) {
            logger.debug("Retornando custo do cache para recurso: {}", resourceId);
            return Mono.just(cached);
        }

        logger.debug("Buscando custo do recurso Azure: {} ({} to {})", resourceId, startDate, endDate);

        String uri = String.format(
                "/subscriptions/%s/providers/Microsoft.CostManagement/query?api-version=2023-11-01",
                config.getSubscriptionId()
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("type", "ActualCost");
        requestBody.put("timeframe", "Custom");
        
        Map<String, String> timePeriod = new HashMap<>();
        timePeriod.put("from", startDate.format(DATE_FORMATTER));
        timePeriod.put("to", endDate.format(DATE_FORMATTER));
        requestBody.put("timePeriod", timePeriod);
        
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("granularity", "Daily");
        
        List<Map<String, Object>> filters = new ArrayList<>();
        Map<String, Object> filter = new HashMap<>();
        filter.put("field", "ResourceId");
        filter.put("operator", "In");
        filter.put("values", List.of(resourceId));
        filters.add(filter);
        dataset.put("filters", filters);
        
        requestBody.put("dataset", dataset);

        return webClient.post()
                .uri(uri)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> parseCost(resourceId, json))
                .doOnNext(cost -> {
                    costCache.put(cacheKey, cost);
                    logger.info("Custo obtido para recurso {}: ${}/mês", resourceId, cost.getMonthlyCost());
                })
                .doOnError(error -> logger.error("Erro ao obter custo do recurso: {}", resourceId, error));
    }

    /**
     * Obtém detalhes de um recurso Azure.
     */
    @CircuitBreaker(name = "azure")
    @Retry(name = "azure")
    public Mono<ResourceDetails> getResourceDetails(String resourceId) {
        logger.debug("Buscando detalhes do recurso Azure: {}", resourceId);

        return webClient.get()
                .uri(resourceId + "?api-version=2021-04-01")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> parseResourceDetails(resourceId, json))
                .doOnError(error -> logger.error("Erro ao obter detalhes do recurso: {}", resourceId, error));
    }

    private ResourceCost parseCost(String resourceId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            BigDecimal totalCost = BigDecimal.ZERO;

            if (root.has("properties") && root.get("properties").has("rows")) {
                JsonNode rows = root.get("properties").get("rows");
                if (rows.isArray()) {
                    for (JsonNode row : rows) {
                        if (row.isArray() && row.size() > 0) {
                            // Primeira coluna geralmente é o custo
                            JsonNode costNode = row.get(0);
                            if (costNode != null && !costNode.isNull()) {
                                try {
                                    BigDecimal cost = new BigDecimal(costNode.asText());
                                    totalCost = totalCost.add(cost);
                                } catch (NumberFormatException e) {
                                    logger.warn("Erro ao parsear custo: {}", costNode.asText());
                                }
                            }
                        }
                    }
                }
            }

            // Calcula custo mensal (assumindo período de 30 dias)
            BigDecimal monthlyCost = totalCost;

            return new ResourceCost(resourceId, totalCost, monthlyCost);
        } catch (Exception e) {
            logger.error("Erro ao parsear custo do recurso", e);
            return new ResourceCost(resourceId, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private ResourceDetails parseResourceDetails(String resourceId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            ResourceDetails details = new ResourceDetails(resourceId);

            if (root.has("resourceGroup")) {
                details.setResourceGroup(root.get("resourceGroup").asText());
            }

            if (root.has("subscriptionId")) {
                details.setSubscription(root.get("subscriptionId").asText());
            }

            if (root.has("type")) {
                details.setResourceType(root.get("type").asText());
            }

            if (root.has("location")) {
                details.setLocation(root.get("location").asText());
            }

            if (root.has("sku")) {
                JsonNode sku = root.get("sku");
                if (sku.has("tier")) {
                    details.setSkuTier(sku.get("tier").asText());
                }
                if (sku.has("name")) {
                    details.setSkuName(sku.get("name").asText());
                }
            }

            // Para App Services, extrair informações específicas
            if (root.has("properties")) {
                JsonNode properties = root.get("properties");
                if (properties.has("sku")) {
                    JsonNode appServiceSku = properties.get("sku");
                    if (appServiceSku.has("name")) {
                        details.setSkuName(appServiceSku.get("name").asText());
                    }
                }
            }

            return details;
        } catch (Exception e) {
            logger.error("Erro ao parsear detalhes do recurso", e);
            return new ResourceDetails(resourceId);
        }
    }

    public static class ResourceCost {
        private final String resourceId;
        private final BigDecimal totalCost;
        private final BigDecimal monthlyCost;

        public ResourceCost(String resourceId, BigDecimal totalCost, BigDecimal monthlyCost) {
            this.resourceId = resourceId;
            this.totalCost = totalCost;
            this.monthlyCost = monthlyCost;
        }

        public String getResourceId() {
            return resourceId;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public BigDecimal getMonthlyCost() {
            return monthlyCost;
        }
    }

    public static class ResourceDetails {
        private final String resourceId;
        private String resourceGroup;
        private String subscription;
        private String resourceType;
        private String skuTier;
        private String skuName;
        private String location;

        public ResourceDetails(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getResourceId() {
            return resourceId;
        }

        public String getResourceGroup() {
            return resourceGroup;
        }

        public void setResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
        }

        public String getSubscription() {
            return subscription;
        }

        public void setSubscription(String subscription) {
            this.subscription = subscription;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getSkuTier() {
            return skuTier;
        }

        public void setSkuTier(String skuTier) {
            this.skuTier = skuTier;
        }

        public String getSkuName() {
            return skuName;
        }

        public void setSkuName(String skuName) {
            this.skuName = skuName;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}

