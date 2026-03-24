# k6 Load Tests

## Prerequisites

```bash
# k6 のインストール
brew install grafana/k6/k6  # macOS
# or: https://grafana.com/docs/k6/latest/get-started/installation/
```

## Environment Variables

```bash
export K6_BASE_URL=http://localhost:8080
export K6_ORG_ID=<organization-uuid>
export K6_STORE_ID=<store-uuid>
export K6_TERMINAL_ID=<terminal-uuid>
export K6_STAFF_ID=<staff-uuid>
```

## Test Scripts

| Script | Purpose | Load Profile |
|--------|---------|-------------|
| `product-listing.js` | Product/category API throughput | Standard (10 VU) |
| `transaction-create.js` | Transaction creation pipeline | Standard (10 VU) |
| `transaction-finalize.js` | Full payment finalization | Smoke (2 VU) |
| `full-checkout-flow.js` | E2E checkout flow, SLO validation | High (500 VU) |
| `stress-test.js` | Mixed workload, find breaking point | Stress (1000 VU) |

## Running Tests

```bash
# Smoke test (quick validation)
k6 run tests/k6/product-listing.js

# Full checkout flow (500+ RPS target, SLO: P95 < 200ms)
k6 run tests/k6/full-checkout-flow.js

# Stress test (find breaking point)
k6 run tests/k6/stress-test.js

# With Grafana Cloud output
k6 run --out cloud tests/k6/full-checkout-flow.js
```

## SLO Targets

| Metric | Target |
|--------|--------|
| P95 Latency | < 200ms |
| P99 Latency | < 500ms |
| Error Rate | < 1% |
| Checkout Success Rate | > 95% |
