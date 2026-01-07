# Mapeamento de Enforcement - Ferramentas e Processos

Este documento mapeia cada **Enforcement** das políticas de governança para **processos, componentes e ferramentas** específicas utilizadas na organização, incluindo referências de mercado e casos de uso reais.

## Ferramentas da Organização

- **Backstage**: Catálogo de serviços (sem uso de catalog-info.yml pela equipe de engenharia de plataforma)
- **Dynatrace**: Observabilidade e detecção de inatividade
- **ARO (Azure Red Hat OpenShift)**: Plataforma Kubernetes
- **ArgoCD**: GitOps e deployment
- **Nexus**: Repositório de artefatos
- **GitHub**: Controle de versão e repositórios
- **GitHub Actions**: CI/CD e pipelines
- **Axway Gateway**: API Gateway principal
- **APIM Azure**: API Gateway gerenciado (Azure)
- **ServiceNow**: CMDB, workflows e automação
- **Azure Managed Identity**: Autenticação service-to-service
- **Terraform**: Infraestrutura como código

## Observações Importantes

- **Contratos de API**: Ficam em repositório separado e são publicados no gateway (Axway Gateway ou APIM Azure)
- **Backstage**: Equipe de engenharia de plataforma não utiliza catalog-info.yml; catálogo é populado via API

---

## Tabela de Mapeamento

