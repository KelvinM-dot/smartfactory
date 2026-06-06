"""Line simulator 集中配置（代码内维护，不使用环境变量）."""

from __future__ import annotations

from pathlib import Path

# ---------------------------------------------------------------------------
# SQLite（与 dataplatform-api application.yml 保持一致，模拟器经 API 读写）
# ---------------------------------------------------------------------------
SQLITE_DATABASE_PATH = "./data/jqhc_dataplatform.db"

# ---------------------------------------------------------------------------
# 智造数据台 API（模拟器 HTTP 推送目标）
# ---------------------------------------------------------------------------
DATAPLATFORM_URL = "http://127.0.0.1:3001"

# ---------------------------------------------------------------------------
# 主数据 JSON（None 表示使用仓库默认路径）
# ---------------------------------------------------------------------------
MASTER_DATA_PATH: str | None = None

_DEFAULT_MASTER = (
    Path(__file__).resolve().parents[2]
    / "schemas"
    / "智造数据台"
    / "presets"
    / "jqhc-factory-master-data.json"
)


def resolve_master_data_path() -> Path:
    if MASTER_DATA_PATH:
        return Path(MASTER_DATA_PATH)
    return _DEFAULT_MASTER

# ---------------------------------------------------------------------------
# 仿真运行参数
# ---------------------------------------------------------------------------
PUSH_INTERVAL_SEC = 2.0
SOURCE_INSTANCE = "sim-fcw-07-001"
SIM_AUTO_START = False
DEFAULT_SCENARIO_ID = "normal_shift"
DEFAULT_SPEED_MULTIPLIER = 10.0

# 启动仿真前是否先调用 API 清库并重灌主数据（与 jqhc-factory-master-data.json 一致）
# 日常联调保持 false，仅手动 reseed 时开启
RESET_DB_BEFORE_START = False

# ---------------------------------------------------------------------------
# 遥测稀疏推送（P1）：活跃工序全量字段，非活跃设备每 N tick 仅 status + power_kw
# ---------------------------------------------------------------------------
TELEMETRY_SPARSE_ENABLED = True
TELEMETRY_IDLE_EQUIPMENT_INTERVAL = 5

# ---------------------------------------------------------------------------
# 物流事件减量（P1）：成品入库跳过 transporting 中间态
# ---------------------------------------------------------------------------
REDUCE_LOGISTICS_EVENTS = True

# ---------------------------------------------------------------------------
# POC 轻量模式（P1）：仅运行代表产线，主数据仍保留全厂 42 条供 UI 展示
# ---------------------------------------------------------------------------
POC_LIGHTWEIGHT_MODE = False
POC_SIM_LINE_IDS = frozenset({
    "FCW-LINE-01",
    "FCW-LINE-07",
    "FCW-LINE-11",
    "SW-LINE-02",
    "SW-LINE-03",
    "SW-LINE-05",
    "SAW-LINE-01",
    "SAW-LINE-02",
    "WR-LINE-01",
    "WR-LINE-05",
    "WR-LINE-10",
    "WR-LINE-15",
})

# 模拟器 HTTP 服务
SIMULATOR_HOST = "0.0.0.0"
SIMULATOR_PORT = 3002
