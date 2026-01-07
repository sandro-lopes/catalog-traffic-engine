# Mapeamento de Enforcement - Novos Componentes Estruturantes

Este documento mapeia cada **Enforcement** das políticas de governança especificamente para **novos componentes estruturantes** criados via Backstage Scaffolder. Como novos componentes nascem com todas as informações necessárias desde a criação, os processos focam em **validações preventivas** e **garantias de conformidade desde o início**.

## Contexto: Novos Componentes Estruturantes

Novos componentes estruturantes criados via Backstage Scaffolder já possuem:

- ✅ **Owner definido** no momento da criação (usuário que criou via Scaffolder)
- ✅ **Classificação correta** (todos são "Componente Estruturante")
- ✅ **Metadados completos** (nome, domínio, tipo, lifecycle inicial)
- ✅ **Estrutura de projeto padronizada** (template aprovado)
- ✅ **Pipelines configurados** (GitHub Actions com validações)
- ✅ **Observabilidade pré-configurada** (Dynatrace OneAgent no template)

## Diferenças em Relação a Componentes Existentes

| Aspecto | Componentes Existentes | Novos Componentes |
|---------|------------------------|-------------------|
| **Validação de Owner** | Reativa (verificar se existe) | Preventiva (já definido na criação) |
| **Classificação** | Reativa (verificar e corrigir) | Preventiva (já correta) |
| **Metadados** | Reativa (completar faltantes) | Preventiva (todos presentes) |
| **Template** | Reativa (validar fingerprint) | Preventiva (só pode usar template aprovado) |
| **Contrato** | Reativa (verificar se existe) | Preventiva (criar junto ou validar criação) |
| **Observabilidade** | Reativa (verificar instalação) | Preventiva (já no template) |

---

## Tabela de Mapeamento para Novos Componentes

