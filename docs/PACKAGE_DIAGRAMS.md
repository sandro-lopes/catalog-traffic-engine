# Diagramas de Pacote - Catalog Traffic Engine

Este documento apresenta a estrutura de pacotes do sistema e suas dependências.

---

## 1. Estrutura de Pacotes Principal

```mermaid
graph TB
    subgraph "com.codingbetter"
        APP[CatalogTrafficEngineApplication]
        
        subgraph "adapters"
            ADAPTER_INT[ActivityAdapter<br/>Interface]
            subgraph "adapters.dynatrace"
                DT_ADAPTER[DynatraceAdapter]
                DT_CLIENT[DynatraceClient]
                DT_BATCH[DynatraceBatchExtractor]
                DT_CONFIG[DynatraceConfig]
            end
            subgraph "adapters.github"
                GH_CLIENT[GitHubClient]
                GH_CONFIG[GitHubConfig]
                GH_META[RepositoryMetadata]
                GH_PARSER[RepositoryNameParser]
            end
            subgraph "adapters.azure"
                AZ_CLIENT[AzureCostClient]
                AZ_CONFIG[AzureCostConfig]
                AZ_MAPPER[AzureResourceMapper]
            end
        end
        
        subgraph "discovery"
            REPO_CAT[RepositoryCatalog]
        end
        
        subgraph "normalization"
            NORM[EventNormalizer]
            ENRICH[EnrichmentService]
            TEMP_AGG[TemporalAggregator]
        end
        
        subgraph "orchestration"
            ORCH[ExtractionOrchestrator]
            PART[ServicePartitioner]
            POOL[WorkerPool]
            CB_MGR[CircuitBreakerManager]
        end
        
        subgraph "kafka"
            KAFKA_PROD[KafkaProducer]
            KAFKA_CONS[KafkaConsumer]
            KAFKA_CONFIG[KafkaConfig]
            TOPIC_CONFIG[TopicConfig]
        end
        
        subgraph "consolidation"
            COORD[ConsolidationCoordinator]
            WORKER[ConsolidationWorker]
            SNAP_GEN[SnapshotGenerator]
            DECISION[DecisionEngine]
            TRAFFIC[TrafficClassifier]
            PART_STRAT[PartitionStrategy]
        end
        
        subgraph "backstage"
            BS_CLIENT[BackstageClient]
            BS_CONFIG[BackstageConfig]
            BS_MAPPER[BackstageMapper]
        end
        
        subgraph "service"
            EXT_SVC[ExtractionService]
            BS_INT[BackstageIntegrationService]
        end
        
        subgraph "finops"
            FINOPS_JOB[FinOpsAnalysisJob]
            UTIL_ANAL[ResourceUtilizationAnalyzer]
            COST_CALC[CostCalculator]
            OPT_REC[OptimizationRecommender]
            ECON_REP[EconomyReportGenerator]
        end
        
        subgraph "schemas"
            SCHEMA_REG[SchemaRegistry]
            subgraph "schemas.v1"
                EVENT[ServiceActivityEvent]
                SNAPSHOT[ServiceActivitySnapshot]
                FINOPS_MET[ServiceFinOpsMetrics]
                COST_REC[CostOptimizationRecommendation]
            end
        end
        
        subgraph "config"
            RESILIENCE_CONFIG[Resilience4jConfig]
        end
    end
    
    style APP fill:#ffcccc
    style ADAPTER_INT fill:#e1f5ff
    style REPO_CAT fill:#e1f5ff
    style NORM fill:#fff4e1
    style ORCH fill:#fff4e1
    style KAFKA_PROD fill:#ffe1f5
    style COORD fill:#e1ffe1
    style BS_INT fill:#f5e1ff
    style FINOPS_JOB fill:#ffcccc,stroke-dasharray: 5 5
```

---

## 2. Dependências entre Pacotes

