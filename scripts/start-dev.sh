#!/usr/bin/env bash
# 开发环境启动顺序：API (3001) → 等待就绪 → 模拟器 (3002) → 前端 (3000)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

wait_api() {
  local i=0
  while [ "$i" -lt 60 ]; do
    if curl -sf "http://127.0.0.1:3001/v1/meta/factory" >/dev/null 2>&1; then
      echo "✓ API ready on :3001"
      return 0
    fi
    sleep 1
    i=$((i + 1))
  done
  echo "✗ API not ready on :3001 after 60s" >&2
  return 1
}

echo "==> [1/3] dataplatform-api :3001"
cd "$ROOT/dataplatform-api"
mvn -q spring-boot:run &
API_PID=$!

wait_api

echo "==> [2/3] line-simulator :3002 (RESET_DB_BEFORE_START 会 reseed)"
cd "$ROOT/line-simulator"
python3 main.py &
SIM_PID=$!

sleep 3
if curl -sf "http://127.0.0.1:3002/sim/status" >/dev/null 2>&1; then
  echo "✓ Simulator ready on :3002"
else
  echo "⚠ Simulator not responding yet; check logs"
fi

echo "==> [3/3] dataplatform-web :3000"
cd "$ROOT/dataplatform-web"
npm run dev &
WEB_PID=$!

echo ""
echo "PIDs: API=$API_PID SIM=$SIM_PID WEB=$WEB_PID"
echo "Open http://localhost:3000"
wait
