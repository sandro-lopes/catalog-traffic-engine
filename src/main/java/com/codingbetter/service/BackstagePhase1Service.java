package com.codingbetter.service;

import com.codingbetter.adapters.github.RepositoryMetadata;
import com.codingbetter.backstage.BackstageClient;
import com.codingbetter.backstage.BackstageMapper;
import com.codingbetter.discovery.RepositoryCatalog;
import com.codingbetter.ownership.InferredOwner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para atualizar Backstage com dados da Fase 1 (descoberta e ownership).
 * Executa após descoberta de repositórios e inferência de ownership.
 */
@Service
public class BackstagePhase1Service {

    private static final Logger logger = LoggerFactory.getLogger(BackstagePhase1Service.class);

    private final RepositoryCatalog repositoryCatalog;
    private final BackstageClient backstageClient;
    private final ObjectMapper objectMapper;

    public BackstagePhase1Service(
            RepositoryCatalog repositoryCatalog,
            BackstageClient backstageClient,
            ObjectMapper objectMapper) {
        this.repositoryCatalog = repositoryCatalog;
        this.backstageClient = backstageClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Atualiza Backstage com dados de descoberta e ownership (Fase 1).
     * Executa periodicamente ou pode ser chamado manualmente.
     */
    @Scheduled(cron = "${backstage.integration.phase1.cron:0 0 3 * * ?}")  // 3 AM diariamente
    public void syncPhase1Data() {
        logger.info("Iniciando sincronização de dados Fase 1 com Backstage");

        repositoryCatalog.discoverRepositories()
                .flatMap(repos -> {
                    logger.info("Sincronizando {} repositórios com Backstage (Fase 1)", repos.size());

                    // Converte para formato Backstage Fase 1
                    Map<String, Map<String, Object>> entitiesData = new HashMap<>();

                    for (RepositoryMetadata repo : repos) {
                        // Cria InferredOwner a partir dos metadados
                        InferredOwner owner = null;
                        if (repo.getPrimaryOwner() != null) {
                            owner = new InferredOwner(
                                    repo.getPrimaryOwner(),
                                    null,
                                    null,
                                    repo.getOwnershipConfidence() != null ? repo.getOwnershipConfidence() : 0.0,
                                    repo.getOwnershipSource() != null ? repo.getOwnershipSource() : "unknown"
                            );
                        }

                        // Converte para formato Backstage
                        var phase1Data = BackstageMapper.toBackstageFormatPhase1(repo, owner, objectMapper);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> phase1Map = (Map<String, Object>) objectMapper.convertValue(phase1Data, Map.class);
                        entitiesData.put(repo.getServiceId(), phase1Map);
                    }

                    if (entitiesData.isEmpty()) {
                        logger.debug("Nenhum repositório para sincronizar");
                        return Mono.empty();
                    }

                    // Atualiza Backstage via API
                    return backstageClient.updateEntitiesBatch(entitiesData)
                            .doOnSuccess(v -> {
                                logger.info("{} repositórios sincronizados com Backstage (Fase 1)", entitiesData.size());
                            })
                            .doOnError(error -> {
                                logger.error("Erro ao sincronizar dados Fase 1 com Backstage", error);
                            });
                })
                .block();
    }

    /**
     * Executa sincronização manualmente (útil para primeira execução).
     */
    public Mono<Void> syncPhase1DataManually() {
        return repositoryCatalog.discoverRepositories()
                .flatMap(repos -> {
                    Map<String, Map<String, Object>> entitiesData = new HashMap<>();

                    for (RepositoryMetadata repo : repos) {
                        InferredOwner owner = null;
                        if (repo.getPrimaryOwner() != null) {
                            owner = new InferredOwner(
                                    repo.getPrimaryOwner(),
                                    null,
                                    null,
                                    repo.getOwnershipConfidence() != null ? repo.getOwnershipConfidence() : 0.0,
                                    repo.getOwnershipSource() != null ? repo.getOwnershipSource() : "unknown"
                            );
                        }

                        var phase1Data = BackstageMapper.toBackstageFormatPhase1(repo, owner, objectMapper);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> phase1Map = (Map<String, Object>) objectMapper.convertValue(phase1Data, Map.class);
                        entitiesData.put(repo.getServiceId(), phase1Map);
                    }

                    return backstageClient.updateEntitiesBatch(entitiesData);
                });
    }
}

