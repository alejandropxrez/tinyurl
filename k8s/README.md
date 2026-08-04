# Kubernetes local workflow

These manifests target Docker Desktop Kubernetes for local learning.

## Apply infrastructure and url-service

```powershell
kubectl apply -f k8s/namespace.yaml `
  -f k8s/redis.yaml `
  -f k8s/postgres-urls.yaml `
  -f k8s/rabbitmq.yaml `
  -f k8s/url-service.yaml
```

## Local app images

Docker Desktop Kubernetes uses containerd inside the Kubernetes node. It does not
automatically see images from the regular Docker Engine image list.

For `url-service`, build the image with Docker Compose or Docker first, then load
it into the Kubernetes node:

```powershell
docker save localhost:5000/distributed-tinyurl-url-service:latest -o C:\tmp\distributed-tinyurl-url-service.tar
docker cp C:\tmp\distributed-tinyurl-url-service.tar desktop-control-plane:/distributed-tinyurl-url-service.tar
docker exec desktop-control-plane bash -lc "ctr -n k8s.io images import /distributed-tinyurl-url-service.tar"
```

The `url-service` Deployment uses `imagePullPolicy: Never`, so Kubernetes expects
the image to already exist in the node's containerd image store.

## Access url-service from the host

The Kubernetes `url-service` Service is internal to the cluster. For local
testing, open a temporary port-forward:

```powershell
kubectl port-forward -n tinyurl svc/url-service 30081:8081
```

Then call:

```powershell
Invoke-RestMethod http://localhost:30081/actuator/health
```
