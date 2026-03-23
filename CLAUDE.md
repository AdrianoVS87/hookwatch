# HookWatch — Project Configuration for Claude

## Project Overview

HookWatch is a webhook management and monitoring platform built with a Java/Spring Boot backend and a React/TypeScript frontend.

## Tech Stack

### Backend
- **Java 21** (LTS)
- **Spring Boot 3.4**
- **PostgreSQL 16** — primary relational database
- **Redis 7** — caching and pub/sub
- **Maven** — build tool

### Frontend
- **React 18**
- **TypeScript 5**
- **Node.js / npm** — package management

## Git Identity

- **Name:** Adriano Viera dos Santos
- **Email:** adrianovs.info@gmail.com

## Commit Convention

This project follows **Conventional Commits**:

```
<type>(optional scope): <description>

Types: feat, fix, docs, style, refactor, test, chore, perf, ci, build
```

Examples:
- `feat(webhook): add retry mechanism with exponential backoff`
- `fix(auth): correct JWT expiration handling`
- `docs: update API documentation`
- `chore(deps): bump spring-boot to 3.4.1`

## Branch Strategy

- `main` — production-ready code
- `feat/<name>` — feature branches
- `fix/<name>` — bug fix branches

## Code Style

- Backend: Google Java Style Guide
- Frontend: ESLint + Prettier defaults
- No commented-out code in commits
- Tests required for business logic

## Project Structure (expected)

```
hookwatch/
├── backend/          # Spring Boot application
│   ├── src/
│   └── pom.xml
├── frontend/         # React + TypeScript app
│   ├── src/
│   └── package.json
├── docker/           # Docker configs
├── docs/             # Project documentation
├── CLAUDE.md         # This file
├── README.md
└── LICENSE
```

## Model Routing (evidence-based, 2026)

Aliases: `/model opus`, `/model sonnet`, `/model codex`, `/model haiku`

### `anthropic/claude-opus-4-6` — use for reasoning-heavy tasks
- Bug with ambiguous root cause: race conditions, Hibernate lifecycle, JVM internals, concurrency
- Architecture decisions with real trade-offs (indexing strategy, caching, consistency)
- Cross-file refactor touching 5+ interdependent classes
- Security review requiring adversarial thinking
- Any bug where the stack trace alone doesn't reveal the cause

### `anthropic/claude-sonnet-4-6` — default for implementation
- Implementing a defined feature in Java/Spring/React
- Writing or fixing tests
- Routine bug with clear and obvious stack trace
- Frontend component and store work

### `openai-codex/gpt-5.3-codex` — for terminal/infra tasks
- Docker, docker-compose, CI/CD, GitHub Actions
- Makefile, shell scripts, nginx/infra config
- Mechanical tasks that are purely terminal-bound
- Auto-fallback when Sonnet is rate-limited

### `anthropic/claude-haiku-4-5` — for trivial tasks
- Generating commit messages
- Trivial formatting or rename refactors
- Summarizing logs or output

### Routing decision rule
> If you need to REASON about WHY something is broken → Opus.
> If you know WHAT to build and just need to build it → Sonnet.
> If it's Docker/shell/infra → Codex.

### Real example from this codebase
The `List.of()` → `ImmutableCollections.uoe()` Hibernate crash is exactly the class of bug
that warrants Opus: requires knowing `.toList()` returns an immutable list since Java 16, and
that Hibernate's `CollectionType.replaceElements()` calls `.clear()` during merge.
A reasoning pass with Opus would have caught this at review time.

Auto-fallback chain: Sonnet → Codex → Opus (rate limit cascade)

## Agent Teams (Swarms)

- Enabled via `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`
- Use for multi-layer parallel work (backend + frontend + infra simultaneously)
- Lead agent plans with Opus, workers execute with Sonnet/Codex
- Single agent for focused single-layer work

## Development Notes

- Always run tests before committing
- Use Docker Compose for local dev environment
- Environment variables via `.env` (never commit secrets)
