# TODO — delivery, containers & CI

Not built. None of this has ever been run — no Docker daemon was available
while the app was written. Treat it as a sketch, not a record.

## Dockerfile

- [ ] Two stages: `maven:3.9-eclipse-temurin-21` build → `eclipse-temurin:21-jre`
- [ ] Build with the wrapper (`./mvnw`), not a host Maven
- [ ] `dependency:go-offline` layer before `COPY src/` — don't re-download the
      world on a code edit
- [ ] `chmod +x mvnw` (don't trust the file mode surviving the context copy)
- [ ] `-DskipTests` in the image build
- [ ] Non-root system user, uid/gid 1001, `nologin`
- [ ] Exec-form `ENTRYPOINT` — shell form swallows SIGTERM and the JVM gets
      SIGKILLed
- [ ] `-XX:MaxRAMPercentage=75` instead of a hardcoded `-Xmx`
- [ ] `curl` for the healthcheck — **unverified**: `apt-get` assumes the
      `-jre` tag is Debian-based. Check, or healthcheck from the host side

## compose.yaml

- [ ] Add an `app` service next to the existing `postgres`
- [ ] Pin `postgres:17-alpine` — `latest` will eventually break `ddl-auto: validate`
- [ ] Interpolate `POSTGRES_*` from the env with defaults
- [ ] Named volume for postgres-data
- [ ] `pg_isready` healthcheck on postgres, `depends_on: service_healthy` on app
      — else Flyway connects to a still-initialising database
- [ ] Healthcheck app on `/actuator/health`, `start_period: 40s`
- [ ] Pass `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`
- [ ] `${JWT_SECRET:?set JWT_SECRET in .env}` — **required, not defaulted**. A
      dev default here boots happily and signs with a git-committed key.

## .env.example + .dockerignore

- [ ] `JWT_SECRET` (`openssl rand -base64 64`, base64, ≥256 bits for HS256),
      `JWT_EXPIRATION_MS`, `POSTGRES_*`. `.gitignore` already ignores `.env`.
- [ ] `.dockerignore`: `target/`, `.git/`, `docs/`, `*.md`, `.env`,
      `compose.yaml`, IDE dirs. Keeps secrets out of layers + stops a local
      `target/` shadowing the built jar.

## application-prod.yml

- [ ] `show-details: never` + `probes.enabled: true`. Actuator exposure is
      already identical in the default profile.

## .github/workflows/ci.yml

- [ ] On push/PR to `main`, `permissions: contents: read`, concurrency group
- [ ] `setup-java` temurin 21, `cache: maven`
- [ ] `./mvnw -B test` — Docker-free, fast signal
- [ ] `./mvnw -B verify` — needs a daemon; `IT_DB_URL` unset → Testcontainers
- [ ] `docker build`, `needs: verify`
- [ ] Upload `target/*.jar` (`if-no-files-found: ignore`)

## Restoring `spring.docker.compose` in application.yml

Dropped with the rest. `spring-boot-docker-compose` is still in `pom.xml`, so
it is currently a no-op. Re-add once compose has two services, or `./mvnw
spring-boot:run` will start the app container too and fight for port 8080:

```yaml
spring:
  docker:
    compose:
      services: postgres
      fail-fast: false
```

Boot 4: `spring.docker.compose.skip` is nested (`skip.in-tests`), not a boolean.

## Postgres for CI

Service container works today (`IT_DB_URL` is already honoured). Needs
`options: --health-cmd "pg_isready …"` or `up --wait` — without the gate the IT
tier migrates against a booting database and it looks like a Flyway bug.
