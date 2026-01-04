# Diagramas de Arquitetura - Catalog Traffic Engine

Este documento apresenta três níveis de diagramas de arquitetura: alto nível (contexto), médio nível (componentes) e baixo nível (detalhado).

---

## 1. Diagrama de Arquitetura - Alto Nível (Contexto)

Visão do sistema no contexto organizacional, mostrando interações com sistemas externos e stakeholders.

```mermaid
graph TB
    subgraph "Organização"
        subgraph "Catalog Traffic Engine"
            ETL[Pipeline ETL]
            KAFKA[Kafka/Event Hub]
            CONSOL[Consolidação]
            BACKSTAGE_INT[Integração Backstage]
        end
        
        BACKSTAGE[Backstage<br/>Catálogo de Serviços]
        DEVS[Times de Desenvolvimento]
        OPS[Equipe de Operações]
        FINOPS[Equipe FinOps]
    end
    
    subgraph "Fontes de Dados"
        DYNATRACE[Dynatrace<br/>APM & Observability]
        GITHUB[GitHub/GitLab<br/>Repositórios]
        AZURE[Azure Cost Management<br/>Custos e Recursos]
    end
    
    DYNATRACE -->|Métricas de Tráfego| ETL
    GITHUB -->|Descoberta de Serviços| ETL
    AZURE -->|Custos e Recursos| ETL
    
    ETL -->|Eventos Normalizados| KAFKA
    KAFKA -->|Eventos Raw| CONSOL
    CONSOL -->|Snapshots| KAFKA
    KAFKA -->|Snapshots| BACKSTAGE_INT
    BACKSTAGE_INT -->|Atualizações| BACKSTAGE
    
    BACKSTAGE -->|Visualização| DEVS
    BACKSTAGE -->|Governança| OPS
    BACKSTAGE -->|Otimizações| FINOPS
    
    style ETL fill:#e1f5ff
    style KAFKA fill:#fff4e1
    style CONSOL fill:#ffe1f5
    style BACKSTAGE_INT fill:#e1ffe1
    style BACKSTAGE fill:#f5e1ff
```

**Legenda:**
- **Azul claro**: Pipeline ETL (Extração, Transformação, Carga)
- **Amarelo claro**: Barramento de Eventos (Kafka)
- **Rosa claro**: Consolidação e Decisão
- **Verde claro**: Integração com Backstage
- **Roxo claro**: Backstage (consumidor)

---

## 2. Diagrama de Arquitetura - Médio Nível (Componentes)

Visão detalhada dos componentes internos e suas interações.

```mermaid
graph TB
    subgraph "Camada de Extração"
        RC[RepositoryCatalog<br/>Descoberta Git]
        DA[DynatraceAdapter]
        DBE[DynatraceBatchExtractor]
        DC[DynatraceClient]
    end
    
    subgraph "Camada de Transformação"
        EN[EventNormalizer<br/>Normalização]
        TA[TemporalAggregator<br/>Agregação]
        ES[EnrichmentService<br/>Enriquecimento]
    end
    
    subgraph "Barramento de Eventos"
        KAFKA_RAW[Kafka Topic<br/>governance.activity.raw]
        KAFKA_SNAP[Kafka Topic<br/>governance.activity.snapshot]
    end
    
    subgraph "Camada de Consolidação"
        CC[ConsolidationCoordinator<br/>Orquestrador]
        CW[ConsolidationWorker<br/>Workers Paralelos]
        SG[SnapshotGenerator<br/>Geração de Snapshots]
        DE[DecisionEngine<br/>Motor de Decisão]
        TC[TrafficClassifier<br/>Classificação]
    end
    
    subgraph "Camada de Integração"
        BIS[BackstageIntegrationService<br/>Integração]
        BC[BackstageClient<br/>Cliente HTTP]
    end
    
    subgraph "Camada FinOps (Futuro)"
        FJ[FinOpsAnalysisJob<br/>Job Semanal]
        RA[ResourceUtilizationAnalyzer<br/>Análise]
        CCALC[CostCalculator<br/>Cálculo de Custos]
        OR[OptimizationRecommender<br/>Recomendações]
    end
    
    RC -->|Lista de Serviços| DA
    DA -->|Métricas| DBE
    DBE -->|Eventos Raw| DC
    DC -->|Dados Normalizados| EN
    
    EN -->|Eventos Normalizados| TA
    TA -->|Eventos Agregados| ES
    ES -->|Eventos Enriquecidos| KAFKA_RAW
    
    KAFKA_RAW -->|Eventos| CW
    CW -->|Eventos Agregados| SG
    SG -->|Dados Consolidados| DE
    DE -->|Classificações| TC
    TC -->|Snapshots| KAFKA_SNAP
    
    KAFKA_SNAP -->|Snapshots| BIS
    BIS -->|Atualizações| BC
    
    CC -->|Coordena| CW
    
    FJ -.->|Análise| RA
    RA -.->|Métricas| CCALC
    CCALC -.->|Custos| OR
    OR -.->|Recomendações| KAFKA_SNAP
    
    style RC fill:#e1f5ff
    style DA fill:#e1f5ff
    style EN fill:#fff4e1
    style TA fill:#fff4e1
    style KAFKA_RAW fill:#ffe1f5
    style KAFKA_SNAP fill:#ffe1f5
    style CC fill:#e1ffe1
    style CW fill:#e1ffe1
    style BIS fill:#f5e1ff
    style FJ fill:#ffcccc,stroke-dasharray: 5 5
```

