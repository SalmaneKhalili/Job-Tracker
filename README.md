# jobtrack

REST API for tracking job applications. Java 21, Spring Boot 4.1, PostgreSQL 17.

Containers, compose and CI are not built. See [docs/TODO.md](docs/TODO.md).

## Stack

| Area | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Build | Maven (wrapper included) |
| Database | PostgreSQL 17 |
| Persistence | Spring Data JPA + Flyway |
| Security | Spring Security, JWT (stateless) |
| Validation | Jakarta Bean Validation |
| API docs | springdoc-openapi |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers |
| Ops | Actuator, correlation ids, OpenAPI |

## Features

- Register and log in, get a signed JWT.
- Track applications with a status: `APPLIED`, `INTERVIEW`, `OFFER`,
  `REJECTED`, `WITHDRAWN`. The API does not police the order, so a mis-click
  is correctable.
- Notes on an application.
- List with pagination, sorting, and filters on status, company and date range.
- Per-status counts for a dashboard.
- Every query is owner scoped, not just the controller. Another user's record
  returns 404 with the same body as a record that does not exist.

```
User 1 ──< N Application 1 ──< N Note
```

Full design notes in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Quickstart

Needs JDK 21 and a PostgreSQL 17: the `postgres` service in `compose.yaml`, or
one you already run.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

```bash
BASE=http://localhost:8080
JSON='Content-Type: application/json'

curl -s -X POST $BASE/api/auth/register -H "$JSON" \
  -d '{"email":"you@example.com","name":"Salmane","password":"secret1234"}'

TOKEN=$(curl -s -X POST $BASE/api/auth/login -H "$JSON" \
  -d '{"email":"you@example.com","password":"secret1234"}' | jq -r .token)

curl -s -X POST $BASE/api/applications \
  -H "Authorization: Bearer $TOKEN" -H "$JSON" \
  -d '{"company":"Example GmbH","role":"Backend Engineer","status":"APPLIED","jobUrl":"https://example.com/jobs/1"}'
```

Swagger UI: <http://localhost:8080/swagger-ui.html>. The Authorize button takes
the token. Full curl reference at the bottom of
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Configuration

Every value has a working local default and an environment override.

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobtrackdb` |
| `DB_USER` | `jobtrackdb` |
| `DB_PASSWORD` | `jobtrackdb` |
| `JWT_SECRET` | a dev key in `application.yml` |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) |

`JWT_SECRET` is base64 and must decode to at least 256 bits for HS256.
Generate one with `openssl rand -base64 64`.

## Tests

```bash
./mvnw test     # unit tier, no Docker needed
./mvnw verify   # adds the integration tier, needs Docker
```

Integration coverage: register, login, CRUD, pagination, filters, notes, stats,
401 on missing/garbage/expired/foreign-key tokens, 409 on duplicate email, 400
validation, cross-user isolation across applications, notes and statistics.

No Docker daemon? Point the suite at a database you already have.

```bash
IT_DB_URL=jdbc:postgresql://localhost:5432/jobtrackdb \
IT_DB_USER=jobtrackdb IT_DB_PASSWORD=jobtrackdb ./mvnw verify
```

## Status

- [x] Implementation
- [x] 95 tests green (33 unit, 62 integration) against real PostgreSQL 17.11
- [ ] Docker, compose, CI ([docs/TODO.md](docs/TODO.md))