```mermaid
graph TB
    subgraph "Camada de Apresentação/Orquestração"
        APP[CatalogTrafficEngineApplication]
        EXT_SVC[ExtractionService]
        BS_INT[BackstageIntegrationService]
    end
    
    subgraph "Camada de Negócio"
        ORCH[orchestration]
        NORM[normalization]
        CONSOL[consolidation]
        FINOPS[finops]
    end
    
    subgraph "Camada de Adaptadores"
        ADAPTERS[adapters]
        DISCOVERY[discovery]
    end
    
    subgraph "Camada de Infraestrutura"
        KAFKA[kafka]
        BACKSTAGE[backstage]
    end
    
    subgraph "Camada de Dados"
        SCHEMAS[schemas]
    end
    
    APP --> EXT_SVC
    APP --> BS_INT
    APP --> FINOPS
    
    EXT_SVC --> ORCH
    EXT_SVC --> NORM
    EXT_SVC --> KAFKA
    
    ORCH --> ADAPTERS
    ORCH --> DISCOVERY
    
    NORM --> SCHEMAS
    NORM --> ADAPTERS
    
    CONSOL --> KAFKA
    CONSOL --> SCHEMAS
    
    BS_INT --> BACKSTAGE
    BS_INT --> KAFKA
    BS_INT --> SCHEMAS
    
    FINOPS --> ADAPTERS
    FINOPS --> SCHEMAS
    FINOPS --> KAFKA
    
    ADAPTERS --> SCHEMAS
    DISCOVERY --> ADAPTERS
    
    KAFKA --> SCHEMAS
    BACKSTAGE --> SCHEMAS
    
    style APP fill:#ffcccc
    style EXT_SVC fill:#ffe1f5
    style BS_INT fill:#ffe1f5
    style ORCH fill:#fff4e1
    style NORM fill:#fff4e1
    style CONSOL fill:#e1ffe1
    style FINOPS fill:#ffcccc,stroke-dasharray: 5 5
    style ADAPTERS fill:#e1f5ff
    style DISCOVERY fill:#e1f5ff
    style KAFKA fill:#ffe1f5
    style BACKSTAGE fill:#f5e1ff
    style SCHEMAS fill:#e1ffe1
```

---

## 3. Detalhamento por Camada

### 3.1 Camada de Adaptadores (adapters.*)

```mermaid
graph LR
    subgraph "adapters"
        INT[ActivityAdapter<br/>Interface]
        
        subgraph "dynatrace"
            ADAPTER[DynatraceAdapter]
            CLIENT[DynatraceClient]
            BATCH[DynatraceBatchExtractor]
            CONFIG[DynatraceConfig]
        end
        
        subgraph "github"
            GH_CLIENT[GitHubClient]
            GH_CONFIG[GitHubConfig]
            GH_META[RepositoryMetadata]
            GH_PARSER[RepositoryNameParser]
        end
        
        subgraph "azure"
            AZ_CLIENT[AzureCostClient]
            AZ_CONFIG[AzureCostConfig]
            AZ_MAPPER[AzureResourceMapper]
        end
    end
    
    ADAPTER -->|implements| INT
    ADAPTER --> CLIENT
    ADAPTER --> BATCH
    CLIENT --> CONFIG
    BATCH --> CLIENT
    
    GH_CLIENT --> GH_CONFIG
    GH_CLIENT --> GH_META
    GH_PARSER --> GH_META
    
    AZ_CLIENT --> AZ_CONFIG
    AZ_MAPPER --> AZ_CLIENT
    
    style INT fill:#e1f5ff
    style ADAPTER fill:#fff4e1
    style CLIENT fill:#fff4e1
```

### 3.2 Camada de Normalização (normalization.*)

```mermaid
graph LR
    subgraph "normalization"
        NORM[EventNormalizer]
        ENRICH[EnrichmentService]
        TEMP_AGG[TemporalAggregator]
    end
    
    NORM --> ENRICH
    NORM --> TEMP_AGG
    
    style NORM fill:#fff4e1
    style ENRICH fill:#e1ffe1
    style TEMP_AGG fill:#e1ffe1
```

### 3.3 Camada de Consolidação (consolidation.*)

```mermaid
graph LR
    subgraph "consolidation"
        COORD[ConsolidationCoordinator]
        WORKER[ConsolidationWorker]
        SNAP_GEN[SnapshotGenerator]
        DECISION[DecisionEngine]
        TRAFFIC[TrafficClassifier]
        PART_STRAT[PartitionStrategy]
    end
    
    COORD --> WORKER
    WORKER --> SNAP_GEN
    SNAP_GEN --> DECISION
    DECISION --> TRAFFIC
    COORD --> PART_STRAT
    
    style COORD fill:#e1ffe1
    style WORKER fill:#fff4e1
    style DECISION fill:#ffe1f5
```

### 3.4 Camada FinOps (finops.*)

```mermaid
graph LR
    subgraph "finops"
        JOB[FinOpsAnalysisJob]
        UTIL[ResourceUtilizationAnalyzer]
        COST[CostCalculator]
        REC[OptimizationRecommender]
        REP[EconomyReportGenerator]
    end
    
    JOB --> REC
    JOB --> REP
    REC --> UTIL
    REC --> COST
    
    style JOB fill:#ffcccc,stroke-dasharray: 5 5
    style REC fill:#fff4e1
    style COST fill:#e1ffe1
```

---

## 4. Princípios de Organização

