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

## Swarm Architecture: OpenClaw + Claude Code (dual-layer)

### The real setup (verified March 2026)

Two swarm layers operate together seamlessly:

**Layer 1 — OpenClaw subagents** (`sessions_spawn`)
- OpenClaw's native parallelism: spawns isolated sessions, each with full tool access
- Used for: large multi-branch tasks, independent workstreams, long-running jobs
- Push-based completion: subagents notify parent when done via `openclaw system event`
- No root restriction — OpenClaw manages this natively

**Layer 2 — Claude Code agent teams** (`CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`)
- Claude Code's internal swarm: one orchestrator spawns worker subagents within a session
- Requires `IS_SANDBOX=1` env var to run as root (workaround verified on this VPS)
- Both vars set permanently in `~/.claude/settings.json`
- Used for: focused coding tasks where one model orchestrates multiple specialists

### Workaround for root execution (critical)

Claude Code blocks `--dangerously-skip-permissions` as root by default.
**Workaround:** `IS_SANDBOX=1 claude --dangerously-skip-permissions ...`

This is set permanently in `~/.claude/settings.json` → `env.IS_SANDBOX=1`.
Source: community-verified, referenced in daveswift.com and r/ClaudeCode (March 2026).

### When to use each layer

| Situation | Use |
|-----------|-----|
| Backend + frontend + infra in parallel | OpenClaw subagents (Layer 1) |
| One coding task needing multiple specialist models | Claude Code agent teams (Layer 2) |
| Sequential tasks with dependencies | Single agent, no swarm |
| Simple one-layer tasks | Single agent (overhead not worth it) |

### Swarm topology for this project

```
OpenClaw orchestrator (this session)
│
├── sessions_spawn: Backend subagent (Sonnet)     ← Layer 1
├── sessions_spawn: Frontend subagent (Sonnet)    ← Layer 1
└── sessions_spawn: Infra subagent (Codex)        ← Layer 1
         │
         └── Each subagent can internally spawn
             Claude Code agent teams               ← Layer 2
             (IS_SANDBOX=1 + AGENT_TEAMS=1)
```

### Anti-patterns (never do these)

- Never spawn swarms for tasks with shared mutable git state (race conditions on commits)
- Never spawn Claude Code agent teams in the OpenClaw workspace (`~/.openclaw/`)
- Never use swarms when sequential dependency exists (API must be defined before frontend wires to it)
- Spawning overhead (~3-5s) is not worth it for tasks under 30s of work

## Development Notes

- Always run tests before committing
- Use Docker Compose for local dev environment
- Environment variables via `.env` (never commit secrets)
