# Guia de Deploy

## Pré-requisitos

- Kubernetes cluster
- Kafka/Confluent Cloud configurado
- Acesso à API GitHub
- Acesso à API Dynatrace
- Acesso à API Backstage

## Build

```bash
mvn clean package
docker build -f docker/Dockerfile.adapter -t catalog-traffic-engine:latest .
```

## Configuração

### Secrets

Criar secret no Kubernetes:

```bash
kubectl create secret generic catalog-traffic-engine-secrets \
  --from-literal=kafka-bootstrap-servers=<kafka-url> \
  --from-literal=github-organization=<github-org> \
  --from-literal=github-api-token=<github-token> \
  --from-literal=dynatrace-api-url=<dynatrace-url> \
  --from-literal=dynatrace-api-token=<token> \
  --from-literal=backstage-api-url=<backstage-url> \
  --from-literal=backstage-api-token=<token> \
  -n governance
```

### Namespace

```bash
kubectl create namespace governance
```

## Deploy

### 1. ConfigMap

```bash
kubectl apply -f k8s/configmap.yaml
```

### 2. Adapters

```bash
kubectl apply -f k8s/adapter-deployment.yaml
kubectl apply -f k8s/adapter-hpa.yaml
```

### 3. Consolidation

```bash
kubectl apply -f k8s/consolidation-coordinator.yaml
kubectl apply -f k8s/consolidation-worker-deployment.yaml
kubectl apply -f k8s/consolidation-worker-hpa.yaml
```

### 4. Kafka Topics

Configurar tópicos conforme `k8s/kafka-topics.yaml` no Confluent Cloud ou via Kafka Admin.

## Verificação

```bash
# Verificar pods
kubectl get pods -n governance

# Verificar logs
kubectl logs -f deployment/catalog-traffic-engine-adapter -n governance

# Verificar HPA
kubectl get hpa -n governance

# Verificar métricas
kubectl port-forward -n governance deployment/catalog-traffic-engine-adapter 8080:8080
curl http://localhost:8080/actuator/metrics
```

## Escalabilidade

### Ajustar número de workers

Editar `k8s/adapter-hpa.yaml` e `k8s/consolidation-worker-hpa.yaml`:

```yaml
minReplicas: 10
maxReplicas: 50
```

### Ajustar recursos

Editar deployments:

```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

## Troubleshooting

### Adapter não extrai dados

1. Verificar credenciais GitHub
2. Verificar credenciais Dynatrace
3. Verificar circuit breaker status
4. Verificar logs para erros de API

### Consolidação lenta

1. Aumentar número de workers
2. Verificar recursos (CPU/memória)
3. Verificar backlog no Kafka

### Backstage não atualiza

1. Verificar credenciais Backstage
2. Verificar buffer de snapshots
3. Verificar logs do BackstageIntegrationService

