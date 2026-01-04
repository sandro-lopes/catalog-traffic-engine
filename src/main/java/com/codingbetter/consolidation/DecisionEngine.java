package com.codingbetter.consolidation;

import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor de decisão que gera snapshots consolidados a partir de eventos.
 * Único ponto de decisão de status (seguindo princípio de separação de responsabilidades).
 */
@Component
public class DecisionEngine {

    private static final Logger logger = LoggerFactory.getLogger(DecisionEngine.class);

    private final TrafficClassifier trafficClassifier;

    public DecisionEngine(TrafficClassifier trafficClassifier) {
        this.trafficClassifier = trafficClassifier;
    }

    /**
     * Gera snapshot consolidado a partir de lista de eventos de um serviço.
     * Processamento determinístico e idempotente.
     */
    public ServiceActivitySnapshot generateSnapshot(String serviceId, List<ServiceActivityEvent> events) {
        logger.debug("Gerando snapshot para serviço: {} ({} eventos)", serviceId, events.size());

        if (events.isEmpty()) {
            return createEmptySnapshot(serviceId);
        }

        // Calcula métricas consolidadas
        long totalActivity = events.stream()
                .mapToLong(ServiceActivityEvent::getActivityCount)
                .sum();

        Instant lastSeen = events.stream()
                .map(e -> e.getWindow().getEnd())
                .max(Instant::compareTo)
                .orElse(Instant.now());

        // Consolida callers (sem duplicatas)
        Set<String> allCallers = events.stream()
                .filter(e -> e.getCallers() != null)
                .flatMap(e -> e.getCallers().stream())
                .collect(Collectors.toSet());

        // Determina confiança (usa o mais alto)
        ServiceActivityEvent.ConfidenceLevel maxConfidence = events.stream()
                .map(ServiceActivityEvent::getConfidenceLevel)
                .max((a, b) -> Integer.compare(a.ordinal(), b.ordinal()))
                .orElse(ServiceActivityEvent.ConfidenceLevel.LOW);

        // Classifica tráfego
        ServiceActivitySnapshot.Classification classification = trafficClassifier.classify(lastSeen);

        // Cria snapshot
        ServiceActivitySnapshot snapshot = new ServiceActivitySnapshot();
        snapshot.setServiceId(serviceId);
        snapshot.setReceivesTraffic(totalActivity > 0);
        snapshot.setTrafficVolume(totalActivity);
        snapshot.setLastSeen(lastSeen);
        snapshot.setActiveCallers(List.copyOf(allCallers));
        snapshot.setConfidenceLevel(maxConfidence);
        snapshot.setClassification(classification);
        snapshot.setSnapshotDate(LocalDate.now());

        logger.debug("Snapshot gerado: serviceId={}, classification={}, trafficVolume={}",
                serviceId, classification, totalActivity);

        return snapshot;
    }

    private ServiceActivitySnapshot createEmptySnapshot(String serviceId) {
        ServiceActivitySnapshot snapshot = new ServiceActivitySnapshot();
        snapshot.setServiceId(serviceId);
        snapshot.setReceivesTraffic(false);
        snapshot.setTrafficVolume(0L);
        snapshot.setLastSeen(Instant.EPOCH);
        snapshot.setActiveCallers(List.of());
        snapshot.setConfidenceLevel(ServiceActivityEvent.ConfidenceLevel.LOW);
        snapshot.setClassification(ServiceActivitySnapshot.Classification.NO_TRAFFIC);
        snapshot.setSnapshotDate(LocalDate.now());
        return snapshot;
    }
}

