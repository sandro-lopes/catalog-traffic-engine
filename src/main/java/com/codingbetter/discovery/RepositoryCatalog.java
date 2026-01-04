package com.codingbetter.discovery;

import com.codingbetter.adapters.github.GitHubClient;
import com.codingbetter.adapters.github.GitHubConfig;
import com.codingbetter.adapters.github.RepositoryMetadata;
import com.codingbetter.adapters.github.RepositoryNameParser;
import com.codingbetter.ownership.CommitterInfo;
import com.codingbetter.ownership.CommitHistoryStrategy;
import com.codingbetter.ownership.InferredOwner;
import com.codingbetter.ownership.OwnerInferenceEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogo de repositórios descobertos via GitHub.
 * Aplica filtros (sigla, tipo, tags) e retorna metadados normalizados.
 */
@Component
public class RepositoryCatalog {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryCatalog.class);

    private final GitHubClient githubClient;
    private final GitHubConfig config;
    private final RepositoryNameParser nameParser;
    private final OwnerInferenceEngine ownerInferenceEngine;
    private final CommitHistoryStrategy commitHistoryStrategy;
    private final Cache<String, List<RepositoryMetadata>> cache;

    @Value("${github.organization:}")
    private String organization;

    public RepositoryCatalog(
            GitHubClient githubClient,
            GitHubConfig config,
            RepositoryNameParser nameParser,
            OwnerInferenceEngine ownerInferenceEngine,
            CommitHistoryStrategy commitHistoryStrategy) {
        this.githubClient = githubClient;
        this.config = config;
        this.nameParser = nameParser;
        this.ownerInferenceEngine = ownerInferenceEngine;
        this.commitHistoryStrategy = commitHistoryStrategy;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(config.getDiscovery().getCacheTtlMinutes()))
                .maximumSize(1) // Apenas uma entrada (lista completa de repositórios)
                .build();
    }

    /**
     * Descobre todos os repositórios aplicando filtros configurados.
     */
    public Mono<List<RepositoryMetadata>> discoverRepositories() {
        String cacheKey = "all-repositories";

        List<RepositoryMetadata> cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            logger.debug("Retornando {} repositórios do cache", cached.size());
            return Mono.just(cached);
        }

        logger.info("Cache expirado ou vazio, consultando GitHub API");
        return githubClient.listAllRepositories(organization)
                .map(this::convertToMetadata)
                .filter(this::matchesFilters)
                .collectList()
                .doOnNext(repos -> {
                    cache.put(cacheKey, repos);
                    logger.info("Cache atualizado com {} repositórios (após filtros)", repos.size());
                });
    }

    /**
     * Converte JsonNode do GitHub para RepositoryMetadata.
     */
    private RepositoryMetadata convertToMetadata(JsonNode repoNode) {
        String name = repoNode.get("name").asText();
        String fullName = repoNode.get("full_name").asText();
        String htmlUrl = repoNode.has("html_url") ? repoNode.get("html_url").asText() : null;
        String url = repoNode.has("url") ? repoNode.get("url").asText() : null;
        String description = repoNode.has("description") && !repoNode.get("description").isNull() 
                ? repoNode.get("description").asText() : null;
        boolean archived = repoNode.has("archived") && repoNode.get("archived").asBoolean();
        boolean disabled = repoNode.has("disabled") && repoNode.get("disabled").asBoolean();

        // Parse do nome
        RepositoryNameParser.ParsedRepository parsed = nameParser.parse(name);
        
        RepositoryMetadata metadata = new RepositoryMetadata();
        metadata.setName(name);
        metadata.setFullName(fullName);
        metadata.setHtmlUrl(htmlUrl);
        metadata.setUrl(url);
        metadata.setDescription(description);
        metadata.setArchived(archived);
        metadata.setDisabled(disabled);

        if (parsed != null) {
            metadata.setSigla(parsed.getSigla());
            metadata.setType(parsed.getType());
            metadata.setServiceName(parsed.getServiceName());
            metadata.setServiceId(name); // serviceId = nome do repositório
        } else {
            // Se não seguir o padrão, usa o nome completo como serviceId
            metadata.setServiceId(name);
        }

        // Busca topics (tags) do repositório
        // Nota: Isso pode ser otimizado fazendo batch requests
        githubClient.getRepositoryTopics(organization, name)
                .subscribe(metadata::setTags);

        // Tenta inferir ownership (sem callers ainda - será atualizado na Fase 2)
        try {
            InferredOwner owner = ownerInferenceEngine.inferOwner(metadata, null, new ArrayList<>());
            if (owner != null && !owner.isUnknown()) {
                metadata.setPrimaryOwner(owner.getOwner());
                metadata.setOwnershipConfidence(owner.getConfidence());
                metadata.setOwnershipSource(owner.getStrategy());
            }
            
            // Coleta top 10 committers (sempre, independente da estratégia de ownership)
            try {
                List<CommitterInfo> topCommitters = commitHistoryStrategy.getTopCommitters(metadata);
                if (topCommitters != null && !topCommitters.isEmpty()) {
                    metadata.setTopCommitters(topCommitters);
                    logger.debug("Top {} committers coletados para: {}", topCommitters.size(), name);
                }
            } catch (Exception e) {
                logger.debug("Erro ao coletar top committers para: {}", name, e);
            }
        } catch (Exception e) {
            logger.debug("Erro ao inferir ownership para: {}", name, e);
        }

        return metadata;
    }

    /**
     * Verifica se o repositório atende aos filtros configurados.
     */
    private boolean matchesFilters(RepositoryMetadata metadata) {
        // Filtro por sigla
        if (config.getDiscovery().getSiglas() != null && !config.getDiscovery().getSiglas().isEmpty()) {
            if (metadata.getSigla() == null || 
                !config.getDiscovery().getSiglas().contains(metadata.getSigla())) {
                return false;
            }
        }

        // Filtro por tipo
        if (config.getDiscovery().getTypes() != null && !config.getDiscovery().getTypes().isEmpty()) {
            if (metadata.getType() == null || 
                !config.getDiscovery().getTypes().contains(metadata.getType())) {
                return false;
            }
        }

        // Filtro por tags (topics)
        if (config.getDiscovery().getTags() != null && !config.getDiscovery().getTags().isEmpty()) {
            List<String> repoTags = metadata.getTags();
            if (repoTags == null || repoTags.isEmpty()) {
                return false;
            }
            Set<String> configTags = config.getDiscovery().getTags().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            boolean hasMatchingTag = repoTags.stream()
                    .map(String::toLowerCase)
                    .anyMatch(configTags::contains);
            if (!hasMatchingTag) {
                return false;
            }
        }

        // Ignora repositórios arquivados ou desabilitados
        if (metadata.isArchived() || metadata.isDisabled()) {
            return false;
        }

        return true;
    }

    /**
     * Invalida o cache (útil para testes ou atualizações forçadas).
     */
    public void invalidateCache() {
        cache.invalidateAll();
        logger.info("Cache de repositórios invalidado");
    }
}

