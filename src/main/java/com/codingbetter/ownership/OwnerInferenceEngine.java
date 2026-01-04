package com.codingbetter.ownership;

import com.codingbetter.adapters.dynatrace.DynatraceClient;
import com.codingbetter.adapters.github.RepositoryMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Engine que executa múltiplas estratégias de inferência de ownership
 * e retorna a melhor inferência baseada em confiança.
 */
@Component
public class OwnerInferenceEngine {

    private static final Logger logger = LoggerFactory.getLogger(OwnerInferenceEngine.class);

    private final List<OwnerInferenceStrategy> strategies;

    @Value("${ownership.inference.enabled:true}")
    private boolean enabled;

    @Value("${ownership.inference.min-confidence:0.60}")
    private double minConfidence;

    public OwnerInferenceEngine(List<OwnerInferenceStrategy> strategies) {
        // Ordena estratégias por prioridade (menor número = maior prioridade)
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(OwnerInferenceStrategy::getPriority))
                .collect(Collectors.toList());
        
        logger.info("OwnerInferenceEngine inicializado com {} estratégias", this.strategies.size());
    }

    /**
     * Infere ownership usando todas as estratégias disponíveis.
     */
    public InferredOwner inferOwner(RepositoryMetadata repo,
                                   DynatraceClient.EntityDetails entityDetails,
                                   List<String> callers) {
        
        if (!enabled) {
            logger.debug("Inferência de ownership desabilitada");
            return InferredOwner.unknown();
        }

        logger.debug("Inferindo ownership para: {}", repo.getServiceId());

        List<InferenceResult> results = new ArrayList<>();

        // Executa todas as estratégias em ordem de prioridade
        for (OwnerInferenceStrategy strategy : strategies) {
            try {
                InferenceResult result = strategy.infer(repo, entityDetails, callers);
                
                if (result != null && result.getConfidence() >= strategy.getMinConfidence()) {
                    results.add(result);
                    logger.debug("Estratégia {} inferiu owner: {} (confiança: {:.2f})", 
                            strategy.getName(), result.getOwner(), result.getConfidence());
                } else if (result != null) {
                    logger.debug("Estratégia {} inferiu owner mas confiança ({:.2f}) abaixo do mínimo ({:.2f})", 
                            strategy.getName(), result.getConfidence(), strategy.getMinConfidence());
                }
            } catch (Exception e) {
                logger.error("Erro ao executar estratégia {} para {}", 
                        strategy.getName(), repo.getServiceId(), e);
            }
        }

        if (results.isEmpty()) {
            logger.debug("Nenhuma estratégia conseguiu inferir ownership para: {}", repo.getServiceId());
            return InferredOwner.unknown();
        }

        // Agrega resultados por confiança
        return aggregateResults(results);
    }

    private InferredOwner aggregateResults(List<InferenceResult> results) {
        // Ordena por confiança (maior primeiro)
        results.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        InferenceResult best = results.get(0);

        // Se há múltiplos resultados com confiança similar (diferença < 0.1), pode agregar
        if (results.size() > 1) {
            InferenceResult second = results.get(1);
            double confidenceDiff = best.getConfidence() - second.getConfidence();
            
            if (confidenceDiff < 0.1 && best.getOwner().equals(second.getOwner())) {
                // Mesmo owner, confianças similares → aumenta confiança
                double aggregatedConfidence = Math.min(0.90, 
                        (best.getConfidence() + second.getConfidence()) / 2.0 + 0.05);
                
                logger.debug("Agregando resultados de múltiplas estratégias (confiança: {:.2f} -> {:.2f})", 
                        best.getConfidence(), aggregatedConfidence);
                
                return new InferredOwner(
                        best.getOwner(),
                        best.getTeam(),
                        best.getArea(),
                        aggregatedConfidence,
                        best.getStrategy() + "+" + second.getStrategy()
                );
            }
        }

        // Verifica se confiança está acima do mínimo
        if (best.getConfidence() < minConfidence) {
            logger.debug("Melhor inferência tem confiança ({:.2f}) abaixo do mínimo ({:.2f})", 
                    best.getConfidence(), minConfidence);
            return InferredOwner.unknown();
        }

        return new InferredOwner(
                best.getOwner(),
                best.getTeam(),
                best.getArea(),
                best.getConfidence(),
                best.getStrategy()
        );
    }
}

