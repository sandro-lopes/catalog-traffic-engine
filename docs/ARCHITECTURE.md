# Arquitetura - Catalog Traffic Engine

## Visão Geral

Catalog Traffic Engine é uma plataforma automatizada para descoberta de serviços, análise de tráfego e governança baseada em evidência de tráfego real extraída automaticamente de ferramentas existentes.

## Princípios Arquiteturais

- **Evidência > Intenção**: Dados reais de tráfego, não documentação manual
- **Automação > Processo manual**: Extração e processamento automatizados
- **Normalização antes da decisão**: Schema canônico versionado
- **Backstage como consumidor**: Read-only, nunca fonte de verdade
- **Governança como produto de dados**: Pipeline ETL completo

## Componentes Principais

### 1. Extração (E)

**Adaptadores**:
- `DynatraceAdapter`: Extração escalável de métricas Dynatrace
- `ElasticAdapter`: Extração de logs (futuro)
- `ApiGatewayAdapter`: Extração de métricas de API Gateway (futuro)

**Características**:
- Processamento paralelo com workers distribuídos
- Rate limiting e circuit breakers
- Cache de service discovery
- Batching inteligente

### 2. Transformação (T)

**Normalizador**:
- Converte eventos raw para schema canônico `ServiceActivityEvent.v1`
- Remove detalhes específicos de negócio
- Adiciona metadados padronizados

**Agregador Temporal**:
- Agrega eventos em janelas de 5 minutos
- Mantém determinismo através de ordenação
- Consolida listas de callers

### 3. Carga (L)

**Kafka/Confluent Cloud**:
- Tópico `governance.activity.raw`: Eventos normalizados
- Tópico `governance.activity.snapshot`: Snapshots consolidados
- Particionamento por `service.id` para paralelismo

### 4. Consolidação

**Job Diário**:
- Executa às 2 AM (configurável)
- Processa últimos 30 dias de eventos
- Gera snapshots via `DecisionEngine`

**DecisionEngine**:
- Aplica regras de classificação:
  - `lastSeen <= 7 dias` → ACTIVE
  - `8 <= lastSeen <= 30 dias` → LOW_USAGE
  - `lastSeen > 30 dias` → NO_TRAFFIC

### 5. Integração Backstage

**BackstageIntegrationService**:
- Consome snapshots do Kafka
- Atualiza entidades em batch
- Mapeia campos para formato Backstage:
  - `governance.traffic`
  - `governance.risk`
  - `governance.integration`

## Escalabilidade

### Para 2.000+ APIs

**Extração**:
- 10-20 workers paralelos
- Batch size: 50-100 serviços
- Rate limit: 10-20 req/s por worker
- Tempo estimado: 2-5 minutos

**Kafka**:
- 30 partições por tópico
- Replicação: 3
- Retention: 35 dias (raw), 90 dias (snapshot)

**Consolidação**:
- 1 worker por partição (30 workers)
- Processamento paralelo independente
- Tempo estimado: 10-30 minutos

**Auto-scaling**:
- HPA baseado em CPU (70%) e memória (80%)
- Adapters: 5-50 pods
- Workers: 10-100 pods

## Fluxo de Dados

```
Dynatrace → Adapter → Normalizer → Aggregator → Kafka (raw)
                                                      ↓
                                              Consolidation Worker
                                                      ↓
                                              DecisionEngine
                                                      ↓
                                              Kafka (snapshot)
                                                      ↓
                                              Backstage
```

## Schemas

### ServiceActivityEvent.v1
- `service.id`: Identificador do serviço
- `activity.count`: Contador de atividade
- `dependencies.callers`: Lista de callers
- `timestamps.window`: Janela de tempo
- `confidence.level`: Nível de confiança (HIGH, MEDIUM, LOW)
- `metadata`: Metadados (environment, source)

### ServiceActivitySnapshot.v1
- `service.id`: Identificador do serviço
- `receivesTraffic`: Boolean
- `trafficVolume`: Volume total
- `lastSeen`: Última atividade
- `activeCallers`: Lista de callers ativos
- `confidenceLevel`: Nível de confiança
- `classification`: ACTIVE, LOW_USAGE, NO_TRAFFIC
- `snapshotDate`: Data do snapshot

## Monitoramento

### Métricas Prometheus
- Taxa de extração (APIs/min)
- Latência p95/p99
- Taxa de erro por worker
- Utilização de recursos
- Backlog Kafka

### Alertas
- Extração > 10 min para 2k APIs
- Consolidação > 1 hora
- Taxa de erro > 5%
- Backlog Kafka > 100k eventos

## Tecnologias

- Java 21
- Spring Boot 3.2
- Apache Kafka / Confluent Cloud
- Resilience4j (circuit breaker, rate limiter, retry)
- Kubernetes
- Prometheus

