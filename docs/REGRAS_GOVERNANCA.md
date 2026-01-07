# Regras de Governança – Resumo para Discussões Internas

Este documento consolida **apenas as regras de governança já definidas**, sem plano, backlog ou implementação, para servir como base objetiva de alinhamento interno, decisões arquiteturais e comunicação com times.

---

## 1. Escopo e Classificação

### RG-01 — Definição de Componente Estruturante

Todo componente que:

* concentra **regra de negócio crítica** (ex: simulação, elegibilidade, proposta)
* é reutilizável por mais de um domínio ou produto
* tem potencial de alto impacto operacional

É classificado como **Componente Estruturante** e está sujeito a todas as regras abaixo.

---

## 2. Criação e Padronização

### RG-02 — Criação obrigatória via Backstage

Nenhum componente estruturante pode ser criado fora do **Backstage Scaffolder oficial**.

### RG-03 — Arquétipos oficiais

Todo estruturante deve nascer a partir de um **arquétipo aprovado**, contendo:

* estrutura de projeto
* pipelines
* observabilidade
* segurança
* documentação mínima

Criações manuais são proibidas.

---

## 3. Ownership e Responsabilidade

### RG-04 — Ownership obrigatório

Todo componente deve ter:

* **time dono oficial**
* **responsável técnico**

Componentes sem owner são considerados **ilegais**.

### RG-05 — Transferência formal de ownership

Mudanças de ownership exigem:

* registro no catálogo
* aceite explícito do novo time

---

## 4. Catálogo e Metadados

### RG-06 — Registro obrigatório no catálogo

Todo componente estruturante deve estar registrado no **System Catalog (Backstage)**.

### RG-07 — Metadados mínimos obrigatórios

Campos obrigatórios incluem:

* nome
* domínio
* owner
* tipo
* ciclo de vida
* criticidade
* dependências

---

## 5. Contratos e Integração

### RG-08 — Contrato explícito obrigatório

Todo estruturante deve possuir contrato formal:

* OpenAPI (HTTP)
* AsyncAPI (eventos)

### RG-09 — OpenAPI fora do repositório é permitido

Desde que:

* exista versionamento
* haja vínculo explícito com o serviço
* o gateway seja a fonte da verdade

---

## 6. Versionamento e Mudanças

### RG-10 — Proibição de breaking change silencioso

Breaking changes exigem:

* nova versão
* comunicação formal
* período de convivência

### RG-11 — Política de versionamento

Versões devem seguir padrão definido (ex: semver ou compatível).

---

## 7. Segurança e Autenticação

### RG-12 — Frontend nunca chama estruturante diretamente

APIs estruturantes **não podem** ser expostas ao frontend.

### RG-13 — Token do usuário não propaga para estruturante

Tokens JWT de frontend não devem ser repassados para APIs estruturantes.

### RG-14 — Comunicação service-to-service obrigatória

Estruturantes devem ser chamados usando:

* OAuth2 client_credentials
* Managed Identity (quando disponível)

---

## 8. Observabilidade e Tráfego

### RG-15 — Observabilidade obrigatória

Todo estruturante deve expor:

* métricas
* logs
* traces

### RG-16 — Detecção automática de inatividade

Serviços sem tráfego detectado por período definido são candidatos a desligamento.

---

## 9. Ciclo de Vida e Desligamento

### RG-17 — Ciclo de vida explícito

Todo componente deve ter status claro:

* experimental
* ativo
* deprecated
* desligado

### RG-18 — Serviços deprecated têm data de fim

Depreciação sem data de desligamento não é permitida.

---

## 10. Convivência com Legado

### RG-19 — Legado é domínio externo

Mainframe e sistemas legados são tratados como **bounded contexts externos**.

### RG-20 — Anti-corruption layer obrigatório

Integrações com legado devem usar camadas de isolamento.

---

## 11. Governança Automatizada

### RG-21 — Governança by design

Regras de governança devem ser aplicadas via:

* pipelines
* políticas
* agentes de IA

Não via aprovação manual.

### RG-22 — Fail fast

Componentes que não atendem às regras:

* não buildam
* não deployam
* não recebem tráfego

---

## 12. Cultura e Incentivos

### RG-23 — Governança integrada ao SDLC

Governança não é etapa paralela, é parte do fluxo de desenvolvimento.

### RG-24 — Incentivo ao desligamento

Times são incentivados a desligar serviços obsoletos sem penalização.

---

## 13. Métricas de Governança

### RG-25 — Governança é mensurável

A governança será avaliada por métricas como:

* % serviços com owner
* % com contrato
* % criados via arquétipo
* tempo médio de desligamento

---

## Princípio Final

> **Se não consegue nascer certo, não nasce.**
