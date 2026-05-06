# Incident Response SOP

> **Scope**: self-hosted / pre-production deployments of open-pos
> **Support model**: maintainer-led; this repository does not currently provide a staffed 24/7 on-call rotation

## Ownership

| Role | Current owner | Contact path |
| --- | --- | --- |
| Incident commander | Repository maintainer | GitHub `@akaitigo` or the team operating the deployment |
| Technical backup | Deployment owner | Your internal pager/chat/on-call system |
| Security escalation | Deployment owner + repository maintainer | GitHub Security Advisory or private security contact configured by the deployer |

## Severity Levels

| Level | Definition | Response target | Examples |
| --- | --- | --- | --- |
| `SEV1` | Full service outage or cross-tenant/security incident | Start within 15 minutes | API gateway outage, data leak suspicion, auth failure across tenants |
| `SEV2` | Core workflow unavailable for part of the product | Start within 30 minutes | Checkout blocked, login broken, message broker unavailable |
| `SEV3` | Partial degradation with workaround | Start within 4 hours | Analytics delay, cache inconsistency, non-critical admin page failure |
| `SEV4` | Minor defect or cosmetic issue | Next business day | Small UI bug, missing log field, flaky local script |

## Detection Sources

- GitHub Actions failures on `main`
- `make local-smoke` / `make grpc-test` failures
- Docker Compose health or container restart loops
- application logs from `make logs` / `make logs-pos`
- operator or user reports from the deployment owner

## Initial Response

1. Record the incident start time and the observed symptom.
2. Decide whether the impact is tenant-scoped or system-wide.
3. Classify severity (`SEV1`-`SEV4`).
4. Open a dedicated incident channel/thread in the deployer's chat system.
5. Avoid posting live access tokens, PKCE artifacts, or customer data into public issues or logs.

## Local / Docker Compose Triage

### Health And Logs

```bash
docker compose -f infra/compose.yml ps
make logs
make logs-pos
make local-smoke
make grpc-test
```

### Fast Recovery Actions

```bash
docker compose -f infra/compose.yml restart postgres redis rabbitmq hydra
make local-down
make local-up-fast
```

### If The Demo Stack Is Suspect

```bash
make docker-down-core
make docker-demo
```

## Kubernetes Deployments

If you run open-pos on Kubernetes, use your platform's rollout and secret-management conventions. The repository does not define a single production Kubernetes control plane, so the deployer is responsible for the exact commands.

Typical checks:

```bash
kubectl get pods -n openpos
kubectl logs -n openpos deployment/<service> --tail=100
kubectl rollout restart deployment/<service> -n openpos
kubectl rollout undo deployment/<service> -n openpos
```

## Service-Specific Hints

| Symptom | First checks | Common actions |
| --- | --- | --- |
| API failures / 5xx | `api-gateway` logs, Hydra reachability, gRPC downstream health | restart gateway, verify OIDC config, inspect gRPC deadlines/timeouts |
| Checkout blocked | `pos-service` logs, Redis/RabbitMQ reachability, product/store gRPC clients | verify downstream services, restart broker/cache, run smoke flow |
| Analytics stale | `analytics-service` logs, RabbitMQ queue depth | inspect event consumer lag, replay/restart consumer if needed |
| Cache anomalies | Redis logs, cache key patterns, fallback behavior | confirm read-through/fallback path, restart Redis if safe |
| E2E-only failure | `make docker-demo`, Playwright artifacts, frontend env files | rebuild demo stack, inspect generated `demo-config.json` |

## Recovery Validation

1. Confirm container or pod health.
2. Re-run `make local-smoke` or the environment-equivalent smoke test.
3. Re-run `make grpc-test` if backend reachability is involved.
4. Confirm the user-visible flow that triggered the incident.
5. Capture whether the system degraded gracefully or failed hard.

## Aftercare

- Create a postmortem within 3 business days for `SEV1`/`SEV2`.
- Link the incident to the fixing PRs and follow-up issues.
- If secrets or tokens may have leaked, rotate them before closing the incident.
- Update this runbook when the incident exposed a missing operational step.
