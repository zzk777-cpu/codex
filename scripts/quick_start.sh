#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

mode="${1:-frontend}"

case "$mode" in
  frontend)
    echo "[quick-start] 启动前端演示（前端 + 后端 + Mock NLP/ES）..."
    bash scripts/run_frontend_demo.sh
    ;;
  api)
    echo "[quick-start] 运行 API 端到端演示（命令行输出）..."
    bash scripts/run_local_demo.sh
    ;;
  *)
    echo "用法: bash scripts/quick_start.sh [frontend|api]"
    echo "  frontend: 启动可视化前端演示（默认）"
    echo "  api     : 运行 API 演示并自动退出"
    exit 1
    ;;
esac
