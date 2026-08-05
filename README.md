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
only by `auth-service`; `url-service` receives only the public key.

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

Prometheus scrapes the services through Docker DNS names. The management ports
are not published to the host by Docker Compose, so normal users only reach the
application ports:

```text
url-service:9001/actuator/prometheus
auth-service:9002/actuator/prometheus
analytics-service:9003/actuator/prometheus
```
