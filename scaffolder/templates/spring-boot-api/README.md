# Template Spring Boot API - Componente Estruturante

Este template do Backstage Scaffolder cria novos componentes estruturantes Spring Boot API com conformidade automática às políticas de governança.

## Características

- ✅ Componente nasce 100% conforme todas as políticas
- ✅ Owner definido automaticamente (usuário que criou)
- ✅ Classificação correta (Componente Estruturante)
- ✅ Metadados completos
- ✅ Observabilidade pré-configurada (Dynatrace OneAgent)
- ✅ Autenticação configurada (OAuth2/Managed Identity)
- ✅ Pipelines de CI/CD com validações de governança
- ✅ Deployment Kubernetes configurado

## Estrutura do Template

```
scaffolder/templates/spring-boot-api/
├── template.yaml          # Definição do template
├── skeleton/              # Arquivos template
│   ├── pom.xml
│   ├── src/main/java/com/codingbetter/COMPONENT_NAME/
│   │   └── COMPONENT_NAME_TITLE_Application.java
│   ├── src/main/resources/application.yml
│   ├── .github/workflows/ci.yml
│   ├── k8s/deployment.yaml
│   ├── openapi.yaml
│   └── README.md
└── README.md
```

## Parâmetros do Template

- **componentName**: Nome do componente (ex: usuario, proposta)
- **domain**: Domínio de negócio (core, produtos, vendas, etc.)
- **componentType**: Tipo (api, bff, gtw, service)
- **lifecycle**: Ciclo de vida inicial (experimental, active)
- **criticality**: Criticidade (critical, high, medium, low)
- **createContract**: Criar contrato OpenAPI inicial? (true/false)
- **description**: Descrição do componente

## Processo de Criação

1. Usuário preenche formulário no Backstage Scaffolder
2. Scaffolder valida todas as informações
3. Scaffolder cria repositório no GitHub
4. Scaffolder cria código do template
5. Scaffolder registra componente no Backstage Catalog
6. Scaffolder cria contrato (se solicitado)
7. Scaffolder publica contrato no gateway
8. Scaffolder registra no Azure AD
9. Scaffolder registra no ServiceNow CMDB
10. Componente nasce 100% conforme

## Nota sobre Erros de Linter

Os arquivos no diretório `skeleton/` contêm placeholders (ex: `${{ parameters.componentName }}`) que serão substituídos pelo Backstage Scaffolder durante a criação do componente. Os erros de linter são esperados e não afetam o funcionamento do template.

## Uso

1. Acesse o Backstage
2. Vá para "Create Component"
3. Selecione "Spring Boot API - Componente Estruturante"
4. Preencha o formulário
5. Clique em "Create"

