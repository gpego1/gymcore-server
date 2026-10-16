# GymCore Backend

API REST inicial para desenvolvimento local, conforme o escopo reduzido de `prompt.txt`. Implementa cadastros, contratos, aceite presencial, mensalidades, recebimentos manuais e decisões de acesso assistido.

**Sem autenticação ou autorização de usuários.** O identificador da academia na URL delimita consultas e vínculos; qualquer cliente com acesso à API pode selecionar outra academia. Não está pronta para exposição pública. Por padrão, o servidor escuta somente em `127.0.0.1`.

## Stack e requisitos

- Java **21** (JDK; configure `JAVA_HOME`).
- Spring Boot **3.5.16**, Spring MVC, Data JPA e Jakarta Validation.
- Maven **3.9.16**, provisionado pelo Wrapper **3.3.4**.
- PostgreSQL; validado com **18.6**. Flyway **11.20.3** inclui `flyway-database-postgresql`; driver PostgreSQL fixado pelo BOM do Spring Boot.
- springdoc-openapi **2.8.17** e JUnit 5.

As versões de Spring Boot e springdoc foram verificadas no Maven Central. A [matriz oficial do springdoc](https://springdoc.org/v2/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot) relaciona Boot 3.5.x à linha 2.8.x. Não são necessários Docker, serviços externos ou chaves de API. A primeira execução do Wrapper requer rede para baixar Maven e dependências.

## Execução local

Crie um banco PostgreSQL vazio e um usuário com permissão para criar tabelas nesse banco. Não aponte para um banco existente sem revisar a migration; não há baseline automático nem recriação de schema.

```powershell
# Ajuste para o JDK 21 instalado na sua máquina.
$env:JAVA_HOME = 'C:\caminho\para\jdk-21'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/gymcore'
$env:DB_USERNAME = 'gymcore'
$env:DB_PASSWORD = '<senha-local>'
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, use `./mvnw` e `export` para as variáveis. Não versione credenciais. A aplicação lê variáveis do processo; não carrega `.env` automaticamente.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/gymcore` | Banco principal |
| `DB_USERNAME` | `gymcore` | Usuário PostgreSQL |
| `DB_PASSWORD` | vazio | Senha definida no ambiente |
| `GYMCORE_TIMEZONE` | `America/Sao_Paulo` | Datas comerciais e vencimentos |
| `SERVER_ADDRESS` | `127.0.0.1` | Interface de escuta |
| `SERVER_PORT` | `8080` | Porta HTTP |

Flyway aplica `src/main/resources/db/migration/V1__create_core.sql`; Hibernate usa `ddl-auto: validate`. Não altere migrations aplicadas: crie novas versões. `flyway clean` permanece desabilitado.

- Swagger: http://localhost:8080/swagger-ui/index.html
- OpenAPI: http://localhost:8080/v3/api-docs
- Build: `.\mvnw.cmd verify`
- Executar JAR: `java -jar target/gymcore-backend-0.1.0.jar`

## Estrutura e modelo

`src/main/java/com/gymcore` organiza os domínios em `entity`, `dto`, `repository`, `service` e `controller`, além de `config`, `exception` e utilitários compartilhados. Não há interfaces genéricas de serviços CRUD.

| Entidade | Responsabilidade e vínculos |
| --- | --- |
| `Tenant` | Academia; possui alunos e planos |
| `Student` | Aluno adulto de uma academia; e-mail único nesse contexto |
| `Plan` | Plano mensal e preço atual |
| `Contract` | Aluno + plano da mesma academia; preço contratado imutável |
| `ContractAcceptance` | Versão/conteúdo dos termos, nome aceito, operador e instante |
| `Invoice` | Competência e vencimento do contrato; `OPEN`, `PAID` ou `VOID` |
| `Payment` | Recebimento integral manual e dados de eventual estorno |
| `AccessAttempt` | Decisão histórica, motivo, operador e instante |

UUIDs são gerados na aplicação. Valores usam `BigDecimal`/`NUMERIC(12,2)` em BRL. Instantes são armazenados como `TIMESTAMPTZ`; datas comerciais usam `LocalDate` e `Clock` injetado. Associações JPA são LAZY, sem cascades de remoção. As FKs compostas incluem academia e ID, impedindo vínculos cruzados no banco.

## Endpoints

Para as rotas abaixo, `A = /api/v1/academias` e `T = /api/v1/academias/{tenantId}`. O Swagger apresenta payloads, enums, filtros e respostas.

| Método e caminho | Operação |
| --- | --- |
| `POST/GET A` | Criar/listar academias |
| `GET/PUT/DELETE A/{id}` | Consultar/atualizar/inativar academia |
| `POST/GET T/alunos` | Criar/listar alunos |
| `GET/PUT/DELETE T/alunos/{id}` | Consultar/atualizar/inativar aluno |
| `POST/GET T/planos` | Criar/listar planos |
| `GET/PUT/DELETE T/planos/{id}` | Consultar/atualizar/inativar plano |
| `GET T/termos-contratuais/vigentes` | Consultar termos aceitos pelo servidor |
| `POST/GET T/contratos` | Matricular/listar contratos |
| `GET T/contratos/{id}` | Consultar contrato e aceite |
| `POST T/contratos/{id}/cancelamento` | Definir término e motivo |
| `POST T/mensalidades/geracao` | Gerar competências devidas até `throughDate` |
| `GET T/mensalidades[/{id}]` | Listar/consultar mensalidades |
| `POST T/mensalidades/{id}/cancelamento` | Registrar correção justificada |
| `POST/GET T/pagamentos` | Registrar/listar recebimentos |
| `GET T/pagamentos/{id}` | Consultar pagamento |
| `POST T/pagamentos/{id}/estorno` | Registrar estorno e reabrir cobrança |
| `POST/GET T/acessos` | Registrar/listar decisões |
| `GET T/acessos/{id}` | Consultar decisão histórica |

Listagens usam `page` a partir de zero e `size` entre 1 e 100 (padrão 20), ordenadas por ID. Cadastros filtram por `name`/`active`; contratos por `studentId`; mensalidades por `studentId`/`contractId`/`status`/`overdue`; pagamentos por `invoiceId`/`reversed`; acessos por `studentId`/`decision`. Respostas paginadas contêm `content`, `page`, `size`, `totalElements` e `totalPages`.

`POST` de criação retorna 201; consultas, atualizações e operações de ciclo de vida retornam 200; inativação retorna 204. Entrada inválida retorna 400; recurso ausente no contexto retorna 404; duplicidade, concorrência e conflitos de estado retornam 409; regras comerciais retornam 422. Erros usam ProblemDetail com `status`, `code`, `detail`, `instance` e, quando aplicável, `fieldErrors`. Detalhes SQL e stacktraces não são enviados ao cliente.

Uma tentativa de acesso com cobranças devidas faltantes retorna **503** `BILLING_SYNC_PENDING`, com a decisão persistida em `accessAttempt`. Execute a geração explícita e registre nova tentativa. Acesso negado por regra retorna 201 com `DENIED`, pois a tentativa foi registrada. Falha de persistência retorna 503 sem alegar que houve registro.

## Regras e decisões adotadas

- Telefone: E.164, `+` seguido de 10 a 15 dígitos, primeiro dígito diferente de zero. E-mail é normalizado para minúsculas. Não se coleta CPF, biometria ou saúde.
- Inativação sempre preserva dados, inclusive academia sem vínculos. Não há reativação nesta etapa. Atualizar cadastro não reativa; inativar aluno impede nova matrícula e acesso, mas preserva dívidas. Academia inativa impede novos cadastros/matrículas e acesso; correções financeiras e geração de dívidas existentes continuam disponíveis.
- Vigência tem **fim inclusivo**. Contratos podem começar no passado ou no futuro; a primeira cobrança integral é criada na matrícula, mesmo para início futuro. Contratos futuros não liberam acesso. Não há pró-rata.
- Não há status de contrato persistido: vigência e cancelamento são determinados pelas datas e pelo motivo. Cancelamento não pode ser retroativo, estender o término ou ser refeito; preserva débitos e não gera multa/reembolso.
- Vencimento usa sempre o dia original do início: 31/01 → 28/02 ou 29/02 → 31/03. Geração explícita inclui apenas vencimentos já devidos dentro da vigência, recupera lacunas e é repetível. GET não altera cobranças.
- Vencimento hoje não é atraso. Acesso verifica contratos atuais e débitos/lacunas de contratos anteriores do aluno. `VOID` representa correção e não é dívida nem lacuna. Sem contrato vigente, acesso é negado.
- Pagamento é integral, único enquanto não estornado. `EXTERNAL_PIX` e `EXTERNAL_TRANSFER` são apenas classificação de recebimentos externos. Estorno mantém o original e permite nova baixa; não transfere dinheiro.
- Contrato, aceite e primeira mensalidade são atômicos. Todas as mutações e decisões de acesso adquirem lock pessimista da academia antes de consultar/alterar agregados. Isso serializa escritas por academia, uma escolha simples para o piloto; pode limitar throughput. Há `@Version`, unicidade de competência e índice parcial para pagamento vigente.
- `recordedBy` é texto informado pela recepção e não identidade autenticada. Termos são uma versão técnica de demonstração no servidor, com snapshot no aceite; não representam assinatura digital ou identidade verificada.

## Fluxo executável

Com a aplicação iniciada, execute `scripts/demo.ps1`. O script cria registros fictícios novos a cada execução e percorre academia → aluno → plano → contrato → mensalidade → pagamento → acesso. Não envia mensagens nem aciona serviços externos. Também é possível executar o mesmo fluxo pelo Swagger:

1. Crie academia, aluno adulto e plano mensal.
2. Consulte termos vigentes e copie a versão retornada.
3. Crie contrato com IDs do aluno/plano, `startDate`, versão, nome aceito e operador.
4. Liste mensalidades; registre pagamento integral usando o ID da cobrança.
5. Registre decisão de acesso com `studentId` e operador.
6. Para contratos antigos, gere competências até hoje antes de avaliar acesso; pague cada dívida individualmente.

## Testes e verificação

```powershell
.\mvnw.cmd test  # testes unitários; integração é ignorada sem configuração explícita
# PostgreSQL temporário separado, sem Docker nem senha do servidor existente:
.\scripts\test-postgres.ps1 -PostgresBin 'C:\Program Files\PostgreSQL\18\bin'
```

O script reserva a porta 55439 (substituível com `-Port`), inicializa um cluster novo no diretório temporário do Windows, escuta apenas em loopback, cria `gymcore_test`, executa `verify` e para o servidor em `finally`. A autenticação `trust` existe somente nesse cluster temporário local. Arquivos e logs são preservados e o caminho é impresso; o banco existente não é modificado.

Alternativa multiplataforma: configure `GYMCORE_TEST_DB_URL`, `GYMCORE_TEST_DB_USERNAME` e `GYMCORE_TEST_DB_PASSWORD` para um **banco PostgreSQL exclusivo de testes**, vazio ou com as migrations deste projeto, e rode `./mvnw verify`. Testes inserem dados fictícios, sem limpeza destrutiva. Não use banco operacional.

Testes cobrem datas, isolamento, constraints reais, aceite, atomicidade, snapshot, inativação, geração repetida/concorrente, matrícula concorrente, pagamento repetido/concorrente, estorno, múltiplas dívidas, decisões de acesso, validação HTTP e Swagger. Não há H2 nem limiar numérico de cobertura. Resultados ficam em `target/surefire-reports`.

Verificação executada em **16/10/2026**, com JDK 21.0.10 e PostgreSQL 18.6: `verify` concluiu com **24 testes aprovados** (4 unitários e 20 de integração), zero falhas, erros ou testes ignorados. O JAR empacotado foi iniciado contra outro banco temporário; OpenAPI (19 caminhos e 31 operações), Swagger e `scripts/demo.ps1` foram exercitados por HTTP. O fluxo terminou com `ALLOWED` / `ELIGIBLE`. Aplicação e instâncias temporárias foram paradas após as verificações.

## Limites do ambiente e do escopo

O checkout inicial continha somente documentos e metadados de IDE, sem Git, entidades, migrations ou configuração de banco. O PostgreSQL existente exigia credenciais não fornecidas: não foi acessado nem alterado. A validação usa instância temporária independente; compatibilidade com qualquer schema externo continua dependendo de acesso e inspeção desse schema.

Docker estava instalado, mas o daemon não estava disponível; os testes usam os binários locais do PostgreSQL. Nenhuma integração externa, autenticação, frontend, job agendado, infraestrutura de deploy ou funcionalidade dos módulos adiados foi adicionada.

A consulta complementar ao repositório remoto do cliente expirou. A implementação se baseia nos documentos locais e no escopo reduzido; não foi validada integração com o frontend, que está fora desta etapa.
