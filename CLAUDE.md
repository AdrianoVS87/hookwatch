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

## Model Routing (dual-provider)

- **Default:** `anthropic/claude-sonnet-4-6` — Java implementation, React frontend, tests, features
- **[OPUS]:** `anthropic/claude-opus-4-6` — architecture, planning, decomposition, code review
- **[CODEX]:** `openai-codex/gpt-5.3-codex` — Docker, CI/CD, Makefile, shell scripts, terminal ops, focused bug fixes
- **[HAIKU]:** `anthropic/claude-haiku-4-5` — commit messages, formatting, trivial tasks
- **Auto-fallback:** when Claude hits rate limit, route to Codex automatically

## Development Notes

- Always run tests before committing
- Use Docker Compose for local dev environment
- Environment variables via `.env` (never commit secrets)