| Política | Enforcement | Ferramentas/Componentes | Processo Passo a Passo | Casos de Uso de Referência |
|----------|-------------|------------------------|------------------------|---------------------------|
| **POL-01** | Classificação obrigatória no Backstage | **Backstage Catalog API**<br/>**ServiceNow CMDB**<br/>**Dynatrace API** | 1. Extrair metadados do GitHub (nome do repositório, tags, descrição)<br/>2. Consultar Dynatrace API para identificar serviços ativos<br/>3. Chamar Backstage Catalog API para verificar se entidade existe<br/>4. Se não existir, criar entidade via Backstage Catalog API com classificação "Componente Estruturante"<br/>5. Atualizar ServiceNow CMDB com informações do componente<br/>6. Executar processo diariamente via job agendado | Classificação automática via metadados extraídos do GitHub e Dynatrace |
| **POL-01** | Componentes mal classificados são bloqueados para evolução | **GitHub Branch Protection**<br/>**GitHub Actions**<br/>**ArgoCD Admission Controller (ARO)** | 1. Configurar GitHub Branch Protection para branches principais (main/master)<br/>2. Criar step no GitHub Actions que executa antes do merge<br/>3. Step chama Backstage Catalog API para buscar entidade pelo nome do repositório<br/>4. Validar se campo "kind" ou "metadata.tags" contém classificação correta<br/>5. Se classificação incorreta, falhar o step e bloquear merge<br/>6. ArgoCD ValidatingAdmissionWebhook intercepta deploy e valida classificação no Backstage<br/>7. Se inválido, rejeitar o deploy com mensagem de erro | Bloqueio de PRs e deploys sem classificação correta no Backstage |
| **POL-02** | Criação manual de repositórios é proibida | **GitHub Organization Settings**<br/>**GitHub App (webhook blocker)**<br/>**Backstage Scaffolder**<br/>**ServiceNow Workflow** | 1. Configurar GitHub Organization Settings para restringir criação de repositórios<br/>2. Criar GitHub App com webhook que monitora eventos de criação de repositório<br/>3. Webhook valida se repositório foi criado via Backstage Scaffolder (verifica metadata ou tag)<br/>4. Se criação manual detectada, GitHub App deleta repositório automaticamente<br/>5. Criar ServiceNow Workflow que notifica equipe de governança sobre tentativa de criação manual<br/>6. Backstage Scaffolder adiciona tag especial ao criar repositório para identificação | Apenas criação via Backstage Scaffolder; GitHub bloqueia criação manual |
| **POL-02** | Pipelines validam fingerprint do template | **GitHub Actions**<br/>**Nexus (artefato validation)** | 1. Criar step no GitHub Actions no início do pipeline de build<br/>2. Step extrai hash/fingerprint dos arquivos de template (pom.xml, Dockerfile, etc.)<br/>3. Compara fingerprint com versão aprovada armazenada no Nexus<br/>4. Se fingerprint não corresponder, falhar o build imediatamente<br/>5. Nexus valida artefato antes de aceitar upload<br/>6. Se artefato não vier de template aprovado, rejeitar upload<br/>7. Registrar tentativas de uso de template não aprovado para auditoria | Validação de template fingerprint em GitHub Actions; validação de artefato no Nexus |
| **POL-03** | Deploy bloqueado sem owner | **ArgoCD Admission Controller (ARO)**<br/>**ARO ValidatingAdmissionWebhook**<br/>**Backstage Catalog API (validação)** | 1. Configurar ArgoCD ValidatingAdmissionWebhook no ARO<br/>2. Webhook intercepta todas as requisições de criação/atualização de Deployment<br/>3. Extrair nome do serviço dos labels ou annotations do Deployment<br/>4. Chamar Backstage Catalog API para buscar entidade pelo serviceId<br/>5. Validar se campo "spec.owner" existe e não está vazio<br/>6. Se owner ausente, rejeitar requisição com status HTTP 403 e mensagem explicativa<br/>7. Registrar tentativa de deploy sem owner para auditoria<br/>8. Notificar equipe de governança sobre bloqueio | Deploy bloqueado no ArgoCD se serviço não tiver owner no Backstage |
| **POL-03** | Serviços sem owner entram em estado RESTRICTED | **Backstage Catalog API**<br/>**ServiceNow Automation**<br/>**Dynatrace API** | 1. Criar job agendado que executa diariamente<br/>2. Job consulta Backstage Catalog API para listar todas as entidades<br/>3. Para cada entidade, verificar se campo "spec.owner" está presente e válido<br/>4. Se owner ausente, atualizar annotation "backstage.io/status" para "RESTRICTED" via Backstage Catalog API<br/>5. Criar ticket no ServiceNow via API de automação notificando sobre serviço sem owner<br/>6. Atualizar ServiceNow CMDB com status RESTRICTED<br/>7. Consultar Dynatrace API para verificar se serviço está ativo<br/>8. Se inativo e sem owner, priorizar ticket como alta criticidade | Estado RESTRICTED automático via ServiceNow workflow baseado em dados do Backstage |
| **POL-04** | Alteração direta de YAML bloqueada | **GitHub Branch Protection**<br/>**Backstage Catalog API (read-only via ServiceNow)** | 1. Configurar GitHub Branch Protection para branches principais<br/>2. Habilitar regra que exige Pull Request para todas as mudanças<br/>3. Configurar regra que bloqueia pushes diretos (force push desabilitado)<br/>4. Backstage Catalog API configurado como read-only para usuários diretos<br/>5. Criar ServiceNow Workflow que permite alterações apenas via ticket aprovado<br/>6. ServiceNow Workflow chama Backstage Catalog API com credenciais de serviço para atualizar<br/>7. Registrar todas as tentativas de alteração direta para auditoria<br/>8. Notificar equipe quando alteração direta for detectada | Apenas mudanças via ServiceNow workflow; GitHub protege branches principais |
| **POL-04** | Auditoria obrigatória | **GitHub Audit Log**<br/>**ServiceNow CMDB**<br/>**Azure Activity Log**<br/>**Dynatrace Audit Trail** | 1. Configurar exportação automática do GitHub Audit Log para ServiceNow<br/>2. ServiceNow CMDB captura todas as mudanças de ownership via webhook do Backstage<br/>3. Azure Activity Log monitora mudanças em recursos relacionados (Managed Identity, etc.)<br/>4. Dynatrace Audit Trail registra mudanças em configurações de serviços<br/>5. Criar processo de consolidação que agrega logs de todas as fontes<br/>6. Armazenar logs consolidados no ServiceNow CMDB com timestamp e usuário<br/>7. Criar dashboard no ServiceNow para visualizar histórico de mudanças<br/>8. Configurar alertas para mudanças críticas (ex: owner de serviço crítico) | Auditoria completa de mudanças de ownership via ServiceNow + logs Azure |
| **POL-05** | Build falha se metadados incompletos | **GitHub Actions**<br/>**Maven/Gradle Plugin**<br/>**Nexus (validação de artefato)** | 1. Criar step no GitHub Actions no stage de build<br/>2. Step extrai metadados do projeto (nome, versão, grupo) do pom.xml ou build.gradle<br/>3. Step chama Backstage Catalog API para buscar entidade correspondente<br/>4. Validar presença de campos obrigatórios: nome, domínio, owner, tipo, ciclo de vida, criticidade<br/>5. Se algum campo ausente, falhar o build com mensagem indicando campos faltantes<br/>6. Maven/Gradle Plugin executa validação adicional durante compilação<br/>7. Plugin verifica se arquivo de metadados existe e está válido<br/>8. Nexus valida metadados antes de aceitar artefato<br/>9. Se validação falhar, rejeitar upload e retornar erro detalhado | Validação de metadados no build via GitHub Actions; artefato validado no Nexus |
| **POL-06** | CI valida existência do contrato | **GitHub Actions**<br/>**Repositório de Contratos (GitHub)**<br/>**Axway Gateway API**<br/>**APIM Azure** | 1. Criar step no GitHub Actions no início do pipeline de CI<br/>2. Step identifica nome do serviço a partir do repositório<br/>3. Step consulta repositório de contratos no GitHub para verificar se arquivo OpenAPI/AsyncAPI existe<br/>4. Se contrato não encontrado no repositório, falhar o step<br/>5. Step valida sintaxe do contrato (YAML/JSON válido)<br/>6. Step chama Axway Gateway API ou APIM Azure Management API para verificar se contrato está publicado<br/>7. Se contrato não publicado no gateway, falhar o step com instruções de publicação<br/>8. Registrar resultado da validação para métricas | CI valida se contrato existe no repositório separado e está publicado no gateway |
| **POL-07** | Pipeline falha sem referência válida ao contrato | **GitHub Actions**<br/>**Axway Gateway API**<br/>**APIM Azure Management API** | 1. Criar step no GitHub Actions no stage de validação<br/>2. Step extrai serviceId do projeto ou configuração<br/>3. Step chama Axway Gateway API para buscar API pelo serviceId<br/>4. Alternativamente, chama APIM Azure Management API se usando APIM<br/>5. Validar se API existe no gateway e está ativa<br/>6. Validar se versão do contrato no gateway corresponde à versão no repositório<br/>7. Se contrato não encontrado ou versão incompatível, falhar o pipeline<br/>8. Gerar relatório de validação com detalhes do contrato encontrado<br/>9. Notificar equipe se validação falhar | Pipeline valida se contrato está publicado no gateway (Axway ou APIM Azure) |
| **POL-08** | Detecção automática de breaking change | **GitHub Actions**<br/>**Repositório de Contratos (GitHub)**<br/>**Axway Gateway**<br/>**APIM Azure (versionamento)** | 1. Criar step no GitHub Actions quando contrato é modificado no repositório<br/>2. Step baixa versão atual do contrato do repositório de contratos<br/>3. Step baixa versão anterior do contrato (última versão publicada)<br/>4. Comparar endpoints, parâmetros, schemas entre versões<br/>5. Detectar mudanças incompatíveis: endpoints removidos, parâmetros obrigatórios adicionados, tipos alterados<br/>6. Se breaking change detectado, marcar PR com label "breaking-change"<br/>7. Validar se versão do contrato foi incrementada (semver)<br/>8. Se breaking change sem incremento de versão major, falhar o step<br/>9. Consultar gateway para verificar versões publicadas<br/>10. Registrar breaking changes detectados para relatório | Detecção via comparação de versões no repositório de contratos e gateway |
| **POL-08** | Bloqueio de deploy | **ArgoCD Admission Controller (ARO)**<br/>**ARO ValidatingAdmissionWebhook** | 1. Configurar ValidatingAdmissionWebhook no ARO que intercepta deploys<br/>2. Webhook recebe requisição de criação/atualização de Deployment<br/>3. Extrair informações do serviço (nome, versão) dos labels<br/>4. Consultar repositório de contratos ou gateway para verificar se breaking change foi detectado<br/>5. Se breaking change detectado e versão não incrementada, rejeitar deploy<br/>6. Retornar status HTTP 403 com mensagem explicando motivo do bloqueio<br/>7. Registrar tentativa de deploy com breaking change para auditoria<br/>8. Notificar equipe de governança sobre bloqueio<br/>9. Criar ticket no ServiceNow automaticamente para rastreamento | Deploy bloqueado no ArgoCD se breaking change detectado |
| **POL-09** | Gateway bloqueia exposição indevida | **Axway Gateway**<br/>**APIM Azure** | 1. Configurar política no Axway Gateway que valida origem da requisição<br/>2. Política verifica header "X-Client-Type" ou "User-Agent" para identificar frontend<br/>3. Política consulta lista de APIs estruturantes (configurada no gateway)<br/>4. Se requisição vier de frontend e destino for API estruturante, bloquear requisição<br/>5. Retornar HTTP 403 com mensagem "Frontend cannot access backend services directly"<br/>6. No APIM Azure, configurar policy similar usando policy XML<br/>7. Policy valida origem e destino da requisição<br/>8. Registrar todas as tentativas de acesso bloqueadas para auditoria<br/>9. Enviar alerta para equipe de segurança quando bloqueio ocorrer | Gateway bloqueia acesso frontend → estruturante via políticas de roteamento |
| **POL-10** | OAuth2 client_credentials obrigatório | **Azure AD**<br/>**Axway Gateway (OAuth2)**<br/>**APIM Azure (OAuth2)** | 1. Registrar aplicação no Azure AD para cada serviço estruturante<br/>2. Configurar tipo de autenticação como "client_credentials"<br/>3. Gerar Client ID e Client Secret para cada serviço<br/>4. Configurar Axway Gateway para validar token OAuth2 em todas as requisições<br/>5. Gateway valida token com Azure AD antes de rotear requisição<br/>6. Se token ausente ou inválido, bloquear requisição com HTTP 401<br/>7. No APIM Azure, configurar OAuth2 policy que valida token<br/>8. Policy extrai token do header Authorization<br/>9. Policy valida token com Azure AD<br/>10. Registrar tentativas de acesso sem token para auditoria | Service-to-service via OAuth2 client_credentials configurado no Azure AD |
| **POL-10** | Managed Identity (quando disponível) | **Azure Managed Identity**<br/>**ARO Service Accounts** | 1. Habilitar Managed Identity para recursos Azure (App Service, Function, etc.)<br/>2. Configurar Azure AD para permitir acesso do Managed Identity<br/>3. Atribuir permissões necessárias ao Managed Identity<br/>4. Código do serviço usa Managed Identity para obter token automaticamente<br/>5. Token é usado para autenticar em outros serviços<br/>6. No ARO, criar Service Account para cada serviço<br/>7. Service Account associado a Secret com credenciais<br/>8. Pods usam Service Account para autenticação automática<br/>9. Validar que serviços não usam credenciais hardcoded<br/>10. Auditoria regular para verificar uso correto de Managed Identity | Managed Identity para serviços no Azure; Service Accounts no ARO |
| **POL-11** | Deploy bloqueado sem observabilidade | **ArgoCD Admission Controller (ARO)**<br/>**Dynatrace OneAgent**<br/>**ARO ValidatingAdmissionWebhook** | 1. Configurar ValidatingAdmissionWebhook no ARO<br/>2. Webhook intercepta criação/atualização de Deployment<br/>3. Verificar se Deployment contém initContainer ou sidecar do Dynatrace OneAgent<br/>4. Verificar se annotations contêm configuração do Dynatrace (environment, tags)<br/>5. Consultar Dynatrace API para verificar se serviço está registrado<br/>6. Se OneAgent não configurado, rejeitar deploy com HTTP 403<br/>7. Mensagem de erro deve indicar como configurar observabilidade<br/>8. Validar presença de labels de observabilidade (app, version, environment)<br/>9. Registrar tentativas de deploy sem observabilidade<br/>10. Criar ticket no ServiceNow para rastreamento | Deploy requer Dynatrace OneAgent instalado e configurado |
| **POL-12** | Detecção automática de inatividade | **Dynatrace API**<br/>**ServiceNow Automation**<br/>**Backstage Catalog API** | 1. Criar job agendado que executa semanalmente<br/>2. Job consulta Backstage Catalog API para listar todos os serviços<br/>3. Para cada serviço, job chama Dynatrace API para buscar métricas de tráfego<br/>4. Consultar métricas dos últimos 30 dias (request count, response time)<br/>5. Se request count for zero por mais de 30 dias, marcar serviço como inativo<br/>6. Atualizar annotation no Backstage via Catalog API com status "INACTIVE"<br/>7. Criar ticket no ServiceNow via API de automação para avaliação de desligamento<br/>8. Ticket inclui dados de métricas do Dynatrace como evidência<br/>9. Notificar owner do serviço sobre detecção de inatividade<br/>10. Registrar detecção para métricas de governança | Detecção automática via métricas de tráfego do Dynatrace |
| **POL-12** | Avaliação automática de desligamento | **ServiceNow Workflow**<br/>**Dynatrace API** | 1. ServiceNow Workflow é acionado quando ticket de inatividade é criado<br/>2. Workflow consulta Dynatrace API para obter métricas detalhadas<br/>3. Workflow verifica dependências no Backstage Catalog API<br/>4. Se serviço tem dependências ativas, marcar ticket como "requer análise manual"<br/>5. Se sem dependências, workflow calcula economia potencial<br/>6. Workflow atualiza ticket com recomendações (desligar, manter, investigar)<br/>7. Workflow cria subtarefas para owner do serviço<br/>8. Workflow agenda follow-up automático em 30 dias<br/>9. Se owner não responder, escalar para gestão<br/>10. Registrar decisões para métricas de governança | Workflow automático de avaliação via ServiceNow baseado em dados do Dynatrace |
| **POL-13** | Ciclo de vida explícito | **Backstage Catalog API**<br/>**ServiceNow CMDB** | 1. Criar processo que valida presença de campo "lifecycle" em todas as entidades<br/>2. Job diário consulta Backstage Catalog API para listar entidades<br/>3. Para cada entidade, validar se campo "spec.lifecycle" existe<br/>4. Se ausente, atualizar entidade com lifecycle padrão "EXPERIMENTAL"<br/>5. Validar que lifecycle está em valores permitidos: EXPERIMENTAL, ACTIVE, DEPRECATED, RETIRED<br/>6. Sincronizar lifecycle do Backstage para ServiceNow CMDB<br/>7. ServiceNow CMDB mantém histórico de mudanças de lifecycle<br/>8. Criar dashboard no Backstage mostrando distribuição de lifecycles<br/>9. Alertar quando serviço fica em DEPRECATED por mais de 90 dias<br/>10. Registrar mudanças de lifecycle para auditoria | Estados EXPERIMENTAL, ACTIVE, DEPRECATED, RETIRED no Backstage |
| **POL-14** | Headers de depreciação obrigatórios | **Axway Gateway**<br/>**APIM Azure**<br/>**Middleware (Spring Boot)** | 1. Configurar política no Axway Gateway que adiciona headers automaticamente<br/>2. Política consulta Backstage Catalog API para verificar lifecycle do serviço<br/>3. Se lifecycle for "DEPRECATED", política adiciona header "Deprecation: true"<br/>4. Política adiciona header "Sunset: <date>" com data de desligamento do Backstage<br/>5. No APIM Azure, configurar policy XML similar<br/>6. Policy adiciona headers baseado em metadados do serviço<br/>7. Middleware Spring Boot valida headers antes de processar requisição<br/>8. Se headers ausentes para serviço deprecated, middleware adiciona automaticamente<br/>9. Registrar todas as respostas com headers de depreciação<br/>10. Alertar quando data de sunset se aproxima (30 dias antes) | Headers `Deprecation: true` e `Sunset: <date>` adicionados pelo gateway |
| **POL-14** | Desligamento automático ao atingir sunsetDate | **Terraform**<br/>**ArgoCD Application Controller**<br/>**ServiceNow Automation** | 1. Criar job agendado que executa diariamente<br/>2. Job consulta Backstage Catalog API para serviços com lifecycle "DEPRECATED"<br/>3. Para cada serviço, verificar se campo "sunsetDate" foi atingido<br/>4. Se sunsetDate passou, acionar ServiceNow Automation<br/>5. ServiceNow Automation cria ticket de desligamento<br/>6. Automation executa Terraform para remover recursos de infraestrutura<br/>7. Terraform destrói recursos Azure relacionados ao serviço<br/>8. ArgoCD Application Controller remove Application do cluster ARO<br/>9. Atualizar Backstage Catalog API com lifecycle "RETIRED"<br/>10. Atualizar ServiceNow CMDB com status "DESLIGADO"<br/>11. Registrar desligamento para métricas de governança | Desligamento automático via Terraform + ArgoCD + ServiceNow workflow |
| **POL-15** | Anti-corruption layer obrigatório | **Axway Gateway**<br/>**APIM Azure**<br/>**Architecture Decision Records (ADR no GitHub)** | 1. Configurar política no gateway que valida padrão de comunicação<br/>2. Política verifica se requisições para sistemas legados passam por camada de transformação<br/>3. Se requisição direta para legado detectada, bloquear e redirecionar para gateway<br/>4. Gateway atua como anti-corruption layer, transformando requisições<br/>5. Validar presença de ADR no repositório GitHub documentando integração com legado<br/>6. ADR deve descrever padrão de isolamento usado<br/>7. Criar processo de review que valida ADR antes de aprovar integração<br/>8. Gateway registra todas as transformações realizadas<br/>9. Alertar quando padrão de anti-corruption layer não for seguido<br/>10. Manter inventário de integrações com legado no ServiceNow | Pattern enforcement via gateway; ADRs documentados no GitHub |
| **POL-16** | Fail fast em build | **GitHub Actions**<br/>**Maven/Gradle Plugin**<br/>**Nexus (validação)** | 1. Configurar step no GitHub Actions como primeiro step do pipeline<br/>2. Step executa validações de todas as políticas de governança<br/>3. Step chama Backstage Catalog API para validar metadados<br/>4. Step valida existência de contrato no repositório<br/>5. Step valida owner, classificação, lifecycle<br/>6. Se qualquer validação falhar, falhar o build imediatamente<br/>7. Maven/Gradle Plugin executa validações adicionais durante compilação<br/>8. Plugin falha build se políticas violadas<br/>9. Nexus rejeita artefato se validações falharem<br/>10. Retornar mensagem de erro clara indicando qual política foi violada | Build falha imediatamente se política violada |
| **POL-16** | Fail fast em deploy | **ArgoCD Admission Controller (ARO)**<br/>**ARO ValidatingAdmissionWebhook** | 1. Configurar ValidatingAdmissionWebhook no ARO<br/>2. Webhook intercepta todas as requisições de deploy<br/>3. Webhook executa validações de governança antes de permitir deploy<br/>4. Validar owner, observabilidade, contrato, classificação<br/>5. Se qualquer validação falhar, rejeitar deploy imediatamente<br/>6. Retornar HTTP 403 com lista de políticas violadas<br/>7. Não permitir deploy parcial ou com exceções<br/>8. Registrar todas as tentativas de deploy rejeitadas<br/>9. Notificar equipe de governança sobre bloqueios<br/>10. Criar ticket no ServiceNow para rastreamento | Deploy rejeitado no ArgoCD se política violada |
| **POL-16** | Fail fast em runtime | **Axway Gateway**<br/>**APIM Azure**<br/>**Dynatrace (circuit breaker)** | 1. Configurar políticas no gateway que validam requisições em runtime<br/>2. Política valida autenticação, origem, destino, formato<br/>3. Se validação falhar, bloquear requisição imediatamente<br/>4. Retornar HTTP 403 ou 401 sem processar requisição<br/>5. Dynatrace circuit breaker monitora padrões de erro<br/>6. Se taxa de erro alta, circuit breaker abre e bloqueia requisições<br/>7. Gateway registra todas as requisições bloqueadas<br/>8. Alertar equipe quando bloqueios ocorrem<br/>9. Não permitir bypass de validações em runtime<br/>10. Manter logs de todas as violações para análise | Runtime bloqueia requisições inválidas via gateway |
| **POL-17** | Governança integrada ao SDLC | **GitHub Actions (workflow)**<br/>**ArgoCD (GitOps)** | 1. Configurar GitHub Actions workflow que executa em todas as etapas do SDLC<br/>2. Workflow valida políticas no commit, PR, build, deploy<br/>3. Workflow não permite pular etapas de validação<br/>4. ArgoCD aplica validações adicionais no momento do deploy<br/>5. GitOps garante que todas as mudanças passem por validação<br/>6. Criar processo que não permite deploy sem passar por todas as validações<br/>7. Integrar validações de governança como parte natural do pipeline<br/>8. Não tratar governança como etapa separada ou opcional<br/>9. Documentar processo de SDLC incluindo validações de governança<br/>10. Treinar equipes sobre integração de governança no SDLC | Governança como parte do pipeline GitHub Actions + GitOps ArgoCD |
| **POL-18** | Incentivo ao desligamento (sem penalização) | **ServiceNow CMDB**<br/>**Backstage (visualização)**<br/>**Dynatrace Dashboards** | 1. Configurar métricas no ServiceNow CMDB que não penalizam desligamento<br/>2. Métricas de governança excluem serviços desligados do cálculo de compliance<br/>3. Backstage exibe serviços desligados em seção separada (não como não conformes)<br/>4. Dynatrace Dashboards mostram economia de custos com desligamentos<br/>5. Criar relatórios que destacam serviços desligados como sucesso<br/>6. Gamificação: pontos/badges para equipes que desligam serviços obsoletos<br/>7. Métricas de governança incluem "serviços desligados" como indicador positivo<br/>8. Não incluir serviços desligados em métricas de "serviços sem owner"<br/>9. Celebrar desligamentos bem executados em comunicados<br/>10. Manter histórico de desligamentos para aprendizado | Métricas não penalizam desligamento; visibilidade no Backstage e Dynatrace |
| **POL-19** | Métricas de governança | **Backstage Metrics Plugin**<br/>**Dynatrace Dashboards**<br/>**ServiceNow Analytics** | 1. Criar job que calcula métricas diariamente<br/>2. Job consulta Backstage Catalog API para listar todos os serviços<br/>3. Calcular % de serviços com owner (serviços com owner / total de serviços)<br/>4. Calcular % criados via arquétipo (serviços com tag de template / total)<br/>5. Calcular % com contrato válido (serviços com contrato no gateway / total)<br/>6. Calcular tempo médio de desligamento (data desligamento - data deprecation)<br/>7. Publicar métricas no Backstage via Metrics Plugin<br/>8. Criar dashboards no Dynatrace visualizando métricas de governança<br/>9. ServiceNow Analytics gera relatórios executivos com métricas<br/>10. Alertar quando métricas ficarem abaixo de thresholds definidos<br/>11. Compartilhar métricas em reuniões de governança | Dashboards de % owner, % contrato, % arquétipo no Backstage e Dynatrace |

