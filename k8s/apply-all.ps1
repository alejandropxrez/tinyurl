$ErrorActionPreference = "Stop"

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/rabbitmq.yaml
kubectl apply -f k8s/postgres-urls.yaml
kubectl apply -f k8s/postgres-auth.yaml
kubectl apply -f k8s/postgres-analytics.yaml
kubectl delete deployment url-service -n tinyurl --ignore-not-found=true
kubectl apply -f k8s/url-service.yaml
kubectl apply -f k8s/auth-service.yaml
kubectl apply -f k8s/analytics-service.yaml

kubectl rollout status deployment/redis -n tinyurl --timeout=120s
kubectl rollout status deployment/rabbitmq -n tinyurl --timeout=120s
kubectl rollout status deployment/postgres-urls -n tinyurl --timeout=120s
kubectl rollout status deployment/postgres-auth -n tinyurl --timeout=120s
kubectl rollout status deployment/postgres-analytics -n tinyurl --timeout=120s
kubectl rollout status statefulset/url-service -n tinyurl --timeout=240s
kubectl rollout status deployment/auth-service -n tinyurl --timeout=180s
kubectl rollout status deployment/analytics-service -n tinyurl --timeout=180s

kubectl get deployments,statefulsets,svc,pvc -n tinyurl
