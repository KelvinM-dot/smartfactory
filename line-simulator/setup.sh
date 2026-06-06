#!/usr/bin/env bash
# Debian/Ubuntu (PEP 668) 必须用虚拟环境，勿对系统 pip3 直接 install。
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v python3 >/dev/null 2>&1; then
  echo "请先安装 python3 与 venv： apt install -y python3 python3-venv python3-pip"
  exit 1
fi

if [ ! -d .venv ]; then
  echo "创建虚拟环境 .venv ..."
  python3 -m venv .venv
fi

echo "安装依赖 ..."
.venv/bin/pip install -U pip
.venv/bin/pip install -r requirements.txt

echo ""
echo "完成。启动模拟器："
echo "  .venv/bin/python main.py"
echo "或："
echo "  ./run.sh"
