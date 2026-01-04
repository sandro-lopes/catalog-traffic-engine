package com.codingbetter.backstage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Cliente HTTP para integração com Backstage.
 * Atualiza entidades de forma read-only com dados do Catalog Traffic Engine.
 */
@Component
public class BackstageClient {

    private static final Logger logger = LoggerFactory.getLogger(BackstageClient.class);

    private final WebClient webClient;
    private final BackstageConfig config;
    private final ObjectMapper objectMapper;

    public BackstageClient(BackstageConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getApi().getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApi().getToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Atualiza uma entidade no Backstage com dados do Catalog Traffic Engine.
     */
    @CircuitBreaker(name = "backstage")
    @Retry(name = "backstage")
    public Mono<Void> updateEntity(String entityName, Map<String, Object> governanceData) {
        logger.debug("Atualizando entidade no Backstage: {}", entityName);

        try {
            String json = objectMapper.writeValueAsString(governanceData);

            return webClient.patch()
                    .uri("/api/catalog/entities/by-name/{kind}/{namespace}/{name}",
                            "Component", "default", entityName)
                    .bodyValue(json)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                    .doOnSuccess(v -> logger.debug("Entidade atualizada: {}", entityName))
                    .doOnError(error -> logger.error("Erro ao atualizar entidade: {}", entityName, error));
        } catch (Exception e) {
            logger.error("Erro ao serializar dados para Backstage: {}", entityName, e);
            return Mono.error(e);
        }
    }

    /**
     * Atualiza múltiplas entidades em batch.
     */
    @CircuitBreaker(name = "backstage")
    @Retry(name = "backstage")
    public Mono<Void> updateEntitiesBatch(Map<String, Map<String, Object>> entitiesData) {
        logger.info("Atualizando {} entidades no Backstage (batch)", entitiesData.size());

        try {
            String json = objectMapper.writeValueAsString(entitiesData);

            return webClient.post()
                    .uri("/api/catalog/entities/batch")
                    .bodyValue(json)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                    .doOnSuccess(v -> logger.info("Batch de {} entidades atualizado", entitiesData.size()))
                    .doOnError(error -> logger.error("Erro ao atualizar batch de entidades", error));
        } catch (Exception e) {
            logger.error("Erro ao serializar batch para Backstage", e);
            return Mono.error(e);
        }
    }
}

