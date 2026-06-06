"""SQLite 存储状态（数据经 dataplatform-api 写入，此处仅展示配置）."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from simulator.config import SQLITE_DATABASE_PATH


def sqlite_config_public() -> dict[str, Any]:
    db_path = Path(SQLITE_DATABASE_PATH)
    return {
        "engine": "sqlite",
        "path": str(db_path),
        "exists": db_path.is_file(),
    }
