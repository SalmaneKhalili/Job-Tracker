# jobtrack

A production-shaped job application tracker REST API.

Built to exercise the full entry-level backend loop: REST design, JWT auth,
PostgreSQL persistence, data migrations, validation, tests against a real
database, and a Dockerized delivery story.

## Stack

| Area | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Build | Maven |
| Database | PostgreSQL |
| Persistence | Spring Data JPA + Flyway |
| Security | Spring Security, JWT (stateless) |
| Validation | Jakarta Bean Validation |
| API docs | springdoc-openapi |
| Tests | JUnit 5, Mockito, Testcontainers |
| Ops | Actuator, JSON logging, multi-stage Dockerfile, docker-compose |

## What it does

- Register / login, issue a signed JWT.
- Track job applications with a status lifecycle
  (`APPLIED → INTERVIEW → OFFER | REJECTED | WITHDRAWN`).
- Attach notes to an application.
- List applications with pagination, sorting and filters (status, company,
  date range).
- Per-status statistics for the dashboard.
- Every resource is scoped to its owner: a user can only read/write their own
  records. That rule is enforced in the query, not just the controller.

## Domain model

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for full UML (class diagram,
package tree, API table, security flow).

```
User 1 ──< N Application 1 ──< N Note
```

## Quickstart

Prerequisites: JDK 21, Docker.

```bash
# 1. start postgres
docker compose up -d postgres

# 2. run the app
./mvnw spring-boot:run

# 3. registered user
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","name":"Salmane","password":"secret1234"}'

# 4. login to get a token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"secret1234"}' \
  | jq -r .token)

# 5. create an application
curl -s -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"company":"Example GmbH","role":"Backend Engineer","status":"APPLIED","jobUrl":"https://example.com/jobs/1"}'
```

Full curl reference lives at the bottom of `docs/ARCHITECTURE.md`.

## Tests

```bash
./mvnw verify
```

Runs the unit suite (Mockito) plus integration tests against a real Postgres
container via Testcontainers, covering: register → login → CRUD → pagination →
stats → auth failures → cross-user isolation.

## Delivery

- `docker-compose.yml` boots app + postgres together; the app container
  healthcheck pings `/actuator/health`.
- `Dockerfile` is multi-stage, runs as a non-root user on a slim JRE image.
- OpenAPI docs at `http://localhost:8080/swagger-ui.html`.

## Deployment

- GitHub Actions CI: every push to a PR branch runs `./mvnw verify` against a
  Testcontainers Postgres.

## Status

- [ ] Implementation
- [ ] Green test suite
- [ ] CI running on GitHub