package com.codingbetter.backstage;

import com.codingbetter.adapters.github.GitHubClient;
import com.codingbetter.adapters.github.RepositoryMetadata;
import com.codingbetter.ownership.InferredOwner;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço para criar Pull Requests com catalog-info.yaml nos repositórios.
 * Processa em batch para respeitar rate limits da GitHub API.
 */
@Service
public class CatalogYamlPRService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogYamlPRService.class);

    private final GitHubClient githubClient;
    private final CatalogYamlGenerator yamlGenerator;

    private final Counter prsCreatedCounter;
    private final Counter prsFailedCounter;

    private final MeterRegistry meterRegistry;

    @Value("${backstage.catalog-yaml.enabled:true}")
    private boolean enabled;

    @Value("${backstage.catalog-yaml.path:.backstage/catalog-info.yaml}")
    private String yamlPath;

    @Value("${backstage.catalog-yaml.branch-prefix:backstage/catalog-info-}")
    private String branchPrefix;

    @Value("${backstage.catalog-yaml.batch-size:50}")
    private int batchSize;

    @Value("${backstage.catalog-yaml.pr-title:chore: Adicionar catalog-info.yaml para Backstage}")
    private String prTitle;

    @Value("${backstage.catalog-yaml.pr-body:Este PR adiciona o arquivo catalog-info.yaml necessário para descoberta no Backstage.\n\nDados gerados automaticamente pelo Catalog Traffic Engine.}")
    private String prBody;

    @Value("${github.organization:}")
    private String organization;

    public CatalogYamlPRService(
            GitHubClient githubClient,
            CatalogYamlGenerator yamlGenerator,
            MeterRegistry meterRegistry) {
        this.githubClient = githubClient;
        this.yamlGenerator = yamlGenerator;
        this.meterRegistry = meterRegistry;

        this.prsCreatedCounter = Counter.builder("governance_catalog_yaml_prs_created_total")
                .description("Total de PRs de catalog-info.yaml criados")
                .register(meterRegistry);

        this.prsFailedCounter = Counter.builder("governance_catalog_yaml_prs_failed_total")
                .description("Total de PRs de catalog-info.yaml que falharam")
                .register(meterRegistry);
    }

    /**
     * Cria Pull Requests com catalog-info.yaml para uma lista de repositórios.
     * Processa em batch para respeitar rate limits.
     */
    @CircuitBreaker(name = "github")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public Mono<Integer> createPRsForRepositories(List<RepositoryMetadata> repos, List<InferredOwner> owners) {
        if (!enabled) {
            logger.info("Criação de PRs de catalog-info.yaml desabilitada");
            return Mono.just(0);
        }

        logger.info("Criando PRs de catalog-info.yaml para {} repositórios", repos.size());

        AtomicInteger createdCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        return Flux.fromIterable(repos)
                .index()
                .flatMap(tuple -> {
                    long index = tuple.getT1();
                    RepositoryMetadata repo = tuple.getT2();
                    InferredOwner owner = index < owners.size() ? owners.get((int) index) : null;

                    return createPRForRepository(repo, owner)
                            .doOnSuccess(created -> {
                                if (created) {
                                    createdCount.incrementAndGet();
                                    prsCreatedCounter.increment();
                                } else {
                                    failedCount.incrementAndGet();
                                    prsFailedCounter.increment();
                                }
                            })
                            .onErrorResume(error -> {
                                logger.error("Erro ao criar PR para repositório: {}", repo.getFullName(), error);
                                failedCount.incrementAndGet();
                                prsFailedCounter.increment();
                                return Mono.just(false);
                            });
                }, 1) // Processa sequencialmente para respeitar rate limit
                .then(Mono.fromCallable(() -> {
                    logger.info("PRs criados: {}, falhas: {}", createdCount.get(), failedCount.get());
                    return createdCount.get();
                }));
    }

    /**
     * Cria um Pull Request com catalog-info.yaml para um repositório específico.
     */
    private Mono<Boolean> createPRForRepository(RepositoryMetadata repo, InferredOwner owner) {
        if (repo.getFullName() == null) {
            logger.warn("RepositoryMetadata sem fullName, pulando criação de PR");
            return Mono.just(false);
        }

        logger.debug("Criando PR de catalog-info.yaml para: {}", repo.getFullName());

        // Gera conteúdo do YAML
        String yamlContent = yamlGenerator.generateCatalogYaml(repo, owner);

        // Gera nome da branch único
        String branchName = branchPrefix + UUID.randomUUID().toString().substring(0, 8);

        // Cria arquivo na branch
        return githubClient.createFile(
                        repo.getFullName(),
                        yamlPath,
                        yamlContent,
                        branchName,
                        "chore: Adicionar catalog-info.yaml para Backstage")
                .flatMap(commitSha -> {
                    if ("unknown".equals(commitSha)) {
                        logger.warn("Falha ao criar arquivo catalog-info.yaml para: {}", repo.getFullName());
                        return Mono.just(false);
                    }

                    // Cria Pull Request
                    return githubClient.createPullRequest(
                                    repo.getFullName(),
                                    prTitle,
                                    prBody,
                                    branchName,
                                    "main") // ou "master", configurável
                            .map(prNumber -> {
                                if (!"unknown".equals(prNumber)) {
                                    logger.info("PR #{} criado com sucesso para: {}", prNumber, repo.getFullName());
                                    return true;
                                } else {
                                    logger.warn("Falha ao criar PR para: {}", repo.getFullName());
                                    return false;
                                }
                            });
                })
                .onErrorResume(error -> {
                    logger.error("Erro ao criar PR para: {}", repo.getFullName(), error);
                    return Mono.just(false);
                });
    }
}

