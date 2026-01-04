package com.codingbetter.normalization;

import com.codingbetter.adapters.ActivityAdapter;
import com.codingbetter.adapters.github.RepositoryMetadata;
import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Normalizador de eventos raw para schema canônico.
 * Remove detalhes específicos de negócio e adiciona metadados padronizados.
 */
@Component
public class EventNormalizer {

    private static final Logger logger = LoggerFactory.getLogger(EventNormalizer.class);

    private final ObjectMapper objectMapper;
    private final EnrichmentService enrichmentService;

    public EventNormalizer(ObjectMapper objectMapper, EnrichmentService enrichmentService) {
        this.objectMapper = objectMapper;
        this.enrichmentService = enrichmentService;
    }

    /**
     * Normaliza um evento raw para ServiceActivityEvent.v1.
     */
    public ServiceActivityEvent normalize(ActivityAdapter.RawActivityEvent rawEvent) {
        try {
            logger.debug("Normalizando evento: serviceId={}, source={}", 
                    rawEvent.getServiceId(), rawEvent.getSource());

            // Extrai dados específicos da fonte
            Map<String, Object> rawData = extractRawData(rawEvent);
            
            // Cria evento normalizado
            ServiceActivityEvent event = new ServiceActivityEvent();
            event.setServiceId(rawEvent.getServiceId());
            event.setActivityCount(extractActivityCount(rawData, rawEvent.getSource()));
            event.setCallers(extractCallers(rawData, rawEvent.getSource()));
            event.setWindow(extractTimeWindow(rawData, rawEvent.getSource()));
            
            // Enriquecimento
            ServiceActivityEvent.Metadata metadata = enrichmentService.enrich(rawEvent);
            event.setMetadata(metadata);
            event.setConfidenceLevel(enrichmentService.calculateConfidence(rawEvent));
            
            // Adiciona informações do repositório se disponível
            RepositoryMetadata repoMetadata = extractRepositoryMetadata(rawData);
            if (repoMetadata != null) {
                ServiceActivityEvent.RepositoryInfo repoInfo = new ServiceActivityEvent.RepositoryInfo(
                        repoMetadata.getName(),
                        repoMetadata.getFullName(),
                        repoMetadata.getSigla(),
                        repoMetadata.getType(),
                        repoMetadata.getServiceName(),
                        repoMetadata.getHtmlUrl() != null ? repoMetadata.getHtmlUrl() : repoMetadata.getUrl()
                );
                event.setRepository(repoInfo);
                event.setDiscoverySource(ServiceActivityEvent.DiscoverySource.GITHUB);
            } else {
                event.setDiscoverySource(ServiceActivityEvent.DiscoverySource.DYNATRACE);
            }

            logger.debug("Evento normalizado: serviceId={}, activityCount={}, discoverySource={}", 
                    event.getServiceId(), event.getActivityCount(), event.getDiscoverySource());

            return event;
        } catch (Exception e) {
            logger.error("Erro ao normalizar evento: serviceId={}", rawEvent.getServiceId(), e);
            throw new RuntimeException("Erro ao normalizar evento", e);
        }
    }

    /**
     * Normaliza fluxo de eventos raw (streaming).
     */
    public Flux<ServiceActivityEvent> normalizeStream(Flux<ActivityAdapter.RawActivityEvent> rawEvents) {
        return rawEvents
                .map(this::normalize)
                .doOnError(error -> logger.error("Erro ao normalizar stream", error))
                .onErrorContinue((error, obj) -> 
                    logger.warn("Evento ignorado devido a erro de normalização", error));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractRawData(ActivityAdapter.RawActivityEvent rawEvent) {
        if (rawEvent.getRawData() instanceof Map) {
            return (Map<String, Object>) rawEvent.getRawData();
        }
        // Tenta converter para Map via JSON
        return objectMapper.convertValue(rawEvent.getRawData(), Map.class);
    }

    private Long extractActivityCount(Map<String, Object> rawData, String source) {
        // Adaptação específica por fonte
        return switch (source) {
            case "dynatrace" -> {
                Object requestCount = rawData.get("requestCount");
                yield requestCount != null ? ((Number) requestCount).longValue() : 0L;
            }
            default -> {
                Object count = rawData.get("count") != null ? rawData.get("count") : rawData.get("activityCount");
                yield count != null ? ((Number) count).longValue() : 0L;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractCallers(Map<String, Object> rawData, String source) {
        Object callers = rawData.get("callers");
        if (callers instanceof List) {
            return (List<String>) callers;
        }
        return List.of();
    }

    private ServiceActivityEvent.TimeWindow extractTimeWindow(Map<String, Object> rawData, String source) {
        // Tenta extrair window da fonte específica
        Object windowStart = rawData.get("windowStart");
        Object windowEnd = rawData.get("windowEnd");

        if (windowStart instanceof Instant start && windowEnd instanceof Instant end) {
            return new ServiceActivityEvent.TimeWindow(start, end);
        }

        // Fallback: usa timestamp atual (não ideal, mas funcional)
        Instant now = Instant.now();
        return new ServiceActivityEvent.TimeWindow(now.minusSeconds(300), now); // 5 minutos
    }

    @SuppressWarnings("unchecked")
    private RepositoryMetadata extractRepositoryMetadata(Map<String, Object> rawData) {
        Object repo = rawData.get("repository");
        if (repo instanceof RepositoryMetadata) {
            return (RepositoryMetadata) repo;
        }
        if (repo instanceof Map) {
            try {
                return objectMapper.convertValue(repo, RepositoryMetadata.class);
            } catch (Exception e) {
                logger.debug("Erro ao converter repository metadata", e);
            }
        }
        return null;
    }
}

