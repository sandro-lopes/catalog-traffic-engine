# Workflows Reutilizáveis para Enforcements de Governança

Este diretório contém workflows reutilizáveis do GitHub Actions que validam cada enforcement mapeado em `ENFORCEMENT_MAPPING_NOVOS_ESTRUTURANTES.md`. Estes workflows podem ser chamados via `uses:` no workflow CI de cada projeto estruturante criado via Scaffolder.

## Estrutura

Cada workflow é uma action reutilizável que pode ser chamada de outros workflows. Todos os workflows seguem o padrão `workflow_call` do GitHub Actions.

## Workflows Disponíveis

### 1. validate-classification.yml (POL-01)
Valida a classificação do componente como "estruturante" no repositório e no Backstage.

**Inputs:**
- `service-name` (required): Nome do serviço/componente
- `backstage-url` (optional): URL da API do Backstage
- `required-tag` (optional): Tag obrigatória (default: "estruturante")

**Secrets:**
- `BACKSTAGE_API_TOKEN` (optional): Token da API do Backstage

**Exemplo de uso:**
```yaml
- name: Validate Classification
  uses: ./.github/workflows-reusable/validate-classification.yml
  with:
    service-name: my-service
    backstage-url: https://backstage.example.com
  secrets:
    BACKSTAGE_API_TOKEN: ${{ secrets.BACKSTAGE_API_TOKEN }}
```

---

### 2. validate-scaffolder-tag.yml (POL-02)
Valida a presença de tag especial indicando que o componente foi criado via Backstage Scaffolder.

**Inputs:**
- `required-tag` (optional): Tag obrigatória (default: "created-via-scaffolder")
- `tag-location` (optional): Onde verificar a tag (repository|backstage|both, default: both)

**Secrets:**
- `BACKSTAGE_API_TOKEN` (optional): Token da API do Backstage

**Exemplo de uso:**
```yaml
- name: Validate Scaffolder Tag
  uses: ./.github/workflows-reusable/validate-scaffolder-tag.yml
  with:
    required-tag: created-via-scaffolder
    tag-location: both
```

---

### 3. validate-template-fingerprint.yml (POL-02)
Valida o fingerprint dos arquivos de template contra um valor aprovado.

**Inputs:**
- `approved-fingerprint` (required): Fingerprint aprovado (SHA256 hash)
- `template-files` (optional): Lista de arquivos separados por vírgula (default: "pom.xml,Dockerfile")
- `fingerprint-algorithm` (optional): Algoritmo de hash (sha256|sha512, default: sha256)

**Exemplo de uso:**
```yaml
- name: Validate Template Fingerprint
  uses: ./.github/workflows-reusable/validate-template-fingerprint.yml
  with:
    approved-fingerprint: ${{ secrets.APPROVED_TEMPLATE_FINGERPRINT }}
    template-files: "pom.xml,Dockerfile"
```

---

### 4. validate-owner.yml (POL-03)
Valida a presença de owner no Backstage e nas annotations do deployment.yaml.

**Inputs:**
- `service-name` (required): Nome do serviço/componente
- `backstage-url` (optional): URL da API do Backstage
- `deployment-path` (optional): Caminho para deployment.yaml (default: "k8s/deployment.yaml")
- `validate-deployment` (optional): Validar owner no deployment (default: true)

**Secrets:**
- `BACKSTAGE_API_TOKEN` (optional): Token da API do Backstage

**Exemplo de uso:**
```yaml
- name: Validate Owner
  uses: ./.github/workflows-reusable/validate-owner.yml
  with:
    service-name: my-service
    backstage-url: https://backstage.example.com
    deployment-path: k8s/deployment.yaml
  secrets:
    BACKSTAGE_API_TOKEN: ${{ secrets.BACKSTAGE_API_TOKEN }}
```

---

### 5. validate-metadata.yml (POL-05)
Valida a presença de todos os metadados obrigatórios no Backstage.

