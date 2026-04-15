#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

cleanup() {
  pkill -f 'scripts/mock_services.py' >/dev/null 2>&1 || true
  pkill -f 'com.example.docsys.StandaloneServer' >/dev/null 2>&1 || true
}
trap cleanup EXIT

python3 scripts/mock_services.py >/tmp/mock_services.log 2>&1 &

mkdir -p backend/out
javac -encoding UTF-8 -d backend/out $(find backend/src/main/java -name '*.java')

SERVICES_NLP_BASE_URL=http://127.0.0.1:18000 \
SERVICES_ES_BASE_URL=http://127.0.0.1:19200 \
SERVICES_ES_INDEX=course_documents \
java -cp backend/out com.example.docsys.StandaloneServer >/tmp/backend.log 2>&1 &

sleep 1

echo "== health =="
curl -s http://127.0.0.1:8080/health

echo -e "\n== analyze =="
curl -s -X POST http://127.0.0.1:8080/api/documents/analyze \
  -H 'Content-Type: application/json' \
  -d '{"title":"机器学习课程教案","content":"本文档介绍文本分类与检索"}'

echo -e "\n== upload-and-index =="
curl -s -X POST http://127.0.0.1:8080/api/documents/upload-and-index \
  -H 'Content-Type: application/json' \
  -d '{"title":"课程论文模板","content":"这是一篇论文写作规范","docType":"论文","courseName":"人工智能"}'

echo -e "\n== search =="
curl -sG 'http://127.0.0.1:8080/api/documents/search' --data-urlencode 'q=论文' --data-urlencode 'size=10'

echo -e "\n\nDemo completed."
