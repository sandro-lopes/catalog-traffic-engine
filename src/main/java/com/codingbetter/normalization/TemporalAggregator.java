package com.codingbetter.normalization;

import com.codingbetter.schemas.v1.ServiceActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agregador temporal de eventos.
 * Agrega eventos em janelas de tempo configuráveis (padrão: 5 minutos).
 * Mantém determinismo através de ordenação.
 */
@Component
public class TemporalAggregator {

    private static final Logger logger = LoggerFactory.getLogger(TemporalAggregator.class);

    @Value("${aggregation.temporal-window-minutes:5}")
    private int windowMinutes;

    /**
     * Agrega eventos em janelas temporais.
     * Eventos são agrupados por service.id e janela de tempo.
     */
    public Flux<ServiceActivityEvent> aggregate(Flux<ServiceActivityEvent> events) {
        logger.debug("Iniciando agregação temporal: window={} minutos", windowMinutes);

        return events
                .collectList()
                .flatMapMany(eventList -> {
                    // Ordena por timestamp para garantir determinismo
                    eventList.sort(Comparator.comparing(e -> e.getWindow().getStart()));

                    // Agrupa por service.id e janela de tempo
                    Map<String, List<ServiceActivityEvent>> grouped = eventList.stream()
                            .collect(Collectors.groupingBy(
                                    event -> createGroupKey(event),
                                    Collectors.toList()
                            ));

                    // Agrega cada grupo
                    List<ServiceActivityEvent> aggregated = grouped.values().stream()
                            .map(this::aggregateGroup)
                            .toList();

                    logger.info("Agregação concluída: {} eventos -> {} agregados", 
                            eventList.size(), aggregated.size());

                    return Flux.fromIterable(aggregated);
                });
    }

    /**
     * Cria chave de agrupamento: serviceId + janela de tempo arredondada.
     */
    private String createGroupKey(ServiceActivityEvent event) {
        Instant windowStart = event.getWindow().getStart();
        Instant roundedStart = roundToWindow(windowStart);
        return event.getServiceId() + ":" + roundedStart.toEpochMilli();
    }

    /**
     * Arredonda timestamp para início da janela.
     */
    private Instant roundToWindow(Instant timestamp) {
        long minutes = timestamp.getEpochSecond() / 60;
        long roundedMinutes = (minutes / windowMinutes) * windowMinutes;
        return Instant.ofEpochSecond(roundedMinutes * 60);
    }

    /**
     * Agrega um grupo de eventos do mesmo serviço na mesma janela.
     */
    private ServiceActivityEvent aggregateGroup(List<ServiceActivityEvent> group) {
        if (group.isEmpty()) {
            throw new IllegalArgumentException("Grupo vazio não pode ser agregado");
        }

        ServiceActivityEvent first = group.get(0);
        ServiceActivityEvent aggregated = new ServiceActivityEvent();

        // Service ID (todos são iguais no grupo)
        aggregated.setServiceId(first.getServiceId());

        // Soma contadores de atividade
        Long totalActivity = group.stream()
                .mapToLong(ServiceActivityEvent::getActivityCount)
                .sum();
        aggregated.setActivityCount(totalActivity);

        // Consolida lista de callers (sem duplicatas)
        Set<String> allCallers = group.stream()
                .filter(e -> e.getCallers() != null)
                .flatMap(e -> e.getCallers().stream())
                .collect(Collectors.toSet());
        aggregated.setCallers(new ArrayList<>(allCallers));

        // Janela de tempo (início do primeiro, fim do último)
        Instant windowStart = group.stream()
                .map(e -> e.getWindow().getStart())
                .min(Instant::compareTo)
                .orElse(first.getWindow().getStart());
        Instant windowEnd = group.stream()
                .map(e -> e.getWindow().getEnd())
                .max(Instant::compareTo)
                .orElse(first.getWindow().getEnd());
        aggregated.setWindow(new ServiceActivityEvent.TimeWindow(windowStart, windowEnd));

        // Confiança: usa o mais alto do grupo
        ServiceActivityEvent.ConfidenceLevel maxConfidence = group.stream()
                .map(ServiceActivityEvent::getConfidenceLevel)
                .max(Comparator.comparing(Enum::ordinal))
                .orElse(ServiceActivityEvent.ConfidenceLevel.LOW);
        aggregated.setConfidenceLevel(maxConfidence);

        // Metadados: usa do primeiro evento
        aggregated.setMetadata(first.getMetadata());

        return aggregated;
    }
}

