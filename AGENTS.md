# GymCore Backend — orientação para agentes

Estado verificado em 16/10/2026. Leia também ../AGENTS.md, quando disponível. O backend está implementado; o cliente irmão ainda usa mocks e não o consome.

## Stack e estrutura

Monólito REST Java 21, Maven, Spring Boot 3.5.16, MVC, Data JPA, Jakarta Validation, PostgreSQL e Flyway 11.20.3. OpenAPI usa springdoc 2.8.17; testes usam Spring Boot Test/JUnit 5. Confira versões no pom.xml.

`src/main/java/com/gymcore/` organiza primeiro por domínio: tenant, student, plan, contract, acceptance, invoice, payment e access. Dentro deles há controller, dto, service, repository, entity e enums conforme necessário. Acceptance é registrado pelo contrato, sem CRUD próprio.

- GymCoreApplication: entrada Spring Boot.
- config/ApplicationConfig: Clock comercial e OpenAPI.
- shared/entity: UUID, versão otimista e contexto de academia; shared/Queries: filtros/paginação; shared/dto/PageResult: resposta paginada.
- exception: BusinessException e tratamento global ProblemDetail.
- src/main/resources/application.yml: configuração; db/migration/V1__create_core.sql: esquema e constraints.
- src/test/java/com/gymcore/invoice/BillingScheduleTest.java: datas; CoreIntegrationTest.java: persistência, concorrência, isolamento, fluxos e HTTP.

Não há autenticação/autorização, processamento de dinheiro, mensagens, dispositivo de acesso ou módulos de treino/evolução/aulas/CRM/PDV. recordedBy é texto sem identidade autenticada. Aceite é snapshot técnico demonstrativo, sem assinatura digital verificada.

## Invariantes

Fluxo: academia → aluno adulto + plano mensal → contrato + aceite + primeira mensalidade → recebimento manual → decisão de acesso.

1. Consulte recursos no contexto da academia. Preserve tenantId, FKs compostas e testes contra vínculos cruzados. A URL não constitui autorização.
2. Aluno exige idade mínima de 18 anos, e-mail normalizado e único por academia e telefone E.164 (+ e 10 a 15 dígitos). Não se coleta CPF, biometria ou saúde.
3. Academia/aluno/plano são inativados, sem exclusão física ou reativação nesta etapa. Histórico e dívidas permanecem; academia inativa ainda permite correções financeiras e geração de dívidas existentes.
4. Contratos do mesmo aluno não se sobrepõem. Fim é inclusivo; preço é snapshot. Contrato, aceite e primeira mensalidade são atômicos.
5. Primeira cobrança integral nasce na matrícula, inclusive para início futuro. Não há pró-rata. Contrato futuro não libera acesso. Cancelamento não é retroativo, não estende vigência nem apaga dívidas; não produz multa/reembolso.
6. BillingSchedule preserva o dia original: 31/01 → último dia de fevereiro → 31/03. Competência é primeiro dia do mês. Geração explícita só alcança a data comercial atual, recupera lacunas e não duplica competências. GET não gera cobranças.
7. Invoice tem OPEN, PAID ou VOID. Atraso é OPEN com vencimento anterior a hoje. Vencer hoje não é atraso. VOID é correção justificada, não dívida nem lacuna.
8. Pagamento é integral, único enquanto não estornado. Estorno preserva o original e reabre cobrança. CASH, EXTERNAL_PIX e EXTERNAL_TRANSFER classificam recebimentos; não movimentam dinheiro.
9. Acesso exige academia/aluno ativos, contrato vigente e ausência de atraso/lacunas relevantes, inclusive de contratos anteriores. AccessService registra decisão histórica; não opera catraca.
10. Mutações/decisões bloqueiam pessimisticamente a academia antes de consultar/alterar agregados. Preserve ordem dos locks, transações, @Version, unicidade de competência e índice parcial de pagamento vigente. A serialização por academia pode limitar throughput.

Use BigDecimal/NUMERIC(12,2) para dinheiro, LocalDate e Clock injetado para datas comerciais e TIMESTAMPTZ para instantes. Preserve associações LAZY, ausência de cascades de remoção e DTOs em vez de entidades expostas. Mantenha regras nos serviços e validação de entrada nos DTOs. Java usa quatro espaços, classes PascalCase e membros camelCase; não há formatter configurado.

