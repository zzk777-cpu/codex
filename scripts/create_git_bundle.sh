#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

BRANCH="${1:-$(git rev-parse --abbrev-ref HEAD)}"
OUT="${2:-codex-${BRANCH}.bundle}"

git bundle create "$OUT" "$BRANCH"

echo "已生成 bundle: $OUT"
echo "你可以在可联网环境执行："
echo "  git clone -b $BRANCH $OUT restored-codex"
