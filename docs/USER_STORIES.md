# User Stories Detalhadas - Catalog Traffic Engine

Este documento contém todas as user stories detalhadas, prontas para desenvolvimento Scrum.

---

## US-001: Descoberta Automática de Repositórios via GitHub API

**Épica**: Descoberta e Identificação de Serviços  
**Fase**: 1.1  
**Prioridade**: Crítica  
**Story Points**: 8  
**Sprint**: 1-2

### Descrição

Como administrador do sistema,  
Quero que o sistema descubra automaticamente todos os repositórios via GitHub API,  
Para que eu possa identificar todos os serviços da organização sem intervenção manual.

### Critérios de Aceite

- [ ] Sistema descobre 100% dos repositórios que seguem o padrão `{sigla}-{tipo}-{nome}`
- [ ] Sistema suporta paginação para lidar com 2.000+ repositórios
- [ ] Cache reduz chamadas à API em >80%
- [ ] Tempo de descoberta < 5 minutos para 2k repositórios
- [ ] Rate limiting respeitado (sem erros 429)
- [ ] Métricas de descoberta disponíveis em Prometheus

### Regras de Negócio

1. Repositórios devem seguir padrão de nomenclatura: `{sigla}-{tipo}-{nome}`
2. Filtros configuráveis por:
   - Sigla (ex: "abc", "xyz")
   - Tipo (ex: "api", "bff", "gtw", "mfe")
   - Tags do GitHub (ex: "production", "active")
