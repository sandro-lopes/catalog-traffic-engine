# Diagramas de Sequência - Catalog Traffic Engine

Este documento apresenta os diagramas de sequência para visualizar o fluxo de dados através das classes do sistema, dividido em três etapas principais: Extração, Transformação e Carga.

---

## 1. Etapa de Extração (Extract)

A etapa de extração é responsável por coletar dados de fontes externas (Dynatrace, Elastic, etc.) e convertê-los em eventos raw. A descoberta de serviços agora é feita via GitHub através do RepositoryCatalog.

```mermaid
sequenceDiagram
    participant Scheduler as Spring Scheduler
    participant ES as ExtractionService
    participant EO as ExtractionOrchestrator
    participant DA as DynatraceAdapter
    participant RC as RepositoryCatalog
    participant GC as GitHubClient
    participant Cache as Caffeine Cache
    participant GitHubAPI as GitHub API
    participant DBE as DynatraceBatchExtractor
    participant DC as DynatraceClient
    participant DynatraceAPI as Dynatrace API

    Scheduler->>ES: executeExtraction() (a cada 5 min)
    activate ES
    
    ES->>ES: Cria TimeWindow (últimos 5 min)
    ES->>EO: orchestrateExtraction(window)
    activate EO
    
    EO->>DA: extractReactive(window)
    activate DA
    
    DA->>RC: discoverRepositories()
    activate RC
    
    RC->>Cache: getIfPresent("all-repositories")
    Cache-->>RC: null (cache expirado)
    
    RC->>GC: listAllRepositories(organization)
    activate GC
    
    loop Paginação (100 repos por página)
        GC->>GitHubAPI: GET /orgs/{org}/repos?per_page=100&page={page}
        GitHubAPI-->>GC: Lista de repositórios (JSON)
        GC-->>RC: Flux<JsonNode>
    end
    
    deactivate GC
    
    RC->>RC: convertToMetadata(JsonNode) - parse nome, tags
    RC->>RC: matchesFilters() - filtra por sigla, tipo, tags
    RC->>RC: ownerInferenceEngine.inferOwner() - infere ownership
    RC->>Cache: put("all-repositories", repositories)
    RC-->>DA: Mono<List<RepositoryMetadata>>
    deactivate RC
    
    DA->>DA: Extrai serviceIds de RepositoryMetadata
    DA->>DA: Cria mapa serviceId -> RepositoryMetadata
    DA->>DBE: extractBatch(serviceIds, window, repoMap)
    activate DBE
    
    DBE->>DBE: Divide em batches (50-100 serviços)
    
    loop Para cada batch
        loop Para cada serviço no batch (paralelo)
            DBE->>DC: getServiceMetrics(serviceId, startTime, endTime)
            activate DC
            DC->>DynatraceAPI: GET /api/v2/timeseries/query?...
            DynatraceAPI-->>DC: Métricas (requestCount)
            DC-->>DBE: DynatraceServiceMetrics
            deactivate DC
            
            DBE->>DC: getServiceCallers(serviceId, startTime, endTime)
            activate DC
            DC->>DynatraceAPI: GET /api/v2/entities/{id}/serviceFromRelationships
            DynatraceAPI-->>DC: Lista de callers
            DC-->>DBE: List<String> callers
            deactivate DC
            
            DBE->>DBE: createRawEvent(serviceId, metrics, callers, window, repoMetadata)
        end
    end
    
    DBE-->>DA: Flux<RawActivityEvent>
    deactivate DBE
    
    DA-->>EO: Flux<RawActivityEvent>
    deactivate DA
    
    EO-->>ES: Flux<RawActivityEvent> (eventos raw de todas as fontes)
    deactivate EO
    
    deactivate ES
```

**Notas importantes:**
- A descoberta de serviços é feita via GitHub (RepositoryCatalog), não mais via Dynatrace
- O cache de repositórios evita consultas repetidas à GitHub API (TTL configurável, padrão 240 minutos)
- RepositoryCatalog aplica filtros (sigla, tipo, tags) e infere ownership
- DynatraceAdapter recebe serviceIds do RepositoryCatalog e extrai métricas para esses serviços
- Processamento paralelo com múltiplos workers (configurável)
- Rate limiting e circuit breaker protegem contra sobrecarga das APIs

---

## 2. Etapa de Transformação (Transform)

A etapa de transformação normaliza eventos raw para o schema canônico e agrega temporalmente. Inclui informações de repositório e discovery source quando disponíveis.