**Legenda:**
- **Linhas sólidas**: Fluxo principal (implementado)
- **Linhas tracejadas**: Fluxo futuro (FinOps)
- **Cores**: Agrupamento por camada funcional

---

## 3. Diagrama de Arquitetura - Baixo Nível (Detalhado)

Visão detalhada das classes, interfaces e dependências internas.

```mermaid
classDiagram
    %% Camada de Descoberta
    class RepositoryCatalog {
        -GitHubClient githubClient
        -RepositoryNameParser nameParser
        +discoverRepositories() Mono~List~RepositoryMetadata~~
    }
    
    class GitHubClient {
        -WebClient webClient
        +listAllRepositories(org) Flux~JsonNode~
        +getRepositoryTopics(org, repo) Mono~List~String~~
    }
    
    class RepositoryNameParser {
        +parse(name) ParsedRepository
        +matchesPattern(name) boolean
    }
    
    %% Camada de Adaptadores
    class DynatraceAdapter {
        -RepositoryCatalog repositoryCatalog
        -DynatraceBatchExtractor batchExtractor
        +extract(TimeWindow) List~RawActivityEvent~
        +extractReactive(TimeWindow) Flux~RawActivityEvent~
    }
    
    class DynatraceBatchExtractor {
        -DynatraceClient dynatraceClient
        +extractBatch(serviceIds, window, repos) Flux~RawActivityEvent~
    }
    
    class DynatraceClient {
        -WebClient webClient
        +getServiceMetrics(serviceId, start, end) Mono~DynatraceServiceMetrics~
        +getServiceCallers(serviceId, start, end) Mono~List~String~~
        +getResourceMetrics(serviceId, start, end) Mono~ResourceMetrics~
        +getServiceEntityDetails(serviceId) Mono~EntityDetails~
    }
    
    %% Camada de Normalização
    class EventNormalizer {
        -ObjectMapper objectMapper
        -EnrichmentService enrichmentService
        +normalize(RawActivityEvent) ServiceActivityEvent
        +normalizeStream(Flux~RawActivityEvent~) Flux~ServiceActivityEvent~
    }
    
    class EnrichmentService {
        +enrich(RawActivityEvent) Metadata
        +calculateConfidence(RawActivityEvent) ConfidenceLevel
    }
    
    class TemporalAggregator {
        +aggregate(Flux~ServiceActivityEvent~) Flux~ServiceActivityEvent~
    }
    
    %% Camada de Kafka
    class KafkaProducer {
        -KafkaTemplate~String, String~ kafkaTemplate
        +publishActivityEvent(ServiceActivityEvent) CompletableFuture
        +publishSnapshot(ServiceActivitySnapshot) CompletableFuture
        +send(topic, key, message) CompletableFuture
    }
    
    class KafkaConsumer {
        -KafkaListener
        +consumeActivityEvent(message, partition, offset)
        +consumeSnapshot(message, partition, offset)
    }
    
    %% Camada de Consolidação
    class ConsolidationCoordinator {
        -KafkaAdminClient adminClient
        +executeConsolidation()
    }
    
    class ConsolidationWorker {
        -KafkaConsumer consumer
        +processPartition(partitionId, windowDays) List~ServiceActivitySnapshot~
    }
    
    class SnapshotGenerator {
        +generateSnapshots(events) List~ServiceActivitySnapshot~
    }
    
    class DecisionEngine {
        -TrafficClassifier classifier
        +generateSnapshot(events) ServiceActivitySnapshot
    }
    
    class TrafficClassifier {
        +classify(lastSeen) Classification
    }
    
    %% Camada de Integração
    class BackstageIntegrationService {
        -BackstageClient backstageClient
        -BackstageMapper mapper
        +consumeSnapshot(message)
    }
    
    class BackstageClient {
        -WebClient webClient
        +updateEntity(serviceId, data) Mono~Void~
    }
    
    %% Camada FinOps
    class FinOpsAnalysisJob {
        -OptimizationRecommender recommender
        -EconomyReportGenerator reportGenerator
        +executeAnalysis()
    }
    
    class OptimizationRecommender {
        -ResourceUtilizationAnalyzer analyzer
        -CostCalculator calculator
        +generateRecommendation(serviceId, start, end) Mono~CostOptimizationRecommendation~
    }
    
    class ResourceUtilizationAnalyzer {
        +analyzeUtilization(serviceId, metrics, details) ServiceFinOpsMetrics
    }
    
    class CostCalculator {
        +calculateCurrentCost(azureCost) BigDecimal
        +calculateOptimizedCost(recommendation, details) BigDecimal
        +calculateSavings(current, optimized, recommendation) CostOptimizationRecommendation
    }
    
    class AzureCostClient {
        -WebClient webClient
        +getResourceCost(resourceId, start, end) Mono~ResourceCost~
        +getResourceDetails(resourceId) Mono~ResourceDetails~
    }
    
    %% Schemas
    class ServiceActivityEvent {
        +String serviceId
        +Long activityCount
        +List~String~ callers
        +TimeWindow window
        +RepositoryInfo repository
        +DiscoverySource discoverySource
    }
    
    class ServiceActivitySnapshot {
        +String serviceId
        +Boolean receivesTraffic
        +Long trafficVolume
        +Instant lastSeen
        +Classification classification
        +ServiceFinOpsMetrics finOpsMetrics
        +CostOptimizationRecommendation costOptimization
    }
    
    %% Relacionamentos
    RepositoryCatalog --> GitHubClient
    RepositoryCatalog --> RepositoryNameParser
    DynatraceAdapter --> RepositoryCatalog
    DynatraceAdapter --> DynatraceBatchExtractor
    DynatraceBatchExtractor --> DynatraceClient
    EventNormalizer --> EnrichmentService
    EventNormalizer --> ServiceActivityEvent
    TemporalAggregator --> ServiceActivityEvent
    KafkaProducer --> ServiceActivityEvent
    KafkaProducer --> ServiceActivitySnapshot
    ConsolidationCoordinator --> ConsolidationWorker
    ConsolidationWorker --> SnapshotGenerator
    SnapshotGenerator --> DecisionEngine
    DecisionEngine --> TrafficClassifier
    DecisionEngine --> ServiceActivitySnapshot
    BackstageIntegrationService --> BackstageClient
    BackstageIntegrationService --> ServiceActivitySnapshot
    FinOpsAnalysisJob --> OptimizationRecommender
    OptimizationRecommender --> ResourceUtilizationAnalyzer
    OptimizationRecommender --> CostCalculator
    OptimizationRecommender --> AzureCostClient
    CostCalculator --> CostOptimizationRecommendation
    ServiceActivitySnapshot --> ServiceFinOpsMetrics
    ServiceActivitySnapshot --> CostOptimizationRecommendation
```

