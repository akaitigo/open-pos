#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.local/pids"
LOG_DIR="$ROOT_DIR/.local/logs"
SKIP_BUILD=0
STARTED_SERVICES=()
RABBITMQ_CONTAINER="${RABBITMQ_CONTAINER:-openpos-rabbitmq}"
RABBITMQ_HOST="${RABBITMQ_HOST:-localhost}"
RABBITMQ_PORT="${RABBITMQ_PORT:-15672}"
RABBITMQ_USER="${RABBITMQ_USER:-openpos}"
RABBITMQ_PASS="${RABBITMQ_PASS:-openpos_dev}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

for arg in "$@"; do
  case "$arg" in
    --skip-build)
      SKIP_BUILD=1
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

require_command curl

mkdir -p "$PID_DIR" "$LOG_DIR"

cleanup_on_failure() {
  local exit_code="$1"

  if [[ "$exit_code" -eq 0 ]]; then
    return
  fi

  echo "Startup failed. Stopping services started in this run..." >&2
  for name in "${STARTED_SERVICES[@]}"; do
    stop_if_running "$name"
  done
}

trap 'exit_code=$?; trap - EXIT; cleanup_on_failure "$exit_code"; exit "$exit_code"' EXIT

wait_for_http() {
  local name="$1"
  local url="$2"
  local pid="${3:-}"
  local attempt

  for attempt in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "$name is ready: $url"
      return 0
    fi
    if [[ -n "$pid" ]] && ! kill -0 "$pid" >/dev/null 2>&1; then
      echo "$name exited before becoming ready. See $LOG_DIR/$name.log" >&2
      return 1
    fi
    sleep 1
  done

  echo "Timed out waiting for $name ($url)" >&2
  return 1
}

ensure_rabbitmq_user() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "docker is required to configure RabbitMQ for local startup" >&2
    exit 1
  fi

  if ! docker inspect "$RABBITMQ_CONTAINER" >/dev/null 2>&1; then
    echo "RabbitMQ container '$RABBITMQ_CONTAINER' is not available. Run 'make up' first." >&2
    exit 1
  fi

  docker exec "$RABBITMQ_CONTAINER" rabbitmqctl add_user "$RABBITMQ_USER" "$RABBITMQ_PASS" >/dev/null 2>&1 || true
  docker exec "$RABBITMQ_CONTAINER" rabbitmqctl change_password "$RABBITMQ_USER" "$RABBITMQ_PASS" >/dev/null 2>&1
  docker exec "$RABBITMQ_CONTAINER" rabbitmqctl set_user_tags "$RABBITMQ_USER" administrator >/dev/null 2>&1
  docker exec "$RABBITMQ_CONTAINER" rabbitmqctl set_permissions -p / "$RABBITMQ_USER" ".*" ".*" ".*" >/dev/null 2>&1
}

stop_if_running() {
  local name="$1"
  local pid_file="$PID_DIR/$name.pid"

  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
      for _ in $(seq 1 10); do
        if ! kill -0 "$pid" >/dev/null 2>&1; then
          break
        fi
        sleep 1
      done
      if kill -0 "$pid" >/dev/null 2>&1; then
        kill -9 "$pid" >/dev/null 2>&1 || true
      fi
    fi
    rm -f "$pid_file"
  fi
}

start_service() {
  local name="$1"
  local health_url="$2"
  shift 2

  local jar="$ROOT_DIR/services/$name/build/$name-0.1.0-SNAPSHOT-runner.jar"
  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  if [[ ! -f "$jar" ]]; then
    echo "Missing runner jar: $jar" >&2
    exit 1
  fi

  stop_if_running "$name"

  (
    cd "$ROOT_DIR"
    nohup env "$@" java -jar "$jar" >"$log_file" 2>&1 < /dev/null &
    echo $! >"$pid_file"
  )

  local pid
  pid="$(cat "$pid_file")"
  wait_for_http "$name" "$health_url" "$pid"
  STARTED_SERVICES+=("$name")
}

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  "$ROOT_DIR/gradlew" \
    :services:product-service:quarkusBuild \
    :services:store-service:quarkusBuild \
    :services:pos-service:quarkusBuild \
    :services:inventory-service:quarkusBuild \
    :services:api-gateway:quarkusBuild \
    -Dquarkus.package.jar.type=uber-jar
fi

ensure_rabbitmq_user

start_service \
  product-service \
  "http://localhost:8081/q/health/live" \
  QUARKUS_HTTP_PORT=8081 \
  QUARKUS_GRPC_SERVER_PORT=9001 \
  QUARKUS_OTEL_ENABLED=false \
  RABBITMQ_HOST="$RABBITMQ_HOST" \
  RABBITMQ_PORT="$RABBITMQ_PORT" \
  RABBITMQ_USER="$RABBITMQ_USER" \
  RABBITMQ_PASS="$RABBITMQ_PASS"
start_service \
  store-service \
  "http://localhost:8082/q/health/live" \
  QUARKUS_HTTP_PORT=8082 \
  QUARKUS_GRPC_SERVER_PORT=9002 \
  QUARKUS_OTEL_ENABLED=false \
  RABBITMQ_HOST="$RABBITMQ_HOST" \
  RABBITMQ_PORT="$RABBITMQ_PORT" \
  RABBITMQ_USER="$RABBITMQ_USER" \
  RABBITMQ_PASS="$RABBITMQ_PASS"
start_service \
  pos-service \
  "http://localhost:8083/q/health/live" \
  QUARKUS_HTTP_PORT=8083 \
  QUARKUS_GRPC_SERVER_PORT=9003 \
  QUARKUS_OTEL_ENABLED=false \
  RABBITMQ_HOST="$RABBITMQ_HOST" \
  RABBITMQ_PORT="$RABBITMQ_PORT" \
  RABBITMQ_USER="$RABBITMQ_USER" \
  RABBITMQ_PASS="$RABBITMQ_PASS"
start_service \
  inventory-service \
  "http://localhost:8084/q/health/live" \
  QUARKUS_HTTP_PORT=8084 \
  QUARKUS_GRPC_SERVER_PORT=9004 \
  QUARKUS_OTEL_ENABLED=false \
  RABBITMQ_HOST="$RABBITMQ_HOST" \
  RABBITMQ_PORT="$RABBITMQ_PORT" \
  RABBITMQ_USER="$RABBITMQ_USER" \
  RABBITMQ_PASS="$RABBITMQ_PASS"
start_service \
  api-gateway \
  "http://localhost:8080/api/health" \
  QUARKUS_HTTP_PORT=8080 \
  QUARKUS_OTEL_ENABLED=false \
  OPENPOS_AUTH_ENABLED="${OPENPOS_AUTH_ENABLED:-false}" \
  PRODUCT_SERVICE_HOST=localhost \
  PRODUCT_SERVICE_PORT=9001 \
  STORE_SERVICE_HOST=localhost \
  STORE_SERVICE_PORT=9002 \
  POS_SERVICE_HOST=localhost \
  POS_SERVICE_PORT=9003 \
  INVENTORY_SERVICE_HOST=localhost \
  INVENTORY_SERVICE_PORT=9004

cat <<EOF
Local backend is running.
Logs: $LOG_DIR
PIDs: $PID_DIR
EOF
