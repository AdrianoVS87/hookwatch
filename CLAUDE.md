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

## Model Routing (live in OpenClaw)

Aliases: `/model opus`, `/model sonnet`, `/model codex`, `/model haiku`

- **Sonnet 4.6 (default):** Java implementation, React frontend, tests, features
- **Codex 5.3 (1st fallback + manual switch):** Docker, CI/CD, Makefile, scripts, terminal ops, focused bug fixes
- **Opus 4.6 (2nd fallback + manual switch):** architecture, planning, decomposition, code review, complex debugging
- **Haiku 4.5 (manual switch):** commit messages, formatting, trivial tasks

Switch on-the-fly: `/model codex` before Docker tasks, `/model opus` before architecture, `/model sonnet` to go back

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
