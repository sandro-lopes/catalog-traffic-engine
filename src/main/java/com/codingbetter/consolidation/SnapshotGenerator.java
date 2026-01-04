package com.codingbetter.consolidation;

import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gerador de snapshots que agrupa eventos por serviço.
 */
@Component
public class SnapshotGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotGenerator.class);

    private final DecisionEngine decisionEngine;

    public SnapshotGenerator(DecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    /**
     * Gera snapshots para todos os serviços a partir de lista de eventos.
     * Agrupa eventos por service.id e gera um snapshot por serviço.
     */
    public List<ServiceActivitySnapshot> generateSnapshots(List<ServiceActivityEvent> events) {
        logger.info("Gerando snapshots para {} eventos", events.size());

        // Agrupa eventos por service.id
        Map<String, List<ServiceActivityEvent>> eventsByService = events.stream()
                .collect(Collectors.groupingBy(ServiceActivityEvent::getServiceId));

        logger.info("Encontrados {} serviços únicos", eventsByService.size());

        // Gera snapshot para cada serviço
        return eventsByService.entrySet().stream()
                .map(entry -> decisionEngine.generateSnapshot(entry.getKey(), entry.getValue()))
                .toList();
    }
}