| Política | Enforcement | Ferramentas/Componentes | Processo Passo a Passo | Pontos de Revalidação |
|----------|-------------|------------------------|------------------------|----------------------|
| **POL-01** | Classificação obrigatória no Backstage | **Backstage Scaffolder**<br/>**Backstage Catalog API** | 1. Backstage Scaffolder já cria entidade com kind "Component" e tag "estruturante"<br/>2. Template do Scaffolder força classificação correta no momento da criação<br/>3. Usuário não pode alterar classificação durante criação<br/>4. Entidade é criada automaticamente no Backstage Catalog via Scaffolder<br/>5. Validação ocorre antes de criar repositório no GitHub<br/>6. Se classificação inválida, Scaffolder bloqueia criação | **Revalidação**: Nenhuma necessária - classificação é imutável após criação |
| **POL-01** | Componentes mal classificados são bloqueados para evolução | **Backstage Scaffolder**<br/>**GitHub Actions**<br/>**ArgoCD Admission Controller (ARO)** | 1. Backstage Scaffolder valida classificação antes de criar repositório<br/>2. Se classificação incorreta, Scaffolder não prossegue com criação<br/>3. GitHub Actions valida classificação no primeiro commit (verificação de segurança)<br/>4. ArgoCD valida classificação no primeiro deploy (verificação de segurança)<br/>5. Como classificação já está correta, validações sempre passam<br/>6. Validações servem como garantia adicional | **Revalidação**: Validações em PR e deploy servem como garantia, mas não devem falhar |
| **POL-02** | Criação manual de repositórios é proibida | **Backstage Scaffolder**<br/>**GitHub Organization Settings** | 1. Backstage Scaffolder é o único ponto de entrada para criação<br/>2. Scaffolder cria repositório no GitHub automaticamente após validações<br/>3. GitHub Organization Settings bloqueia criação manual de repositórios<br/>4. Usuário não tem permissão para criar repositório diretamente no GitHub<br/>5. Scaffolder adiciona tag especial ao repositório para identificação<br/>6. Qualquer repositório sem tag é considerado ilegal | **Revalidação**: GitHub Actions valida presença de tag em cada PR |
| **POL-02** | Pipelines validam fingerprint do template | **Backstage Scaffolder Template**<br/>**GitHub Actions**<br/>**Nexus** | 1. Backstage Scaffolder usa apenas templates aprovados com fingerprint conhecido<br/>2. Template aprovado já tem fingerprint registrado no Nexus<br/>3. Scaffolder valida fingerprint antes de gerar código<br/>4. Primeiro commit já contém código do template aprovado<br/>5. GitHub Actions valida fingerprint no primeiro build (garantia)<br/>6. Nexus valida fingerprint antes de aceitar primeiro artefato<br/>7. Como código vem do template aprovado, validação sempre passa | **Revalidação**: GitHub Actions valida em cada build para garantir que código não foi alterado manualmente |
| **POL-03** | Deploy bloqueado sem owner | **Backstage Scaffolder**<br/>**ArgoCD Admission Controller (ARO)** | 1. Backstage Scaffolder captura owner automaticamente (usuário que criou)<br/>2. Scaffolder preenche campo "spec.owner" na entidade do Backstage<br/>3. Scaffolder adiciona annotation "backstage.io/owner" no Deployment template<br/>4. Primeiro deploy já contém owner correto<br/>5. ArgoCD ValidatingAdmissionWebhook valida owner (garantia)<br/>6. Como owner já está presente, validação sempre passa | **Revalidação**: ArgoCD valida owner em cada deploy para garantir que não foi removido |
| **POL-03** | Serviços sem owner entram em estado RESTRICTED | **Backstage Catalog API**<br/>**ServiceNow Automation** | 1. Como novos componentes já têm owner, este enforcement não se aplica<br/>2. Job de validação diária verifica se owner foi removido posteriormente<br/>3. Se owner removido (cenário raro), ServiceNow Automation atualiza status<br/>4. Este é um enforcement de segurança para mudanças futuras | **Revalidação**: Job diário verifica se owner permanece válido (mudanças futuras) |
| **POL-04** | Alteração direta de YAML bloqueada | **GitHub Branch Protection**<br/>**Backstage Catalog API** | 1. Backstage Scaffolder cria repositório com branch protection já configurada<br/>2. Branch main/master já está protegida desde a criação<br/>3. Usuário não pode fazer push direto (sempre via PR)<br/>4. Backstage Catalog API permanece read-only para usuários<br/>5. Mudanças de ownership futuras devem passar por ServiceNow workflow | **Revalidação**: GitHub Branch Protection valida em cada tentativa de push |
| **POL-04** | Auditoria obrigatória | **GitHub Audit Log**<br/>**Backstage Catalog API**<br/>**ServiceNow CMDB** | 1. Criação via Backstage Scaffolder já é registrada no GitHub Audit Log<br/>2. Backstage Catalog API registra criação da entidade com timestamp e usuário<br/>3. ServiceNow CMDB recebe webhook de criação e registra automaticamente<br/>4. Auditoria completa desde o primeiro momento<br/>5. Histórico completo disponível desde a criação | **Revalidação**: Auditoria contínua para todas as mudanças futuras |
| **POL-05** | Build falha se metadados incompletos | **Backstage Scaffolder**<br/>**GitHub Actions**<br/>**Nexus** | 1. Backstage Scaffolder força preenchimento de todos os metadados obrigatórios<br/>2. Scaffolder não permite criar componente sem preencher: nome, domínio, owner, tipo, lifecycle, criticidade<br/>3. Template já inclui arquivo de metadados com todos os campos<br/>4. Primeiro build valida presença de metadados (garantia)<br/>5. Nexus valida metadados antes de aceitar artefato<br/>6. Como metadados já estão completos, validação sempre passa | **Revalidação**: GitHub Actions valida em cada build para garantir que metadados não foram removidos |
| **POL-06** | CI valida existência do contrato | **Backstage Scaffolder**<br/>**GitHub Actions**<br/>**Repositório de Contratos** | 1. Backstage Scaffolder pode criar contrato inicial junto com o componente (opção no template)<br/>2. Se opção selecionada, Scaffolder cria arquivo OpenAPI básico no repositório de contratos<br/>3. Scaffolder publica contrato inicial no gateway (Axway ou APIM Azure)<br/>4. Primeiro build valida que contrato existe e está publicado<br/>5. Se contrato não foi criado, Scaffolder pode bloquear criação ou criar automaticamente<br/>6. GitHub Actions valida contrato em cada build (garantia) | **Revalidação**: GitHub Actions valida em cada build que contrato ainda existe e está atualizado |
| **POL-07** | Pipeline falha sem referência válida ao contrato | **Backstage Scaffolder**<br/>**GitHub Actions**<br/>**Axway Gateway API**<br/>**APIM Azure** | 1. Backstage Scaffolder publica contrato no gateway durante criação<br/>2. Scaffolder adiciona referência ao contrato nos metadados do Backstage<br/>3. Primeiro pipeline valida que contrato está publicado (garantia)<br/>4. GitHub Actions valida referência em cada build<br/>5. Como contrato já está publicado, validação sempre passa | **Revalidação**: GitHub Actions valida em cada build que contrato ainda está publicado no gateway |
| **POL-08** | Detecção automática de breaking change | **GitHub Actions**<br/>**Repositório de Contratos**<br/>**Axway Gateway** | 1. Contrato inicial criado pelo Scaffolder é versionado (v1.0.0)<br/>2. Qualquer mudança no contrato é detectada via GitHub Actions<br/>3. GitHub Actions compara versão atual com versão anterior<br/>4. Se breaking change detectado, GitHub Actions bloqueia merge<br/>5. Scaffolder força incremento de versão para breaking changes<br/>6. Validação ocorre desde o primeiro PR que modifica contrato | **Revalidação**: GitHub Actions valida breaking changes em cada PR que modifica contrato |
| **POL-08** | Bloqueio de deploy | **ArgoCD Admission Controller (ARO)** | 1. ArgoCD valida breaking changes antes de permitir deploy<br/>2. Como contrato inicial é v1.0.0, primeiro deploy sempre passa<br/>3. Deploys futuros validam se breaking change foi aprovado<br/>4. Validação serve como garantia adicional | **Revalidação**: ArgoCD valida em cada deploy se breaking changes foram tratados corretamente |
| **POL-09** | Gateway bloqueia exposição indevida | **Axway Gateway**<br/>**APIM Azure**<br/>**Backstage Scaffolder** | 1. Backstage Scaffolder configura API no gateway com políticas corretas desde o início<br/>2. Scaffolder adiciona tag "estruturante" na configuração do gateway<br/>3. Gateway aplica política que bloqueia acesso frontend → estruturante automaticamente<br/>4. Política já está ativa desde a primeira publicação<br/>5. Não é necessário configurar manualmente | **Revalidação**: Gateway valida políticas em cada requisição (runtime) |
| **POL-10** | OAuth2 client_credentials obrigatório | **Azure AD**<br/>**Backstage Scaffolder**<br/>**Axway Gateway** | 1. Backstage Scaffolder registra aplicação no Azure AD automaticamente<br/>2. Scaffolder gera Client ID e Client Secret<br/>3. Scaffolder configura gateway para exigir OAuth2 desde o início<br/>4. Template do serviço já inclui configuração de autenticação<br/>5. Primeira requisição já exige token OAuth2<br/>6. Não é necessário configurar manualmente | **Revalidação**: Gateway valida token OAuth2 em cada requisição (runtime) |
| **POL-10** | Managed Identity (quando disponível) | **Azure Managed Identity**<br/>**Backstage Scaffolder**<br/>**ARO Service Accounts** | 1. Backstage Scaffolder habilita Managed Identity para recursos Azure automaticamente<br/>2. Scaffolder configura permissões necessárias no Azure AD<br/>3. Template do serviço já inclui configuração de Managed Identity<br/>4. No ARO, Scaffolder cria Service Account automaticamente<br/>5. Deployment template já referencia Service Account correto<br/>6. Tudo configurado desde o primeiro deploy | **Revalidação**: ArgoCD valida em cada deploy que Managed Identity/Service Account está configurado |
| **POL-11** | Deploy bloqueado sem observabilidade | **Backstage Scaffolder Template**<br/>**ArgoCD Admission Controller** | 1. Template do Backstage Scaffolder já inclui Dynatrace OneAgent como initContainer<br/>2. Template já inclui annotations de observabilidade (environment, tags)<br/>3. Template já inclui labels obrigatórios (app, version, environment)<br/>4. Primeiro deploy já tem observabilidade completa<br/>5. ArgoCD valida presença de OneAgent (garantia)<br/>6. Como já está no template, validação sempre passa | **Revalidação**: ArgoCD valida em cada deploy que observabilidade não foi removida |
| **POL-12** | Detecção automática de inatividade | **Dynatrace API**<br/>**Backstage Catalog API** | 1. Como componente é novo, não há histórico de inatividade inicial<br/>2. Componente começa com lifecycle "EXPERIMENTAL" ou "ACTIVE"<br/>3. Job de detecção de inatividade monitora desde o primeiro deploy<br/>4. Detecção ocorre após período mínimo (ex: 30 dias)<br/>5. Não se aplica imediatamente após criação | **Revalidação**: Job semanal monitora inatividade após período inicial |
| **POL-12** | Avaliação automática de desligamento | **ServiceNow Workflow**<br/>**Dynatrace API** | 1. Não se aplica imediatamente após criação<br/>2. Workflow é acionado apenas se inatividade for detectada<br/>3. Processo é o mesmo para novos e existentes após período inicial | **Revalidação**: Workflow avalia quando inatividade é detectada |
| **POL-13** | Ciclo de vida explícito | **Backstage Scaffolder**<br/>**Backstage Catalog API** | 1. Backstage Scaffolder define lifecycle inicial como "EXPERIMENTAL" ou "ACTIVE"<br/>2. Scaffolder força seleção de lifecycle durante criação<br/>3. Campo "spec.lifecycle" já está preenchido desde a criação<br/>4. Usuário não pode criar sem definir lifecycle<br/>5. Lifecycle é sincronizado com ServiceNow CMDB automaticamente | **Revalidação**: Job diário valida que lifecycle permanece válido e atualizado |
| **POL-14** | Headers de depreciação obrigatórios | **Axway Gateway**<br/>**APIM Azure** | 1. Como componente é novo, não está deprecated inicialmente<br/>2. Quando lifecycle mudar para "DEPRECATED", gateway adiciona headers automaticamente<br/>3. Gateway consulta Backstage para verificar lifecycle<br/>4. Headers são adicionados automaticamente quando necessário | **Revalidação**: Gateway valida lifecycle em cada requisição e adiciona headers se deprecated |
| **POL-14** | Desligamento automático ao atingir sunsetDate | **Terraform**<br/>**ArgoCD**<br/>**ServiceNow** | 1. Não se aplica imediatamente após criação<br/>2. Processo é acionado quando componente entra em DEPRECATED com sunsetDate<br/>3. Processo é o mesmo para novos e existentes | **Revalidação**: Job diário verifica sunsetDate quando componente está deprecated |
| **POL-15** | Anti-corruption layer obrigatório | **Axway Gateway**<br/>**Backstage Scaffolder** | 1. Backstage Scaffolder força seleção de padrão de integração durante criação<br/>2. Se integração com legado, Scaffolder exige criação de ADR no GitHub<br/>3. Scaffolder configura gateway como anti-corruption layer se necessário<br/>4. Template já inclui padrão correto desde o início<br/>5. Não é necessário configurar manualmente | **Revalidação**: Gateway valida padrão de integração em cada requisição |
| **POL-16** | Fail fast em build | **Backstage Scaffolder**<br/>**GitHub Actions** | 1. Backstage Scaffolder garante que componente nasce conforme todas as políticas<br/>2. Primeiro build já tem todas as validações configuradas<br/>3. GitHub Actions valida todas as políticas desde o primeiro commit<br/>4. Como tudo já está correto, validações sempre passam<br/>5. Validações servem como garantia e proteção contra mudanças futuras | **Revalidação**: GitHub Actions valida em cada build para garantir conformidade contínua |
| **POL-16** | Fail fast em deploy | **ArgoCD Admission Controller** | 1. ArgoCD valida todas as políticas antes do primeiro deploy<br/>2. Como componente já está conforme, primeiro deploy sempre passa<br/>3. Validações servem como garantia e proteção contra mudanças futuras | **Revalidação**: ArgoCD valida em cada deploy para garantir conformidade contínua |
| **POL-16** | Fail fast em runtime | **Axway Gateway**<br/>**APIM Azure** | 1. Gateway já está configurado com políticas corretas desde a criação<br/>2. Primeira requisição já é validada corretamente<br/>3. Políticas estão ativas desde o início | **Revalidação**: Gateway valida em cada requisição (runtime contínuo) |
| **POL-17** | Governança integrada ao SDLC | **Backstage Scaffolder**<br/>**GitHub Actions**<br/>**ArgoCD** | 1. Backstage Scaffolder cria componente já com governança integrada<br/>2. GitHub Actions workflow já está configurado com todas as validações<br/>3. ArgoCD já está configurado com políticas desde o primeiro deploy<br/>4. Governança não é etapa separada, é parte natural do componente<br/>5. Não é necessário adicionar governança posteriormente | **Revalidação**: Validações contínuas em cada etapa do SDLC |
| **POL-18** | Incentivo ao desligamento (sem penalização) | **ServiceNow CMDB**<br/>**Backstage** | 1. Componente novo não precisa ser desligado inicialmente<br/>2. Quando necessário desligar no futuro, processo não penaliza<br/>3. Métricas não incluem componentes desligados como não conformes | **Revalidação**: Métricas são calculadas excluindo componentes desligados |
| **POL-19** | Métricas de governança | **Backstage Metrics Plugin**<br/>**Dynatrace Dashboards** | 1. Componente novo já contribui positivamente para métricas desde a criação<br/>2. Componente já tem owner, metadados, contrato, observabilidade<br/>3. Métricas são calculadas incluindo novos componentes<br/>4. Novos componentes aumentam % de conformidade | **Revalidação**: Métricas são recalculadas regularmente incluindo novos componentes |

