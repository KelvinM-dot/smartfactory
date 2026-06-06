#!/usr/bin/env bash
# 演示环境一键启动：API → 等待就绪 → 模拟器 → 前端
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
API_PORT="${API_PORT:-3001}"
SIM_PORT="${SIM_PORT:-3002}"
WEB_PORT="${WEB_PORT:-3000}"
API_URL="http://127.0.0.1:${API_PORT}"

echo "==> 启动 API (port ${API_PORT})"
cd "$ROOT/dataplatform-api"
mvn -q spring-boot:run -Dspring-boot.run.arguments="--server.port=${API_PORT}" &
API_PID=$!

cleanup() {
  echo ""
  echo "==> 停止服务"
  kill "$API_PID" 2>/dev/null || true
  kill "$SIM_PID" 2>/dev/null || true
  kill "$WEB_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "==> 等待 API 就绪 (${API_URL})"
deadline=$((SECONDS + 90))
until curl -sf "${API_URL}/v1/meta/factory" >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "API 启动超时" >&2
    exit 1
  fi
  sleep 1
done
echo "    API 已就绪"

echo "==> 启动模拟器 (port ${SIM_PORT})"
cd "$ROOT/line-simulator"
python3 main.py &
SIM_PID=$!
sleep 3

echo "==> 启动前端 (port ${WEB_PORT})"
cd "$ROOT/dataplatform-web"
npm run dev -- --host 0.0.0.0 --port "${WEB_PORT}" &
WEB_PID=$!

echo ""
echo "演示环境已启动："
echo "  前端  http://127.0.0.1:${WEB_PORT}"
echo "  API   ${API_URL}"
echo "  模拟器 http://127.0.0.1:${SIM_PORT}"
echo "按 Ctrl+C 停止全部服务"
wait
