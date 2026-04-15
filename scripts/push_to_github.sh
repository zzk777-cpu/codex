#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "用法: bash scripts/push_to_github.sh <github_repo_url> [branch]"
  echo "示例: bash scripts/push_to_github.sh https://github.com/<you>/<repo>.git main"
  exit 1
fi

REPO_URL="$1"
BRANCH="${2:-$(git rev-parse --abbrev-ref HEAD)}"

if ! git config user.name >/dev/null; then
  echo "请先设置 git 用户名: git config user.name \"你的名字\""
  exit 1
fi

if ! git config user.email >/dev/null; then
  echo "请先设置 git 邮箱: git config user.email \"you@example.com\""
  exit 1
fi

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REPO_URL"
else
  git remote add origin "$REPO_URL"
fi

echo "准备推送到: $REPO_URL"
echo "分支: $BRANCH"

git push -u origin "$BRANCH"

echo "推送完成。"
