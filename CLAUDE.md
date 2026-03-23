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

Research sources: morphllm.com benchmarks, faros.ai real-world developer reviews,
emergent.sh Claude vs Codex comparison, Anthropic engineering blog (Feb 2026).

---

### `anthropic/claude-opus-4-6` — reasoning-first tasks

Use when the problem requires **thinking before coding**, not just coding.

- Bug with non-obvious root cause: race conditions, Hibernate lifecycle quirks, JVM internals
- Architecture decisions with real trade-offs (consistency vs latency, indexing strategy)
- Security review requiring adversarial thinking (auth bypass, injection vectors)
- Cross-file refactor touching 5+ interdependent classes
- Any situation where the stack trace alone doesn't reveal the root cause

**Evidence:** Opus 4.6 leads GPQA Diamond (PhD-level reasoning). Extended thinking
traces execution paths across interdependent systems. Caught the List.of() →
ImmutableCollections.uoe() Hibernate crash that Sonnet missed (March 2026).

---

### `anthropic/claude-sonnet-4-6` — default implementation model

Use for **well-defined tasks with clear scope**.

- Feature implementation in Java/Spring/React (scope is clear)
- Writing unit + integration tests
- Routine bug with clear stack trace pointing to the cause
- Frontend component, store, and hook work
- API endpoint implementation following established patterns

**Evidence:** Best cost-performance ratio. SWE-bench 80.8%. Handles 95% of
day-to-day implementation tasks. 3-4x more token-efficient than Opus.

---

### `openai-codex/gpt-5.3-codex` — execution-first tasks (NOT just fallback)

Codex is a **first-class choice** for specific task types where it empirically
outperforms Claude:

- **Adversarial code review** — use Codex as a "tough second reviewer" after
  Claude drafts: catches edge cases, inconsistencies, forgotten null checks.
  (Source: faros.ai real-world developer reviews, Feb 2026)
- **Tool-heavy workflows** — 47% token reduction in workflows with many tool
  calls vs Claude. (Source: morphllm.com, 2026)
- **One-shot autonomous execution** — long-running terminal tasks where Codex
  runs fully unattended and reports back. Slower but reliable.
- **Docker, CI/CD, GitHub Actions, Makefile, nginx** — infrastructure-as-code
  where execution speed matters more than reasoning depth
- **Auto-fallback** when Sonnet/Opus hit rate limits

---

### `anthropic/claude-haiku-4-5` — speed-sensitive trivial tasks

- Generating conventional commit messages
- Trivial rename/formatting refactors
- Summarizing log output or CI results
- Quick lookups with no reasoning required

---

### Decision framework

```
Is the root cause unclear? → Opus (reason first)
Is the scope well-defined? → Sonnet (implement)
Is it infra/shell/Docker? → Codex (execute)
Is it trivial/fast? → Haiku (speed)

Need tough code review? → Draft with Sonnet, review with Codex
```

Auto-fallback chain: Sonnet → Codex → Opus

---

## Agent Teams / Swarms (Claude Code experimental)

Enabled via `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` in `~/.claude/settings.json`.

**When to use swarms:**
- Multi-layer parallel work: backend + frontend + infra can run simultaneously
- Independent subtasks with no shared state (different files, different services)
- Tasks where parallelism reduces wall-clock time significantly

**When NOT to use swarms:**
- Tasks with shared mutable state (concurrent git commits = conflicts)
- Sequential dependencies (frontend can't be wired until API is defined)
- Simple single-layer tasks (spawning overhead isn't worth it)

**Swarm topology for this project:**
```
Orchestrator (Opus) — plans decomposition, reviews results
├── Backend agent (Sonnet) — Java/Spring implementation
├── Frontend agent (Sonnet) — React/TypeScript implementation
└── Infra agent (Codex) — Docker/CI/nginx configuration
```

Orchestrator reviews all agent output before committing.
Never spawn swarms for work in the OpenClaw workspace itself.

## Development Notes

- Always run tests before committing
- Use Docker Compose for local dev environment
- Environment variables via `.env` (never commit secrets)
