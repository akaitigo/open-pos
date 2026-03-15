#!/bin/bash
# Register OAuth2 clients in ORY Hydra for OpenPOS applications.
# This script is idempotent — it deletes existing clients before re-creating them.

set -euo pipefail

HYDRA_ADMIN="${HYDRA_ADMIN_URL:-http://hydra:4445}"

echo "Waiting for Hydra admin API to become ready..."
for i in $(seq 1 30); do
  if wget --quiet --tries=1 --spider "${HYDRA_ADMIN}/health/alive" 2>/dev/null; then
    echo "Hydra admin API is ready."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "ERROR: Hydra admin API did not become ready in time."
    exit 1
  fi
  sleep 2
done

# Helper: delete client if it exists, then create it
create_client() {
  local client_id="$1"
  shift

  # Delete if exists (ignore errors)
  hydra delete oauth2-client --endpoint "$HYDRA_ADMIN" "$client_id" 2>/dev/null || true

  hydra create oauth2-client \
    --endpoint "$HYDRA_ADMIN" \
    --id "$client_id" \
    "$@"

  echo "Created OAuth2 client: $client_id"
}

# POS Terminal client (public, PKCE)
create_client "pos-terminal" \
  --name "POS Terminal" \
  --grant-type authorization_code,refresh_token \
  --response-type code \
  --scope openid,offline_access \
  --redirect-uri "http://localhost:5173/callback" \
  --token-endpoint-auth-method none

# Admin Dashboard client (public, PKCE)
create_client "admin-dashboard" \
  --name "Admin Dashboard" \
  --grant-type authorization_code,refresh_token \
  --response-type code \
  --scope openid,offline_access,admin \
  --redirect-uri "http://localhost:5174/callback" \
  --token-endpoint-auth-method none

echo "All OAuth2 clients registered successfully."