**Inputs:**
- `service-name` (required): Nome do serviço/componente
- `backstage-url` (optional): URL da API do Backstage
- `required-fields` (optional): Lista de campos obrigatórios separados por vírgula (default: "name,domain,owner,type,lifecycle,criticality")

**Secrets:**
- `BACKSTAGE_API_TOKEN` (required): Token da API do Backstage

**Exemplo de uso:**
```yaml
- name: Validate Metadata
  uses: ./.github/workflows-reusable/validate-metadata.yml
  with:
    service-name: my-service
    backstage-url: https://backstage.example.com
  secrets:
    BACKSTAGE_API_TOKEN: ${{ secrets.BACKSTAGE_API_TOKEN }}
```

---

### 6. validate-contract-exists.yml (POL-06)
Valida a existência do contrato no repositório de contratos.

**Inputs:**
- `service-name` (required): Nome do serviço/componente
- `contracts-repo-url` (required): URL base do repositório de contratos
- `contract-path` (optional): Caminho para o arquivo de contrato (default: "openapi.yaml")
- `contract-format` (optional): Formato do contrato (openapi|asyncapi, default: openapi)
- `fail-if-missing` (optional): Falhar se contrato não existir (default: true)

**Secrets:**
- `CONTRACTS_REPO_TOKEN` (optional): Token para acessar repositório de contratos (se privado)

**Exemplo de uso:**
```yaml
- name: Validate Contract Exists
  uses: ./.github/workflows-reusable/validate-contract-exists.yml
  with:
    service-name: my-service
    contracts-repo-url: https://github.com/org/contracts
    contract-path: openapi.yaml
  secrets:
    CONTRACTS_REPO_TOKEN: ${{ secrets.CONTRACTS_REPO_TOKEN }}
```

---

### 7. validate-contract-gateway.yml (POL-07)
Valida se o contrato está publicado no gateway (Axway ou APIM Azure).

**Inputs:**
- `service-name` (required): Nome do serviço/componente
- `gateway-url` (required): URL da API do gateway
- `gateway-type` (optional): Tipo do gateway (axway|apim, default: axway)
- `api-version` (optional): Versão da API para verificar (default: "1.0.0")
- `fail-if-missing` (optional): Falhar se contrato não estiver publicado (default: true)

**Secrets:**
- `GATEWAY_TOKEN` (required): Token da API do gateway

**Exemplo de uso:**
```yaml
- name: Validate Contract in Gateway
  uses: ./.github/workflows-reusable/validate-contract-gateway.yml
  with:
    service-name: my-service
    gateway-url: https://gateway.example.com
    gateway-type: axway
  secrets:
    GATEWAY_TOKEN: ${{ secrets.GATEWAY_TOKEN }}
```

---

### 8. detect-breaking-changes.yml (POL-08)
Detecta breaking changes no contrato OpenAPI usando ferramenta de comparação.

**Inputs:**
- `contract-path` (required): Caminho para o arquivo de contrato
- `previous-version` (optional): Versão anterior para comparar (git ref, default: "HEAD~1")
- `current-version` (optional): Versão atual para comparar (git ref, default: "HEAD")
- `breaking-change-tool` (optional): Ferramenta a usar (oasdiff|spectral, default: oasdiff)
- `fail-on-breaking` (optional): Falhar se breaking changes detectados (default: true)

**Exemplo de uso:**
```yaml
- name: Detect Breaking Changes
  uses: ./.github/workflows-reusable/detect-breaking-changes.yml
  with:
    contract-path: openapi.yaml
    previous-version: HEAD~1
    current-version: HEAD
    breaking-change-tool: oasdiff
```

---

### 9. validate-observability.yml (POL-11)
Valida a configuração de observabilidade no deployment.yaml (Dynatrace OneAgent, annotations, labels).