**Legenda:**
- **Classes azuis**: Componentes principais
- **Classes verdes**: Schemas de dados
- **Setas**: Dependências e relacionamentos

---

## 4. Fluxo de Dados Completo

```mermaid
flowchart TD
    START([Início]) --> DISCOVERY[Descoberta de Serviços<br/>RepositoryCatalog]
    DISCOVERY --> EXTRACT[Extração<br/>DynatraceAdapter]
    EXTRACT --> NORMALIZE[Normalização<br/>EventNormalizer]
    NORMALIZE --> AGGREGATE[Agregação Temporal<br/>TemporalAggregator]
    AGGREGATE --> ENRICH[Enriquecimento<br/>EnrichmentService]
    ENRICH --> KAFKA_RAW[Kafka Raw<br/>governance.activity.raw]
    
    KAFKA_RAW --> CONSOL[Consolidação Diária<br/>ConsolidationCoordinator]
    CONSOL --> WORKER[Workers Paralelos<br/>ConsolidationWorker]
    WORKER --> GENERATE[Geração de Snapshots<br/>SnapshotGenerator]
    GENERATE --> DECIDE[Decisão<br/>DecisionEngine]
    DECIDE --> CLASSIFY[Classificação<br/>TrafficClassifier]
    CLASSIFY --> KAFKA_SNAP[Kafka Snapshot<br/>governance.activity.snapshot]
    
    KAFKA_SNAP --> BACKSTAGE_INT[Integração Backstage<br/>BackstageIntegrationService]
    BACKSTAGE_INT --> BACKSTAGE[Backstage<br/>Catálogo Atualizado]
    
    KAFKA_SNAP -.->|Futuro| FINOPS[Análise FinOps<br/>FinOpsAnalysisJob]
    FINOPS -.->|Futuro| OPTIMIZE[Otimizações<br/>OptimizationRecommender]
    
    style DISCOVERY fill:#e1f5ff
    style EXTRACT fill:#e1f5ff
    style NORMALIZE fill:#fff4e1
    style AGGREGATE fill:#fff4e1
    style KAFKA_RAW fill:#ffe1f5
    style CONSOL fill:#e1ffe1
    style KAFKA_SNAP fill:#ffe1f5
    style BACKSTAGE_INT fill:#f5e1ff
    style FINOPS fill:#ffcccc,stroke-dasharray: 5 5
```

