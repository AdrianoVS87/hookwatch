# ADR-0003: Flyway for schema version control

**Status:** Accepted  
**Date:** 2026-03-23  
**Deciders:** Adriano Viera dos Santos

---

## Context

HookWatch's domain model evolves as new features ship (e.g., adding indexes,
new columns, altering types). Schema changes need to be:
- **Auditable** — reviewable in Git like any other code change
- **Reproducible** — same migration produces identical schema across dev/staging/prod
- **Safe** — rollback possible, CI fails if migration is broken before reaching prod

Two approaches were considered:

| Option | Pros | Cons |
|--------|------|------|
| Hibernate `ddl-auto: update` | Zero config | Silent data loss on column rename; not safe for production |
| Flyway versioned migrations | Auditable, rollback-safe, CI-validatable | Requires SQL discipline |

---

## Decision

Use **Flyway** with versioned SQL migration files in `src/main/resources/db/migration/`.

Migration naming convention: `V{version}__{description}.sql`  
Example: `V3__create_traces_and_spans.sql`

Profile strategy:
- **dev** (`H2`): `flyway.enabled=false`, Hibernate `ddl-auto: create-drop`. Fast
  iteration, no migration overhead during local development.
- **docker/prod** (`PostgreSQL 16`): `flyway.enabled=true`. All schema changes
  go through versioned migrations.

---

## Consequences

- **Positive:** Schema history is a first-class Git artifact. Every PR with a
  schema change includes the migration file, making review straightforward.
- **Positive:** `flyway validate` in CI catches drift between entity classes and
  migrations before deployment.
- **Positive:** Flyway's checksums detect accidental migration file edits in
  production.
- **Negative:** Dev and prod schemas can diverge subtly (H2 vs PostgreSQL type
  system). Mitigated by running integration tests against PostgreSQL via
  Testcontainers (`@ActiveProfiles("docker")` in test classes).
- **Negative:** Renaming a column requires a multi-step migration (add new column,
  backfill, drop old). This is the correct approach — it makes breaking changes
  explicit and observable.

---

## Migration checklist (for contributors)

Before adding a migration:
1. Is the change backward-compatible? (additive = safe, rename = careful)
2. Does the migration include an index for every foreign key? (see V2, V3)
3. Does CI run `mvn flyway:validate` against a Testcontainers PostgreSQL? (it does)
4. Is the migration idempotent? Use `IF NOT EXISTS` where applicable.
