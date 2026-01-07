# Políticas Oficiais de Governança de Componentes Estruturantes

Este documento traduz **integralmente** o conteúdo do documento **"Regras de Governança – Resumo para Discussões Internas"** em **políticas oficiais**, com linguagem normativa, critérios de enforcement e aplicabilidade clara.

Cada política abaixo é numerada para manter rastreabilidade direta com a regra original (RG-XX).

---

## POL-01 — Política de Classificação de Componentes Estruturantes

**Origem:** RG-01

### Política

Todo componente que concentre regra de negócio crítica, seja reutilizável entre domínios ou possua alto impacto operacional **deve ser classificado formalmente como Componente Estruturante**.

### Enforcement

* Classificação obrigatória no Backstage
* Componentes mal classificados são bloqueados para evolução

---

## POL-02 — Política de Criação Exclusiva via Backstage

**Origem:** RG-02, RG-03

### Política

Componentes estruturantes **devem ser criados exclusivamente** por meio dos arquétipos oficiais do Backstage.

### Enforcement

* Criação manual de repositórios é proibida
* Pipelines validam fingerprint do template

---

## POL-03 — Política de Ownership Obrigatório

**Origem:** RG-04

### Política

Todo componente estruturante deve possuir:

* time dono oficial
* responsável técnico

### Enforcement

* Deploy bloqueado sem owner
* Serviços sem owner entram em estado RESTRICTED

---

## POL-04 — Política de Transferência de Ownership

**Origem:** RG-05

### Política

Transferência de ownership exige aceite explícito do novo time por workflow oficial.

### Enforcement

* Alteração direta de YAML bloqueada
* Auditoria obrigatória

---

## POL-05 — Política de Catálogo e Metadados Obrigatórios

**Origem:** RG-06, RG-07

### Política

Todo componente estruturante deve estar registrado no catálogo com metadados mínimos obrigatórios:

* nome
* domínio
* owner
* tipo
* ciclo de vida
* criticidade
* dependências

### Enforcement

* Build falha se metadados incompletos

---

## POL-06 — Política de Contratos Explícitos

**Origem:** RG-08

### Política

Todo componente estruturante deve possuir contrato explícito (OpenAPI ou AsyncAPI).

### Enforcement

* CI valida existência do contrato

---

## POL-07 — Política de OpenAPI Externo Obrigatório

**Origem:** RG-09

### Política

O contrato OpenAPI **não pode residir no repositório do serviço** e deve ser resolvido via gateway.

### Enforcement

* Pipeline falha sem referência válida ao contrato

---

## POL-08 — Política de Versionamento e Breaking Change

**Origem:** RG-10, RG-11

### Política

Breaking changes silenciosos são proibidos. Mudanças incompatíveis exigem nova versão do contrato.

### Enforcement

* Detecção automática de breaking change
* Bloqueio de deploy

---

## POL-09 — Política de Segurança e Exposição

**Origem:** RG-12, RG-13

### Política

APIs estruturantes:

* não podem ser acessadas diretamente pelo frontend
* não podem receber token JWT do usuário final

### Enforcement

* Gateway bloqueia exposição indevida

---

## POL-10 — Política de Comunicação Service-to-Service

**Origem:** RG-14

### Política

Comunicação entre serviços estruturantes deve usar OAuth2 client_credentials ou Managed Identity.

Exceções via token exchange são permitidas apenas para auditoria ou autorização regulatória.

---

## POL-11 — Política de Observabilidade Obrigatória

**Origem:** RG-15

### Política

Todo componente estruturante deve expor logs, métricas e traces padronizados.

### Enforcement

* Deploy bloqueado sem observabilidade

---

## POL-12 — Política de Detecção de Inatividade

**Origem:** RG-16

### Política

Serviços sem tráfego detectado por período definido entram automaticamente em avaliação de desligamento.

---

## POL-13 — Política de Ciclo de Vida

**Origem:** RG-17

### Política

Todo componente deve declarar ciclo de vida explícito:

* EXPERIMENTAL
* ACTIVE
* DEPRECATED
* RETIRED

---

## POL-14 — Política de Depreciação e Sunset

**Origem:** RG-18

### Política

Serviços em estado DEPRECATED devem possuir data de desligamento definida e comunicada.

### Enforcement

* Headers de depreciação obrigatórios
* Desligamento automático ao atingir sunsetDate

---

## POL-15 — Política de Convivência com Legado

**Origem:** RG-19, RG-20

### Política

Sistemas legados são tratados como bounded contexts externos e exigem anti-corruption layer.

---

## POL-16 — Política de Governança Automatizada

**Origem:** RG-21, RG-22

### Política

Regras de governança devem ser aplicadas por automação, não por aprovação manual.

### Enforcement

* Fail fast em build, deploy ou runtime

---

## POL-17 — Política de Integração ao SDLC

**Origem:** RG-23

### Política

Governança é parte integrante do SDLC e não etapa paralela.

---

## POL-18 — Política de Incentivo ao Desligamento

**Origem:** RG-24

### Política

Times não serão penalizados por desligar sistemas obsoletos.

---

## POL-19 — Política de Métricas de Governança

**Origem:** RG-25

### Política

Governança deve ser mensurada continuamente por métricas oficiais.

### Métricas

* % serviços com owner
* % criados via arquétipo
* % com contrato válido
* tempo médio de desligamento

---

## Princípio Final

> Se não atende às políticas, não evolui, não deploya e não opera.
