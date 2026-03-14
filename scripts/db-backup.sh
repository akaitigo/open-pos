#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/dev-stack.sh"

backup_arg="${1:-}"
timestamp="$(date +%Y%m%d-%H%M%S)"
default_backup_path="$ROOT_DIR/.local/backups/openpos-$timestamp.sql"
backup_path="${backup_arg:-$default_backup_path}"
backup_path="$(resolve_path "$backup_path")"

require_command docker
ensure_postgres_is_available

mkdir -p "$(dirname "$backup_path")"

compose exec -T postgres \
  pg_dump \
  -U openpos \
  -d openpos \
  --clean \
  --create \
  --if-exists \
  --no-owner \
  --no-privileges >"$backup_path"

printf 'Wrote PostgreSQL backup to %s\n' "$backup_path"
