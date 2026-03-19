# Production Deployment Guide

This guide covers deploying open-pos to a Kubernetes cluster on GCP.

## Prerequisites

- GKE cluster (1.28+) or compatible K8s environment
- `kubectl` configured for the target cluster
- Cloud SQL (PostgreSQL 17) instance provisioned
- Memorystore (Redis 7) instance provisioned
- Container registry access (ghcr.io/akaitigo/open-pos or your own)
- ORY Hydra v2.2 deployed and configured
- RabbitMQ 4 cluster (CloudAMQP or self-managed)
- `buf` CLI for proto code generation (build time only)

## Deployment Order

Apply manifests in this order to satisfy dependency chains:

```bash
# 1. Namespace
kubectl apply -f infra/k8s/namespace.yaml

# 2. Secrets (DB credentials, Hydra secrets, RabbitMQ credentials)
kubectl apply -f infra/k8s/secrets.yaml

# 3. ConfigMaps (per-service environment overrides)
kubectl apply -f infra/k8s/configmaps.yaml

# 4. Database migrations (Flyway Jobs - must complete before services start)
kubectl apply -f infra/k8s/flyway-job.yaml
kubectl wait --for=condition=complete --timeout=300s job/flyway-migrate -n openpos

# 5. Backend services (gRPC services first, then api-gateway)
kubectl apply -f infra/k8s/product-service.yaml
kubectl apply -f infra/k8s/store-service.yaml
kubectl apply -f infra/k8s/pos-service.yaml
kubectl apply -f infra/k8s/inventory-service.yaml
kubectl apply -f infra/k8s/analytics-service.yaml
kubectl apply -f infra/k8s/api-gateway.yaml

# 6. Ingress (expose api-gateway and frontends)
kubectl apply -f infra/k8s/ingress.yaml
```

## Database Migration with Flyway

Each backend service runs Flyway migrations at startup (`quarkus.flyway.migrate-at-start=true`). For production deployments, use a dedicated Kubernetes Job to run migrations before the services start:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: flyway-migrate
  namespace: openpos
spec:
  template:
    spec:
      containers:
        - name: flyway
          image: ghcr.io/akaitigo/open-pos/flyway-migrate:latest
          env:
            - name: DB_URL
              valueFrom:
                secretKeyRef:
                  name: openpos-secrets
                  key: db-url
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: openpos-secrets
                  key: db-user
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: openpos-secrets
                  key: db-password
      restartPolicy: Never
  backoffLimit: 3
```

Migration files are located at:

```
services/{service}/src/main/resources/db/migration/V{N}__{description}.sql
```

Schemas per service:

| Service | Schema |
|---------|--------|
| pos-service | `pos_schema` |
| product-service | `product_schema` |
| inventory-service | `inventory_schema` |
| analytics-service | `analytics_schema` |
| store-service | `store_schema` |

## Secrets Management

For production, use GCP Secret Manager. Each service's `application.properties` supports `${ENV_VAR}` placeholders:

| Secret | Used By | Description |
|--------|---------|-------------|
| `DB_URL` | All services | JDBC connection string |
| `DB_USER` | All services | Database username |
| `DB_PASSWORD` | All services | Database password |
| `REDIS_URL` | All services | Redis connection URI |
| `RABBITMQ_HOST` | pos, inventory, analytics | RabbitMQ hostname |
| `RABBITMQ_PORT` | pos, inventory, analytics | RabbitMQ AMQP port |
| `RABBITMQ_USER` | pos, inventory, analytics | RabbitMQ username |
| `RABBITMQ_PASS` | pos, inventory, analytics | RabbitMQ password |
| `HYDRA_JWKS_URL` | api-gateway | Hydra JWKS endpoint |
| `HYDRA_ISSUER` | api-gateway | Hydra token issuer URL |
| `OTEL_ENDPOINT` | All services | OpenTelemetry collector |

Create a Kubernetes Secret:

```bash
kubectl create secret generic openpos-secrets \
  --namespace=openpos \
  --from-literal=db-url='jdbc:postgresql://CLOUD_SQL_IP:5432/openpos' \
  --from-literal=db-user='openpos' \
  --from-literal=db-password='STRONG_PASSWORD' \
  --from-literal=redis-url='redis://MEMORYSTORE_IP:6379' \
  --from-literal=rabbitmq-host='RABBITMQ_HOST' \
  --from-literal=rabbitmq-pass='RABBITMQ_PASSWORD'
```

## Health Check Verification

After deployment, verify all services are healthy:

```bash
# Check pod status
kubectl get pods -n openpos

# api-gateway health (HTTP)
kubectl port-forward svc/api-gateway 8080:80 -n openpos &
curl http://localhost:8080/api/health/live

# gRPC services health (via grpcurl)
kubectl port-forward svc/product-service 9001:9001 -n openpos &
grpcurl -plaintext localhost:9001 grpc.health.v1.Health/Check

# All services should report SERVING or UP
```

K8s readiness and liveness probes are configured in each service manifest:

| Service | Health Path | Port |
|---------|-----------|------|
| api-gateway | `/api/health/live` | 8080 |
| product-service | `/health` | HTTP aux port |
| store-service | `/health` | HTTP aux port |
| pos-service | `/health` | HTTP aux port |
| inventory-service | `/health` | HTTP aux port |
| analytics-service | `/health` | HTTP aux port |

## Rollback Procedures

### Application rollback

```bash
# Roll back a Deployment to the previous revision
kubectl rollout undo deployment/api-gateway -n openpos
kubectl rollout undo deployment/product-service -n openpos
# ... repeat for each service

# Verify rollback
kubectl rollout status deployment/api-gateway -n openpos
```

### Database rollback

Flyway does not support automatic rollback for SQL migrations. To revert:

1. **Identify the target version**: check `flyway_schema_history` table in each schema
2. **Apply a corrective migration**: create a new `V{N+1}__revert_{description}.sql`
3. **Never delete or modify existing migration files** already applied in production

For emergency rollback:

```bash
# Restore from a database backup (Cloud SQL)
gcloud sql backups restore BACKUP_ID --restore-instance=INSTANCE_NAME

# Or restore from manual backup
psql -h CLOUD_SQL_IP -U openpos -d openpos < backup.sql
```

### Full rollback checklist

1. Scale down all services: `kubectl scale deployment --all --replicas=0 -n openpos`
2. Restore database if schema changes were applied
3. Deploy the previous container image tags
4. Scale back up: `kubectl scale deployment --all --replicas=2 -n openpos`
5. Verify health checks pass
6. Notify the team and create a postmortem

## Monitoring

- **Prometheus**: All services expose `/q/metrics` (Micrometer)
- **OpenTelemetry**: Distributed tracing via `OTEL_ENDPOINT`
- **Structured logging**: JSON format in production (`%prod.quarkus.log.console.json=true`)
- **Grafana dashboards**: Pre-configured in `infra/grafana/`

## Frontend Deployment

Build the SPA frontends and deploy to Cloud CDN or a static hosting service:

```bash
pnpm install
VITE_API_URL=https://api.your-domain.com pnpm build

# Upload to Cloud Storage
gsutil -m rsync -r apps/pos-terminal/dist gs://your-bucket/pos-terminal
gsutil -m rsync -r apps/admin-dashboard/dist gs://your-bucket/admin-dashboard
```

Set the following build-time environment variables:

| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | Production api-gateway URL |
| `VITE_HYDRA_PUBLIC_URL` | Production Hydra public endpoint |
| `VITE_OIDC_CLIENT_ID` | OAuth2 client ID per app |
| `VITE_OIDC_REDIRECT_URI` | OAuth2 callback URL per app |
