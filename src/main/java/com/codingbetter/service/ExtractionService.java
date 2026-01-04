package com.codingbetter.service;

import com.codingbetter.kafka.KafkaProducer;
import com.codingbetter.normalization.EventNormalizer;
import com.codingbetter.normalization.TemporalAggregator;
import com.codingbetter.orchestration.ExtractionOrchestrator;
import com.codingbetter.schemas.v1.ServiceActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Serviço principal de extração que orquestra todo o pipeline ETL.
 * Executa periodicamente para extrair, normalizar, agregar e publicar eventos.
 */
@Service
public class ExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(ExtractionService.class);

    private final ExtractionOrchestrator orchestrator;
    private final EventNormalizer normalizer;
    private final TemporalAggregator aggregator;
    private final KafkaProducer kafkaProducer;

    public ExtractionService(
            ExtractionOrchestrator orchestrator,
            EventNormalizer normalizer,
            TemporalAggregator aggregator,
            KafkaProducer kafkaProducer) {
        this.orchestrator = orchestrator;
        this.normalizer = normalizer;
        this.aggregator = aggregator;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * Executa extração completa do pipeline ETL.
     * Agendado para executar a cada 5 minutos.
     */
    @Scheduled(fixedRateString = "${extraction.interval-minutes:5}", initialDelay = 60000)
    public void executeExtraction() {
        logger.info("Iniciando execução de extração");

        try {
            // Define janela de tempo (últimos 5 minutos)
            Instant end = Instant.now();
            Instant start = end.minus(5, ChronoUnit.MINUTES);
            var window = new com.codingbetter.adapters.ActivityAdapter.TimeWindow(start, end);

            // Pipeline completo: Extração -> Normalização -> Agregação -> Publicação
            orchestrator.orchestrateExtraction(window)
                    .transform(normalizer::normalizeStream)
                    .transform(aggregator::aggregate)
                    .flatMap(event -> {
                        kafkaProducer.publishActivityEvent(event);
                        return Flux.just(event);
                    })
                    .doOnComplete(() -> logger.info("Extração concluída com sucesso"))
                    .doOnError(error -> logger.error("Erro na extração", error))
                    .blockLast(); // Em produção, considerar processamento assíncrono contínuo

        } catch (Exception e) {
            logger.error("Erro crítico na execução de extração", e);
        }
    }

    /**
     * Executa extração manual (útil para testes ou execução sob demanda).
     */
    public void executeExtractionManual(Instant start, Instant end) {
        logger.info("Executando extração manual: {} to {}", start, end);

        var window = new com.codingbetter.adapters.ActivityAdapter.TimeWindow(start, end);

        orchestrator.orchestrateExtraction(window)
                .transform(normalizer::normalizeStream)
                .transform(aggregator::aggregate)
                .flatMap(event -> {
                    kafkaProducer.publishActivityEvent(event);
                    return Flux.just(event);
                })
                .doOnComplete(() -> logger.info("Extração manual concluída"))
                .doOnError(error -> logger.error("Erro na extração manual", error))
                .blockLast();
    }
}

