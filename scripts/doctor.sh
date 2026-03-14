#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAILURES=0
WARNINGS=0

pass() {
  printf 'OK   %s\n' "$*"
}

warn() {
  printf 'WARN %s\n' "$*"
  WARNINGS=$((WARNINGS + 1))
}

fail() {
  printf 'FAIL %s\n' "$*" >&2
  FAILURES=$((FAILURES + 1))
}

extract_version() {
  grep -Eo '[0-9]+([.][0-9]+)+' <<<"$1" | head -n 1 || true
}

version_ge() {
  awk -v actual="$1" -v minimum="$2" '
    BEGIN {
      actual_count = split(actual, actual_parts, ".")
      minimum_count = split(minimum, minimum_parts, ".")
      max_count = actual_count > minimum_count ? actual_count : minimum_count

      for (i = 1; i <= max_count; i++) {
        actual_value = i <= actual_count ? actual_parts[i] + 0 : 0
        minimum_value = i <= minimum_count ? minimum_parts[i] + 0 : 0

        if (actual_value > minimum_value) {
          exit 0
        }
        if (actual_value < minimum_value) {
          exit 1
        }
      }

      exit 0
    }
  '
}

require_command() {
  local name="$1"
  local hint="$2"

  if command -v "$name" >/dev/null 2>&1; then
    pass "$name is installed"
    return 0
  fi

  fail "$name is not installed. $hint"
  return 1
}

optional_command() {
  local name="$1"
  local hint="$2"

  if command -v "$name" >/dev/null 2>&1; then
    pass "$name is installed"
    return 0
  fi

  warn "$name is not installed. $hint"
  return 0
}

check_version() {
  local label="$1"
  local actual="$2"
  local minimum="$3"

  if [[ -z "$actual" ]]; then
    fail "Could not determine $label version"
    return 1
  fi

  if version_ge "$actual" "$minimum"; then
    pass "$label $actual (required >= $minimum)"
    return 0
  fi

  fail "$label $actual is too old (required >= $minimum)"
  return 1
}

check_java() {
  if ! require_command java "Install Java 21 (GraalVM CE recommended)."; then
    return
  fi

  local output version
  output="$(java -version 2>&1 | head -n 1)"
  version="$(extract_version "$output")"
  check_version "Java" "$version" "21"
}

check_node() {
  if ! require_command node "Install Node.js 22 or newer."; then
    return
  fi

  local version
  version="$(node -v | sed 's/^v//')"
  check_version "Node.js" "$version" "22"
}

check_pnpm() {
  if ! require_command pnpm "Install pnpm 10 or newer."; then
    return
  fi

  local version
  version="$(pnpm -v)"
  check_version "pnpm" "$version" "10"
}

check_buf() {
  if ! require_command buf "Install buf 1.x to lint and generate protobuf code."; then
    return
  fi

  local output version
  output="$(buf --version)"
  version="$(extract_version "$output")"
  check_version "buf" "$version" "1"
}

check_docker() {
  if ! require_command docker "Install Docker Desktop or Docker Engine with Compose."; then
    return
  fi

  local server_version
  server_version="$(docker version --format '{{.Server.Version}}' 2>/dev/null || true)"
  if [[ -n "$server_version" ]]; then
    pass "Docker daemon is reachable ($server_version)"
  else
    fail "Docker daemon is not reachable. Start Docker before running local-demo/docker-demo."
  fi

  local compose_output compose_version
  compose_output="$(docker compose version 2>&1 || true)"
  compose_version="$(extract_version "$compose_output")"
  if [[ -n "$compose_version" ]]; then
    pass "Docker Compose is available ($compose_version)"
  else
    fail "Docker Compose is not available via 'docker compose'"
  fi
}

check_utilities() {
  require_command curl "Install curl for seed/smoke scripts." || true
  require_command jq "Install jq for seed/smoke scripts." || true
  require_command bc "Install bc for seed output formatting." || true
  optional_command grpcurl "Install grpcurl to use 'make grpc-test'." || true
}

check_workspace_state() {
  if [[ -d "$ROOT_DIR/node_modules" ]]; then
    pass "pnpm dependencies are installed"
  else
    warn "node_modules is missing. Run 'pnpm install'."
  fi

  if [[ -f "$ROOT_DIR/apps/pos-terminal/public/demo-config.json" ]] && [[ -f "$ROOT_DIR/apps/admin-dashboard/public/demo-config.json" ]]; then
    pass "demo-config.json files are present"
  else
    warn "demo-config.json files are missing. Run 'make local-demo' or 'make docker-demo' after setup."
  fi
}

echo "open-pos doctor"
echo

check_java
check_node
check_pnpm
check_buf
check_docker
check_utilities
check_workspace_state

echo
if [[ "$FAILURES" -gt 0 ]]; then
  printf 'Doctor found %d failure(s) and %d warning(s).\n' "$FAILURES" "$WARNINGS" >&2
  exit 1
fi

printf 'Doctor passed with %d warning(s).\n' "$WARNINGS"