---

## Resumo: Validações Preventivas vs Reativas

### Validações Preventivas (No Momento da Criação)

Estas validações ocorrem **antes** do componente ser criado e garantem que ele nasce conforme:

- ✅ **Owner**: Capturado automaticamente do usuário que cria
- ✅ **Classificação**: Forçada pelo Scaffolder (sempre "estruturante")
- ✅ **Metadados**: Scaffolder força preenchimento de todos os campos
- ✅ **Template**: Apenas templates aprovados podem ser usados
- ✅ **Observabilidade**: Template já inclui Dynatrace OneAgent
- ✅ **Autenticação**: Template já inclui OAuth2/Managed Identity
- ✅ **Gateway**: Scaffolder configura políticas corretas
- ✅ **Contrato**: Scaffolder pode criar contrato inicial

### Validações de Garantia (Após Criação)

Estas validações ocorrem **após** a criação para garantir que conformidade é mantida:

- 🔍 **GitHub Actions**: Valida em cada build que políticas não foram violadas
- 🔍 **ArgoCD**: Valida em cada deploy que configurações não foram alteradas
- 🔍 **Gateway**: Valida em cada requisição que políticas estão ativas
- 🔍 **Jobs Agendados**: Validações periódicas para garantir conformidade contínua

### Pontos de Revalidação Críticos

