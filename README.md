# Distributed TinyURL

Distributed URL shortener built as a Java/Spring Boot microservices project.

## Services

- `url-service`: creates short URLs, resolves redirects, publishes click events, uses Redis for redirect cache and rate limiting.
- `auth-service`: owns user/auth data.
- `analytics-service`: consumes click events from RabbitMQ and stores click analytics.

## Local infrastructure

Docker Compose runs:

- 3 Postgres databases
- RabbitMQ
- Redis
- the 3 Spring Boot services

Generate local JWT keys before starting the services:

```powershell
.\scripts\generate-jwt-keys.ps1 -WriteEnvFile
```

This writes a local `.env` file used by Docker Compose. The private key is used
only by `auth-service`; `url-service` receives only the public key. The same
file also provides local Postgres, RabbitMQ, and Grafana credentials used by
Docker Compose. Local passwords are generated unless you pass explicit values.

```powershell
docker compose up -d --build
```

## Kubernetes

Docker Desktop Kubernetes manifests live in `k8s/`.

Because Docker Desktop Kubernetes uses `containerd`, local app images must be
imported into the Kubernetes node before the app Deployments can run:

```powershell
docker compose build url-service auth-service analytics-service
.\k8s\import-images.ps1
.\k8s\apply-all.ps1
.\scripts\generate-jwt-keys.ps1
```

After `apply-all.ps1`, run the Kubernetes key commands printed by
`generate-jwt-keys.ps1`, then restart `auth-service` and `url-service`.

See `k8s/README.md` for the full local workflow and smoke-test commands.

## ID generation

`url-service` generates short codes from Snowflake-style IDs encoded as Base62.
The generated IDs combine timestamp, node id, and per-millisecond sequence bits.

Docker Compose uses `SNOWFLAKE_NODE_ID=1` because it runs one `url-service`
container.

In Kubernetes, `url-service` runs as a StatefulSet with hostname-based node id
derivation enabled. Each Pod derives its node id from the stable Pod ordinal:
`url-service-0` uses node id `1`, `url-service-1` uses node id `2`, and so on.

## Observability

Each Spring Boot service exposes Prometheus metrics through Actuator on an
internal management port:

```text
url-service:9001/actuator/prometheus
auth-service:9002/actuator/prometheus
analytics-service:9003/actuator/prometheus
```

Docker Compose also runs Prometheus and Grafana:

```text
Prometheus: http://localhost:9090
Grafana:    http://localhost:3000
```

Prometheus scrapes the services through Docker DNS names. Grafana is
provisioned with the Prometheus datasource and a `TinyURL Overview` dashboard
when Docker Compose starts. The management ports are not published to the host
by Docker Compose, so normal users only reach the application ports:

```text
url-service:       http://localhost:8081
auth-service:      http://localhost:8082
analytics-service: http://localhost:8083
```

## Resilience

`url-service` uses Resilience4j around infrastructure calls that should degrade
gracefully:

- RabbitMQ click-event publishing retries briefly, then falls back so redirects
  are not blocked by analytics outages.
- Redis redirect-cache failures fall back to Postgres as the source of truth.
- Redis rate-limit failures currently fail open and record `error_allowed`, so
  URL creation remains available while the degraded behavior is visible in
  metrics.
- Redis and RabbitMQ client calls have short local timeouts so infrastructure
  failures do not hold request threads for too long.

Run the local resilience smoke test with Docker Compose already up:

```powershell
.\scripts\verify-resilience.ps1
```

The script verifies that redirects survive RabbitMQ outages and that URL
creation plus redirects survive Redis outages. After it runs, inspect
Prometheus/Grafana for Resilience4j metrics and custom `error` outcomes.

## CI

GitHub Actions runs on pushes to `master` and on pull requests. The workflow:

- tests `auth-service`, `url-service`, and `analytics-service` in parallel with
  each service Maven wrapper
- validates `docker-compose.yaml`
- builds the three service Docker images

## Pending Production Hardening

- Add durable click-event delivery with an outbox pattern or event log so
  redirects do not lose analytics events when RabbitMQ is unavailable.
- Add `eventId` to click events and make analytics consumers idempotent to
  handle retries, redelivery, and duplicate messages.
- Decide the source of truth for click counts: either remove `urls.click_count`
  from `url-service` or update it asynchronously from analytics.
- Replace direct `COUNT(*)` analytics queries with precomputed aggregates by
  short code and time window.
- Make Redis rate limiting atomic with Lua or another single-command approach
  so `INCR` and `EXPIRE` cannot become inconsistent.
- Handle trusted proxy headers for rate limiting instead of relying only on
  `request.getRemoteAddr()`.
- Add protection against Redis cache stampede for popular short codes.
- Add JWT key rotation support using `kid` headers and a published or managed
  key set.
- Add stronger refresh-token concurrency protection so two simultaneous refresh
  requests cannot both succeed.
- Populate or intentionally remove `userAgent` and `ipHash` in click analytics,
  with a documented privacy policy.
- Add dead-letter queues and retry policies for analytics event consumption.
- Add load and resilience tests for high redirect throughput, RabbitMQ
  redelivery, Redis outages, and duplicate click events.
- Revisit Snowflake node-id assignment if moving away from Kubernetes
  StatefulSets or scaling beyond 1024 `url-service` nodes.

## Known Tradeoffs

- Redirect availability is prioritized over analytics completeness.
- URL ownership is stored by `userId` without a cross-service foreign key.
- Analytics is eventually consistent.
- Redis is treated as an optimization, while Postgres remains the source of
  truth for URLs.
- Current rate limiting fails open when Redis is unavailable.