---

## Ferramentas por Categoria (Stack da Organização)

### Catálogo e Descoberta
- **Backstage**: Catálogo de serviços (populado via API, sem catalog-info.yml pela equipe de plataforma)
- **ServiceNow CMDB**: Catálogo de configuração, auditoria, workflows
- **GitHub**: Descoberta de repositórios e metadados
- **Dynatrace**: Detecção de serviços e métricas de tráfego

### CI/CD e Pipelines
- **GitHub Actions**: Validações, testes, enforcement em PR
- **ArgoCD**: GitOps, admission controllers no ARO
- **Nexus**: Repositório de artefatos, validação de builds

### Plataforma e Infraestrutura
- **ARO (Azure Red Hat OpenShift)**: Plataforma Kubernetes
- **Terraform**: Infraestrutura como código
- **Azure Managed Identity**: Autenticação service-to-service

### API Gateway
- **Axway Gateway**: API Gateway principal
- **APIM Azure**: API Gateway gerenciado (Azure)

### Observabilidade
- **Dynatrace**: APM, detecção de inatividade, métricas de tráfego

### Segurança e Autenticação
- **Azure AD**: OAuth2, Managed Identity
- **Azure Managed Identity**: Autenticação service-to-service

### Automação e Workflows
- **ServiceNow**: CMDB, workflows, automação, auditoria
- **GitHub**: Controle de versão, repositórios, audit log