---

## 5. Tecnologias e Dependências

```mermaid
graph LR
    subgraph "Runtime"
        JAVA[Java 21]
        SPRING[Spring Boot 3.3]
        REACTOR[Project Reactor]
    end
    
    subgraph "Integração"
        KAFKA_LIB[Spring Kafka]
        WEBFLUX[Spring WebFlux]
        RESILIENCE[Resilience4j]
    end
    
    subgraph "Cache"
        CAFFEINE[Caffeine Cache]
    end
    
    subgraph "Observabilidade"
        MICROMETER[Micrometer]
        PROMETHEUS[Prometheus]
    end
    
    subgraph "Infraestrutura"
        K8S[Kubernetes]
        KAFKA_INFRA[Kafka/Confluent]
        DYNATRACE_INFRA[Dynatrace API]
        GITHUB_INFRA[GitHub API]
        AZURE_INFRA[Azure API]
    end
    
    JAVA --> SPRING
    SPRING --> REACTOR
    SPRING --> KAFKA_LIB
    SPRING --> WEBFLUX
    SPRING --> RESILIENCE
    SPRING --> CAFFEINE
    SPRING --> MICROMETER
    MICROMETER --> PROMETHEUS
    
    KAFKA_LIB --> KAFKA_INFRA
    WEBFLUX --> DYNATRACE_INFRA
    WEBFLUX --> GITHUB_INFRA
    WEBFLUX --> AZURE_INFRA
    
    SPRING --> K8S
```

---

## Notas sobre os Diagramas

### Alto Nível (Contexto)
- Foco em **quem** usa o sistema e **quais** sistemas externos interagem
- Mostra o valor de negócio e stakeholders
- Útil para apresentações executivas

### Médio Nível (Componentes)
- Foco em **o quê** o sistema faz (componentes funcionais)
- Mostra fluxo de dados entre camadas
- Útil para arquitetos e desenvolvedores sênior

### Baixo Nível (Detalhado)
- Foco em **como** o sistema funciona (classes e dependências)
- Mostra detalhes de implementação
- Útil para desenvolvedores implementando ou mantendo o código

