# TODO: Testcontainers Integration Tests

**Target day:** Tuesday
**Branch:** improve/testcontainers
**Effort:** ~3h

## What to implement

### 1. Add Testcontainers dependency
- `api/pom.xml`: add `spring-boot-testcontainers` + `testcontainers:postgresql` + `testcontainers:junit-jupiter`
- Remove H2 from test scope

### 2. Base integration test class
- `api/src/test/java/com/hookwatch/BaseIntegrationTest.java`
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@Testcontainers` with `@Container PostgreSQLContainer`
- `@DynamicPropertySource` to inject real DB URL

### 3. TraceService integration tests
- `TraceServiceIntegrationTest.java`
- Test: create trace with 5 spans → retrieve → verify all spans persisted
- Test: tenant isolation — key A cannot read tenant B data
- Test: BCrypt validation — wrong key returns 401

### 4. AgentController integration tests
- `AgentControllerIntegrationTest.java`
- Full HTTP round-trip via TestRestTemplate
- Test: GET /agents returns only authenticated tenant's agents

### 5. TraceController pagination test
- Page size enforcement (max 100)
- Sort by startedAt DESC works correctly

## How to run
```bash
cd api && mvn test -Dspring.profiles.active=test
```

## Done criteria
- All tests green
- No H2 in test classpath
- CI passes with real PostgreSQL
