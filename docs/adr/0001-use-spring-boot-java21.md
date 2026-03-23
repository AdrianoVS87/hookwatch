# ADR-0001: Spring Boot 3.4 + Java 21 as the API runtime

**Status:** Accepted  
**Date:** 2026-03-23  
**Deciders:** Adriano Viera dos Santos

---

## Context

HookWatch needs a backend capable of handling high-throughput webhook delivery,
streaming telemetry to connected clients in real time, and persisting structured
trace/span data with sub-second query latency. The API must be maintainable,
testable, and easy to onboard new engineers.

The team evaluated three runtimes:

| Option | Strengths | Weaknesses |
|--------|-----------|------------|
| Spring Boot 3.4 + Java 21 | Mature ecosystem, virtual threads, strong JPA/JDBC story, first-class testcontainers support | JVM startup time (mitigated by CDS/AOT) |
| Quarkus + Java 21 | Faster cold start, native image | Smaller ecosystem, steeper learning curve |
| Node.js (Fastify) | Fast I/O, shared language with frontend | Weaker typing story for domain model, less mature JPA equivalent |

---

## Decision

Use **Spring Boot 3.4** with **Java 21** and Maven.

Key factors:

1. **Virtual threads (Project Loom)** — Java 21 GA virtual threads let us use
   `spring.threads.virtual.enabled=true` and get near-reactive throughput with
   blocking JDBC code. No reactive programming complexity.

2. **Spring Data JPA maturity** — `@EntityGraph`, `@Query` with JPQL, and
   Testcontainers integration are battle-tested at Google, Netflix, and Airbnb
   scale. The N+1 problem is solvable with known tools (`@EntityGraph`).

3. **SseEmitter** — Spring MVC's `SseEmitter` gives us push-based real-time
   updates to the React canvas without the overhead of a full WebSocket server.

4. **Flyway** — schema-as-code with versioned SQL migrations. Every schema
   change is reviewable, rollback-safe, and CI-validated.

5. **springdoc-openapi** — zero-config Swagger UI from annotations. Live API
   docs at `/swagger-ui.html` with no manual YAML maintenance.

---

## Consequences

- **Positive:** Proven at scale. Rich ecosystem (Actuator, Micrometer, Testcontainers).
  JPA handles 95% of query needs; native SQL available via `@Query(nativeQuery=true)`.
- **Positive:** Virtual threads eliminate the need for WebFlux reactive chains for
  this workload level.
- **Negative:** JVM cold start is ~3-4s in Docker. Mitigated by health checks in
  `docker-compose.yml` with `depends_on: condition: service_healthy`.
- **Negative:** H2 in dev profile doesn't support `JSONB` column type — mitigated
  by removing `columnDefinition = "jsonb"` from `Trace.metadata` and relying on
  Hibernate's JSON mapping for both dialects.

---

## References

- [Spring Boot 3.4 release notes](https://spring.io/blog/2024/11/21/spring-boot-3-4-0-available-now)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Testcontainers Spring Boot](https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/)
