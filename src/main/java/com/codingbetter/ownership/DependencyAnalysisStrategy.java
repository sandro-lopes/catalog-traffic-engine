package com.codingbetter.ownership;

import com.codingbetter.adapters.dynatrace.DynatraceClient;
import com.codingbetter.adapters.github.RepositoryMetadata;
import com.codingbetter.adapters.github.RepositoryNameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Estratégia de inferência de ownership baseada em análise de dependências.
 * Se um serviço é chamado principalmente por serviços de um time específico,
 * infere que o serviço pertence ao mesmo time.
 * 
 * Requer dados de atividade (disponível após Fase 2).
 */
@Component
public class DependencyAnalysisStrategy implements OwnerInferenceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalysisStrategy.class);

    private final RepositoryNameParser nameParser;
    private final Map<String, String> siglaToOwnerMap; // Cache de siglas com owners conhecidos

    @Value("${ownership.inference.dependency-analysis.enabled:false}")
    private boolean enabled;

    @Value("${ownership.inference.dependency-analysis.min-caller-percent:0.70}")
    private double minCallerPercent;

    public DependencyAnalysisStrategy(RepositoryNameParser nameParser) {
        this.nameParser = nameParser;
        this.siglaToOwnerMap = new HashMap<>();
        // Em produção, carregar de configuração ou banco de dados
    }

    @Override
    public InferenceResult infer(RepositoryMetadata repo,
                                 DynatraceClient.EntityDetails entityDetails,
                                 List<String> callers) {
        
        if (!enabled) {
            return null;
        }

        if (callers == null || callers.isEmpty()) {
            logger.debug("Sem callers disponíveis para análise de dependências: {}", repo.getServiceId());
            return null;
        }

        logger.debug("Analisando dependências para inferir ownership: {} ({} callers)", 
                repo.getServiceId(), callers.size());

        // Agrupa callers por sigla
        Map<String, Long> callersBySigla = callers.stream()
                .map(caller -> {
                    RepositoryNameParser.ParsedRepository parsed = nameParser.parse(caller);
                    return parsed != null ? parsed.getSigla() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(sigla -> sigla, Collectors.counting()));

        if (callersBySigla.isEmpty()) {
            logger.debug("Nenhum caller com sigla identificável: {}", repo.getServiceId());
            return null;
        }

        // Encontra sigla mais frequente
        Map.Entry<String, Long> topSigla = callersBySigla.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (topSigla == null) {
            return null;
        }

        // Calcula percentual de callers da sigla mais frequente
        double topSiglaPercent = topSigla.getValue() / (double) callers.size();

        if (topSiglaPercent < minCallerPercent) {
            logger.debug("Sigla mais frequente ({}) tem apenas {:.2f}% dos callers (mínimo: {:.2f}%)", 
                    topSigla.getKey(), topSiglaPercent, minCallerPercent);
            return null;
        }

        // Busca owner conhecido para esta sigla
        String owner = siglaToOwnerMap.get(topSigla.getKey().toLowerCase());
        
        if (owner == null) {
            logger.debug("Sigla {} identificada mas sem owner conhecido", topSigla.getKey());
            return null;
        }

        // Calcula confiança baseada no percentual de callers
        // Se >90% dos callers são da mesma sigla → alta confiança
        // Se 70-90% → média confiança
        double confidence = topSiglaPercent >= 0.90 ? 0.70 : 0.55;

        logger.info("Owner inferido via análise de dependências: {} -> {} (sigla: {}, confiança: {:.2f})", 
                repo.getServiceId(), owner, topSigla.getKey(), confidence);

        return new InferenceResult(
                owner,
                null, // Team não inferido por dependências
                topSigla.getKey(), // Area = sigla
                confidence,
                getName()
        );
    }

    /**
     * Adiciona mapeamento sigla -> owner (útil para popular cache).
     */
    public void addSiglaOwnerMapping(String sigla, String owner) {
        siglaToOwnerMap.put(sigla.toLowerCase(), owner);
        logger.debug("Mapeamento adicionado: {} -> {}", sigla, owner);
    }

    @Override
    public String getName() {
        return "dependency-analysis";
    }

    @Override
    public double getMinConfidence() {
        return 0.50; // Aceita inferências com confiança >= 50%
    }

    @Override
    public int getPriority() {
        return 2; // Segunda estratégia (após commit history)
    }
}