### 4.1 Separação por Responsabilidade

- **adapters**: Tradução de dados de fontes externas
- **discovery**: Descoberta de serviços
- **normalization**: Normalização e enriquecimento
- **orchestration**: Orquestração de processos
- **consolidation**: Consolidação e decisão
- **finops**: Análise financeira (futuro)
- **kafka**: Infraestrutura de mensageria
- **backstage**: Integração com Backstage
- **schemas**: Schemas de dados versionados
- **service**: Serviços de alto nível

### 4.2 Dependências Permitidas

```
schemas (nenhuma dependência interna)
    ↑
adapters, discovery
    ↑
normalization, orchestration
    ↑
consolidation, finops
    ↑
kafka, backstage
    ↑
service
    ↑
CatalogTrafficEngineApplication
```

### 4.3 Regras de Dependência

1. **schemas**: Não depende de nenhum outro pacote interno
2. **adapters**: Depende apenas de schemas
3. **normalization**: Depende de adapters e schemas
4. **consolidation**: Depende de schemas e kafka
5. **service**: Pode depender de todos os outros pacotes
6. **finops**: Depende de adapters, schemas e kafka (isolado)

---

## 5. Mapeamento para Componentes de Deploy

```mermaid
graph TB
    subgraph "Deployment: adapter"
        ADAPTER_POD[DynatraceAdapter]
        BATCH_POD[DynatraceBatchExtractor]
        ORCH_POD[ExtractionOrchestrator]
        NORM_POD[EventNormalizer]
    end
    
    subgraph "Deployment: consolidation-coordinator"
        COORD_POD[ConsolidationCoordinator]
    end
    
    subgraph "Deployment: consolidation-worker"
        WORKER_POD[ConsolidationWorker]
        SNAP_POD[SnapshotGenerator]
        DEC_POD[DecisionEngine]
    end
    
    subgraph "Deployment: backstage-integration"
        BS_INT_POD[BackstageIntegrationService]
        BS_CLIENT_POD[BackstageClient]
    end
    
    subgraph "Deployment: finops (futuro)"
        FINOPS_POD[FinOpsAnalysisJob]
    end
    
    ADAPTER_POD --> ADAPTER_POD
    BATCH_POD --> ADAPTER_POD
    ORCH_POD --> ADAPTER_POD
    NORM_POD --> ADAPTER_POD
    
    COORD_POD --> COORD_POD
    
    WORKER_POD --> WORKER_POD
    SNAP_POD --> WORKER_POD
    DEC_POD --> WORKER_POD
    
    BS_INT_POD --> BS_INT_POD
    BS_CLIENT_POD --> BS_INT_POD
    
    FINOPS_POD -.->|Futuro| FINOPS_POD
    
    style ADAPTER_POD fill:#e1f5ff
    style COORD_POD fill:#e1ffe1
    style WORKER_POD fill:#e1ffe1
    style BS_INT_POD fill:#f5e1ff
    style FINOPS_POD fill:#ffcccc,stroke-dasharray: 5 5
```

---

## 6. Visão de Módulos (Futuro)

Para evoluir para uma arquitetura modular:

```mermaid
graph TB
    subgraph "Módulo: Core"
        SCHEMAS[schemas]
        CONFIG[config]
    end
    
    subgraph "Módulo: Discovery"
        DISCOVERY[discovery]
        ADAPTERS_GIT[adapters.github]
    end
    
    subgraph "Módulo: Extraction"
        ADAPTERS_DT[adapters.dynatrace]
        NORM[normalization]
        ORCH[orchestration]
    end
    
    subgraph "Módulo: Consolidation"
        CONSOL[consolidation]
    end
    
    subgraph "Módulo: Integration"
        BACKSTAGE[backstage]
        KAFKA[kafka]
    end
    
    subgraph "Módulo: FinOps"
        FINOPS[finops]
        ADAPTERS_AZ[adapters.azure]
    end
    
    DISCOVERY --> SCHEMAS
    ADAPTERS_GIT --> SCHEMAS
    ADAPTERS_DT --> SCHEMAS
    NORM --> SCHEMAS
    ORCH --> SCHEMAS
    CONSOL --> SCHEMAS
    BACKSTAGE --> SCHEMAS
    KAFKA --> SCHEMAS
    FINOPS --> SCHEMAS
    ADAPTERS_AZ --> SCHEMAS
    
    style SCHEMAS fill:#e1ffe1
    style DISCOVERY fill:#e1f5ff
    style ADAPTERS_DT fill:#e1f5ff
    style CONSOL fill:#e1ffe1
    style FINOPS fill:#ffcccc,stroke-dasharray: 5 5
```

