#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/dev-stack.sh"

require_command docker

mode="$(detect_core_backend_mode)"
ensure_supported_mode "$mode"
mapfile -t optional_services < <(running_optional_container_services)

stop_detected_core_backend "$mode"
compose down
docker volume rm -f "$POSTGRES_VOLUME_NAME" >/dev/null 2>&1 || true
ensure_infra
start_core_backend_for_mode "$mode"
restart_optional_container_services "${optional_services[@]}"

bash "$ROOT_DIR/scripts/seed.sh"
bash "$ROOT_DIR/scripts/local-demo-smoke.sh"

cat <<EOF
Reset complete.
Demo data has been reseeded and the API smoke check passed.
EOF
