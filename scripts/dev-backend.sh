#!/usr/bin/env bash
# dev-backend: Start all backend services in quarkusDev mode with proper process management.
# Tracks PIDs, propagates SIGTERM/SIGINT for graceful shutdown.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.local/pids/dev"
LOG_DIR="$ROOT_DIR/.local/logs/dev"

SERVICES=(
  product-service
  store-service
  pos-service
  inventory-service
  analytics-service
  api-gateway
)

CHILD_PIDS=()
SHUTTING_DOWN=0

mkdir -p "$PID_DIR" "$LOG_DIR"

cleanup() {
  if [[ "$SHUTTING_DOWN" -eq 1 ]]; then
    return
  fi
  SHUTTING_DOWN=1

  echo ""
  echo "Shutting down dev-backend services..."

  for pid in "${CHILD_PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill -TERM "$pid" 2>/dev/null || true
    fi
  done

  # Wait up to 15 seconds for graceful shutdown
  local deadline=$((SECONDS + 15))
  local all_stopped=0
  while [[ "$SECONDS" -lt "$deadline" ]]; do
    all_stopped=1
    for pid in "${CHILD_PIDS[@]}"; do
      if kill -0 "$pid" 2>/dev/null; then
        all_stopped=0
        break
      fi
    done
    if [[ "$all_stopped" -eq 1 ]]; then
      break
    fi
    sleep 1
  done

  # Force-kill any remaining processes
  for pid in "${CHILD_PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      echo "Force-killing PID $pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  done

  # Clean up PID files
  for svc in "${SERVICES[@]}"; do
    rm -f "$PID_DIR/$svc.pid"
  done

  echo "All dev-backend services stopped."
}

trap cleanup EXIT INT TERM

start_dev_service() {
  local name="$1"
  local log_file="$LOG_DIR/$name.log"
  local pid_file="$PID_DIR/$name.pid"

  # Stop if already running from a previous dev-backend invocation
  if [[ -f "$pid_file" ]]; then
    local old_pid
    old_pid="$(cat "$pid_file")"
    if kill -0 "$old_pid" 2>/dev/null; then
      echo "Stopping previous $name (PID $old_pid)"
      kill -TERM "$old_pid" 2>/dev/null || true
      for _ in $(seq 1 5); do
        kill -0 "$old_pid" 2>/dev/null || break
        sleep 1
      done
      kill -9 "$old_pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi

  (
    cd "$ROOT_DIR/services/$name"
    exec "$ROOT_DIR/gradlew" quarkusDev >"$log_file" 2>&1
  ) &

  local pid=$!
  echo "$pid" > "$pid_file"
  CHILD_PIDS+=("$pid")
  echo "Started $name (PID $pid) -> $log_file"
}

echo "=== open-pos dev-backend ==="
echo "Starting ${#SERVICES[@]} services in quarkusDev mode..."
echo "Logs: $LOG_DIR"
echo ""

for svc in "${SERVICES[@]}"; do
  start_dev_service "$svc"
done

echo ""
echo "All services started. Press Ctrl+C to stop all."
echo "Use 'tail -f $LOG_DIR/<service>.log' to view individual logs."
echo ""

# Wait for any child to exit; if one crashes, report it
while true; do
  for i in "${!CHILD_PIDS[@]}"; do
    pid="${CHILD_PIDS[$i]}"
    if ! kill -0 "$pid" 2>/dev/null; then
      svc="${SERVICES[$i]}"
      wait "$pid" 2>/dev/null || true
      exit_code=$?
      if [[ "$SHUTTING_DOWN" -eq 0 ]]; then
        echo "WARNING: $svc (PID $pid) exited with code $exit_code"
      fi
    fi
  done

  # Check if all children have exited
  all_done=1
  for pid in "${CHILD_PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      all_done=0
      break
    fi
  done

  if [[ "$all_done" -eq 1 ]]; then
    break
  fi

  sleep 2
done
