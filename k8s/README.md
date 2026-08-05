# Kubernetes local workflow

These manifests target Docker Desktop Kubernetes for local learning.

## What runs in Kubernetes

The `tinyurl` namespace contains:

- `url-service`
- `auth-service`
- `analytics-service`
- `postgres-urls`
- `postgres-auth`
- `postgres-analytics`
- `rabbitmq`
- `redis`

Each service gets a stable Kubernetes `Service` DNS name. For example,
`url-service` connects to Postgres with:

```text
jdbc:postgresql://postgres-urls:5432/tinyurl_urls
```

Kubernetes DNS resolves `postgres-urls` to the matching database Service inside
the `tinyurl` namespace.

## Snowflake node ids

`url-service` uses Snowflake-style IDs for short-code generation.

The Kubernetes manifest runs `url-service` as a StatefulSet with stable Pod
names:

```text
url-service-0
url-service-1
```

The app derives the Snowflake node id from that ordinal plus
`APP_SNOWFLAKE_NODE_ID_OFFSET`. With the local offset of `1`, the first two Pods
use node ids `1` and `2`. This behavior is enabled by:

```text
APP_SNOWFLAKE_DERIVE_NODE_ID_FROM_HOSTNAME=true
```

## JWT signing keys

`auth-service` signs JWTs with an RSA private key. Services that need to trust
those tokens, such as `url-service`, verify them with the matching RSA public
key.

The private key belongs only to `auth-service`. The public key can be shared
with resource services because it can verify tokens but cannot sign new ones.

Generate a local keypair:

```powershell
.\scripts\generate-jwt-keys.ps1
```

For Kubernetes, first apply the manifests so the namespace and ConfigMaps exist,
then run the Kubernetes commands printed by the script. After updating the keys,
restart the services:

```powershell
kubectl rollout restart deployment/auth-service -n tinyurl
kubectl rollout restart statefulset/url-service -n tinyurl
```

## Local app images

Docker Desktop Kubernetes uses `containerd` inside the Kubernetes node. It does
not automatically see images from the regular Docker Engine image list.

The application workloads use `imagePullPolicy: Never`, so the images must
already exist in the Kubernetes node image store.

First generate Docker Compose JWT keys and build the service images:

```powershell
.\scripts\generate-jwt-keys.ps1 -WriteEnvFile
docker compose build url-service auth-service analytics-service
```

Then import them into Docker Desktop Kubernetes:

```powershell
.\k8s\import-images.ps1
```

The script tags each image as `localhost:5000/...`, saves it, copies it into the
`desktop-control-plane` node, and imports it with `ctr -n k8s.io images import`.

## Apply the full stack

```powershell
.\k8s\apply-all.ps1
```

The script applies manifests in dependency order and waits for every Deployment
to roll out.

To inspect the cluster manually:

```powershell
kubectl get deployments,svc,pvc -n tinyurl
kubectl get statefulsets -n tinyurl
kubectl get pods -n tinyurl -o wide
```

## Access services from the host

The application Services are internal `ClusterIP` Services. For local testing,
open temporary port-forwards:

```powershell
kubectl port-forward -n tinyurl svc/url-service 30081:8081
kubectl port-forward -n tinyurl svc/auth-service 30082:8082
kubectl port-forward -n tinyurl svc/analytics-service 30083:8083
```

Then call health endpoints:

```powershell
Invoke-RestMethod http://localhost:30081/actuator/health
Invoke-RestMethod http://localhost:30082/actuator/health
Invoke-RestMethod http://localhost:30083/actuator/health
```

## Full smoke test

With `url-service` forwarded to `30081`, `auth-service` forwarded to `30082`,
and `analytics-service` forwarded to `30083`:

```powershell
$registerBody = @{
  email = "ada@example.com"
  password = "strong-password"
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:30082/api/v1/auth/register" -ContentType "application/json" -Body $registerBody

$loginBody = @{
  email = "ada@example.com"
  password = "strong-password"
} | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:30082/api/v1/auth/login" -ContentType "application/json" -Body $loginBody

$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$body = @{ originalUrl = "https://www.google.com" } | ConvertTo-Json
$created = Invoke-RestMethod -Method Post -Uri "http://localhost:30081/api/v1/urls" -Headers $headers -ContentType "application/json" -Body $body
$code = $created.shortCode
Invoke-WebRequest -Uri "http://localhost:30081/$code" -MaximumRedirection 0
Start-Sleep -Seconds 2
Invoke-RestMethod -Uri "http://localhost:30083/api/v1/analytics/urls/$code/clicks"
```

Expected result: the redirect returns `302`, and analytics returns a click count
of `1` or higher.
