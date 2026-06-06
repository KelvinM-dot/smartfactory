"""共享主数据工具常量与读写辅助."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
MASTER_PATH = ROOT / "schemas" / "智造数据台" / "presets" / "jqhc-factory-master-data.json"
REFERENCE_SIM_LINES = frozenset({"FCW-LINE-07", "SW-LINE-02", "WR-LINE-01"})
# 向后兼容
DETAILED_LINES = REFERENCE_SIM_LINES


def simulation_line_ids(data: dict[str, Any] | None = None) -> frozenset[str]:
    """返回 master-data 中 simulation_enabled=true 的产线集合."""
    if data is None:
        data = load_master()
    return frozenset(
        l["product_line_id"]
        for l in data.get("product_lines", [])
        if l.get("simulation_enabled")
    )


def load_master(path: Path | None = None) -> dict[str, Any]:
    p = path or MASTER_PATH
    with p.open(encoding="utf-8") as f:
        return json.load(f)


def save_master(data: dict[str, Any], path: Path | None = None) -> Path:
    p = path or MASTER_PATH
    with p.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    return p
