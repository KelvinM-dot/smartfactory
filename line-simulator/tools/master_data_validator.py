#!/usr/bin/env python3
"""校验主数据：产线 ↔ 设备 ↔ 数据点 ↔ 孪生布局 一致性."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

_TOOLS_DIR = Path(__file__).resolve().parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))
from master_data_lib import MASTER_PATH, load_master


def validate(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    lines = {l["product_line_id"]: l for l in data.get("product_lines", [])}
    registry = {l["product_line_id"]: l for l in data.get("line_registry", lines.values())}

    if len(lines) != 42:
        errors.append(f"product_lines 应为 42 条，当前 {len(lines)}")
    if len(registry) != 42:
        errors.append(f"line_registry 应为 42 条，当前 {len(registry)}")

    sim_lines = sorted(lid for lid, l in lines.items() if l.get("simulation_enabled"))
    if len(sim_lines) < 3:
        errors.append(f"simulation_enabled 产线过少: {len(sim_lines)}")

    equipment = data.get("equipment", [])
    eq_by_line: dict[str, list[str]] = {}
    for eq in equipment:
        lid = eq.get("product_line_id")
        eq_by_line.setdefault(lid, []).append(eq.get("equipment_id"))

    for lid in sim_lines:
        if not eq_by_line.get(lid):
            errors.append(f"精细产线 {lid} 无设备")

    dp_eq = {dp.get("equipment_id") for dp in data.get("data_points", [])}
    for eq in equipment:
        eid = eq.get("equipment_id")
        if eq.get("equipment_type") == "agv_station":
            continue
        if eq.get("product_line_id") in sim_lines and eid not in dp_eq:
            errors.append(f"设备 {eid} 缺少 data_points")

    layouts = data.get("twin_layouts", {})
    for lid in lines:
        if lid not in layouts:
            errors.append(f"产线 {lid} 缺少 twin_layouts")
    for lid in sim_lines:
        layout = layouts.get(lid, {})
        if not layout.get("twin_3d_ready"):
            errors.append(f"精细产线 {lid} twin_layouts.twin_3d_ready 应为 true")

    plan = data.get("annual_production_plan", {})
    targets = plan.get("annual_targets_t", {})
    if targets.get("total", 0) < 100_000:
        errors.append("annual_production_plan.total 应 ≥ 100000")

    profile = data.get("factory_profile", {})
    if profile.get("workforce_planned") != 405:
        errors.append("factory_profile.workforce_planned 应为 405")

    orders = data.get("production_orders", [])
    if not orders:
        errors.append("production_orders 为空")
    for order in orders:
        if not order.get("delivery_sla_days"):
            errors.append(f"订单 {order.get('production_order_id')} 缺少 delivery_sla_days")

    return errors


def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else MASTER_PATH
    data = load_master(path)
    errors = validate(data)
    if errors:
        print("VALIDATION FAILED:")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"OK: {path} validated ({len(data.get('product_lines', []))} lines)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
