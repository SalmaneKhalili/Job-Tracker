# TODO: delivery, containers and CI

Not built. None of this has ever been run, because no Docker daemon was
available while the app was written. Treat it as a sketch.

## Dockerfile

- [ ] Two stages: `maven:3.9-eclipse-temurin-21` build, then
      `eclipse-temurin:21-jre`
- [ ] Build with the wrapper (`./mvnw`), not a host Maven
- [ ] `dependency:go-offline` layer before `COPY src/`, so a code edit does not
      re-download the world
- [ ] `chmod +x mvnw`. Do not trust the file mode to survive the context copy.
- [ ] `-DskipTests` in the image build
- [ ] Non-root system user, uid/gid 1001, `nologin`
- [ ] Exec form `ENTRYPOINT`. Shell form swallows SIGTERM and the JVM gets
      SIGKILLed.
- [ ] `-XX:MaxRAMPercentage=75` instead of a hardcoded `-Xmx`
- [ ] `curl` for the healthcheck. Unverified: `apt-get` assumes the `-jre` tag
      is Debian based. Check it, or healthcheck from the host side.

## compose.yaml

- [ ] Add an `app` service beside the existing `postgres`
- [ ] Pin `postgres:17-alpine`. `latest` will eventually break
      `ddl-auto: validate`.
- [ ] Interpolate `POSTGRES_*` from the environment, with defaults
- [ ] Named volume for postgres data
- [ ] `pg_isready` healthcheck on postgres, `depends_on: service_healthy` on
      app. Without it Flyway connects to a database that is still initialising.
- [ ] Healthcheck the app on `/actuator/health`, `start_period: 40s`
- [ ] Pass `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`,
      `JWT_EXPIRATION_MS`
- [ ] `${JWT_SECRET:?set JWT_SECRET in .env}`. Required, not defaulted. A dev
      default here boots happily and signs with a key that is in git history.

## .env.example and .dockerignore

- [ ] `JWT_SECRET` via `openssl rand -base64 64` (base64, at least 256 bits for
      HS256), `JWT_EXPIRATION_MS`, `POSTGRES_*`. `.gitignore` already ignores
      `.env`.
- [ ] `.dockerignore`: `target/`, `.git/`, `docs/`, `*.md`, `.env`,
      `compose.yaml`, IDE dirs. Keeps secrets out of layers, and stops a local
      `target/` from shadowing the built jar.

## application-prod.yml

- [ ] `show-details: never` plus `probes.enabled: true`. Actuator exposure is
      already identical in the default profile.

## .github/workflows/ci.yml

- [ ] Trigger on push and pull request to `main`, `permissions: contents: read`,
      concurrency group
- [ ] `setup-java` with temurin 21 and `cache: maven`
- [ ] `./mvnw -B test`. Docker free, and the fast signal.
- [ ] `./mvnw -B verify`. Needs a daemon. With `IT_DB_URL` unset the suite falls
      back to Testcontainers.
- [ ] `docker build`, with `needs: verify`
- [ ] Upload `target/*.jar` with `if-no-files-found: ignore`

## Restoring `spring.docker.compose` in application.yml

Dropped along with the rest. `spring-boot-docker-compose` is still in `pom.xml`,
so it is currently a no-op. Re-add it once compose has two services, or
`./mvnw spring-boot:run` starts the app container too and fights for port 8080.

```yaml
spring:
  docker:
    compose:
      services: postgres
      fail-fast: false
```

Boot 4 makes `spring.docker.compose.skip` a nested flag (`skip.in-tests`), not a
boolean.

## Postgres for CI

A service container works today, since `IT_DB_URL` is already honoured. It needs
`options: --health-cmd "pg_isready ..."` or `up --wait`. Without the gate the
integration tier migrates against a booting database, which looks like a Flyway
bug.