```mermaid
sequenceDiagram
    participant ES as ExtractionService
    participant EN as EventNormalizer
    participant ESvc as EnrichmentService
    participant TA as TemporalAggregator
    participant Schema as ServiceActivityEvent.v1

    ES->>EN: normalizeStream(Flux<RawActivityEvent>)
    activate EN
    
    loop Para cada RawActivityEvent
        EN->>EN: extractRawData(rawEvent)
        EN->>EN: extractActivityCount(rawData, source)
        EN->>EN: extractCallers(rawData, source)
        EN->>EN: extractTimeWindow(rawData, source)
        EN->>EN: extractRepositoryMetadata(rawData)
        
        EN->>ESvc: enrich(rawEvent)
        activate ESvc
        ESvc->>ESvc: extractEnvironment(rawEvent)
        ESvc-->>EN: Metadata(environment, source)
        deactivate ESvc
        
        EN->>ESvc: calculateConfidence(rawEvent)
        activate ESvc
        ESvc-->>EN: ConfidenceLevel (HIGH/MEDIUM/LOW)
        deactivate ESvc
        
        EN->>Schema: new ServiceActivityEvent()
        EN->>Schema: setServiceId()
        EN->>Schema: setActivityCount()
        EN->>Schema: setCallers()
        EN->>Schema: setWindow()
        EN->>Schema: setMetadata()
        EN->>Schema: setConfidenceLevel()
        alt RepositoryMetadata disponível
            EN->>Schema: setRepository(repoInfo)
            EN->>Schema: setDiscoverySource(GITHUB)
        else Sem RepositoryMetadata
            EN->>Schema: setDiscoverySource(DYNATRACE)
        end
        
        EN-->>EN: ServiceActivityEvent normalizado
    end
    
    EN-->>ES: Flux<ServiceActivityEvent>
    deactivate EN
    
    ES->>TA: aggregate(Flux<ServiceActivityEvent>)
    activate TA
    
    TA->>TA: collectList() (coleta todos os eventos)
    TA->>TA: sort() (ordena por timestamp)
    TA->>TA: groupBy(serviceId + roundedWindow)
    
    loop Para cada grupo (serviceId + janela de 5 min)
        TA->>TA: aggregateGroup(events)
        TA->>TA: sum(activityCount)
        TA->>TA: consolidate(callers) - remove duplicatas
        TA->>TA: min(window.start), max(window.end)
        TA->>TA: max(confidenceLevel)
        TA->>Schema: new ServiceActivityEvent() (agregado)
    end
    
    TA-->>ES: Flux<ServiceActivityEvent> (agregado)
    deactivate TA
    
    ES->>ES: Eventos prontos para publicação
```

**Notas importantes:**
- Normalização remove detalhes específicos de cada fonte
- EventNormalizer extrai RepositoryMetadata quando disponível (descoberta via GitHub)
- Discovery source é definido (GITHUB ou DYNATRACE) baseado na presença de RepositoryMetadata
- Enriquecimento adiciona metadados padronizados
- Agregação temporal reduz volume em ~90% (janelas de 5 minutos)
- Processamento determinístico através de ordenação

---

## 3. Etapa de Carga (Load) e Consolidação

A etapa de carga publica eventos no Kafka e executa consolidação diária para gerar snapshots.

### 3.1 Publicação Inicial (ExtractionService)

```mermaid
sequenceDiagram
    participant ES as ExtractionService
    participant KP as KafkaProducer
    participant Kafka as Kafka Topic
    participant Topic as governance.activity.raw

    ES->>KP: publishActivityEvent(event)
    activate KP
    
    KP->>KP: objectMapper.writeValueAsString(event)
    KP->>KP: new ProducerRecord(topic, serviceId, json)
    KP->>Kafka: send(record)
    activate Kafka
    
    Kafka->>Kafka: hash(serviceId) % partitions
    Kafka->>Topic: write(partition, offset, json)
    Topic-->>Kafka: offset confirmado
    Kafka-->>KP: SendResult
    deactivate Kafka
    
    KP-->>ES: CompletableFuture<SendResult>
    deactivate KP
```

### 3.2 Consolidação Diária

