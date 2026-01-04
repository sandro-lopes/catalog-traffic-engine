# Product Backlog - Catalog Traffic Engine

Este documento apresenta o backlog do produto organizado por épicas e fases de implementação.

---

## Épica 1: Descoberta e Identificação de Serviços

**Objetivo**: Identificar automaticamente todos os serviços da organização e seus proprietários.

**Prioridade**: Alta  
**Fase**: 1  
**Duração Estimada**: 7 semanas

### User Stories

1. **US-001**: Como administrador, quero que o sistema descubra automaticamente repositórios via GitHub API para identificar todos os serviços da organização
2. **US-002**: Como administrador, quero que o sistema filtre repositórios por sigla, tipo e tags para focar apenas nos serviços relevantes
3. **US-003**: Como administrador, quero que o sistema crie Pull Requests com catalog-info.yaml para cada repositório descoberto
4. **US-004**: Como desenvolvedor, quero ver os top 10 committers do meu serviço no Backstage para identificar os principais contribuidores
5. **US-005**: Como administrador, quero que o sistema infira automaticamente o owner de um serviço baseado no histórico de commits
6. **US-006**: Como administrador, quero que o sistema atualize o Backstage com dados de descoberta e ownership automaticamente

---

## Épica 2: Extração e Normalização de Dados

**Objetivo**: Extrair métricas de atividade de sistemas legados e normalizar para schema canônico.

**Prioridade**: Alta  
**Fase**: 2  
**Duração Estimada**: 5 semanas

### User Stories

7. **US-007**: Como administrador, quero que o sistema extraia métricas de atividade do Dynatrace para todos os serviços descobertos
8. **US-008**: Como administrador, quero que o sistema normalize eventos de diferentes fontes para um schema canônico único
9. **US-009**: Como administrador, quero que o sistema agregue eventos em janelas temporais para reduzir volume de dados
10. **US-010**: Como administrador, quero que o sistema atualize o Backstage com dados de atividade (receivesTraffic, lastSeen, trafficVolume)

---

## Épica 3: Pipeline e Consolidação

**Objetivo**: Publicar eventos no Kafka e consolidar em snapshots diários para tomada de decisão.

**Prioridade**: Alta  
**Fase**: 3  
**Duração Estimada**: 5 semanas

### User Stories

11. **US-011**: Como administrador, quero que o sistema publique eventos normalizados no Kafka para processamento assíncrono
12. **US-012**: Como administrador, quero que o sistema consolide eventos diariamente em snapshots de status
13. **US-013**: Como administrador, quero que o sistema classifique automaticamente serviços como ACTIVE, LOW_USAGE ou NO_TRAFFIC
14. **US-014**: Como administrador, quero que o sistema atualize o Backstage com classificação de serviços automaticamente

---

## Épica 4: Integração e Visualização

**Objetivo**: Integrar com Backstage e criar visualizações para stakeholders.

**Prioridade**: Média  
**Fase**: 4  
**Duração Estimada**: 4 semanas

### User Stories

15. **US-015**: Como desenvolvedor, quero ver o status de governança do meu serviço no Backstage
16. **US-016**: Como administrador, quero visualizar dashboard com métricas de governança em tempo real
17. **US-017**: Como administrador, quero gerar relatórios executivos de governança em PDF/CSV
18. **US-018**: Como desenvolvedor, quero consultar snapshots de governança via API REST

---

## Épica 5: FinOps e Otimização

**Objetivo**: Identificar oportunidades de otimização de custos baseadas em utilização de recursos.

**Prioridade**: Baixa  
**Fase**: 5  
**Duração Estimada**: 5 semanas

### User Stories

19. **US-019**: Como administrador, quero que o sistema analise utilização de recursos (CPU, memória) dos serviços
20. **US-020**: Como administrador, quero que o sistema calcule custos atuais e otimizados dos serviços no Azure
21. **US-021**: Como administrador, quero que o sistema gere recomendações de otimização (downscale, rightsize, decommission)
22. **US-022**: Como administrador, quero visualizar relatórios de economia potencial com cálculos auditáveis
23. **US-023**: Como administrador, quero que o sistema atualize o Backstage com métricas FinOps e recomendações

---

## Ordem de Priorização

1. **Fase 1** (Épica 1): Descoberta e Identificação - **CRÍTICO**
2. **Fase 2** (Épica 2): Extração e Normalização - **ALTA**
3. **Fase 3** (Épica 3): Pipeline e Consolidação - **ALTA**
4. **Fase 4** (Épica 4): Integração e Visualização - **MÉDIA**
5. **Fase 5** (Épica 5): FinOps e Otimização - **BAIXA**

---

## Definição de Pronto (DoD)

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

---

## Critérios de Aceite Globais

- Sistema deve escalar para 2.000+ serviços
- Taxa de erro < 1% em todas as operações
- Tempo de resposta < 5 segundos para operações síncronas
- Disponibilidade > 99.9%
- Todas as operações devem ser auditáveis e determinísticas

