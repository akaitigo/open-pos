#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/dev-stack.sh"

backup_arg="${1:-}"
[[ -n "$backup_arg" ]] || {
  echo "Usage: make db-restore FILE=.local/backups/<backup>.sql" >&2
  exit 1
}

backup_path="$(resolve_path "$backup_arg")"
[[ -f "$backup_path" ]] || {
  echo "Backup file not found: $backup_path" >&2
  exit 1
}

require_command docker

mode="$(detect_core_backend_mode)"
ensure_supported_mode "$mode"
mapfile -t optional_services < <(running_optional_container_services)

stop_detected_core_backend "$mode"
stop_optional_container_services "${optional_services[@]}"
compose stop hydra >/dev/null 2>&1 || true
ensure_postgres
ensure_postgres_is_available

printf 'Restoring PostgreSQL backup from %s\n' "$backup_path"

compose exec -T postgres \
  psql \
  -U openpos \
  -d postgres \
  -v ON_ERROR_STOP=1 \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'openpos' AND pid <> pg_backend_pid();" >/dev/null

compose exec -T postgres \
  psql \
  -U openpos \
  -d postgres \
  -v ON_ERROR_STOP=1 \
  -q <"$backup_path" >/dev/null

ensure_infra
if [[ "$mode" != "none" ]]; then
  start_core_backend_for_mode "$mode"
fi
restart_optional_container_services "${optional_services[@]}"

printf 'Restored PostgreSQL backup from %s\n' "$backup_path"
