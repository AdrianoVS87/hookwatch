# Contributing to HookWatch

Thank you for your interest. This document covers the development setup,
branching strategy, commit conventions, and code review expectations.

---

## Local development setup

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 21 (LTS) | Backend runtime |
| Maven | 3.9+ | Build tool |
| Node.js | 20 LTS | Frontend tooling |
| Docker | 24+ | Local services |
| Docker Compose | v2 | Orchestration |

### Quick start

```bash
git clone git@github.com:AdrianoVS87/hookwatch.git
cd hookwatch

# Start PostgreSQL + Redis
make up

# Backend (dev profile uses H2, no Docker needed)
cd api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# API available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui/index.html
# Demo data seeded automatically (API key: demo-key-hookwatch)

# Frontend (in a separate terminal)
cd web
cp .env.example .env
npm install
npm run dev
# UI available at http://localhost:5173
```

### Full Docker stack

```bash
make build   # Build all images
make up      # Start all services
make logs    # Tail logs
make down    # Stop services
make clean   # Stop + remove volumes (resets all data)
```

---

## Branching strategy

```
main                    ← production-ready, protected
├── feat/<name>         ← new features
├── fix/<name>          ← bug fixes
├── chore/<name>        ← tooling, deps, config
└── docs/<name>         ← documentation only
```

- Branch from `main`
- Keep branches short-lived (< 1 week)
- One logical change per PR
- All merges via Pull Request — no direct pushes to `main`

---

## Commit conventions

This project uses [Conventional Commits](https://www.conventionalcommits.org/).

```
<type>(<scope>): <description>

Types: feat, fix, docs, style, refactor, test, chore, perf, ci, build
Scope: api, web, infra, db (optional but helpful)
```

**Examples:**
```
feat(api): add GET /api/v1/agents list endpoint
fix(api): use mutable ArrayList for spans to avoid Hibernate merge failure
perf(api): add @EntityGraph to eliminate N+1 on paginated trace list
test(api): add Testcontainers integration test for TraceRepository
docs: add ADR-0002 for SSE over WebSocket decision
chore(infra): upgrade postgres image to 16.3-alpine
```

**Rules:**
- Description in imperative mood: "add" not "added", "fix" not "fixes"
- No period at end of description
- 72 character limit on description line
- Body explains *why*, not *what* (the diff shows the what)

---

## Code standards

### Backend (Java)

- Every public method has a Javadoc comment
- No `@SuppressWarnings` without explanation
- `@Transactional(readOnly = true)` on all read-only service methods
- Use `Optional<T>` for nullable return values — never return `null`
- Use `ArrayList` (mutable) for JPA collection fields — Hibernate calls `.clear()` during merge
- Log at `INFO` for business events, `DEBUG` for internals, `WARN` for recoverable errors
- Never log sensitive data (API keys, PII)

### Frontend (TypeScript)

- `strict: true` — zero `any` types, ever
- Zustand stores own all async state — components are pure render
- `css custom properties` via `var(--token)` — no hardcoded hex in components
- Framer Motion animations max 200ms, `ease-out` — no bounce
- All icons via `lucide-react` at `size={14}` or `size={16}`, `strokeWidth={1.5}`

---

## Testing

### Backend

```bash
cd api
mvn test                    # Unit + slice tests (H2)
mvn verify                  # Full suite including Testcontainers
```

Test pyramid:
- **Repository tests** (`@DataJpaTest`) — validate JPA queries against real Postgres via Testcontainers
- **Controller tests** (`@WebMvcTest`) — validate HTTP contract via MockMvc
- **Integration tests** (`@SpringBootTest`) — full context, reserved for cross-layer scenarios

### Frontend

```bash
cd web
npm run build    # TypeScript type check + Vite build
```

---

## Pull request checklist

Before marking a PR ready for review:

- [ ] `mvn test` passes locally
- [ ] `npm run build` passes with zero TS errors
- [ ] New public methods have Javadoc
- [ ] Schema changes include a Flyway migration
- [ ] No `.toList()` on collections that Hibernate will manage (use `new ArrayList<>(...)`)
- [ ] Commit messages follow Conventional Commits
- [ ] PR description explains *why* the change is needed

---

## Architecture decisions

Significant technical decisions are documented as Architecture Decision Records
in [`docs/adr/`](docs/adr/). If your PR makes a non-trivial architectural choice,
add an ADR. Use the existing files as templates.
