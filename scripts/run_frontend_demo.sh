#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

cleanup() {
  pkill -f 'scripts/mock_services.py' >/dev/null 2>&1 || true
  pkill -f 'com.example.docsys.StandaloneServer' >/dev/null 2>&1 || true
  pkill -f 'python3 -m http.server 5173' >/dev/null 2>&1 || true
}
trap cleanup EXIT

python3 scripts/mock_services.py >/tmp/mock_services.log 2>&1 &

mkdir -p backend/out
javac -encoding UTF-8 -d backend/out $(find backend/src/main/java -name '*.java')

SERVICES_NLP_BASE_URL=http://127.0.0.1:18000 \
SERVICES_ES_BASE_URL=http://127.0.0.1:19200 \
SERVICES_ES_INDEX=course_documents \
java -cp backend/out com.example.docsys.StandaloneServer >/tmp/backend.log 2>&1 &

(cd frontend && python3 -m http.server 5173 >/tmp/frontend.log 2>&1 &) 

sleep 1

echo "Frontend demo is running:"
echo "- Frontend: http://127.0.0.1:5173"
echo "- Backend : http://127.0.0.1:8080"
echo "- Mock NLP: http://127.0.0.1:18000"
echo "- Mock ES : http://127.0.0.1:19200"

echo

echo "You can press Ctrl+C to stop all services."
wait
