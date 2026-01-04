package com.codingbetter.adapters.dynatrace;

import com.codingbetter.adapters.ActivityAdapter;
import com.codingbetter.discovery.RepositoryCatalog;
import com.codingbetter.adapters.github.RepositoryMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter Dynatrace escalável para 2k+ APIs.
 * Implementa processamento paralelo, batching e rate limiting.
 * Agora usa RepositoryCatalog como fonte primária de descoberta.
 */
@Component
public class DynatraceAdapter implements ActivityAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DynatraceAdapter.class);

    private final RepositoryCatalog repositoryCatalog;
    private final DynatraceBatchExtractor batchExtractor;
    private final AdapterMetadata metadata;

    public DynatraceAdapter(
            RepositoryCatalog repositoryCatalog,
            DynatraceBatchExtractor batchExtractor) {
        this.repositoryCatalog = repositoryCatalog;
        this.batchExtractor = batchExtractor;
        this.metadata = new AdapterMetadata("DynatraceAdapter", "1.0.0", "dynatrace");
    }

    @Override
    public List<RawActivityEvent> extract(TimeWindow window) {
        logger.info("Iniciando extração Dynatrace: window={} to {}", window.getStart(), window.getEnd());

        // Usa repositórios como fonte primária
        return repositoryCatalog.discoverRepositories()
                .flatMapMany(repos -> {
                    List<String> serviceIds = repos.stream()
                            .map(RepositoryMetadata::getServiceId)
                            .collect(Collectors.toList());
                    
                    // Cria mapa de serviceId -> RepositoryMetadata
                    Map<String, RepositoryMetadata> repoMap = repos.stream()
                            .collect(Collectors.toMap(RepositoryMetadata::getServiceId, repo -> repo));
                    
                    logger.info("Extraindo métricas para {} serviços descobertos via GitHub", serviceIds.size());
                    return batchExtractor.extractBatch(serviceIds, window, repoMap);
                })
                .collectList()
                .block(); // Em produção, considerar retornar Mono/Flux
    }

    /**
     * Versão reativa para integração com pipelines assíncronos.
     */
    public Flux<RawActivityEvent> extractReactive(TimeWindow window) {
        logger.info("Iniciando extração Dynatrace (reativa): window={} to {}", window.getStart(), window.getEnd());

        return repositoryCatalog.discoverRepositories()
                .flatMapMany(repos -> {
                    List<String> serviceIds = repos.stream()
                            .map(RepositoryMetadata::getServiceId)
                            .collect(Collectors.toList());
                    
                    // Cria mapa de serviceId -> RepositoryMetadata
                    Map<String, RepositoryMetadata> repoMap = repos.stream()
                            .collect(Collectors.toMap(RepositoryMetadata::getServiceId, repo -> repo));
                    
                    logger.info("Extraindo métricas para {} serviços descobertos via GitHub", serviceIds.size());
                    return batchExtractor.extractBatch(serviceIds, window, repoMap);
                });
    }

    @Override
    public AdapterMetadata getMetadata() {
        return metadata;
    }
}

