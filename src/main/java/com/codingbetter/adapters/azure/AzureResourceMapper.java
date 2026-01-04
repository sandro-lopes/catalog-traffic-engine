package com.codingbetter.adapters.azure;

import com.codingbetter.adapters.dynatrace.DynatraceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mapeia serviceId do Dynatrace para Azure ResourceId.
 * Usa tags do Dynatrace ou mapeamento manual via configuração.
 */
@Component
public class AzureResourceMapper {

    private static final Logger logger = LoggerFactory.getLogger(AzureResourceMapper.class);

    private final DynatraceClient dynatraceClient;
    private final Map<String, String> manualMapping; // serviceId -> Azure ResourceId

    public AzureResourceMapper(DynatraceClient dynatraceClient) {
        this.dynatraceClient = dynatraceClient;
        this.manualMapping = new HashMap<>();
        // Em produção, carregar de configuração ou banco de dados
    }

    /**
     * Mapeia serviceId para Azure ResourceId.
     * Prioridade:
     * 1. Tags do Dynatrace (azure.resourceId)
     * 2. Mapeamento manual
     * 3. null se não encontrado
     */
    public Optional<String> mapToAzureResourceId(String serviceId) {
        // Tenta obter das tags do Dynatrace
        return dynatraceClient.getServiceEntityDetails(serviceId)
                .map(entity -> {
                    if (entity.getAzureResourceId() != null && !entity.getAzureResourceId().isEmpty()) {
                        logger.debug("Azure ResourceId encontrado via tags Dynatrace: {} -> {}", 
                                serviceId, entity.getAzureResourceId());
                        return Optional.of(entity.getAzureResourceId());
                    }
                    return Optional.<String>empty();
                })
                .blockOptional()
                .orElseGet(() -> {
                    // Fallback: mapeamento manual
                    String resourceId = manualMapping.get(serviceId);
                    if (resourceId != null) {
                        logger.debug("Azure ResourceId encontrado via mapeamento manual: {} -> {}", 
                                serviceId, resourceId);
                        return Optional.of(resourceId);
                    }
                    logger.warn("Azure ResourceId não encontrado para serviço: {}", serviceId);
                    return Optional.empty();
                });
    }

    /**
     * Adiciona mapeamento manual (útil para serviços sem tags).
     */
    public void addManualMapping(String serviceId, String azureResourceId) {
        manualMapping.put(serviceId, azureResourceId);
        logger.info("Mapeamento manual adicionado: {} -> {}", serviceId, azureResourceId);
    }
}