## API

Base `/api/v1/academias`; recursos internos em `/api/v1/academias/{tenantId}`. Consulte controllers/DTOs e /v3/api-docs para payloads exatos.

- Academias, alunos e planos: criar/listar/consultar/atualizar/inativar.
- /termos-contratuais/vigentes: termos aceitos pelo servidor.
- /contratos e /contratos/{id}/cancelamento: matrícula, consulta e término justificado.
- /mensalidades, /mensalidades/geracao e /mensalidades/{id}/cancelamento: consulta, geração e correção.
- /pagamentos e /pagamentos/{id}/estorno: recebimentos/estornos.
- /acessos: registro/consulta de decisões.

Listas têm page desde zero, size de 1 a 100 (padrão 20), ordenação por ID e resposta content, page, size, totalElements e totalPages. Filtros variam por controller.

Criação retorna 201; consulta/atualização/ciclo de vida, 200; inativação, 204. ProblemDetail: entrada inválida 400, recurso ausente no contexto 404, conflito 409 e regra comercial 422. Não exponha SQL ou stacktrace.

Acesso negado por regra retorna 201/DENIED, pois foi registrado. Competências faltantes geram UNAVAILABLE; controller retorna 503/BILLING_SYNC_PENDING com tentativa persistida em accessAttempt. Gere mensalidades explicitamente e tente novamente. Não confunda com falha de persistência sem registro.

## Ambiente e execução

Na pasta gymcore-backend, use JDK 21/JAVA_HOME, Wrapper Maven e PostgreSQL. Primeiro uso pode exigir download. Banco deve ser exclusivo para a finalidade escolhida.

O application.yml atual exige explicitamente DB_URL, DB_USERNAME, DB_PASSWORD, SERVER_ADDRESS, SERVER_PORT e GYMCORE_TIMEZONE, sem valores padrão. Há divergência com a tabela de padrões do README; siga o arquivo de configuração. A aplicação não carrega .env automaticamente; não imprima nem versione seu conteúdo.

```powershell
# Configure DB_URL, DB_USERNAME e DB_PASSWORD no ambiente do processo.
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '8080'
$env:GYMCORE_TIMEZONE = 'America/Sao_Paulo'
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd verify
```

Swagger: http://127.0.0.1:8080/swagger-ui/index.html; OpenAPI: /v3/api-docs. JAR: target/gymcore-backend-0.1.0.jar. Linux/macOS usam ./mvnw.

Flyway aplica migrations; Hibernate apenas valida (ddl-auto: validate). Open-in-view e flyway clean estão desabilitados. Não reescreva migrations aplicadas nem recrie schema operacional para contornar erros.

## Verificação

- mvnw test executa unitários; CoreIntegrationTest é ignorado sem GYMCORE_TEST_DB_URL. Build verde com testes ignorados não comprova persistência.
- Para integração, configure GYMCORE_TEST_DB_URL, GYMCORE_TEST_DB_USERNAME e GYMCORE_TEST_DB_PASSWORD para PostgreSQL exclusivo de testes, além das variáveis exigidas, e execute verify. Não use banco operacional.
- Quando disponível, scripts/test-postgres.ps1 -PostgresBin '<diretório bin PostgreSQL>' prepara cluster temporário e executa verify. scripts/demo.ps1 cria dados fictícios e percorre fluxo HTTP com a API iniciada. Leia scripts antes de executá-los.
- scripts/, .mvn/ e documentos de referência estão ignorados pelo .gitignore atual e podem existir apenas neste workspace. Confira disponibilidade em clone limpo.
- Cubra mudanças de regras com testes relevantes de datas, isolamento, atomicidade, concorrência, estorno e acesso. Use PostgreSQL para constraints reais; não substitua silenciosamente por H2. Relatórios ficam em target/surefire-reports. Não há limiar numérico de cobertura.
- Informe validações reais e testes ignorados; resultados históricos do README não comprovam execução atual.

Consulte prompt.txt para o escopo reduzido desta etapa. Documentos maiores são contexto/roadmap e não autorizam módulos adicionais. Atualize README e este guia ao alterar configuração/API. Não exponha publicamente uma API sem autenticação nem acione integrações reais por causa dos briefs históricos.
