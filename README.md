# Catalog Traffic Engine

Catalog Traffic Engine é uma plataforma automatizada para descoberta de serviços, análise de tráfego e governança baseada em evidência de tráfego real extraída automaticamente de ferramentas existentes.

## Arquitetura

O sistema segue uma arquitetura ETL (Extract, Transform, Load) com os seguintes componentes:

- **Extração (E)**: Adaptadores para Dynatrace, Elastic, API Gateway
- **Transformação (T)**: Normalização e agregação temporal
- **Carga (L)**: Publicação em Kafka/Confluent Cloud
- **Consolidação**: Job diário que gera snapshots de status
- **Integração**: Consumo read-only pelo Backstage

## Escalabilidade

Projetado para escalar para 2.000+ APIs com:
- Processamento paralelo distribuído
- Kafka particionado
- Auto-scaling via Kubernetes HPA
- Rate limiting e circuit breakers

## Tecnologias

- Java 21
- Spring Boot 3.2
- Apache Kafka / Confluent Cloud
- Resilience4j
- Kubernetes

## Documentação

- **Arquitetura**: `docs/ARCHITECTURE.md` - Visão geral da arquitetura e componentes
- **Diagramas de Arquitetura**: `docs/ARCHITECTURE_DIAGRAMS.md` - Diagramas de alto, médio e baixo nível
- **Diagramas de Pacote**: `docs/PACKAGE_DIAGRAMS.md` - Estrutura de pacotes e dependências
- **Diagramas de Sequência**: `docs/SEQUENCE_DIAGRAMS.md` - Fluxo detalhado por etapas (Extração, Transformação, Carga)
- **Plano de Implementação**: `docs/IMPLEMENTATION_PLAN.md` - Plano detalhado de implementação em fases
- **Deploy**: `docs/DEPLOYMENT.md` - Guia de deploy e configuração
- **API**: `docs/API.md` - Referência de API e schemas

