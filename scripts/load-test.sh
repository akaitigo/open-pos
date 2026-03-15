#\!/usr/bin/env bash
# Load test script for transaction processing throughput (#104)
# Requires: hey (go install github.com/rakyll/hey@latest)
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
ORG_ID="${ORG_ID:-}"
CONCURRENCY="${CONCURRENCY:-10}"
REQUESTS="${REQUESTS:-200}"

if [ -z "$ORG_ID" ]; then
  echo "Usage: ORG_ID=<uuid> ./scripts/load-test.sh"
  exit 1
fi

if \! command -v hey &> /dev/null; then
  echo "hey is not installed. Install with: go install github.com/rakyll/hey@latest"
  exit 1
fi

echo "=== OpenPOS Load Test ==="
echo "API: $API_URL"
echo "Organization: $ORG_ID"
echo "Concurrency: $CONCURRENCY"
echo "Total requests: $REQUESTS"
echo ""

echo "--- Health Check ---"
hey -n 50 -c 5 \
  -H "X-Organization-Id: $ORG_ID" \
  "$API_URL/api/health/live"

echo ""
echo "--- Product List ---"
hey -n "$REQUESTS" -c "$CONCURRENCY" \
  -H "X-Organization-Id: $ORG_ID" \
  "$API_URL/api/products?page=1&pageSize=20"

echo ""
echo "--- Transaction List ---"
hey -n "$REQUESTS" -c "$CONCURRENCY" \
  -H "X-Organization-Id: $ORG_ID" \
  "$API_URL/api/transactions?page=1&pageSize=10"

echo ""
echo "=== Load Test Complete ==="
