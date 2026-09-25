# TODO — delivery, containers & CI

Deliberately **not** implemented in this repository. The application code, the
API and the test suite are finished and green; everything below is the
infrastructure layer, left undone on purpose.

The short version: `./mvnw spring-boot:run` plus a Postgres you start yourself
is the whole story today. There is no `Dockerfile`, no `.dockerignore`, no
`.env.example`, no `.github/`, no `application-prod.yml`, and `compose.yaml` is
the bare `postgres` service it started life as.

This file is a specification, not a description of working code. Nothing here
has ever been executed — the Docker daemon was never available on this machine.
Treat every snippet as a starting point to rewrite, and check it before you
trust it.

---

## 1. `Dockerfile`

Two stages, so the runtime image carries neither Maven nor the build cache.

- **Build** — `maven:3.9-eclipse-temurin-21`. Run the project wrapper
  (`./mvnw`) rather than a host Maven, so the image is built with the version
  `pom.xml` and `.mvn/wrapper/maven-wrapper.properties` already pin.
  - `COPY .mvn/ .mvn/`, `COPY mvnw pom.xml ./`, then a `dependency:go-offline`
    layer **before** `COPY src/ src/`. Code edits must not re-download the world.
  - Build with `-DskipTests`. The test tier needs a database, and the image
    build should not.
  - `RUN chmod +x mvnw` — do not rely on the file mode surviving a context copy
    on every filesystem.
- **Runtime** — `eclipse-temurin:21-jre`.
  - A non-root system user (uid/gid 1001, `nologin`). The app writes nothing
    outside `/tmp`.
  - `COPY --from=build --chown=... target/jobtrack-*.jar /app/app.jar`.
  - `EXPOSE 8080`; `ENTRYPOINT` execs `java` so the JVM is PID 1 and receives
    `SIGTERM` (shell-form entrypoints swallow it and the container is `SIGKILL`ed
    after the grace period).
  - A JVM flag like `-XX:MaxRAMPercentage=75` is the cheap way to size the heap
    to a container memory limit without hardcoding `-Xmx`.
  - Only needed for a healthcheck: `curl`. **Unverified assumption** — this
    stage was never built, and `apt-get install` implies the tag is
    Debian/Ubuntu-based. `eclipse-temurin:21-jre` is Ubuntu-based today; the
    `-jre` tag is not a stable platform promise. Check it, or drop `curl` and
    use a compose-level healthcheck from the host side instead.

Both base tags (`maven:3.9-eclipse-temurin-21`, `eclipse-temurin:21-jre`) were
confirmed to exist via the Docker Hub registry API (HTTP 200), which is the only
part of this section that was actually checked.

## 2. `compose.yaml`

Keep the existing `postgres` service and add an `app` service beside it.

- Pin the Postgres tag (`postgres:17-alpine`). The committed `compose.yaml`
  says `postgres:latest`, and `latest` will eventually be a major version the
  Flyway migrations and `ddl-auto: validate` do not agree with.
- Interpolate the credentials from the environment with defaults
  (`${POSTGRES_DB:-jobtrackdb}`) instead of hardcoding them, and add a named
  volume so data survives `docker compose down`.
- **Healthcheck the database** (`pg_isready -U … -d …`) and gate the app on it:
  `depends_on: { postgres: { condition: service_healthy } }`. Without this, the
  app starts, Flyway connects to a Postgres that is still initialising, and the
  boot fails for a reason that looks nothing like the cause.
- **Healthcheck the app** too — `curl -fsS http://localhost:8080/actuator/health`
  — so `docker compose up --wait` and any future orchestrator know when it is
  actually serving. A generous `start_period` (Flyway migrations plus JVM start
  is tens of seconds, not milliseconds).
- Pass the app's config as env: `DB_URL`, `DB_USER`, `DB_PASSWORD`,
  `JWT_SECRET`, `JWT_EXPIRATION_MS`. `application.yml` already reads all five
  from the environment with local defaults, so nothing new is needed there.
- `SPRING_PROFILES_ACTIVE=prod` if the prod profile is reinstated.

## 3. `src/main/resources/application-prod.yml`

The removed file held only Actuator exposure for a container:

```yaml
management:
  endpoints:
    web: { exposure: { include: health,info,metrics } }
  endpoint:
    health: { show-details: never, probes: { enabled: true } }
logging:
  level:
    root: INFO
    org.salmanekhalili.jobtrack: INFO
```

