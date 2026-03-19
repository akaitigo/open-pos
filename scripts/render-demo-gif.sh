#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INPUT_FILE="${1:-$ROOT_DIR/.local/demo-assets/pos-checkout.webm}"
OUTPUT_FILE="${2:-$ROOT_DIR/docs/assets/demo/pos-checkout.gif}"
FPS="${FPS:-12}"
WIDTH="${WIDTH:-1200}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command ffmpeg
require_command gifski

if [[ ! -f "$INPUT_FILE" ]]; then
  echo "Raw demo capture not found: $INPUT_FILE" >&2
  echo "Run 'pnpm demo:capture' first." >&2
  exit 1
fi

mkdir -p "$(dirname "$OUTPUT_FILE")"

ffmpeg \
  -y \
  -i "$INPUT_FILE" \
  -vf "fps=${FPS},scale=${WIDTH}:-1:flags=lanczos" \
  -f yuv4mpegpipe - \
  | gifski -o "$OUTPUT_FILE" --fps "$FPS" --width "$WIDTH" -

echo "Wrote $OUTPUT_FILE"
