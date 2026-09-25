# jobtrack

A production-shaped job application tracker REST API.

Built to exercise the full entry-level backend loop: REST design, JWT auth,
PostgreSQL persistence, data migrations, validation, and tests against a real
database.

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

Containers, compose and CI are deliberately **not** built here — that work is
outlined in [`docs/TODO.md`](docs/TODO.md).

## What it does

- Register / login, issue a signed JWT.
- Track job applications with a status lifecycle
  (`APPLIED → INTERVIEW → OFFER | REJECTED | WITHDRAWN`).
- Attach notes to an application.
- List applications with pagination, sorting and filters (status, company,
  date range).
- Per-status statistics for the dashboard.
- Every resource is scoped to its owner: a user can only read/write their own
  records. That rule is enforced in the query, not just the controller — and a
  record belonging to somebody else answers `404`, identically to a record that
  does not exist.

## Domain model

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for full UML (class diagram,
package tree, API table, security flow, error shapes, and a complete curl
reference).

```
User 1 ──< N Application 1 ──< N Note
```

## Quickstart

Prerequisites: JDK 21, and a PostgreSQL 17 — either the `postgres` service in
`compose.yaml`, or one you already run.

```bash
# 1. start postgres (the only service in compose.yaml)
docker compose up -d postgres

# 2. run the app
./mvnw spring-boot:run

# 3. register a user
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

Swagger UI: <http://localhost:8080/swagger-ui.html> (the "Authorize" button takes
the token from step 4).

Full curl reference lives at the bottom of `docs/ARCHITECTURE.md`.

## Configuration

Everything has a working local default and is overridable by environment
variable:

| Variable | Default | Notes |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobtrackdb` | |
| `DB_USER` / `DB_PASSWORD` | `jobtrackdb` | |
| `JWT_SECRET` | a dev key in `application.yml` | `openssl rand -base64 64`; set it in the environment for anything but local dev |
| `JWT_EXPIRATION_MS` | `86400000` | 24 h |

## Tests

```bash
./mvnw test     # unit tier: Mockito + validation. No Docker needed.
./mvnw verify   # + integration tier against a real Postgres (needs Docker)
```

The integration suite covers register → login → CRUD → pagination → filters →
notes → stats, plus 401 on missing/garbage/expired/foreign-key tokens, 409 on
duplicate email, 400 validation, and cross-user isolation across applications,
notes and statistics.

No Docker daemon? Point the suite at a database you already have:

```bash
IT_DB_URL=jdbc:postgresql://localhost:5432/jobtrackdb \
IT_DB_USER=jobtrackdb IT_DB_PASSWORD=jobtrackdb ./mvnw verify
```

## Status

- [x] Implementation
- [x] Green test suite — 95 tests (33 unit, 62 integration) pass against a real
      PostgreSQL 17.11, including the cross-user isolation matrix
- [ ] Docker / compose / CI — specified in [`docs/TODO.md`](docs/TODO.md)
