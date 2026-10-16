# Repository Guidelines

## Project Structure & Module Organization

This workspace currently contains planning material, not an implemented backend:

- `prompt.txt`: the reduced implementation scope and technical requirements.
- `GymCore_Escopo_MVP_Backend.md`: broader product and backend proposal.
- `GymCore_Documentacao_Tecnica.pdf` and `GymCore_MVP.pdf`: supporting references.
- `.idea/`: local IDE metadata.

For implementation scope, follow `prompt.txt` where it narrows the broader documents. References to frontend files describe a separate client repository, not files available here.

When scaffolding, use Maven directories: `src/main/java`, `src/main/resources`, and `src/test/java`. Organize a simple monolith by domain, separating `controller`, `dto`, `service`, `repository`, `entity`, and optional `enums` packages. Place Flyway migrations under `src/main/resources/db/migration`.

## Build, Test, and Development Commands

No build manifest, Maven Wrapper, or runnable commands exist yet. The intended stack is Java 21, Spring Boot, Maven, and PostgreSQL. After adding the wrapper, document and verify these PowerShell commands:

- `.\mvnw.cmd spring-boot:run`: start the local API.
- `.\mvnw.cmd test`: run tests.
- `.\mvnw.cmd verify`: run the configured verification lifecycle and package the application.

Record database prerequisites and environment variables in the implementation README.

## Coding Style & Naming Conventions

Until tooling establishes a shared format, use four-space Java indentation, `PascalCase` classes, `camelCase` methods and fields, and lowercase package names. No formatter or linter is configured.

Keep business rules in services and expose DTOs rather than JPA entities. Use `BigDecimal` for money, explicit date types, and named enum persistence. Avoid Lombok `@Data` on entities. Name migrations descriptively, such as `V1__create_students.sql`; never rewrite applied migrations.

## Testing Guidelines

JUnit is planned; no tests or numeric coverage threshold exist yet. Use descriptive `*Test` classes. Cover tenant isolation, contract overlap, invoice idempotency, short-month due dates, concurrent payments, reversals, and access decisions. Verify persistence against PostgreSQL; use Testcontainers when Docker is available. Report checks that could not run.

## Commit & Pull Request Guidelines

No Git history is available here, so no existing commit convention can be inferred. Use concise imperative subjects, such as `Add invoice generation service`. PRs should explain behavior, scope, related issues, migration impact, and validation results.

## Configuration & Scope

Keep credentials outside versioned files. Preserve financial history and scope business queries by academy. The initial API excludes authentication and external integrations; document its local-development limitation explicitly.