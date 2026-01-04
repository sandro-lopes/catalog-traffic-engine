package com.codingbetter.finops;

import com.codingbetter.discovery.RepositoryCatalog;
import com.codingbetter.kafka.KafkaProducer;
import com.codingbetter.schemas.v1.CostOptimizationRecommendation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Job semanal de análise FinOps.
 * Coleta métricas, gera recomendações e publica no Kafka.
 */
@Component
public class FinOpsAnalysisJob {

    private static final Logger logger = LoggerFactory.getLogger(FinOpsAnalysisJob.class);

    private final RepositoryCatalog repositoryCatalog;
    private final OptimizationRecommender optimizationRecommender;
    private final EconomyReportGenerator reportGenerator;
    private final KafkaProducer kafkaProducer;
    private final MeterRegistry meterRegistry;

    // Métricas Prometheus
    private final Counter servicesAnalyzedCounter;
    private final Counter opportunitiesCounter;

    public FinOpsAnalysisJob(
            RepositoryCatalog repositoryCatalog,
            OptimizationRecommender optimizationRecommender,
            EconomyReportGenerator reportGenerator,
            KafkaProducer kafkaProducer,
            MeterRegistry meterRegistry) {
        this.repositoryCatalog = repositoryCatalog;
        this.optimizationRecommender = optimizationRecommender;
        this.reportGenerator = reportGenerator;
        this.kafkaProducer = kafkaProducer;
        this.meterRegistry = meterRegistry;

        // Inicializa métricas
        this.servicesAnalyzedCounter = Counter.builder("finops_services_analyzed_total")
                .description("Total de serviços analisados pelo job FinOps")
                .register(meterRegistry);

        this.opportunitiesCounter = Counter.builder("finops_optimization_opportunities_total")
                .description("Total de oportunidades de otimização identificadas")
                .register(meterRegistry);
    }

    /**
     * Executa análise FinOps semanalmente.
     * Cron: segunda-feira 3 AM
     */
    @Scheduled(cron = "${finops.job.cron:0 3 * * 1}")
    public void executeAnalysis() {
        logger.info("Iniciando job de análise FinOps");

        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(30, ChronoUnit.DAYS); // Últimos 30 dias

        AtomicInteger analyzedCount = new AtomicInteger(0);
        AtomicInteger opportunitiesCount = new AtomicInteger(0);

        // 1. Descobre todos os serviços
        repositoryCatalog.discoverRepositories()
                .flatMapMany(repos -> {
                    logger.info("Analisando {} serviços para FinOps", repos.size());
                    return Flux.fromIterable(repos);
                })
                .flatMap(repo -> {
                    String serviceId = repo.getServiceId();
                    logger.debug("Analisando FinOps para serviço: {}", serviceId);

                    return optimizationRecommender.generateRecommendation(serviceId, startTime, endTime)
                            .doOnNext(recommendation -> {
                                analyzedCount.incrementAndGet();
                                if (recommendation.getType() != null) {
                                    opportunitiesCount.incrementAndGet();
                                }
                            })
                            .doOnError(error -> logger.error("Erro ao analisar serviço: {}", serviceId, error))
                            .onErrorResume(error -> Mono.empty()); // Continua com outros serviços
                }, 10) // Paralelismo: 10 serviços por vez
                .collectList()
                .doOnNext(recommendations -> {
                    logger.info("Análise FinOps concluída: {} serviços analisados, {} oportunidades identificadas",
                            analyzedCount.get(), opportunitiesCount.get());

                    // Atualiza métricas
                    servicesAnalyzedCounter.increment(analyzedCount.get());
                    opportunitiesCounter.increment(opportunitiesCount.get());

                    // Gera relatório
                    EconomyReportGenerator.EconomyReport report = reportGenerator.generateReport(
                            endTime, recommendations);

                    // Atualiza gauges de economia (registra valores diretamente)
                    io.micrometer.core.instrument.Gauge.builder("finops_potential_monthly_savings_usd",
                            () -> report.getTotalMonthlySavings().doubleValue())
                            .description("Economia potencial mensal total (USD)")
                            .register(meterRegistry);
                    io.micrometer.core.instrument.Gauge.builder("finops_potential_annual_savings_usd",
                            () -> report.getTotalAnnualSavings().doubleValue())
                            .description("Economia potencial anual total (USD)")
                            .register(meterRegistry);

                    // Publica recomendações no Kafka
                    publishRecommendations(recommendations);

                    // Exporta relatório (opcional: salvar em arquivo ou banco)
                    String jsonReport = reportGenerator.exportToJson(report);
                    logger.info("Relatório FinOps gerado: {}", jsonReport);
                })
                .block();

        logger.info("Job de análise FinOps concluído");
    }

    private void publishRecommendations(List<CostOptimizationRecommendation> recommendations) {
        logger.info("Publicando {} recomendações no Kafka", recommendations.size());

        for (CostOptimizationRecommendation recommendation : recommendations) {
            if (recommendation.getType() == null) {
                continue; // Ignora recomendações sem tipo
            }

            try {
                String message = kafkaProducer.getObjectMapper().writeValueAsString(recommendation);
                kafkaProducer.send("governance.finops.recommendations", recommendation.getServiceId(), message);
                
                // Atualiza métrica por tipo
                meterRegistry.counter("finops_recommendations_by_type",
                        Tags.of("type", recommendation.getType().name()))
                        .increment();
            } catch (Exception e) {
                logger.error("Erro ao publicar recomendação no Kafka: {}", recommendation.getServiceId(), e);
            }
        }
    }
}