```mermaid
sequenceDiagram
    participant Scheduler as Spring Scheduler (Cron)
    participant CC as ConsolidationCoordinator
    participant Admin as Kafka AdminClient
    participant CW as ConsolidationWorker
    participant Kafka as Kafka Topic
    participant SG as SnapshotGenerator
    participant DE as DecisionEngine
    participant TC as TrafficClassifier
    participant KP as KafkaProducer
    participant Topic as governance.activity.snapshot
    participant BIS as BackstageIntegrationService
    participant BC as BackstageClient
    participant Backstage as Backstage API

    Scheduler->>CC: executeConsolidation() (2 AM diário)
    activate CC
    
    CC->>Admin: describeTopics("governance.activity.raw")
    Admin-->>CC: List<Integer> partitions (30 partições)
    
    par Processamento paralelo por partição
        CC->>CW: processPartition(partitionId=0, windowDays=30)
        activate CW
        
        CW->>Kafka: assign(partition=0)
        CW->>Kafka: seekToBeginning()
        
        loop Lê eventos dos últimos 30 dias
            CW->>Kafka: poll(timeout=5s)
            Kafka-->>CW: ConsumerRecords
            CW->>CW: filter(timestamp >= cutoffTime)
            CW->>CW: deserialize(ServiceActivityEvent)
        end
        
        CW->>SG: generateSnapshots(events)
        activate SG
        
        SG->>SG: groupBy(serviceId)
        
        loop Para cada serviceId
            SG->>DE: generateSnapshot(serviceId, events)
            activate DE
            
            DE->>DE: sum(activityCount) -> trafficVolume
            DE->>DE: max(window.end) -> lastSeen
            DE->>DE: consolidate(callers) -> activeCallers
            DE->>DE: max(confidenceLevel) -> confidenceLevel
            
            DE->>TC: classify(lastSeen)
            activate TC
            TC->>TC: daysSinceLastSeen = now - lastSeen
            alt lastSeen <= 7 dias
                TC-->>DE: ACTIVE
            else 8 <= lastSeen <= 30 dias
                TC-->>DE: LOW_USAGE
            else lastSeen > 30 dias
                TC-->>DE: NO_TRAFFIC
            end
            deactivate TC
            
            DE->>DE: new ServiceActivitySnapshot()
            DE-->>SG: ServiceActivitySnapshot
            deactivate DE
        end
        
        SG-->>CW: List<ServiceActivitySnapshot>
        deactivate SG
        
        loop Para cada snapshot
            CW->>KP: publishSnapshot(snapshot)
            activate KP
            KP->>Topic: send(serviceId, snapshotJson)
            Topic-->>KP: SendResult
            KP-->>CW: CompletableFuture
            deactivate KP
        end
        
        CW-->>CC: List<ServiceActivitySnapshot>
        deactivate CW
    end
    
    CC->>CC: waitForAllWorkers()
    CC-->>CC: Job concluído
    deactivate CC
    
    Note over Topic: Snapshots publicados no tópico
    
    Topic->>BIS: consumeSnapshot(message)
    activate BIS
    
    BIS->>BIS: objectMapper.readValue(snapshot)
    BIS->>BIS: snapshotBuffer.put(serviceId, snapshot)
    
    BIS-->>BIS: Buffer acumulado
    deactivate BIS
    
    Note over BIS: Scheduler executa a cada 60s
    
    BIS->>BIS: syncToBackstage()
    activate BIS
    
    BIS->>BIS: BackstageMapper.toBackstageFormat(snapshots)
    BIS->>BC: updateEntitiesBatch(entitiesData)
    activate BC
    
    BC->>Backstage: POST /api/catalog/entities/batch
    Backstage-->>BC: 200 OK
    BC-->>BIS: Mono<Void>
    deactivate BC
    
    BIS->>BIS: snapshotBuffer.clear()
    BIS-->>BIS: Sincronização concluída
    deactivate BIS
```

**Notas importantes:**
- Consolidação processa 30 dias de eventos em paralelo (1 worker por partição)
- Classificação determinística baseada em regras de negócio
- Snapshots são publicados no Kafka para consumo assíncrono
- Backstage recebe atualizações em batch a cada 60 segundos

---

## Resumo do Fluxo Completo

```
1. EXTRAÇÃO (a cada 5 min)
   Scheduler → ExtractionService → ExtractionOrchestrator → 
   DynatraceAdapter → RepositoryCatalog → GitHubClient → GitHub API
   DynatraceAdapter → DynatraceBatchExtractor → DynatraceClient → Dynatrace API

2. TRANSFORMAÇÃO (streaming)
   EventNormalizer → EnrichmentService → TemporalAggregator
   (Inclui extração de RepositoryMetadata e definição de discoverySource)

3. CARGA INICIAL
   KafkaProducer → Kafka (governance.activity.raw)

4. CONSOLIDAÇÃO (diária às 2 AM)
   ConsolidationCoordinator → ConsolidationWorker (paralelo) → 
   SnapshotGenerator → DecisionEngine → TrafficClassifier → 
   KafkaProducer → Kafka (governance.activity.snapshot)

5. INTEGRAÇÃO BACKSTAGE (a cada 60s)
   BackstageIntegrationService → BackstageClient → Backstage API
```

---

## Pontos de Escalabilidade

- **Extração**: Processamento paralelo com 10-20 workers, rate limiting por worker
- **Transformação**: Streaming reativo, sem bloqueios
- **Carga**: Kafka particionado (30 partições), permite paralelismo horizontal
- **Consolidação**: 1 worker por partição (30 workers paralelos)
- **Backstage**: Batch updates reduzem carga na API

