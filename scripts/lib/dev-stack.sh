#!/usr/bin/env bash

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/compose.yml"
PID_DIR="$ROOT_DIR/.local/pids"
LOG_DIR="$ROOT_DIR/.local/logs"
POSTGRES_VOLUME_NAME="${POSTGRES_VOLUME_NAME:-openpos_openpos-pgdata}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

service_pid_is_running() {
  local name="$1"
  local pid_file="$PID_DIR/$name.pid"

  [[ -f "$pid_file" ]] || return 1

  local pid
  pid="$(cat "$pid_file")"
  kill -0 "$pid" >/dev/null 2>&1
}

service_container_is_running() {
  local name="$1"

  compose ps --status running --services 2>/dev/null | grep -Fxq "$name"
}

running_optional_container_services() {
  local services=()

  if service_container_is_running inventory-service; then
    services+=("inventory-service")
  fi

  if service_container_is_running analytics-service; then
    services+=("analytics-service")
  fi

  if [[ "${#services[@]}" -gt 0 ]]; then
    printf '%s\n' "${services[@]}"
  fi
}

detect_core_backend_mode() {
  local host_running=0
  local docker_running=0

  if service_pid_is_running product-service || service_pid_is_running store-service || \
    service_pid_is_running pos-service || service_pid_is_running api-gateway; then
    host_running=1
  fi

  if service_container_is_running product-service || service_container_is_running store-service || \
    service_container_is_running pos-service || service_container_is_running api-gateway; then
    docker_running=1
  fi

  if [[ "$host_running" -eq 1 && "$docker_running" -eq 1 ]]; then
    echo "mixed"
    return 0
  fi

  if [[ "$host_running" -eq 1 ]]; then
    echo "host"
    return 0
  fi

  if [[ "$docker_running" -eq 1 ]]; then
    echo "docker"
    return 0
  fi

  echo "none"
}

ensure_supported_mode() {
  local mode="$1"

  if [[ "$mode" == "mixed" ]]; then
    cat >&2 <<EOF
Detected both host-run and containerized core backend services.
Stop one mode first with 'make local-down' or 'make docker-down-core', then retry.
EOF
    exit 1
  fi
}

stop_detected_core_backend() {
  local mode="$1"

  case "$mode" in
    host)
      bash "$ROOT_DIR/scripts/local-stack-down.sh"
      ;;
    docker)
      compose stop api-gateway product-service store-service pos-service >/dev/null
      ;;
    none)
      ;;
    *)
      echo "Unsupported backend mode: $mode" >&2
      exit 1
      ;;
  esac
}

start_core_backend_for_mode() {
  local mode="$1"

  case "$mode" in
    host)
      bash "$ROOT_DIR/scripts/local-stack-up.sh" --skip-build
      ;;
    docker)
      compose up -d --wait product-service store-service pos-service api-gateway
      ;;
    none)
      bash "$ROOT_DIR/scripts/local-stack-up.sh"
      ;;
    *)
      echo "Unsupported backend mode: $mode" >&2
      exit 1
      ;;
  esac
}

restart_optional_container_services() {
  local service

  for service in "$@"; do
    [[ -n "$service" ]] || continue
    compose up -d --wait "$service"
  done
}

stop_optional_container_services() {
  local service

  for service in "$@"; do
    [[ -n "$service" ]] || continue
    compose stop "$service" >/dev/null
  done
}

ensure_infra() {
  compose up -d --wait postgres redis rabbitmq hydra
}

ensure_postgres() {
  compose up -d --wait postgres
}

ensure_postgres_is_available() {
  require_command docker

  if ! service_container_is_running postgres; then
    cat >&2 <<EOF
PostgreSQL is not running.
Start the stack first with 'make up', 'make local-demo', or 'make docker-demo'.
EOF
    exit 1
  fi
}

resolve_path() {
  local path="$1"

  if [[ "$path" = /* ]]; then
    printf '%s\n' "$path"
    return 0
  fi

  printf '%s\n' "$ROOT_DIR/$path"
}