**Inputs:**
- `deployment-path` (optional): Caminho para deployment.yaml (default: "k8s/deployment.yaml")
- `required-annotations` (optional): Lista de annotations obrigatórias separadas por vírgula
- `required-labels` (optional): Lista de labels obrigatórias separadas por vírgula
- `validate-dynatrace` (optional): Validar configuração do Dynatrace OneAgent (default: true)

**Exemplo de uso:**
```yaml
- name: Validate Observability
  uses: ./.github/workflows-reusable/validate-observability.yml
  with:
    deployment-path: k8s/deployment.yaml
    validate-dynatrace: true
```

---

### 10. fail-fast-build.yml (POL-16)
Workflow agregador que executa todas as validações acima e falha rápido se alguma falhar.

**Inputs:**
- `service-name` (required): Nome do serviço/componente
- `backstage-url` (optional): URL da API do Backstage
- `contracts-repo-url` (optional): URL do repositório de contratos
- `gateway-url` (optional): URL da API do gateway
- `gateway-type` (optional): Tipo do gateway (axway|apim, default: axway)
- `deployment-path` (optional): Caminho para deployment.yaml (default: "k8s/deployment.yaml")
- `approved-fingerprint` (optional): Fingerprint aprovado do template
- `skip-validations` (optional): Lista de validações para pular separadas por vírgula

**Secrets:**
- `BACKSTAGE_API_TOKEN` (optional): Token da API do Backstage
- `GATEWAY_TOKEN` (optional): Token da API do gateway
- `CONTRACTS_REPO_TOKEN` (optional): Token do repositório de contratos

**Exemplo de uso:**
```yaml
jobs:
  validate-governance:
    uses: ./.github/workflows-reusable/fail-fast-build.yml
    with:
      service-name: ${{ github.event.repository.name }}
      backstage-url: https://backstage.example.com
      contracts-repo-url: https://github.com/org/contracts
      gateway-url: https://gateway.example.com
      gateway-type: axway
      approved-fingerprint: ${{ secrets.APPROVED_TEMPLATE_FINGERPRINT }}
    secrets:
      BACKSTAGE_API_TOKEN: ${{ secrets.BACKSTAGE_API_TOKEN }}
      GATEWAY_TOKEN: ${{ secrets.GATEWAY_TOKEN }}
      CONTRACTS_REPO_TOKEN: ${{ secrets.CONTRACTS_REPO_TOKEN }}
```

## Integração no Scaffolder

O workflow `fail-fast-build.yml` deve ser chamado no workflow CI de cada projeto estruturante. O template do Scaffolder já inclui um exemplo em `scaffolder/templates/spring-boot-api/skeleton/.github/workflows/ci.yml`.

## Ferramentas Necessárias

Os workflows utilizam as seguintes ferramentas (instaladas automaticamente quando necessário):

- `jq`: Parsing JSON
- `curl`: Chamadas HTTP
- `yq`: Parsing YAML
- `oasdiff`: Detecção de breaking changes em contratos OpenAPI
- `spectral`: Linting de contratos OpenAPI (alternativa)

## Como Adicionar Novos Workflows

1. Crie um novo arquivo `.yml` no diretório `workflows-reusable/`
2. Use o formato `workflow_call` do GitHub Actions
3. Defina inputs e secrets necessários
4. Implemente a lógica de validação
5. Adicione documentação neste README
6. Atualize `fail-fast-build.yml` para incluir o novo workflow (se aplicável)

## Notas Importantes

- Todos os workflows são **fail-fast**: param na primeira validação que falhar
- Workflows individuais podem ser chamados separadamente ou via `fail-fast-build.yml`
- Secrets devem ser configurados no repositório ou organização GitHub
- Alguns workflows são opcionais (dependem de configurações específicas da organização)

## Referências

- [GitHub Actions Reusable Workflows](https://docs.github.com/en/actions/using-workflows/reusing-workflows)
- [ENFORCEMENT_MAPPING_NOVOS_ESTRUTURANTES.md](../../docs/ENFORCEMENT_MAPPING_NOVOS_ESTRUTURANTES.md)

