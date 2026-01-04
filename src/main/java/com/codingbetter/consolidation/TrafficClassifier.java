package com.codingbetter.consolidation;

import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Classificador de tráfego baseado em regras de negócio.
 * Aplica regras determinísticas para classificar status de serviços.
 */
@Component
public class TrafficClassifier {

    private static final int ACTIVE_THRESHOLD_DAYS = 7;
    private static final int LOW_USAGE_THRESHOLD_DAYS = 30;

    /**
     * Classifica um serviço baseado na última atividade observada.
     * Regras:
     * - lastSeen <= 7 dias → ACTIVE
     * - 8 <= lastSeen <= 30 dias → LOW_USAGE
     * - lastSeen > 30 dias → NO_TRAFFIC
     */
    public ServiceActivitySnapshot.Classification classify(Instant lastSeen) {
        if (lastSeen == null) {
            return ServiceActivitySnapshot.Classification.NO_TRAFFIC;
        }

        long daysSinceLastSeen = ChronoUnit.DAYS.between(lastSeen, Instant.now());

        if (daysSinceLastSeen <= ACTIVE_THRESHOLD_DAYS) {
            return ServiceActivitySnapshot.Classification.ACTIVE;
        } else if (daysSinceLastSeen <= LOW_USAGE_THRESHOLD_DAYS) {
            return ServiceActivitySnapshot.Classification.LOW_USAGE;
        } else {
            return ServiceActivitySnapshot.Classification.NO_TRAFFIC;
        }
    }
}

