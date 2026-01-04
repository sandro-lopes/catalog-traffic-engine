# Estratégias de Inferência de Ownership

Este documento descreve as estratégias implementadas para inferir automaticamente o ownership (proprietário) de serviços quando não há informação explícita disponível (ex: `CODEOWNERS`).

## Visão Geral

O sistema de inferência de ownership utiliza múltiplas estratégias que analisam diferentes sinais para identificar o proprietário de um serviço. As estratégias são executadas em ordem de prioridade e a melhor inferência (baseada em confiança) é selecionada.

## Estratégias Implementadas

### 1. Commit History (Alta Confiança - 75-85%)

**Arquivo**: `src/main/java/com/codingbetter/ownership/CommitHistoryStrategy.java`

**Descrição**: Analisa o histórico de commits do repositório para identificar o autor mais frequente.

**Funcionamento**:
- Analisa commits dos últimos 6-12 meses (configurável)
- Identifica top committers
- Aplica peso maior para commits recentes (últimos 3 meses)
- Ignora bots (`-bot`, `-ci`, `-automation`)
- Calcula confiança baseada em:
  - Percentual de commits do top author
  - Número total de commits
  - Recência dos commits

**Configuração**:
```yaml
ownership:
  inference:
    commit-history:
      lookback-months: 6
      recent-months: 3
      recent-weight: 2.0
      min-commits: 5
      ignore-bots: true
      bot-patterns:
        - ".*-bot$"
        - ".*-ci$"
        - ".*-automation$"
```

**Confiança**:
- Alta (75-85%): Top author tem >50% dos commits, muitos commits recentes
- Média (60-75%): Top author tem 30-50% dos commits
- Baixa (<60%): Top author tem <30% dos commits

**Limitações**:
- Requer acesso à GitHub API
- Pode não funcionar bem para repositórios com muitos contribuidores
- Não identifica time, apenas autor individual

### 2. Dependency Analysis (Baixa-Média Confiança - 50-65%)

**Arquivo**: `src/main/java/com/codingbetter/ownership/DependencyAnalysisStrategy.java`

**Descrição**: Analisa quem chama o serviço (callers) para inferir ownership baseado em padrões de uso.

**Funcionamento**:
- Analisa lista de callers do serviço (disponível após Fase 2)
- Agrupa callers por sigla
- Se >70% dos callers são da mesma sigla → infere ownership
- Requer mapeamento sigla → owner (configuração manual ou banco de dados)

**Configuração**:
```yaml
ownership:
  inference:
    dependency-analysis:
      enabled: false  # Habilitar após Fase 2
      min-caller-percent: 0.70
```

**Confiança**:
- Alta (70%): >90% dos callers são da mesma sigla
- Média (55%): 70-90% dos callers são da mesma sigla

**Limitações**:
- Requer dados de atividade (disponível apenas após Fase 2)
- Requer mapeamento sigla → owner
- Pode não funcionar bem para serviços públicos ou muito utilizados

## Engine de Inferência

**Arquivo**: `src/main/java/com/codingbetter/ownership/OwnerInferenceEngine.java`

O `OwnerInferenceEngine` executa todas as estratégias disponíveis e seleciona a melhor inferência baseada em:

1. **Prioridade**: Estratégias são executadas em ordem de prioridade (menor número = maior prioridade)
2. **Confiança**: A inferência com maior confiança é selecionada
3. **Agregação**: Se múltiplas estratégias inferem o mesmo owner com confianças similares, a confiança é agregada

**Configuração**:
```yaml
ownership:
  inference:
    enabled: true
    min-confidence: 0.60  # Inferências com confiança < 60% são descartadas
```

## Integração

### RepositoryCatalog

O `RepositoryCatalog` integra automaticamente o `OwnerInferenceEngine` durante a descoberta de repositórios:

```java
// Tenta inferir ownership durante descoberta
InferredOwner owner = ownerInferenceEngine.inferOwner(metadata, null, new ArrayList<>());
if (owner != null && !owner.isUnknown()) {
    metadata.setPrimaryOwner(owner.getOwner());
    metadata.setOwnershipConfidence(owner.getConfidence());
    metadata.setOwnershipSource(owner.getStrategy());
}
```

### Backstage Integration

Os dados de ownership inferidos são incluídos no `catalog-info.yaml` e nas atualizações via API:

```yaml
annotations:
  governance.ownership.primaryOwner: "joao.silva@empresa.com"
  governance.ownership.inferredFrom: "commit-history"
  governance.ownership.confidence: "0.75"
```

## Métricas

O sistema expõe métricas Prometheus para monitoramento:

- `governance_ownership_inference_attempts_total{strategy="commit-history"}`
- `governance_ownership_inference_success_total{strategy="commit-history"}`
- `governance_ownership_inference_confidence_avg{strategy="commit-history"}`
- `governance_ownership_inferred_total{confidence_level="high|medium|low"}`
- `governance_ownership_unknown_total`

## Estratégias Não Implementadas

As seguintes estratégias foram consideradas mas **não implementadas** devido a baixa confiabilidade no contexto da organização:

1. **Sigla Mapping**: Sigla não indica time específico (apenas tribo/cluster)
2. **GitHub Tags**: Tags podem ser inconsistentes
3. **Dynatrace Tags**: Tags podem estar desatualizadas
4. **Azure Resource Group**: Não confiável para ownership
5. **Description Keywords**: Muito subjetivo
6. **Service Name Keywords**: Muito subjetivo
7. **Clustering**: Complexo e pouco confiável

## Melhorias Futuras

1. **Machine Learning**: Treinar modelo com dados históricos de ownership conhecido
2. **Team Mapping**: Integrar com sistema de times/equipes da organização
3. **Historical Analysis**: Analisar mudanças de ownership ao longo do tempo
4. **Multi-Signal Aggregation**: Combinar múltiplos sinais com pesos aprendidos

## Referências

- [Backstage Ownership Model](https://backstage.io/docs/features/software-catalog/descriptor-format#specowner-optional)
- [GitHub API - Commits](https://docs.github.com/en/rest/commits/commits)
- [Prometheus Metrics](https://prometheus.io/docs/concepts/metric_types/)

