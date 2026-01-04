package com.codingbetter.normalization;

import com.codingbetter.adapters.ActivityAdapter;
import com.codingbetter.schemas.v1.ServiceActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Serviço de enriquecimento de eventos.
 * Adiciona metadados padronizados e calcula nível de confiança.
 */
@Service
public class EnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentService.class);

    @Value("${aggregation.temporal-window-minutes:5}")
    private int temporalWindowMinutes;

    @Value("${spring.application.name:catalog-traffic-engine}")
    private String applicationName;

    /**
     * Enriquece evento com metadados padronizados.
     */
    public ServiceActivityEvent.Metadata enrich(ActivityAdapter.RawActivityEvent rawEvent) {
        String environment = extractEnvironment(rawEvent);
        String source = rawEvent.getSource();

        return new ServiceActivityEvent.Metadata(environment, source);
    }

    /**
     * Calcula nível de confiança baseado na fonte e qualidade dos dados.
     */
    public ServiceActivityEvent.ConfidenceLevel calculateConfidence(ActivityAdapter.RawActivityEvent rawEvent) {
        // Lógica de cálculo de confiança
        return switch (rawEvent.getSource()) {
            case "dynatrace" -> ServiceActivityEvent.ConfidenceLevel.HIGH; // Dynatrace é fonte primária
            case "elastic" -> ServiceActivityEvent.ConfidenceLevel.MEDIUM; // Logs podem ter gaps
            case "apigateway" -> ServiceActivityEvent.ConfidenceLevel.MEDIUM;
            default -> ServiceActivityEvent.ConfidenceLevel.LOW;
        };
    }

    private String extractEnvironment(ActivityAdapter.RawActivityEvent rawEvent) {
        // Em produção, extrair de variáveis de ambiente ou configuração
        return System.getenv().getOrDefault("ENVIRONMENT", "production");
    }
}

