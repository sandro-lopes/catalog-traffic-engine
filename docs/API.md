# API Reference

## Endpoints de Observabilidade

### Health Check

```
GET /actuator/health
```

Retorna status de saúde da aplicação.

**Resposta:**
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP"
    },
    "kafka": {
      "status": "UP"
    }
  }
}
```

### Métricas

```
GET /actuator/metrics
```

Lista métricas disponíveis.

```
GET /actuator/metrics/{metricName}
```

Retorna valor de uma métrica específica.

**Exemplo:**
```
GET /actuator/metrics/governance_discovery_repositories_total
```

### Prometheus

```
GET /actuator/prometheus
```

Exporta métricas no formato Prometheus.

---

## Endpoints de Governança

### Listar Snapshots

```
GET /api/v1/snapshots
```

Lista todos os snapshots de governança com paginação.

**Query Parameters:**
- `page` (int, default: 0): Número da página
- `size` (int, default: 100): Tamanho da página
- `classification` (string, optional): Filtrar por classificação (ACTIVE, LOW_USAGE, NO_TRAFFIC)
- `sigla` (string, optional): Filtrar por sigla

**Resposta:**
```json
{
  "content": [
    {
      "service.id": "abc-api-usuario",
      "receivesTraffic": true,
      "trafficVolume": 1250,
      "lastSeen": "2024-01-15T09:30:00Z",
      "activeCallers": ["service1", "service2"],
      "confidenceLevel": "HIGH",
      "classification": "ACTIVE",
      "snapshotDate": "2024-01-15"
    }
  ],
  "page": {
    "number": 0,
    "size": 100,
    "totalElements": 2000,
    "totalPages": 20
  }
}
```

### Obter Snapshot por Service ID

```
GET /api/v1/snapshots/{serviceId}
```

Retorna snapshot detalhado de um serviço específico.

**Path Parameters:**
- `serviceId` (string): ID do serviço

**Resposta:**
```json
{
  "service.id": "abc-api-usuario",
  "receivesTraffic": true,
  "trafficVolume": 1250,
  "lastSeen": "2024-01-15T09:30:00Z",
  "activeCallers": ["service1", "service2"],
  "confidenceLevel": "HIGH",
  "classification": "ACTIVE",
  "snapshotDate": "2024-01-15",
  "finOpsMetrics": {
    "cpuUtilization": {
      "average": 15.5,
      "p95": 45.2
    },
    "memoryUtilization": {
      "average": 12.3,
      "p95": 38.7
    }
  },
  "costOptimization": {
    "recommendation": "DOWNSCALE",
    "currentMonthlyCost": 219.00,
    "optimizedMonthlyCost": 54.75,
    "monthlySavings": 164.25
  }
}
```

### Gerar Relatório

```
GET /api/v1/snapshots/report
```

Gera relatório executivo de governança.

**Query Parameters:**
- `format` (string, default: "pdf"): Formato do relatório (pdf, csv)
- `date` (string, optional): Data do snapshot (formato: YYYY-MM-DD)

**Resposta:**
- PDF: `Content-Type: application/pdf`
- CSV: `Content-Type: text/csv`

---

## Endpoints de Descoberta

### Listar Repositórios Descobertos

```
GET /api/v1/repositories
```

Lista todos os repositórios descobertos.

**Query Parameters:**
- `page` (int, default: 0): Número da página
- `size` (int, default: 100): Tamanho da página
- `sigla` (string, optional): Filtrar por sigla
- `type` (string, optional): Filtrar por tipo (api, bff, gtw, mfe)

**Resposta:**
```json
{
  "content": [
    {
      "name": "abc-api-usuario",
      "fullName": "empresa/abc-api-usuario",
      "sigla": "abc",
      "type": "api",
      "serviceName": "usuario",
      "serviceId": "abc-api-usuario",
      "primaryOwner": "joao.silva@empresa.com",
      "ownershipConfidence": 0.75,
      "ownershipSource": "commit-history",
      "topCommitters": [
        {
          "id": "joao.silva",
          "email": "joao.silva@empresa.com",
          "name": "João Silva",
          "commitCount": 45,
          "weightedScore": 67.5
        }
      ]
    }
  ],
  "page": {
    "number": 0,
    "size": 100,
    "totalElements": 2000,
    "totalPages": 20
  }
}
```

### Obter Repositório por Service ID

```
GET /api/v1/repositories/{serviceId}
```

Retorna detalhes de um repositório específico.

**Path Parameters:**
- `serviceId` (string): ID do serviço

---

## Endpoints de FinOps

### Listar Recomendações de Otimização

```
GET /api/v1/finops/recommendations
```

Lista recomendações de otimização de custos.

**Query Parameters:**
- `type` (string, optional): Filtrar por tipo (DOWNSCALE, RIGHTSIZE, DECOMMISSION)
- `minSavings` (double, optional): Filtrar por economia mínima (USD)

**Resposta:**
```json
{
  "content": [
    {
      "serviceId": "abc-api-usuario",
      "recommendation": {
        "type": "DOWNSCALE",
        "currentResources": {
          "cpuCores": 4,
          "memoryGB": 8,
          "instanceCount": 2
        },
        "suggestedResources": {
          "cpuCores": 2,
          "memoryGB": 4,
          "instanceCount": 1
        },
        "currentMonthlyCost": 219.00,
        "optimizedMonthlyCost": 54.75,
        "monthlySavings": 164.25,
        "annualSavings": 1971.00,
        "savingsPercent": 75.0,
        "confidence": "HIGH",
        "rationale": "Utilização média de 15% CPU e 12% memória por >70% do tempo",
        "risks": []
      }
    }
  ]
}
```

### Gerar Relatório de Economia

```
GET /api/v1/finops/report
```

Gera relatório de economia potencial.

**Query Parameters:**
- `format` (string, default: "pdf"): Formato do relatório (pdf, csv)

---

## Autenticação

Todos os endpoints de API requerem autenticação via Bearer token.

**Header:**
```
Authorization: Bearer {token}
```

---

## Configuração

### Variáveis de Ambiente

**Kafka:**
- `KAFKA_BOOTSTRAP_SERVERS`: URL do Kafka

**Dynatrace:**
- `DYNATRACE_API_URL`: URL da API Dynatrace
- `DYNATRACE_API_TOKEN`: Token de autenticação Dynatrace

**Backstage:**
- `BACKSTAGE_API_URL`: URL da API Backstage
- `BACKSTAGE_API_TOKEN`: Token de autenticação Backstage

**GitHub:**
- `GITHUB_ORGANIZATION`: Organização do GitHub
- `GITHUB_API_URL`: URL da API GitHub (default: https://api.github.com)
- `GITHUB_API_TOKEN`: Token de autenticação GitHub

**Azure:**
- `AZURE_COST_API_URL`: URL da API Azure Cost Management
- `AZURE_COST_API_TOKEN`: Token de autenticação Azure
- `AZURE_SUBSCRIPTION_ID`: ID da assinatura Azure

**Geral:**
- `ENVIRONMENT`: Ambiente (production, staging, etc.)

### Propriedades de Configuração

Ver `src/main/resources/application.yml` para todas as opções configuráveis.

---

## Documentação OpenAPI/Swagger

A documentação completa da API está disponível via Swagger UI:

```
GET /swagger-ui.html
```

Ou via OpenAPI JSON:

```
GET /v3/api-docs
```

## Schemas

### ServiceActivityEvent.v1

```json
{
  "service.id": "string",
  "activity.count": 0,
  "dependencies.callers": ["string"],
  "timestamps.window": {
    "start": "2024-01-01T00:00:00Z",
    "end": "2024-01-01T00:05:00Z"
  },
  "confidence.level": "HIGH|MEDIUM|LOW",
  "metadata": {
    "environment": "string",
    "source": "string"
  }
}
```

### ServiceActivitySnapshot.v1

```json
{
  "service.id": "string",
  "receivesTraffic": true,
  "trafficVolume": 0,
  "lastSeen": "2024-01-01T00:00:00Z",
  "activeCallers": ["string"],
  "confidenceLevel": "HIGH|MEDIUM|LOW",
  "classification": "ACTIVE|LOW_USAGE|NO_TRAFFIC",
  "snapshotDate": "2024-01-01",
  "finOpsMetrics": {
    "serviceId": "string",
    "analysisPeriod": {
      "start": "2024-01-01T00:00:00Z",
      "end": "2024-01-31T23:59:59Z"
    },
    "cpuUtilization": {
      "average": 15.5,
      "p50": 12.3,
      "p95": 45.2,
      "p99": 67.8,
      "peakUtilization": 89.5,
      "lowUtilizationPercent": 75.0
    },
    "memoryUtilization": {
      "average": 12.3,
      "p50": 10.1,
      "p95": 38.7,
      "p99": 56.2,
      "peakUtilization": 78.9,
      "lowUtilizationPercent": 80.0
    },
    "currentResources": {
      "cpuCores": 4,
      "memoryGB": 8,
      "instanceCount": 2,
      "resourceType": "App Service"
    },
    "utilizationPattern": {
      "avgUtilizationPercent": 13.9,
      "peakHours": ["09:00", "10:00", "11:00"],
      "lowUsageHours": ["00:00", "01:00", "02:00"]
    }
  },
  "costOptimization": {
    "type": "DOWNSCALE|RIGHTSIZE|DECOMMISSION",
    "suggestedResources": {
      "cpuCores": 2,
      "memoryGB": 4,
      "instanceCount": 1
    },
    "currentMonthlyCost": 219.00,
    "optimizedMonthlyCost": 54.75,
    "monthlySavings": 164.25,
    "annualSavings": 1971.00,
    "savingsPercent": 75.0,
    "confidence": "HIGH|MEDIUM|LOW",
    "rationale": "Utilização média de 15% CPU e 12% memória por >70% do tempo",
    "risks": ["Risco de degradação em picos de tráfego"]
  }
}
```

### CommitterInfo

```json
{
  "id": "joao.silva",
  "email": "joao.silva@empresa.com",
  "name": "João Silva",
  "commitCount": 45,
  "weightedScore": 67.5
}
```

