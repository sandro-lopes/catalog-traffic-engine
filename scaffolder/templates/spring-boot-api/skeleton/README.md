# ${{ parameters.componentName | title }}

${{ parameters.description }}

## Informações do Componente

- **Nome**: ${{ parameters.componentName }}
- **Tipo**: ${{ parameters.componentType }}
- **Domínio**: ${{ parameters.domain }}
- **Ciclo de Vida**: ${{ parameters.lifecycle }}
- **Criticidade**: ${{ parameters.criticality }}
- **Owner**: ${{ parameters.owner }}

## Características

Este componente foi criado via Backstage Scaffolder e já está 100% conforme todas as políticas de governança:

- ✅ Owner definido
- ✅ Classificação correta (Componente Estruturante)
- ✅ Metadados completos
- ✅ Observabilidade configurada (Dynatrace OneAgent)
- ✅ Autenticação configurada (OAuth2/Managed Identity)
- ✅ Contrato OpenAPI ${{ parameters.createContract ? 'criado e publicado' : 'não criado' }}
- ✅ Pipelines de CI/CD configurados
- ✅ Deployment Kubernetes configurado

## Estrutura do Projeto

```
${{ parameters.componentName }}/
├── src/
│   ├── main/
│   │   ├── java/com/codingbetter/${{ parameters.componentName }}/
│   │   │   └── ${{ parameters.componentName | title }}Application.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── k8s/
│   └── deployment.yaml
├── .github/
│   └── workflows/
│       └── ci.yml
${{ parameters.createContract ? '├── openapi.yaml' : '' }}
├── pom.xml
└── README.md
```

## Desenvolvimento Local

### Pré-requisitos

- Java 21
- Maven 3.8+
- Docker (opcional, para testes)

### Executar Localmente

```bash
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Build e Deploy

### Build

```bash
mvn clean package
```

### Deploy

O deploy é feito via ArgoCD GitOps. Faça push para a branch `main` e o ArgoCD fará o deploy automaticamente.

## Observabilidade

- **Métricas**: Disponível em `/actuator/metrics` e `/actuator/prometheus`
- **Health**: Disponível em `/actuator/health`
- **Dynatrace**: OneAgent configurado automaticamente no deployment

## Segurança

- **Autenticação**: OAuth2 client_credentials obrigatório
- **Managed Identity**: Configurado para recursos Azure
- **Service Account**: Configurado para ARO

## Validações de Governança

O pipeline de CI valida automaticamente:

- ✅ Presença de owner no Backstage
- ✅ Existência de contrato (se criado)
- ✅ Publicação do contrato no gateway (se criado)
- ✅ Template fingerprint
- ✅ Metadados completos

## Contrato da API

${{ parameters.createContract ? `O contrato OpenAPI está disponível em:
- Repositório: \`${{ parameters.contractsRepoUrl }}/${{ parameters.componentName }}/openapi.yaml\`
- Gateway: Publicado no Axway Gateway ou APIM Azure` : 'Contrato OpenAPI não foi criado durante a criação do componente. Para criar, siga o processo de criação de contratos.' }}

## Suporte

Para questões ou problemas, entre em contato com o owner: **${{ parameters.owner }}**

## Links Úteis

- [Backstage Catalog](${{ steps.register.output.entityRef }})
- [GitHub Repository](${{ steps.publish.output.remoteUrl }})
${{ parameters.createContract ? `- [Contrato OpenAPI](${{ steps.create-contract.output.contractUrl }})` : '' }}

