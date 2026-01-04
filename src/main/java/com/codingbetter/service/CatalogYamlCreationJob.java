package com.codingbetter.service;

import com.codingbetter.backstage.CatalogYamlPRService;
import com.codingbetter.discovery.RepositoryCatalog;
import com.codingbetter.ownership.InferredOwner;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Job para criar Pull Requests com catalog-info.yaml nos repositórios.
 * Executa uma vez (ou periodicamente se necessário reprocessar).
 */
@Component
public class CatalogYamlCreationJob {

    private static final Logger logger = LoggerFactory.getLogger(CatalogYamlCreationJob.class);

    private final RepositoryCatalog repositoryCatalog;
    private final CatalogYamlPRService prService;
    private final MeterRegistry meterRegistry;

    private final Counter jobExecutionsCounter;

    @Value("${backstage.catalog-yaml.enabled:true}")
    private boolean enabled;

    @Value("${backstage.catalog-yaml.job.enabled:true}")
    private boolean jobEnabled;

    @Value("${backstage.catalog-yaml.job.cron:0 0 2 * * ?}")  // 2 AM diariamente (opcional)
    private String cronExpression;

    public CatalogYamlCreationJob(
            RepositoryCatalog repositoryCatalog,
            CatalogYamlPRService prService,
            MeterRegistry meterRegistry) {
        this.repositoryCatalog = repositoryCatalog;
        this.prService = prService;
        this.meterRegistry = meterRegistry;

        this.jobExecutionsCounter = Counter.builder("governance_catalog_yaml_job_executions_total")
                .description("Total de execuções do job de criação de catalog-info.yaml")
                .register(meterRegistry);
    }

    /**
     * Executa criação de PRs com catalog-info.yaml.
     * Pode ser executado manualmente ou via scheduler.
     */
    @Scheduled(cron = "${backstage.catalog-yaml.job.cron:0 0 2 * * ?}")
    public void execute() {
        if (!enabled || !jobEnabled) {
            logger.debug("Job de criação de catalog-info.yaml desabilitado");
            return;
        }

        logger.info("Iniciando job de criação de catalog-info.yaml");

        jobExecutionsCounter.increment();

        repositoryCatalog.discoverRepositories()
                .flatMap(repos -> {
                    logger.info("Processando {} repositórios para criação de catalog-info.yaml", repos.size());

                    // Extrai owners inferidos dos metadados
                    List<InferredOwner> owners = repos.stream()
                            .map(repo -> {
                                if (repo.getPrimaryOwner() != null) {
                                    return new InferredOwner(
                                            repo.getPrimaryOwner(),
                                            null,
                                            null,
                                            repo.getOwnershipConfidence() != null ? repo.getOwnershipConfidence() : 0.0,
                                            repo.getOwnershipSource() != null ? repo.getOwnershipSource() : "unknown"
                                    );
                                }
                                return InferredOwner.unknown();
                            })
                            .collect(Collectors.toList());

                    return prService.createPRsForRepositories(repos, owners);
                })
                .doOnSuccess(count -> {
                    logger.info("Job de criação de catalog-info.yaml concluído: {} PRs criados", count);
                })
                .doOnError(error -> {
                    logger.error("Erro ao executar job de criação de catalog-info.yaml", error);
                })
                .block();
    }

    /**
     * Executa criação de PRs manualmente (útil para primeira execução).
     */
    public Mono<Integer> executeManually() {
        logger.info("Executando criação de catalog-info.yaml manualmente");
        return repositoryCatalog.discoverRepositories()
                .flatMap(repos -> {
                    List<InferredOwner> owners = repos.stream()
                            .map(repo -> {
                                if (repo.getPrimaryOwner() != null) {
                                    return new InferredOwner(
                                            repo.getPrimaryOwner(),
                                            null,
                                            null,
                                            repo.getOwnershipConfidence() != null ? repo.getOwnershipConfidence() : 0.0,
                                            repo.getOwnershipSource() != null ? repo.getOwnershipSource() : "unknown"
                                    );
                                }
                                return InferredOwner.unknown();
                            })
                            .collect(Collectors.toList());

                    return prService.createPRsForRepositories(repos, owners);
                });
    }
}

