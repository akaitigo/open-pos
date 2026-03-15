#!/usr/bin/env bash
# Proto ドキュメント自動生成スクリプト (#221)
# buf を使用して proto ファイルから Markdown ドキュメントを生成する。
#
# 使い方:
#   ./scripts/proto-docs.sh
#
# 出力先: docs/proto/ ディレクトリ
#
# 前提条件:
#   - buf CLI がインストールされていること
#   - protoc-gen-doc がインストールされていること
#     go install github.com/pseudomuto/protoc-gen-doc/cmd/protoc-gen-doc@latest

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROTO_DIR="${ROOT_DIR}/proto"
OUTPUT_DIR="${ROOT_DIR}/docs/proto"

mkdir -p "${OUTPUT_DIR}"

echo "=== Proto ドキュメント生成 ==="

# Check if protoc-gen-doc is available
if ! command -v protoc-gen-doc &>/dev/null; then
    echo "protoc-gen-doc が見つかりません。"
    echo "インストール: go install github.com/pseudomuto/protoc-gen-doc/cmd/protoc-gen-doc@latest"
    echo ""
    echo "代わりに buf の組み込み機能でコメント付きリストを生成します..."

    # Fallback: generate documentation from proto file comments
    for proto_file in "${PROTO_DIR}"/openpos/*/v1/*.proto; do
        service_name=$(basename "$(dirname "$(dirname "${proto_file}")")")
        output_file="${OUTPUT_DIR}/${service_name}.md"

        echo "  ${proto_file} -> ${output_file}"

        {
            echo "# ${service_name} API Reference"
            echo ""
            echo "> Auto-generated from \`$(basename "${proto_file}")\`"
            echo ""
            echo "## Services"
            echo ""
            grep -E "^(service |  rpc |// )" "${proto_file}" | sed 's|^//|#####|' || true
            echo ""
            echo "## Messages"
            echo ""
            grep -E "^(message |  // |  [a-z])" "${proto_file}" | head -200 || true
        } > "${output_file}"
    done

    echo ""
    echo "=== 生成完了: ${OUTPUT_DIR} ==="
    exit 0
fi

# Use protoc-gen-doc for proper documentation
for proto_file in "${PROTO_DIR}"/openpos/*/v1/*.proto; do
    service_name=$(basename "$(dirname "$(dirname "${proto_file}")")")
    output_file="${OUTPUT_DIR}/${service_name}.md"

    echo "  ${proto_file} -> ${output_file}"

    protoc \
        --doc_out="${OUTPUT_DIR}" \
        --doc_opt=markdown,"${service_name}.md" \
        -I "${PROTO_DIR}" \
        "${proto_file}"
done

echo ""
echo "=== 生成完了: ${OUTPUT_DIR} ==="