3. Repositórios arquivados ou desabilitados são ignorados
4. Cache TTL padrão: 240 minutos (configurável)

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/adapters/github/GitHubClient.java`
- `src/main/java/com/codingbetter/discovery/RepositoryCatalog.java`
- `src/main/java/com/codingbetter/adapters/github/RepositoryNameParser.java`

**Configuração:**
```yaml
github:
  organization: ${GITHUB_ORGANIZATION:}
  api:
    url: ${GITHUB_API_URL:https://api.github.com}
    token: ${GITHUB_API_TOKEN:}
  discovery:
    siglas: ${GITHUB_SIGLAS:}
    types: ${GITHUB_TYPES:api,bff,gtw,mfe}
    tags: ${GITHUB_TAGS:}
    cache-ttl-minutes: 240
```

**Métricas Prometheus:**
- `governance_discovery_repositories_total`
- `governance_discovery_repositories_filtered_total`
- `governance_discovery_duration_seconds`
- `governance_discovery_cache_hits_total`
- `governance_discovery_cache_misses_total`

**Resilience:**
- Circuit breaker: `github`
- Rate limiter: 30 requests/minuto
- Retry: 3 tentativas com backoff exponencial

### Testes

- [ ] Teste unitário: `RepositoryNameParser` parseia corretamente nomes válidos
- [ ] Teste unitário: `RepositoryNameParser` rejeita nomes inválidos
- [ ] Teste de integração: `GitHubClient` lista repositórios com paginação
- [ ] Teste de integração: `RepositoryCatalog` aplica filtros corretamente
- [ ] Teste de carga: 2.000 repositórios em < 5 minutos

### Dependências

- GitHub API token configurado
- Acesso à organização do GitHub

---

## US-002: Filtragem de Repositórios por Sigla, Tipo e Tags

**Épica**: Descoberta e Identificação de Serviços  
**Fase**: 1.1  
**Prioridade**: Crítica  
**Story Points**: 5  
**Sprint**: 1

### Descrição

Como administrador do sistema,  
Quero que o sistema filtre repositórios por sigla, tipo e tags do GitHub,  
Para focar apenas nos serviços relevantes da minha área.

### Critérios de Aceite

- [ ] Sistema filtra por sigla (lista configurável)
- [ ] Sistema filtra por tipo (api, bff, gtw, mfe, etc.)
- [ ] Sistema filtra por tags do GitHub (topics)
- [ ] Filtros são aplicados em conjunto (AND)
- [ ] Repositórios arquivados/desabilitados são sempre ignorados
- [ ] Filtros são configuráveis via variáveis de ambiente

### Regras de Negócio

1. Filtro por sigla: lista de siglas permitidas (ex: "abc,xyz")
2. Filtro por tipo: lista de tipos permitidos (ex: "api,bff,gtw,mfe")
3. Filtro por tags: lista de tags obrigatórias (ex: "production,active")
4. Todos os filtros são aplicados em conjunto (AND lógico)
5. Se nenhum filtro configurado, todos os repositórios válidos são incluídos

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/discovery/RepositoryCatalog.java`
- `src/main/java/com/codingbetter/adapters/github/GitHubConfig.java`

**Lógica de Filtragem:**
```java
private boolean matchesFilters(RepositoryMetadata metadata) {
    // Filtro por sigla
    if (siglas configuradas && metadata.sigla não está na lista) return false;
    
    // Filtro por tipo
    if (tipos configurados && metadata.type não está na lista) return false;
    
    // Filtro por tags
    if (tags configuradas && metadata.tags não contém nenhuma tag) return false;
    
    // Ignora arquivados/desabilitados
    if (metadata.archived || metadata.disabled) return false;
    
    return true;
}
```

### Testes

- [ ] Teste unitário: Filtro por sigla funciona corretamente
- [ ] Teste unitário: Filtro por tipo funciona corretamente
- [ ] Teste unitário: Filtro por tags funciona corretamente
- [ ] Teste unitário: Filtros combinados funcionam (AND)
- [ ] Teste unitário: Repositórios arquivados são ignorados

---

## US-003: Criação de Pull Requests com catalog-info.yaml

**Épica**: Descoberta e Identificação de Serviços  
**Fase**: 1.1  
**Prioridade**: Alta  
**Story Points**: 8  
**Sprint**: 2

### Descrição

Como desenvolvedor,  
Quero que o sistema crie Pull Requests com catalog-info.yaml para cada repositório descoberto,  
Para que meu serviço seja automaticamente descoberto no Backstage.

### Critérios de Aceite

- [ ] Sistema gera catalog-info.yaml válido para cada repositório
- [ ] Sistema cria Pull Request em cada repositório
- [ ] 80%+ dos PRs são aprovados/mergeados
- [ ] Processamento em batch (50 PRs por vez) para respeitar rate limits
- [ ] Métricas de PRs disponíveis (criados, mergeados, rejeitados)

### Regras de Negócio

1. catalog-info.yaml é criado no caminho `.backstage/catalog-info.yaml` (configurável)
2. Branch criada com prefixo `backstage/catalog-info-` + UUID
3. PR title: "chore: Adicionar catalog-info.yaml para Backstage"
4. PR body explica que é gerado automaticamente pelo Catalog Traffic Engine
5. Processamento em batch de 50 PRs por vez
6. Rate limiting respeitado (30 requests/minuto)

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/backstage/CatalogYamlGenerator.java`
- `src/main/java/com/codingbetter/backstage/CatalogYamlPRService.java`
- `src/main/java/com/codingbetter/service/CatalogYamlCreationJob.java`

**Estrutura do catalog-info.yaml:**
```yaml
apiVersion: backstage.io/v1alpha1
kind: Component
metadata:
  name: abc-api-usuario
  description: API de Usuários - Descoberto automaticamente
  annotations:
    github.com/project-slug: empresa/abc-api-usuario
    governance.discovery.sigla: abc
    governance.discovery.type: api
    governance.discovery.serviceName: usuario
    governance.discovery.discoveredAt: "2024-01-15T10:00:00Z"
    governance.ownership.primaryOwner: "joao.silva@empresa.com"
    governance.ownership.inferredFrom: commit-history
    governance.ownership.confidence: "0.75"
    governance.contributors.topCommitters: [...]
spec:
  type: service
  lifecycle: production
  owner: joao.silva@empresa.com
```

**Métricas Prometheus:**
- `governance_catalog_yaml_prs_created_total`
- `governance_catalog_yaml_prs_merged_total`
- `governance_catalog_yaml_prs_rejected_total`
- `governance_catalog_yaml_prs_pending_total`

### Testes

- [ ] Teste unitário: `CatalogYamlGenerator` gera YAML válido
- [ ] Teste unitário: YAML gerado é parseável pelo Backstage
- [ ] Teste de integração: `CatalogYamlPRService` cria PR corretamente
- [ ] Teste de integração: Processamento em batch funciona
- [ ] Teste de carga: 100 PRs criados sem erros de rate limit

### Dependências

- US-001 (Descoberta de Repositórios)
- GitHub API token com permissão para criar PRs

---

## US-004: Exibir Top 10 Committers no Backstage

**Épica**: Descoberta e Identificação de Serviços  
**Fase**: 1.2  
**Prioridade**: Média  
**Story Points**: 5  
**Sprint**: 3

### Descrição

Como desenvolvedor,  
Quero ver os top 10 committers do meu serviço no Backstage,  
Para identificar rapidamente os principais contribuidores do projeto.

### Critérios de Aceite

- [ ] Sistema coleta top 10 committers de cada repositório
- [ ] Cada committer inclui: id (GitHub username), email, nome, commitCount, weightedScore
- [ ] Committers são ordenados por score ponderado (commits recentes têm peso maior)
- [ ] Bots são automaticamente ignorados
- [ ] Dados aparecem no Backstage na anotação `governance.contributors.topCommitters`

### Regras de Negócio

1. Top 10 committers baseado em:
   - Número total de commits (últimos 6-12 meses)
   - Commits recentes têm peso 2x maior (últimos 3 meses)
2. Bots são identificados por padrões: `.*-bot$`, `.*-ci$`, `.*-automation$`
3. Score ponderado = commitCount + (recentCommits * 2.0)
4. Ordenação: maior score primeiro

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/ownership/CommitHistoryStrategy.java`
- `src/main/java/com/codingbetter/ownership/CommitterInfo.java`
- `src/main/java/com/codingbetter/backstage/BackstageMapper.java`

**Estrutura de CommitterInfo:**
```java
public class CommitterInfo {
    private String id;        // GitHub username/login
    private String email;     // Email do committer
    private String name;      // Nome do committer
    private int commitCount;  // Total de commits
    private double weightedScore; // Score ponderado
}
```

**Formato no Backstage:**
```json
{
  "annotations": {
    "governance.contributors.topCommitters": [
      {
        "id": "joao.silva",
        "email": "joao.silva@empresa.com",
        "name": "João Silva",
        "commitCount": 45,
        "weightedScore": 67.5
      },
      ...
    ]
  }
}
```

**Métricas Prometheus:**
- `governance_committers_collected_total`
- `governance_committers_collection_duration_seconds`

### Testes

- [ ] Teste unitário: `CommitHistoryStrategy.getTopCommitters()` retorna top 10
- [ ] Teste unitário: Committers são ordenados por score
- [ ] Teste unitário: Bots são ignorados
- [ ] Teste unitário: Commits recentes têm peso maior
- [ ] Teste de integração: Dados aparecem no Backstage

### Dependências

- US-001 (Descoberta de Repositórios)
- GitHub API com acesso a commits

---

## US-005: Inferência Automática de Ownership via Commit History

**Épica**: Descoberta e Identificação de Serviços  
**Fase**: 1.2  
**Prioridade**: Alta  
**Story Points**: 8  
**Sprint**: 3

### Descrição

Como administrador do sistema,  
Quero que o sistema infira automaticamente o owner de um serviço baseado no histórico de commits,  
Para identificar proprietários sem depender de CODEOWNERS manual.

### Critérios de Aceite

- [ ] Sistema analisa últimos 6-12 meses de commits (configurável)
- [ ] Sistema identifica top committer como owner primário
- [ ] Sistema calcula confiança baseada em percentual de commits
- [ ] 60%+ dos serviços têm owner inferido (confiança >= 60%)
- [ ] Confiança média das inferências > 65%
- [ ] Bots são automaticamente ignorados
- [ ] Commits recentes têm peso maior (2x)

### Regras de Negócio

1. **Análise de Commits:**
   - Período: últimos 6-12 meses (configurável)
   - Commits recentes (últimos 3 meses) têm peso 2x maior
   - Mínimo de 5 commits para inferir ownership

2. **Cálculo de Confiança:**
   - Base: 50% + (percentual de commits do top author * 35%)
   - Máximo: 85%
   - Se top author tem <30% dos commits: confiança reduzida em 20%

3. **Filtros:**
   - Bots são ignorados (padrões: `.*-bot$`, `.*-ci$`, `.*-automation$`)
   - Apenas commits de autores humanos são considerados

4. **Threshold:**
   - Inferências com confiança < 60% são descartadas
   - Owner retornado como "unknown" se confiança insuficiente

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/ownership/CommitHistoryStrategy.java`
- `src/main/java/com/codingbetter/ownership/OwnerInferenceEngine.java`
- `src/main/java/com/codingbetter/adapters/github/GitHubClient.java`

**Configuração:**
```yaml
ownership:
  inference:
    enabled: true
    min-confidence: 0.60
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

**Métricas Prometheus:**
- `governance_ownership_inference_attempts_total{strategy="commit-history"}`
- `governance_ownership_inference_success_total{strategy="commit-history"}`
- `governance_ownership_inference_confidence_avg{strategy="commit-history"}`
- `governance_ownership_inferred_total{confidence_level="high|medium|low"}`
- `governance_ownership_unknown_total`

### Testes

- [ ] Teste unitário: Top committer é identificado corretamente
- [ ] Teste unitário: Confiança é calculada corretamente
- [ ] Teste unitário: Bots são ignorados
- [ ] Teste unitário: Commits recentes têm peso maior
- [ ] Teste de integração: Inferência funciona com dados reais do GitHub
- [ ] Teste de validação: 60%+ dos serviços têm owner inferido

### Dependências

- US-001 (Descoberta de Repositórios)
- GitHub API com acesso a commits

---

## US-006: Atualização Automática do Backstage com Dados de Descoberta

**Épica**: Descoberta e Identificação de Serviços  
**Fase**: 1.2  
**Prioridade**: Alta  
**Story Points**: 5  
**Sprint**: 3-4

### Descrição

Como administrador do sistema,  
Quero que o sistema atualize automaticamente o Backstage com dados de descoberta e ownership,  
Para que os desenvolvedores vejam informações atualizadas sem intervenção manual.

### Critérios de Aceite

- [ ] Sistema atualiza Backstage via API após descoberta de repositórios
- [ ] Dados incluídos: nome, sigla, tipo, serviceName, owner, top committers
- [ ] Atualização em batch (100 entidades por vez)
- [ ] Taxa de sucesso >99%
- [ ] Erros são tratados e logados
- [ ] Métricas de atualização disponíveis

### Regras de Negócio

1. **Dados da Fase 1:**
   - Metadados básicos (nome, sigla, tipo, serviceName)
   - Owner inferido (se disponível)
   - Top 10 committers
   - Data de descoberta

2. **Frequência:**
   - Atualização diária (3 AM)
   - Pode ser executada manualmente

3. **Tratamento de Erros:**
   - Retry com backoff exponencial
   - Circuit breaker para proteger Backstage
   - Erros são logados mas não interrompem processamento

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/service/BackstagePhase1Service.java`
- `src/main/java/com/codingbetter/backstage/BackstageMapper.java`
- `src/main/java/com/codingbetter/backstage/BackstageClient.java`

**Formato de Atualização:**
```json
{
  "metadata": {
    "annotations": {
      "governance.discovery.sigla": "abc",
      "governance.discovery.type": "api",
      "governance.discovery.serviceName": "usuario",
      "governance.discovery.discoveredAt": "2024-01-15T10:00:00Z",
      "governance.ownership.primaryOwner": "joao.silva@empresa.com",
      "governance.ownership.inferredFrom": "commit-history",
      "governance.ownership.confidence": "0.75",
      "governance.contributors.topCommitters": [...]
    }
  },
  "spec": {
    "type": "service",
    "lifecycle": "production",
    "owner": "joao.silva@empresa.com"
  }
}
```

**Métricas Prometheus:**
- `governance_backstage_updates_total{phase="1"}`
- `governance_backstage_errors_total{phase="1"}`
- `governance_backstage_duration_seconds{phase="1"}`

### Testes

- [ ] Teste de integração: `BackstagePhase1Service` atualiza Backstage corretamente
- [ ] Teste de integração: Atualização em batch funciona
- [ ] Teste de integração: Erros são tratados corretamente
- [ ] Teste de carga: 100 entidades atualizadas sem erros

### Dependências

- US-001 (Descoberta de Repositórios)
- US-004 (Top 10 Committers)
- US-005 (Inferência de Ownership)
- Backstage API configurada

---

## US-007: Extração de Métricas de Atividade do Dynatrace

**Épica**: Extração e Normalização de Dados  
**Fase**: 2.1  
**Prioridade**: Alta  
**Story Points**: 13  
**Sprint**: 5-7

### Descrição

Como administrador do sistema,  
Quero que o sistema extraia métricas de atividade do Dynatrace para todos os serviços descobertos,  
Para identificar quais serviços estão recebendo tráfego.

### Critérios de Aceite

- [ ] Sistema extrai métricas de 2.000+ serviços em < 10 minutos
- [ ] Taxa de erro < 1%
- [ ] Circuit breakers protegem contra falhas da API
- [ ] Processamento paralelo com workers configuráveis
- [ ] Métricas de extração disponíveis

### Regras de Negócio

1. **Métricas Extraídas:**
   - Request count (total de requisições)
   - Callers (quem chama o serviço)
   - Janela temporal: últimos 5 minutos

2. **Processamento:**
   - Batch size: 50 serviços por vez
   - Workers paralelos: 20 (configurável)
   - Rate limit: 10 requests/segundo

3. **Resiliência:**
   - Circuit breaker: abre após 50% de falhas
   - Retry: 3 tentativas com backoff exponencial
   - Timeout: 30 segundos por requisição

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/adapters/dynatrace/DynatraceAdapter.java`
- `src/main/java/com/codingbetter/adapters/dynatrace/DynatraceClient.java`
- `src/main/java/com/codingbetter/adapters/dynatrace/DynatraceBatchExtractor.java`

**Métricas Prometheus:**
- `governance_extraction_services_total`
- `governance_extraction_duration_seconds`
- `governance_extraction_errors_total`
- `governance_extraction_rate_per_second`

### Testes

- [ ] Teste de integração: Extração funciona com Dynatrace API (mock)
- [ ] Teste de carga: 2.000 serviços em < 10 minutos
- [ ] Teste de resiliência: Circuit breaker funciona corretamente
- [ ] Teste de retry: Retry funciona em caso de falhas temporárias

### Dependências

- US-001 (Descoberta de Repositórios)
- Dynatrace API token configurado

---

## US-008: Normalização de Eventos para Schema Canônico

**Épica**: Extração e Normalização de Dados  
**Fase**: 2.2  
**Prioridade**: Alta  
**Story Points**: 8  
**Sprint**: 7-8

### Descrição

Como administrador do sistema,  
Quero que o sistema normalize eventos de diferentes fontes para um schema canônico único,  
Para garantir consistência e facilitar processamento posterior.

### Critérios de Aceite

- [ ] 100% dos eventos raw são normalizados com sucesso
- [ ] Schema validado contra JSON Schema
- [ ] Eventos de diferentes fontes (Dynatrace, etc.) são normalizados
- [ ] Campos obrigatórios sempre presentes
- [ ] Métricas de normalização disponíveis

### Regras de Negócio

1. **Schema Canônico: ServiceActivityEvent.v1**
   - `service.id`: ID único do serviço
   - `activity.count`: Número de requisições/eventos
   - `dependencies.callers`: Lista de serviços que chamam este serviço
   - `timestamps.window`: Janela temporal (start, end)
   - `confidence.level`: Nível de confiança (HIGH, MEDIUM, LOW)
   - `metadata`: Metadados adicionais (environment, source)
   - `repository`: Metadados do repositório (opcional)
   - `discoverySource`: Fonte de descoberta (GITHUB, DYNATRACE, BOTH)

2. **Validação:**
   - Campos obrigatórios devem estar presentes
   - Tipos de dados devem estar corretos
   - Valores devem estar dentro de ranges válidos

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/normalization/EventNormalizer.java`
- `src/main/java/com/codingbetter/schemas/v1/ServiceActivityEvent.java`

**Métricas Prometheus:**
- `governance_normalization_events_total`
- `governance_normalization_errors_total`
- `governance_normalization_duration_seconds`

### Testes

- [ ] Teste unitário: Normalização de eventos Dynatrace
- [ ] Teste unitário: Validação de schema funciona
- [ ] Teste unitário: Campos obrigatórios são validados
- [ ] Teste de integração: 100% dos eventos são normalizados

### Dependências

- US-007 (Extração de Métricas)

---

## US-009: Agregação Temporal de Eventos

**Épica**: Extração e Normalização de Dados  
**Fase**: 2.2  
**Prioridade**: Média  
**Story Points**: 5  
**Sprint**: 8

### Descrição

Como administrador do sistema,  
Quero que o sistema agregue eventos em janelas temporais,  
Para reduzir volume de dados e facilitar análise.

### Critérios de Aceite

- [ ] Eventos são agregados em janelas de 5 minutos (configurável)
- [ ] Agregação mantém determinismo
- [ ] Métricas de agregação disponíveis

### Regras de Negócio

1. **Janela Temporal:**
   - Padrão: 5 minutos
   - Configurável via `aggregation.temporal-window-minutes`

2. **Agregação:**
   - `activity.count`: Soma de todos os eventos na janela
   - `callers`: União de todos os callers únicos
   - `window`: Start = início da janela, End = fim da janela

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/normalization/TemporalAggregator.java`

**Métricas Prometheus:**
- `governance_aggregation_windows_total`
- `governance_aggregation_events_per_window_avg`

### Testes

- [ ] Teste unitário: Agregação em janelas de 5 minutos
- [ ] Teste unitário: Determinismo mantido
- [ ] Teste unitário: Callers únicos são preservados

### Dependências

- US-008 (Normalização de Eventos)

---

## US-010: Atualização do Backstage com Dados de Atividade

**Épica**: Extração e Normalização de Dados  
**Fase**: 2.2  
**Prioridade**: Média  
**Story Points**: 5  
**Sprint**: 8-9

### Descrição

Como desenvolvedor,  
Quero ver dados de atividade do meu serviço no Backstage,  
Para entender se o serviço está recebendo tráfego.

### Critérios de Aceite

- [ ] Sistema atualiza Backstage com dados de atividade (Fase 2)
- [ ] Dados incluídos: receivesTraffic, lastSeen, trafficVolume, activeCallers
- [ ] Atualização automática a cada 60 segundos (configurável)
- [ ] Taxa de sucesso >99%

### Regras de Negócio

1. **Dados da Fase 2:**
   - `receivesTraffic`: boolean (activityCount > 0)
   - `lastSeen`: timestamp da última atividade
   - `trafficVolume`: número total de requisições
   - `activeCallers`: lista de serviços que chamam este serviço

2. **Frequência:**
   - Atualização a cada 60 segundos (configurável)
   - Processamento em batch (100 entidades por vez)

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/service/BackstageIntegrationService.java`
- `src/main/java/com/codingbetter/backstage/BackstageMapper.java`

**Formato de Atualização (Fase 2):**
```json
{
  "metadata": {
    "annotations": {
      "governance.activity.receivesTraffic": "true",
      "governance.activity.lastSeen": "2024-01-15T09:30:00Z",
      "governance.activity.trafficVolume": "1250",
      "governance.activity.activeCallers": "['service1', 'service2']"
    }
  }
}
```

### Testes

- [ ] Teste de integração: Dados de atividade aparecem no Backstage
- [ ] Teste de integração: Atualização automática funciona

### Dependências

- US-008 (Normalização de Eventos)
- US-009 (Agregação Temporal)
- Backstage API configurada

---

## US-011: Publicação de Eventos no Kafka

**Épica**: Pipeline e Consolidação  
**Fase**: 3.1  
**Prioridade**: Alta  
**Story Points**: 8  
**Sprint**: 9-10

### Descrição

Como administrador do sistema,  
Quero que o sistema publique eventos normalizados no Kafka,  
Para processamento assíncrono e escalável.

### Critérios de Aceite

- [ ] Eventos publicados com sucesso >99.9%
- [ ] Particionamento uniforme por serviceId
- [ ] Consumer lag < 1000 eventos
- [ ] Métricas de Kafka disponíveis

### Regras de Negócio

1. **Tópico:** `governance.activity.raw`
2. **Particionamento:** Por `service.id` (hash)
3. **Retenção:** 35 dias
4. **Replicação:** 3 réplicas

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/kafka/KafkaProducer.java`
- `src/main/java/com/codingbetter/kafka/TopicConfig.java`

**Métricas Prometheus:**
- `governance_kafka_produce_total`
- `governance_kafka_produce_errors_total`
- `governance_kafka_lag`
- `governance_kafka_throughput_per_second`

### Testes

- [ ] Teste de integração: Eventos publicados no Kafka
- [ ] Teste de integração: Particionamento funciona corretamente
- [ ] Teste de carga: 10.000 eventos publicados sem erros

### Dependências

- US-008 (Normalização de Eventos)
- Kafka/Confluent Cloud configurado

---

## US-012: Consolidação Diária em Snapshots

**Épica**: Pipeline e Consolidação  
**Fase**: 3.2  
**Prioridade**: Alta  
**Story Points**: 13  
**Sprint**: 11-13

### Descrição

Como administrador do sistema,  
Quero que o sistema consolide eventos diariamente em snapshots de status,  
Para ter uma visão consolidada do estado de cada serviço.

### Critérios de Aceite

- [ ] Consolidação completa em < 30 minutos para 2k serviços
- [ ] Snapshots gerados para 100% dos serviços
- [ ] Job executa diariamente sem falhas (2 AM)
- [ ] Snapshots publicados no Kafka

### Regras de Negócio

1. **Janela de Análise:** 30 dias
2. **Cálculos:**
   - `trafficVolume`: Soma de todos os eventos
   - `lastSeen`: Timestamp do evento mais recente
   - `activeCallers`: União de todos os callers únicos
3. **Frequência:** Diária às 2 AM

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/consolidation/ConsolidationCoordinator.java`
- `src/main/java/com/codingbetter/consolidation/ConsolidationWorker.java`
- `src/main/java/com/codingbetter/consolidation/SnapshotGenerator.java`

**Métricas Prometheus:**
- `governance_consolidation_services_total`
- `governance_consolidation_duration_seconds`
- `governance_consolidation_errors_total`

### Testes

- [ ] Teste de integração: Consolidação funciona com dados reais
- [ ] Teste de carga: 2.000 serviços em < 30 minutos
- [ ] Teste de agendamento: Job executa diariamente

### Dependências

- US-011 (Publicação no Kafka)
- Kafka consumer configurado

---

## US-013: Classificação Automática de Serviços

**Épica**: Pipeline e Consolidação  
**Fase**: 3.2  
**Prioridade**: Alta  
**Story Points**: 8  
**Sprint**: 12-13

### Descrição

Como administrador do sistema,  
Quero que o sistema classifique automaticamente serviços como ACTIVE, LOW_USAGE ou NO_TRAFFIC,  
Para identificar serviços que podem ser descomissionados.

### Critérios de Aceite

- [ ] Classificação baseada em `lastSeen`
- [ ] Regras: <=7 dias = ACTIVE, 8-30 dias = LOW_USAGE, >30 dias = NO_TRAFFIC
- [ ] Classificação correta validada manualmente (amostra)
- [ ] Métricas de classificação disponíveis

### Regras de Negócio

1. **ACTIVE:** `lastSeen <= 7 dias`
2. **LOW_USAGE:** `8 dias <= lastSeen <= 30 dias`
3. **NO_TRAFFIC:** `lastSeen > 30 dias`

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/consolidation/DecisionEngine.java`
- `src/main/java/com/codingbetter/consolidation/TrafficClassifier.java`

**Métricas Prometheus:**
- `governance_consolidation_classifications_total{type="ACTIVE"}`
- `governance_consolidation_classifications_total{type="LOW_USAGE"}`
- `governance_consolidation_classifications_total{type="NO_TRAFFIC"}`

### Testes

- [ ] Teste unitário: Classificação baseada em lastSeen
- [ ] Teste unitário: Regras aplicadas corretamente
- [ ] Teste de validação: Classificação correta (amostra)

### Dependências

- US-012 (Consolidação Diária)

---

## US-014: Atualização do Backstage com Classificação

**Épica**: Pipeline e Consolidação  
**Fase**: 3.2  
**Prioridade**: Média  
**Story Points**: 5  
**Sprint**: 13-14

### Descrição

Como desenvolvedor,  
Quero ver a classificação do meu serviço no Backstage,  
Para entender se o serviço está ativo, com baixo uso ou sem tráfego.

### Critérios de Aceite

- [ ] Sistema atualiza Backstage com classificação (Fase 3)
- [ ] Dados incluídos: status, confidenceLevel, snapshotDate
- [ ] Atualização automática após consolidação diária
- [ ] Taxa de sucesso >99%

### Regras de Negócio

1. **Dados da Fase 3:**
   - `classification.status`: ACTIVE, LOW_USAGE, NO_TRAFFIC
   - `classification.confidenceLevel`: HIGH, MEDIUM, LOW
   - `classification.snapshotDate`: Data do snapshot

### Detalhamentos Técnicos

**Formato de Atualização (Fase 3):**
```json
{
  "metadata": {
    "annotations": {
      "governance.classification.status": "ACTIVE",
      "governance.classification.confidenceLevel": "HIGH",
      "governance.classification.snapshotDate": "2024-01-15"
    }
  }
}
```

### Testes

- [ ] Teste de integração: Classificação aparece no Backstage
- [ ] Teste de integração: Atualização automática funciona

### Dependências

- US-013 (Classificação Automática)
- Backstage API configurada

---

## US-015: Visualização de Status de Governança no Backstage

**Épica**: Integração e Visualização  
**Fase**: 4.1  
**Prioridade**: Média  
**Story Points**: 5  
**Sprint**: 14-15

### Descrição

Como desenvolvedor,  
Quero ver o status completo de governança do meu serviço no Backstage,  
Para ter visibilidade sobre atividade, ownership e classificação.

### Critérios de Aceite

- [ ] Todos os dados de governança visíveis no Backstage
- [ ] Dados atualizados automaticamente
- [ ] Interface clara e fácil de entender

### Regras de Negócio

1. **Dados Exibidos:**
   - Metadados de descoberta (sigla, tipo, serviceName)
   - Ownership (primaryOwner, confidence, source)
   - Top 10 committers
   - Atividade (receivesTraffic, lastSeen, trafficVolume)
   - Classificação (status, confidenceLevel)
   - Callers ativos

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/service/BackstageIntegrationService.java`
- `src/main/java/com/codingbetter/backstage/BackstageMapper.java`

### Testes

- [ ] Teste de integração: Todos os dados aparecem no Backstage
- [ ] Teste de usabilidade: Interface é clara e fácil de entender

### Dependências

- US-006 (Atualização Backstage Fase 1)
- US-010 (Atualização Backstage Fase 2)
- US-014 (Atualização Backstage Fase 3)

---

## US-016: Dashboard de Métricas de Governança

**Épica**: Integração e Visualização  
**Fase**: 4.2  
**Prioridade**: Média  
**Story Points**: 8  
**Sprint**: 15-16

### Descrição

Como administrador do sistema,  
Quero visualizar dashboard com métricas de governança em tempo real,  
Para monitorar saúde do sistema e identificar problemas.

### Critérios de Aceite

- [ ] Dashboard Grafana criado
- [ ] Métricas principais exibidas:
   - Total de serviços
   - Distribuição por classificação
   - Serviços sem owner
   - Taxa de atualização
- [ ] Dashboard atualizado em tempo real

### Regras de Negócio

1. **Métricas Principais:**
   - Total de serviços descobertos
   - Serviços por classificação (ACTIVE, LOW_USAGE, NO_TRAFFIC)
   - Percentual de serviços com owner
   - Taxa de atualização do Backstage
   - Erros de processamento

### Detalhamentos Técnicos

**Métricas Prometheus Disponíveis:**
- `governance_discovery_repositories_total`
- `governance_consolidation_classifications_total{type}`
- `governance_ownership_inferred_total`
- `governance_backstage_updates_total`
- `governance_backstage_errors_total`

### Testes

- [ ] Teste de integração: Dashboard conecta ao Prometheus
- [ ] Teste de usabilidade: Dashboard é fácil de usar

### Dependências

- Prometheus configurado
- Grafana configurado

---

## US-017: Geração de Relatórios Executivos

**Épica**: Integração e Visualização  
**Fase**: 4.2  
**Prioridade**: Baixa  
**Story Points**: 8  
**Sprint**: 16-17

### Descrição

Como administrador do sistema,  
Quero gerar relatórios executivos de governança em PDF/CSV,  
Para compartilhar status com stakeholders.

### Critérios de Aceite

- [ ] Relatórios gerados em PDF e CSV
- [ ] Relatórios incluem:
   - Resumo executivo
   - Distribuição por classificação
   - Serviços sem owner
   - Top 10 serviços mais ativos
- [ ] Relatórios gerados automaticamente (semanal)

### Regras de Negócio

1. **Conteúdo do Relatório:**
   - Resumo executivo (1 página)
   - Distribuição por classificação (gráfico)
   - Lista de serviços sem owner
   - Top 10 serviços mais ativos
   - Métricas de atualização

2. **Frequência:** Semanal (segunda-feira)

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/service/ReportService.java` (a criar)

### Testes

- [ ] Teste unitário: Relatório PDF gerado corretamente
- [ ] Teste unitário: Relatório CSV gerado corretamente
- [ ] Teste de integração: Relatório semanal funciona

### Dependências

- Biblioteca de geração de PDF (ex: iText, Apache PDFBox)
- US-012 (Consolidação Diária)

---

## US-018: API REST para Consulta de Snapshots

**Épica**: Integração e Visualização  
**Fase**: 4.2  
**Prioridade**: Média  
**Story Points**: 8  
**Sprint**: 17-18

### Descrição

Como desenvolvedor,  
Quero consultar snapshots de governança via API REST,  
Para integrar com outras ferramentas.

### Critérios de Aceite

- [ ] Endpoint `GET /api/v1/snapshots` lista todos os snapshots
- [ ] Endpoint `GET /api/v1/snapshots/{serviceId}` retorna snapshot específico
- [ ] Endpoint `GET /api/v1/snapshots/report` gera relatório
- [ ] API documentada com OpenAPI/Swagger
- [ ] Autenticação implementada

### Regras de Negócio

1. **Endpoints:**
   - `GET /api/v1/snapshots?page=0&size=100` - Lista paginada
   - `GET /api/v1/snapshots/{serviceId}` - Detalhes de um serviço
   - `GET /api/v1/snapshots/report?format=pdf|csv` - Relatório

2. **Autenticação:** Bearer token

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/controller/SnapshotController.java` (a criar)

**Documentação OpenAPI:**
- Swagger UI disponível em `/swagger-ui.html`

### Testes

- [ ] Teste de integração: Endpoints funcionam corretamente
- [ ] Teste de autenticação: Autenticação funciona
- [ ] Teste de documentação: Swagger UI funciona

### Dependências

- Spring Web (já incluído)
- SpringDoc OpenAPI (a adicionar)

---

## US-019: Análise de Utilização de Recursos

**Épica**: FinOps e Otimização  
**Fase**: 5.1  
**Prioridade**: Baixa  
**Story Points**: 13  
**Sprint**: 19-21

### Descrição

Como administrador do sistema,  
Quero que o sistema analise utilização de recursos (CPU, memória) dos serviços,  
Para identificar oportunidades de otimização.

### Critérios de Aceite

- [ ] Sistema coleta métricas de CPU e memória do Dynatrace
- [ ] Análise de 30 dias de dados
- [ ] Identificação de padrões de utilização
- [ ] Métricas de FinOps disponíveis

### Regras de Negócio

1. **Métricas Coletadas:**
   - CPU utilization (média, p50, p95, p99)
   - Memory utilization (média, p50, p95, p99)
   - Request rate (req/s)
   - Response time (p50, p95, p99)

2. **Análise:**
   - Janela: 30 dias
   - Identifica percentual de tempo em cada faixa de utilização

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/finops/ResourceUtilizationAnalyzer.java`
- `src/main/java/com/codingbetter/adapters/dynatrace/DynatraceClient.java`

**Métricas Prometheus:**
- `finops_services_analyzed_total`
- `finops_low_utilization_services_total`

### Testes

- [ ] Teste de integração: Métricas coletadas do Dynatrace
- [ ] Teste unitário: Análise de utilização funciona corretamente

### Dependências

- US-007 (Extração de Métricas)
- Dynatrace API com acesso a métricas de recursos

---

## US-020: Cálculo de Custos Atuais e Otimizados

**Épica**: FinOps e Otimização  
**Fase**: 5.1  
**Prioridade**: Baixa  
**Story Points**: 13  
**Sprint**: 21-23

### Descrição

Como administrador do sistema,  
Quero que o sistema calcule custos atuais e otimizados dos serviços no Azure,  
Para demonstrar economia potencial.

### Critérios de Aceite

- [ ] Custos calculados com precisão >95%
- [ ] Mapeamento serviceId → Azure ResourceId funciona
- [ ] Cálculo de custos otimizados baseado em recomendações
- [ ] Economia potencial calculada (mensal e anual)

### Regras de Negócio

1. **Custos Atuais:**
   - Coletados do Azure Cost Management API
   - Breakdown por componente (compute, storage, network)

2. **Custos Otimizados:**
   - Baseados em recomendações (downscale, rightsize)
   - Calculados usando pricing do Azure

3. **Economia:**
   - Mensal = custo atual - custo otimizado
   - Anual = economia mensal * 12
   - Percentual = (economia / custo atual) * 100

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/finops/CostCalculator.java`
- `src/main/java/com/codingbetter/adapters/azure/AzureCostClient.java`

**Métricas Prometheus:**
- `finops_potential_monthly_savings_usd`
- `finops_potential_annual_savings_usd`

### Testes

- [ ] Teste de integração: Custos coletados do Azure
- [ ] Teste unitário: Cálculos de economia corretos
- [ ] Teste de validação: Precisão >95%

### Dependências

- US-019 (Análise de Utilização)
- Azure Cost Management API configurada

---

## US-021: Geração de Recomendações de Otimização

**Épica**: FinOps e Otimização  
**Fase**: 5.2  
**Prioridade**: Baixa  
**Story Points**: 13  
**Sprint**: 23-25

### Descrição

Como administrador do sistema,  
Quero que o sistema gere recomendações de otimização (downscale, rightsize, decommission),  
Para identificar ações concretas de economia.

### Critérios de Aceite

- [ ] Recomendações geradas com confiança >70%
- [ ] Tipos de recomendação: DOWNSCALE, RIGHTSIZE, DECOMMISSION
- [ ] Recomendações incluem recursos sugeridos e economia estimada
- [ ] Riscos potenciais identificados

### Regras de Negócio

1. **DOWNSCALE:**
   - avgUtilization < 30% por >70% do tempo
   - Reduz instanceCount ou recursos

2. **RIGHTSIZE:**
   - avgUtilization < 10% por >90% do tempo
   - Reduz SKU/Tier (ex: Premium → Standard)

3. **DECOMMISSION:**
   - classification = NO_TRAFFIC
   - Economia = 100% do custo atual

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/finops/OptimizationRecommender.java`

**Métricas Prometheus:**
- `finops_optimization_opportunities_total{type}`
- `finops_recommendations_by_type{type="DOWNSCALE|RIGHTSIZE|DECOMMISSION"}`

### Testes

- [ ] Teste unitário: Recomendações geradas corretamente
- [ ] Teste de validação: Confiança >70%

### Dependências

- US-019 (Análise de Utilização)
- US-020 (Cálculo de Custos)

---

## US-022: Relatórios de Economia Potencial

**Épica**: FinOps e Otimização  
**Fase**: 5.2  
**Prioridade**: Baixa  
**Story Points**: 8  
**Sprint**: 25-26

### Descrição

Como administrador do sistema,  
Quero visualizar relatórios de economia potencial com cálculos auditáveis,  
Para justificar investimento em otimização.

### Critérios de Aceite

- [ ] Relatórios mostram economia clara e auditável
- [ ] Breakdown por tipo de recomendação
- [ ] Cálculos detalhados (custo atual, otimizado, economia)
- [ ] ROI e payback period calculados

### Regras de Negócio

1. **Conteúdo do Relatório:**
   - Total de serviços analisados
   - Serviços com oportunidades
   - Economia potencial mensal/anual total
   - Breakdown por tipo (DOWNSCALE, RIGHTSIZE, DECOMMISSION)
   - Top 10 oportunidades

2. **Cálculos:**
   - ROI = (annualSavings - implementationCost) / implementationCost * 100
   - Payback period = implementationCost / monthlySavings (meses)

### Detalhamentos Técnicos

**Arquivos Principais:**
- `src/main/java/com/codingbetter/finops/EconomyReportGenerator.java`

### Testes

- [ ] Teste unitário: Relatórios gerados corretamente
- [ ] Teste de validação: Cálculos são auditáveis

### Dependências

- US-021 (Recomendações de Otimização)

---

## US-023: Atualização do Backstage com Métricas FinOps

**Épica**: FinOps e Otimização  
**Fase**: 5.2  
**Prioridade**: Baixa  
**Story Points**: 5  
**Sprint**: 26

### Descrição

Como desenvolvedor,  
Quero ver métricas FinOps e recomendações do meu serviço no Backstage,  
Para entender oportunidades de otimização.

### Critérios de Aceite

- [ ] Sistema atualiza Backstage com métricas FinOps (Fase 5)
- [ ] Dados incluídos: utilização, custos, recomendações
- [ ] Atualização automática após análise semanal

### Regras de Negócio

1. **Dados da Fase 5:**
   - Métricas de recursos (CPU, memória)
   - Custos atuais e otimizados
   - Recomendações de otimização
   - Economia potencial

### Detalhamentos Técnicos

**Formato de Atualização (Fase 5):**
```json
{
  "metadata": {
    "annotations": {
      "governance.finops.cpuUtilization": "15.5",
      "governance.finops.memoryUtilization": "12.3",
      "governance.finops.currentMonthlyCost": "219.00",
      "governance.finops.optimizedMonthlyCost": "54.75",
      "governance.finops.monthlySavings": "164.25",
      "governance.finops.recommendation": "DOWNSCALE"
    }
  }
}
```

### Testes

- [ ] Teste de integração: Métricas FinOps aparecem no Backstage
- [ ] Teste de integração: Atualização automática funciona

### Dependências

- US-021 (Recomendações de Otimização)
- Backstage API configurada

---

## Resumo de Story Points por Fase

- **Fase 1**: 39 pontos (US-001 a US-006)
- **Fase 2**: 31 pontos (US-007 a US-010)
- **Fase 3**: 34 pontos (US-011 a US-014)
- **Fase 4**: 29 pontos (US-015 a US-018)
- **Fase 5**: 52 pontos (US-019 a US-023)

**Total**: 185 pontos

---

## Definição de Pronto (DoD) - Revisão

Para uma user story ser considerada "Pronta", ela deve:

- [ ] Código implementado e revisado
- [ ] Testes unitários escritos e passando (>80% cobertura)
- [ ] Testes de integração escritos e passando
- [ ] Documentação técnica atualizada
- [ ] Métricas Prometheus implementadas
- [ ] Logs estruturados implementados
- [ ] Aprovado em code review
- [ ] Deploy em ambiente de staging
- [ ] Validado pelo Product Owner
- [ ] Performance validada (se aplicável)