The only real difference from the default profile is `show-details: never`
(health output without auth) and `probes.enabled` (adds the `/actuator/health/
liveness` and `/readiness` groups Kubernetes and most PaaS health checks
actually call). The actuator exposure itself is already identical in the default
profile — see the "Ops" row in `README.md` and `management.*` in
`application.yml`.

## 4. `.env.example` and `.dockerignore`

- `.env.example` — a template with `JWT_SECRET` (generate with
  `openssl rand -base64 64`; it is read as base64 and must decode to at least
  256 bits for HS256), `JWT_EXPIRATION_MS`, and the three `POSTGRES_*` values.
  Copy to `.env`; `.gitignore` already ignores `.env`.
  - Be aware that `JWT_SECRET` must be marked **required** in the `app` service
    (`${JWT_SECRET:?set JWT_SECRET in .env}`). A silent `${JWT_SECRET:-devdefault}`
    in a compose file is a production footgun: it boots, and it signs with a key
    that is in git history.
- `.dockerignore` — at minimum `target/`, `.git/`, `.gitignore`, `docs/`,
  `*.md`, `.env`, `compose.yaml`, and the IDE directories. Two reasons beyond
  context size: it keeps secrets out of image layers, and it stops a local
  `target/` from shadowing the jar the build stage is supposed to produce.

## 5. GitHub Actions

`.github/workflows/ci.yml`, triggered on push and pull request against `main`,
with `permissions: contents: read` and a `concurrency` group that cancels
superseded runs.

- `actions/setup-java` with `distribution: temurin`, `java-version: '21'`,
  `cache: maven`.
- Step 1 — `./mvnw -B test`. This tier is deliberately Docker-free (see
  `ARCHITECTURE.md` §7): it is the fast signal, and it should not be gated on a
  Docker daemon being healthy.
- Step 2 — `./mvnw -B verify`. This **does** need a daemon: `AbstractApiIT`
  falls back to Testcontainers when `IT_DB_URL` is unset, and GitHub-hosted
  runners have one. Setting `IT_DB_URL` instead is not worth it here — a fresh
  container per run is more isolated than a service container, and the schema is
  migrated by the suite either way.
- Step 3 — `docker build` the image, `needs: verify` so a red test never burns
  image-build minutes.
- Optional but cheap: upload `target/*.jar` as an artifact with
  `if-no-files-found: ignore` so a failed run is still diagnosable.

## 6. Follow-on: `spring.docker.compose` in `application.yml`

The `spring.docker.compose` block was removed from `application.yml` along with
the rest of the container work. Understand what it did before re-adding it:

- `pom.xml` has always depended on `spring-boot-docker-compose`. With the block
  absent, that dependency is a no-op: nothing is auto-started, and
  `./mvnw spring-boot:run` simply connects to whatever is on `localhost:5432`.
- As soon as `compose.yaml` gains a second service, that dependency starts
  **both** — so `./mvnw spring-boot:run` would build and launch the app in a
  container and fight the JVM you just started for port 8080. The fix is to
  restrict it to the database:

  ```yaml
  spring:
    docker:
      compose:
        services: postgres   # never: nothing, once compose has an app service
        fail-fast: false     # do not fail the JVM boot if docker is not running
  ```

- `fail-fast: false` matters more than it looks: it is the difference between
  "you forgot to start the database" and the app refusing to boot because Docker
  happened to be down when you did not need it.
- In Boot 4 `spring.docker.compose.skip` is a **nested** flag
  (`skip.in-tests: true`), not the boolean it was in Boot 2. `application-test.yml`
  sets it so a test context never races the development database for port 5432.

## 7. If you want the test tier to stop needing Docker

Not urgent, but it is the one place where DevOps and the test design intersect.
`AbstractApiIT` already accepts `IT_DB_URL` / `IT_DB_USER` / `IT_DB_PASSWORD` and
uses Testcontainers only as a fallback, so a service container in CI works today:

```yaml
services:
  postgres:
    image: postgres:17-alpine
    env:
      POSTGRES_DB: jobtrackdb
      POSTGRES_USER: jobtrackdb
      POSTGRES_PASSWORD: jobtrackdb
    options: >-
      --health-cmd "pg_isready -U jobtrackdb -d jobtrackdb"
      --health-interval 5s --health-retries 10
    ports: ['5432:5432']
```

The thing to get right is `options: --health-cmd` (or `docker compose up --wait`):
without a health gate, the IT tier starts migrating against a database that is
still booting, and the failure looks like a Flyway bug.