Mesmo para novos componentes, é necessário revalidar:

1. **Mudanças Futuras**: Quando código, configuração ou metadados são alterados
2. **Breaking Changes**: Quando contrato é modificado
3. **Mudanças de Ownership**: Se ownership for transferido
4. **Mudanças de Lifecycle**: Quando componente entra em DEPRECATED
5. **Remoção de Configurações**: Se observabilidade, autenticação ou políticas forem removidas

---

## Fluxo de Criação de Novo Componente Estruturante

```
1. Usuário acessa Backstage Scaffolder
   ↓
2. Seleciona template aprovado
   ↓
3. Preenche formulário obrigatório:
   - Nome do componente
   - Domínio
   - Tipo
   - Lifecycle inicial
   - Criticidade
   - Criar contrato inicial? (sim/não)
   ↓
4. Scaffolder valida todas as informações
   ↓
5. Scaffolder cria:
   - Repositório no GitHub
   - Entidade no Backstage Catalog
   - Contrato no repositório de contratos (se solicitado)
   - Publicação no gateway (se contrato criado)
   - Registro no Azure AD (OAuth2)
   - Managed Identity (se Azure)
   - Service Account (se ARO)
   - Registro no ServiceNow CMDB
   ↓
6. Scaffolder configura:
   - Branch protection no GitHub
   - GitHub Actions workflow com validações
   - Deployment template com observabilidade
   - Políticas no gateway
   ↓
7. Componente nasce 100% conforme todas as políticas
   ↓
8. Validações contínuas garantem conformidade futura
```