### Contratos de API
- **Repositório de Contratos (GitHub)**: Contratos OpenAPI/AsyncAPI em repositório separado
- **Axway Gateway / APIM Azure**: Publicação e versionamento de contratos

---

## Casos de Uso de Referência por Empresa

### Netflix
- **Enforcement**: Validação de templates, bloqueio de deploy sem owner
- **Ferramentas**: Spinnaker, OPA, Kubernetes Admission Controllers
- **Referência**: [Netflix Tech Blog - Policy as Code](https://netflixtechblog.com)

### Spotify
- **Enforcement**: Backstage como única fonte de verdade, scaffolder obrigatório
- **Ferramentas**: Backstage, GitHub Actions, OPA
- **Referência**: [Backstage.io](https://backstage.io)

### Stripe
- **Enforcement**: Contratos OpenAPI obrigatórios, detecção de breaking changes
- **Ferramentas**: Spectral, API Gateway, versionamento
- **Referência**: [Stripe API Versioning](https://stripe.com/docs/api/versioning)

### Uber
- **Enforcement**: Service mesh para segurança, OAuth2 client_credentials
- **Ferramentas**: Istio, OPA, Kubernetes
- **Referência**: [Uber Engineering Blog](https://eng.uber.com)

### Capital One
- **Enforcement**: Gateway bloqueia exposição indevida, auditoria completa
- **Ferramentas**: API Gateway, CloudTrail, Splunk
- **Referência**: [Capital One Tech Blog](https://www.capitalone.com/tech)

### American Airlines
- **Enforcement**: Classificação automática, estado RESTRICTED
- **Ferramentas**: Backstage, ServiceNow
- **Referência**: [Backstage Community](https://backstage.io/community)

---

## Padrões de Implementação

### Padrão 1: Fail Fast em Build (GitHub Actions)
```yaml
# .github/workflows/validate-governance.yml
name: Validate Governance
on: [pull_request]
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - name: Validate Backstage Metadata
        run: |
          # Valida se serviço tem owner no Backstage
          curl -H "Authorization: Bearer ${{ secrets.BACKSTAGE_TOKEN }}" \
            https://backstage.example.com/api/catalog/entities/by-name/component/${{ github.repository }} | \
            jq -e '.spec.owner' || exit 1
      
      - name: Validate Contract in Gateway
        run: |
          # Valida se contrato está publicado no gateway
          curl -H "Authorization: Bearer ${{ secrets.AXWAY_TOKEN }}" \
            https://gateway.example.com/api/contracts/${{ github.repository }} || exit 1
```

### Padrão 2: Admission Controller para Deploy (ArgoCD + ARO)
```yaml
# ValidatingAdmissionWebhook no ARO
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  name: governance-validator
webhooks:
  - name: owner-validator
    clientConfig:
      url: https://governance-validator.example.com/validate
    rules:
      - operations: ["CREATE", "UPDATE"]
        apiGroups: ["apps"]
        resources: ["deployments"]
    # Valida owner via Backstage API
```

### Padrão 3: Gateway Policy Enforcement (Axway Gateway)
```yaml
# Política no Axway Gateway
policy:
  name: block-frontend-to-backend
  rules:
    - condition: request.headers['X-Client-Type'] == 'frontend'
      action: deny
      message: "Frontend cannot call backend services directly"
```

### Padrão 4: Validação de Contrato no Gateway (APIM Azure)
```bicep
// Terraform/ARM para APIM Azure
resource "azurerm_api_management_api" "service" {
  name                = var.service_name
  resource_group_name = var.resource_group
  api_management_name = var.apim_name
  
  # Contrato vem do repositório separado
  import {
    content_format = "openapi"
    content_value  = file("${var.contracts_repo_path}/${var.service_name}/openapi.yaml")
  }
  
  # Validação de breaking change
  policy {
    xml_content = <<XML
    <policies>
      <inbound>
        <validate-content>
          <openapi-spec>${file("${var.contracts_repo_path}/${var.service_name}/openapi.yaml")}</openapi-spec>
        </validate-content>
      </inbound>
    </policies>
    XML
  }
}
```

### Padrão 5: ServiceNow Workflow para Ownership Transfer
```javascript
// ServiceNow Workflow Script
// Valida transferência de ownership
(function executeRule(current, previous) {
    var newOwner = current.getValue('owner');
    var oldOwner = previous.getValue('owner');
    
    if (newOwner != oldOwner) {
        // Cria ticket de aprovação
        var approval = new GlideRecord('sysapproval_approver');
        approval.initialize();
        approval.setValue('approver', newOwner);
        approval.setValue('state', 'requested');
        approval.insert();
        
        // Bloqueia mudança até aprovação
        current.setValue('state', 'pending_approval');
    }
})(current, previous);
```

---

## Métricas de Sucesso

Para cada enforcement, medir:

- **Taxa de Compliance**: % de serviços que atendem à política
- **Tempo de Detecção**: Tempo entre violação e detecção
- **Tempo de Correção**: Tempo entre detecção e correção
- **Taxa de Falsos Positivos**: % de bloqueios incorretos
- **Taxa de Adoção**: % de times usando ferramentas corretamente

---

## Próximos Passos

1. **Priorizar Enforcements**: Identificar quais são críticos para começar
2. **Selecionar Ferramentas**: Escolher stack baseado em ambiente e orçamento
3. **Implementar Gradualmente**: Começar com enforcements de build, depois deploy, depois runtime
4. **Medir e Ajustar**: Coletar métricas e ajustar políticas conforme necessário
5. **Comunicar e Treinar**: Educar times sobre novas políticas e ferramentas

---

## Fluxo de Contratos de API

### Arquitetura de Contratos

1. **Repositório Separado**: Contratos OpenAPI/AsyncAPI ficam em repositório GitHub dedicado
2. **Publicação no Gateway**: Contratos são publicados automaticamente no Axway Gateway ou APIM Azure
3. **Validação em CI**: GitHub Actions valida se contrato existe e está publicado
4. **Versionamento**: Gateway gerencia versionamento de contratos

### Exemplo de Fluxo

```
Repositório de Contratos (GitHub)
    ↓ (GitHub Actions)
Validação e Lint
    ↓ (Terraform/API)
Publicação no Gateway (Axway/APIM Azure)
    ↓ (Validação)
Deploy bloqueado se contrato não publicado
```

## Observações sobre Backstage

- **Equipe de Engenharia de Plataforma**: Não utiliza catalog-info.yml
- **População do Catálogo**: Feita via Backstage Catalog API
- **Metadados**: Extraídos do GitHub, Dynatrace e ServiceNow
- **Automação**: Integração via APIs para manter catálogo atualizado

## Referências

- [Backstage Documentation](https://backstage.io/docs)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io)
- [Axway Gateway Documentation](https://docs.axway.com)
- [Azure API Management Documentation](https://docs.microsoft.com/azure/api-management)
- [Dynatrace API Documentation](https://www.dynatrace.com/support/help/dynatrace-api)
- [ServiceNow CMDB Documentation](https://docs.servicenow.com)
- [Azure Red Hat OpenShift Documentation](https://docs.microsoft.com/azure/openshift)

