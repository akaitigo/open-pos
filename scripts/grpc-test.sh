#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAILURES=0
SKIPS=0
TMP_DIR=""
DEFAULT_ORG_ID="${ORG_ID:-00000000-0000-0000-0000-000000000000}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

pass() {
  printf 'OK   %s\n' "$*"
}

skip() {
  printf 'SKIP %s\n' "$*"
  SKIPS=$((SKIPS + 1))
}

fail() {
  printf 'FAIL %s\n' "$*" >&2
  FAILURES=$((FAILURES + 1))
}

check_health() {
  local name="$1"
  local host="$2"
  local port="$3"
  local required="$4"
  local output

  if output="$(grpcurl \
    -plaintext \
    -connect-timeout 2 \
    -max-time 5 \
    -import-path "$TMP_DIR" \
    -proto grpc/health/v1/health.proto \
    -rpc-header "x-organization-id: $DEFAULT_ORG_ID" \
    -d '{}' \
    "$host:$port" \
    grpc.health.v1.Health/Check 2>&1)"; then
    if grep -Fq '"SERVING"' <<<"$output"; then
      pass "$name gRPC health is SERVING on $host:$port"
      return 0
    fi
    fail "$name returned an unexpected gRPC health payload: $output"
    return 1
  fi

  if [[ "$required" == "required" ]]; then
    fail "$name gRPC health check failed on $host:$port: $output"
    return 1
  fi

  skip "$name is not reachable on $host:$port"
  return 0
}

require_command grpcurl

mkdir -p "$ROOT_DIR/.local/tmp"
TMP_DIR="$(mktemp -d "$ROOT_DIR/.local/tmp/grpc-health.XXXXXX")"
mkdir -p "$TMP_DIR/grpc/health/v1"

trap 'rm -rf "$TMP_DIR"' EXIT

cat >"$TMP_DIR/grpc/health/v1/health.proto" <<'EOF'
syntax = "proto3";

package grpc.health.v1;

message HealthCheckRequest {
  string service = 1;
}

message HealthCheckResponse {
  enum ServingStatus {
    UNKNOWN = 0;
    SERVING = 1;
    NOT_SERVING = 2;
    SERVICE_UNKNOWN = 3;
  }

  ServingStatus status = 1;
}

service Health {
  rpc Check(HealthCheckRequest) returns (HealthCheckResponse);
}
EOF

check_health "product-service" "localhost" "9001" "required"
check_health "store-service" "localhost" "9002" "required"
check_health "pos-service" "localhost" "9003" "required"
check_health "inventory-service" "localhost" "9004" "required"
check_health "analytics-service" "localhost" "9005" "optional"

if [[ "$FAILURES" -gt 0 ]]; then
  printf 'gRPC health check failed: %d failure(s), %d skip(s).\n' "$FAILURES" "$SKIPS" >&2
  exit 1
fi

printf 'gRPC health check passed with %d skip(s).\n' "$SKIPS"