---

## Vantagens para Novos Componentes

1. **Zero Fricção**: Componente nasce já conforme, sem necessidade de correções
2. **Tempo de Criação**: Mais rápido, pois tudo é automático
3. **Consistência**: Todos os componentes seguem mesmo padrão
4. **Menos Erros**: Validações preventivas evitam problemas
5. **Governança By Design**: Governança integrada desde o início

---

## Comparação: Novos vs Existentes

| Aspecto | Componentes Existentes | Novos Componentes |
|---------|------------------------|-------------------|
| **Tempo de Conformidade** | Semanas/meses (correções) | Imediato (nasce conforme) |
| **Esforço** | Alto (correções manuais) | Baixo (automático) |
| **Taxa de Conformidade Inicial** | Variável (depende de correções) | 100% (garantido) |
| **Validações** | Reativas (corrigir problemas) | Preventivas (evitar problemas) |
| **Risco** | Alto (pode não corrigir tudo) | Baixo (garantido pelo Scaffolder) |

---

## Referências

- [Backstage Scaffolder Documentation](https://backstage.io/docs/features/software-catalog/software-catalog-overview)
- [Backstage Template System](https://backstage.io/docs/features/software-catalog/descriptor-format)
- [ArgoCD GitOps](https://argo-cd.readthedocs.io)
- [Azure Managed Identity](https://docs.microsoft.com/azure/active-directory/managed-identities-azure-resources)

