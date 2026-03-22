# HookWatch 🪝

> Webhook delivery platform with real-time trace visualization.

![CI](https://github.com/AdrianoVS87/hookwatch/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-18-61dafb?logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue?logo=typescript)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## Architecture

```mermaid
graph LR
    Browser["🌐 Browser"]
    Web["Web\n(React 18 + Vite)"]
    API["API\n(Spring Boot 3.4)"]
    PG["PostgreSQL 16"]
    Redis["Redis 7"]

    Browser --> Web
    Web --> API
    API --> PG
    API --> Redis
```

## Quick Start

**Prerequisites:** Docker + Make

```bash
git clone git@github.com:AdrianoVS87/hookwatch.git
cd hookwatch
make up
```

- API: http://localhost:8080
- Web: http://localhost:3000
- Swagger UI: http://localhost:8080/swagger-ui.html

### Other commands

```bash
make logs      # tail all service logs
make build     # rebuild images
make down      # stop services
make clean     # stop + remove volumes
```

## Screenshots

> [screenshots coming soon]

## Development

### Backend

```bash
cd api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd web
npm install
npm run dev
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Follow [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat(scope): description`
   - `fix(scope): description`
   - `docs: description`
   - `chore(scope): description`
4. Open a Pull Request targeting `main`

## License

MIT © 2026 Adriano Viera dos Santos
