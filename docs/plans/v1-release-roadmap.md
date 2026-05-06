# open-pos v1.0 Release Readiness

> **Last Updated**: 2026-05-07
> **Status**: Living document
> **Purpose**: stale issue backlog ではなく、現時点の v1 release readiness を判定する

## Executive Summary

open-pos は、v1 に必要な実装の多くをすでに持っています。特に、frontend integration、OIDC/PKCE auth、analytics、tenant-aware data access、CI/security baseline は「未実装」ではありません。

一方で、まだ v1 の一般公開を断言しづらい理由は、機能不足よりも運用整備と最終 verification にあります。したがって、この文書では「何が未着手か」ではなく、「何が release gate としてまだ弱いか」を管理します。

## Current Release Posture

- **Product posture**: self-hostable beta / release-candidate baseline
- **Not yet claimed**: staffed production operations, managed SaaS support, 24/7 on-call
- **What is already true**: local demo, CI/security gates, multi-tenant auth, analytics, offline-capable frontend baseline

## What Is Already Implemented

### Authentication And Tenant Boundary

- ORY Hydra based OIDC/PKCE flow is implemented
- frontend stores and validates OAuth2 `state` to mitigate CSRF
- RBAC roles are wired through the current auth surface
- analytics / gateway / service layer already carry organization-aware access patterns

### Reliability Baseline

- gRPC client deadlines are configured in `api-gateway` and downstream clients
- CI includes frontend/backend validation, security scanning, CodeQL, and Playwright E2E
- dependency hygiene is actively maintained and the current audit gate is green for high/critical issues

### Product Surface

- POS terminal, admin dashboard, analytics, inventory, and product/store management are present
- `make local-demo` and `make docker-demo` provide reproducible demo paths

## Remaining Release Gates

| Gate | Status | What is missing |
| --- | --- | --- |
| Documentation accuracy | In progress | README / roadmap / runbook must consistently describe the current posture |
| Secret handling | Needs work | secret rotation and history scan procedures must be explicit |
| Incident ownership | Needs work | maintainer/deployer escalation path must be defined without placeholders |
| Production deployment guidance | Needs work | environment docs and release docs must cover the supported deployment model |
| End-to-end resilience verification | Needs work | offline sync conflict handling and degraded-mode expectations should be documented and re-verified |
| Business/legal verification | Needs work | invoice/privacy/compliance claims need final release-oriented confirmation |

## Release Decision Framework

### Eligible To Call "v1.0"

The repository can be tagged as `v1.0` when all of the following are true:

- the public docs match the actual product and operating model
- CI/security gates are green on `main`
- release-critical secrets and history scans are complete
- incident response ownership is documented
- local/demo/release verification flows are reproducible by another engineer

### Not A Blocker By Itself

The following are not, by themselves, reasons to block `v1.0`:

- the existence of future-looking enhancement issues
- the absence of managed SaaS operations
- large but known-maintainable files, as long as correctness and release verification are intact

## Changes From The March 2026 Review

The earlier review document treated several items as open that are now implemented in code:

- PKCE `state` validation
- analytics tenant-scoped access patterns
- gRPC deadline configuration
- CI-linked dependency audit and dependency grouping baseline
- frontend/admin analytics and management surfaces

Those items should no longer be tracked as "missing features". They are now part of the verified baseline and should only reappear here if regression evidence is found.

## Operational Recommendation

Treat the current repository as:

- **good enough for self-hosted evaluation and controlled rollout**
- **not yet something to market as a fully managed production SaaS**

That distinction keeps the public messaging honest while still recognizing the implementation maturity already present in the codebase.
