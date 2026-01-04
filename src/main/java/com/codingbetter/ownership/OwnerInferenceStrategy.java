package com.codingbetter.ownership;

import com.codingbetter.adapters.dynatrace.DynatraceClient;
import com.codingbetter.adapters.github.RepositoryMetadata;

import java.util.List;

/**
 * Interface para estratégias de inferência de ownership.
 */
public interface OwnerInferenceStrategy {

    /**
     * Infere ownership baseado na estratégia.
     * @param repo Metadados do repositório
     * @param entityDetails Detalhes da entidade no Dynatrace (opcional)
     * @param callers Lista de callers do serviço (opcional, disponível após Fase 2)
     * @return InferenceResult ou null se não conseguir inferir
     */
    InferenceResult infer(RepositoryMetadata repo,
                         DynatraceClient.EntityDetails entityDetails,
                         List<String> callers);

    /**
     * Nome da estratégia para logging/métricas.
     */
    String getName();

    /**
     * Confiança mínima aceitável para esta estratégia.
     */
    double getMinConfidence();

    /**
     * Prioridade (ordem de execução). Menor número = maior prioridade.
     */
    int getPriority();
}

