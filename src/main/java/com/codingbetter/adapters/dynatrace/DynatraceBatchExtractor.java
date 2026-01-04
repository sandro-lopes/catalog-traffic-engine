package com.codingbetter.adapters.dynatrace;

import com.codingbetter.adapters.ActivityAdapter;
import com.codingbetter.adapters.github.RepositoryMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Extrator em batch para processar múltiplos serviços em paralelo.
 * Respeita rate limiting e processa em batches configuráveis.
 */
@Component
public class DynatraceBatchExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DynatraceBatchExtractor.class);

    private final DynatraceClient dynatraceClient;
    private final DynatraceConfig config;

    public DynatraceBatchExtractor(DynatraceClient dynatraceClient, DynatraceConfig config) {
        this.dynatraceClient = dynatraceClient;
        this.config = config;
    }

    /**
     * Extrai métricas para uma lista de serviços em batches paralelos.
     * @param serviceIds Lista de IDs de serviços
     * @param window Janela de tempo
     * @return Flux de eventos raw
     */
    public Flux<ActivityAdapter.RawActivityEvent> extractBatch(
            List<String> serviceIds,
            ActivityAdapter.TimeWindow window) {
        return extractBatch(serviceIds, window, null);
    }

    /**
     * Extrai métricas para uma lista de serviços com informações de repositório.
     * @param serviceIds Lista de IDs de serviços
     * @param window Janela de tempo
     * @param repositories Mapa de serviceId -> RepositoryMetadata (opcional)
     * @return Flux de eventos raw
     */
    public Flux<ActivityAdapter.RawActivityEvent> extractBatch(
            List<String> serviceIds,
            ActivityAdapter.TimeWindow window,
            Map<String, RepositoryMetadata> repositories) {

        int batchSize = config.getExtraction().getBatchSize();
        AtomicInteger processed = new AtomicInteger(0);
        int total = serviceIds.size();

        logger.info("Iniciando extração em batch: {} serviços, batch size: {}", total, batchSize);

        return Flux.fromIterable(serviceIds)
                .buffer(batchSize) // Divide em batches
                .flatMap(batch -> processBatch(batch, window, repositories, processed, total), config.getExtraction().getMaxWorkers())
                .doOnComplete(() -> logger.info("Extração em batch concluída: {}/{} serviços processados", processed.get(), total));
    }

    private Flux<ActivityAdapter.RawActivityEvent> processBatch(
            List<String> batch,
            ActivityAdapter.TimeWindow window,
            Map<String, RepositoryMetadata> repositories,
            AtomicInteger processed,
            int total) {

        long startTime = window.getStart().toEpochMilli();
        long endTime = window.getEnd().toEpochMilli();

        return Flux.fromIterable(batch)
                .flatMap(serviceId -> {
                    // Captura serviceId para usar nos callbacks
                    final String currentServiceId = serviceId;
                    logger.debug("Processando serviço: {}", currentServiceId);
                    
                    RepositoryMetadata repoMetadata = repositories != null ? repositories.get(currentServiceId) : null;
                    
                    return dynatraceClient.getServiceMetrics(currentServiceId, startTime, endTime)
                            .flatMap(metrics -> dynatraceClient.getServiceCallers(currentServiceId, startTime, endTime)
                                    .map(callers -> createRawEvent(currentServiceId, metrics, callers, window, repoMetadata)))
                            .subscribeOn(Schedulers.parallel())
                            .doOnNext(event -> {
                                int count = processed.incrementAndGet();
                                if (count % 10 == 0) {
                                    logger.info("Progresso: {}/{} serviços processados", count, total);
                                }
                            })
                            .onErrorResume(error -> {
                                logger.error("Erro ao processar serviço: {}", currentServiceId, error);
                                return Mono.empty(); // Continua processando outros serviços
                            });
                }, config.getExtraction().getMaxWorkers()); // Paralelismo dentro do batch
    }

    private ActivityAdapter.RawActivityEvent createRawEvent(
            String serviceId,
            DynatraceClient.DynatraceServiceMetrics metrics,
            List<String> callers,
            ActivityAdapter.TimeWindow window,
            RepositoryMetadata repositoryMetadata) {

        Map<String, Object> rawDataMap = new HashMap<>();
        rawDataMap.put("requestCount", metrics.getRequestCount());
        rawDataMap.put("callers", callers);
        rawDataMap.put("windowStart", window.getStart());
        rawDataMap.put("windowEnd", window.getEnd());
        
        // Adiciona informações do repositório se disponível
        if (repositoryMetadata != null) {
            rawDataMap.put("repository", repositoryMetadata);
        }

        DynatraceRawData rawData = new DynatraceRawData(
                metrics.getRequestCount(),
                callers,
                window.getStart(),
                window.getEnd(),
                repositoryMetadata
        );

        return new ActivityAdapter.RawActivityEvent(serviceId, rawData, "dynatrace");
    }

    private static class DynatraceRawData {
        private final long requestCount;
        private final List<String> callers;
        private final Instant windowStart;
        private final Instant windowEnd;
        private final RepositoryMetadata repositoryMetadata;

        public DynatraceRawData(long requestCount, List<String> callers, Instant windowStart, Instant windowEnd, RepositoryMetadata repositoryMetadata) {
            this.requestCount = requestCount;
            this.callers = callers;
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
            this.repositoryMetadata = repositoryMetadata;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public List<String> getCallers() {
            return callers;
        }

        public Instant getWindowStart() {
            return windowStart;
        }

        public Instant getWindowEnd() {
            return windowEnd;
        }

        public RepositoryMetadata getRepositoryMetadata() {
            return repositoryMetadata;
        }
    }
}

