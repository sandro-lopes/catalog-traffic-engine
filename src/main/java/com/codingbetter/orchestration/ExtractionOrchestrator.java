package com.codingbetter.orchestration;

import com.codingbetter.adapters.ActivityAdapter;
import com.codingbetter.adapters.dynatrace.DynatraceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orquestrador de extração distribuída.
 * Coordena múltiplos adapters e distribui trabalho entre workers.
 */
@Component
public class ExtractionOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ExtractionOrchestrator.class);

    private final List<ActivityAdapter> adapters;
    private final ServicePartitioner partitioner;

    public ExtractionOrchestrator(List<ActivityAdapter> adapters) {
        this.adapters = adapters;
        this.partitioner = new ServicePartitioner();
        logger.info("ExtractionOrchestrator inicializado com {} adapters", adapters.size());
    }

    /**
     * Orquestra extração de todos os adapters configurados.
     * @param window Janela de tempo para extração
     * @return Flux de eventos raw de todas as fontes
     */
    public Flux<ActivityAdapter.RawActivityEvent> orchestrateExtraction(ActivityAdapter.TimeWindow window) {
        logger.info("Iniciando orquestração de extração: window={} to {}", window.getStart(), window.getEnd());

        AtomicInteger adapterCount = new AtomicInteger(0);

        return Flux.fromIterable(adapters)
                .flatMap(adapter -> {
                    int count = adapterCount.incrementAndGet();
                    logger.info("Processando adapter {}/{}: {}", count, adapters.size(), adapter.getMetadata().getName());

                    try {
                        if (adapter instanceof DynatraceAdapter dynatraceAdapter) {
                            // Usa versão reativa para melhor performance
                            return dynatraceAdapter.extractReactive(window)
                                    .doOnError(error -> logger.error("Erro no adapter {}", adapter.getMetadata().getName(), error))
                                    .onErrorResume(error -> Flux.empty()); // Continua com outros adapters
                        } else {
                            // Para outros adapters, usa método padrão
                            List<ActivityAdapter.RawActivityEvent> events = adapter.extract(window);
                            return Flux.fromIterable(events != null ? events : new ArrayList<>())
                                    .doOnError(error -> logger.error("Erro no adapter {}", adapter.getMetadata().getName(), error))
                                    .onErrorResume(error -> Flux.empty());
                        }
                    } catch (Exception e) {
                        logger.error("Erro ao processar adapter: {}", adapter.getMetadata().getName(), e);
                        return Flux.empty(); // Continua com outros adapters
                    }
                })
                .doOnComplete(() -> logger.info("Orquestração de extração concluída"))
                .doOnError(error -> logger.error("Erro na orquestração de extração", error));
    }

    /**
     * Retorna lista de adapters configurados.
     */
    public List<ActivityAdapter> getAdapters() {
        return new ArrayList<>(adapters);
    }

    /**
     * Adiciona um novo adapter dinamicamente.
     */
    public void addAdapter(ActivityAdapter adapter) {
        adapters.add(adapter);
        logger.info("Adapter adicionado: {}", adapter.getMetadata().getName());
    }
}

