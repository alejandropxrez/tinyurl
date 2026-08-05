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
```

See `k8s/README.md` for the full local workflow and smoke-test commands.

## ID generation

`url-service` generates short codes from Snowflake-style IDs encoded as Base62.
The generated IDs combine timestamp, node id, and per-millisecond sequence bits.

Docker Compose uses `SNOWFLAKE_NODE_ID=1` because it runs one `url-service`
container.

In Kubernetes, `url-service` runs as a StatefulSet with hostname-based node id
derivation enabled. Each Pod derives its node id from the stable Pod ordinal:
`url-service-0` uses node id `1`, `url-service-1` uses node id `2`, and so on.
