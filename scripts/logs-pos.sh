#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/dev-stack.sh"

mode="$(detect_core_backend_mode)"
ensure_supported_mode "$mode"

case "$mode" in
  host)
    log_file="$LOG_DIR/pos-service.log"
    if [[ ! -f "$log_file" ]]; then
      echo "Missing host-run log file: $log_file" >&2
      exit 1
    fi
    exec tail -n 200 -f "$log_file"
    ;;
  docker)
    exec compose logs -f pos-service
    ;;
  none)
    echo "pos-service is not running in host or container mode." >&2
    exit 1
    ;;
esac
