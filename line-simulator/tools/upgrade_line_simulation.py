#!/usr/bin/env python3
"""
将登记产线升级为与 FCW-LINE-07 / SW-LINE-02 / WR-LINE-01 同级的 full 仿真数据。

用法:
  python upgrade_line_simulation.py --line FCW-LINE-01
  python upgrade_line_simulation.py --category flux_core_wire
  python upgrade_line_simulation.py --all
  python upgrade_line_simulation.py --all --dry-run
"""

from __future__ import annotations

import argparse
import copy
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

_TOOLS_DIR = Path(__file__).resolve().parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))

from master_data_lib import MASTER_PATH, REFERENCE_SIM_LINES, load_master, save_master

REFERENCE_BY_CATEGORY: dict[str, str] = {
    "flux_core_wire": "FCW-LINE-07",
    "solid_wire": "SW-LINE-02",
    "submerged_arc_wire": "SW-LINE-02",
    "welding_rod": "WR-LINE-01",
}

AGV_BY_CATEGORY: dict[str, str] = {
    "flux_core_wire": "AGV-05",
    "solid_wire": "AGV-03",
    "submerged_arc_wire": "AGV-03",
    "welding_rod": "AGV-01",
}

SLOT_BY_STEP: dict[str, dict[str, str]] = {
    "flux_core_wire": {
        "cut_strip": "STRIP",
        "powder_mixing": "MIXER",
        "filling_forming": "FILLER",
        "rough_drawing": "DRAW-R",
        "fine_drawing": "DRAW-F",
        "copper_plating": "PLATING",
        "winding": "WIND",
        "packaging": "PACK",
        "stock_in_out": "AGV",
    },
    "solid_wire": {
        "rough_drawing": "DRAW-R",
        "fine_drawing": "DRAW-F",
        "copper_plating": "PLATING",
        "winding": "WIND",
        "packaging": "PACK",
        "stock_in_out": "AGV",
    },
    "submerged_arc_wire": {
        "rough_drawing": "DRAW-R",
        "fine_drawing": "DRAW-F",
        "copper_plating": "PLATING",
        "winding": "WIND",
        "packaging": "PACK",
        "stock_in_out": "AGV",
    },
    "welding_rod": {
        "wire_drawing": "DRAW",
        "cutting": "CUT",
        "powder_mixing": "MIXER",
        "coating": "COAT",
        "drying": "DRY",
        "packaging": "PACK",
        "stock_in_out": "AGV",
    },
}

PRIMARY_RECIPE_BY_CATEGORY: dict[str, str] = {
    "flux_core_wire": "FCW-HY830-V3",
    "solid_wire": "SW-ER50-V2",
    "submerged_arc_wire": "SAW-H10Mn2-V1",
    "welding_rod": "WR-E7014-V1",
}

PRODUCT_ID_BY_CATEGORY: dict[str, str] = {
    "flux_core_wire": "PROD-FCW-HY830-16",
    "solid_wire": "PROD-SW-ER50-12",
    "submerged_arc_wire": "PROD-SAW-H10Mn2-40",
    "welding_rod": "PROD-WR-E7014-32",
}


def line_code(line_id: str) -> str:
    """FCW-LINE-01 → FCW01, SAW-LINE-02 → SAW02"""
    m = re.match(r"^([A-Z]+)-LINE-(\d+)$", line_id)
    if not m:
        return line_id.replace("-", "")
    return f"{m.group(1)}{int(m.group(2)):02d}"


def deep_replace(obj: Any, mapping: dict[str, str]) -> Any:
    if isinstance(obj, str):
        for old, new in mapping.items():
            obj = obj.replace(old, new)
        return obj
    if isinstance(obj, list):
        return [deep_replace(x, mapping) for x in obj]
    if isinstance(obj, dict):
        return {deep_replace(k, mapping): deep_replace(v, mapping) for k, v in obj.items()}
    return obj


def find_line(data: dict[str, Any], line_id: str) -> dict[str, Any]:
    for line in data.get("product_lines", []):
        if line.get("product_line_id") == line_id:
            return line
    raise KeyError(f"产线不存在: {line_id}")


def purge_line_assets(data: dict[str, Any], line_id: str) -> None:
    eq_ids = {
        e["equipment_id"]
        for e in data.get("equipment", [])
        if e.get("product_line_id") == line_id
    }
    data["equipment"] = [e for e in data.get("equipment", []) if e.get("product_line_id") != line_id]
    data["data_points"] = [
        d for d in data.get("data_points", [])
        if d.get("equipment_id") not in eq_ids
    ]
    profiles = data.setdefault("equipment_telemetry_profiles", {})
    for eid in eq_ids:
        profiles.pop(eid, None)
    data["recipes"] = [r for r in data.get("recipes", []) if r.get("product_line_id") != line_id]
    data["product_batches"] = [b for b in data.get("product_batches", []) if b.get("product_line_id") != line_id]
    data["production_orders"] = [
        o for o in data.get("production_orders", [])
        if line_id not in (o.get("assigned_line_ids") or [])
    ]


def upgrade_line(data: dict[str, Any], target_line_id: str, *, force: bool = False) -> list[str]:
    logs: list[str] = []
    target = find_line(data, target_line_id)
    if target.get("simulation_enabled") and not force:
        logs.append(f"SKIP {target_line_id} 已是 full 仿真线")
        return logs

    category = str(target.get("product_category", ""))
    ref_line_id = REFERENCE_BY_CATEGORY.get(category)
    if not ref_line_id:
        logs.append(f"ERROR {target_line_id} 未知品类 {category}")
        return logs

    code = line_code(target_line_id)
    ref_eq = [e for e in data.get("equipment", []) if e.get("product_line_id") == ref_line_id]
    if not ref_eq:
        logs.append(f"ERROR {target_line_id} 参考线 {ref_line_id} 无设备")
        return logs

    purge_line_assets(data, target_line_id)

    id_map: dict[str, str] = {}
    slot_map = SLOT_BY_STEP.get(category, {})
    for eq in sorted(ref_eq, key=lambda x: x.get("process_step_id", "")):
        step = str(eq.get("process_step_id", ""))
        slot = slot_map.get(step, step.upper()[:6])
        new_id = f"{code}-{slot}"
        id_map[eq["equipment_id"]] = new_id

    profiles = data.setdefault("equipment_telemetry_profiles", {})
    ref_profiles = data.get("equipment_telemetry_profiles", {})

    for eq in ref_eq:
        old_id = eq["equipment_id"]
        new_id = id_map[old_id]
        new_eq = copy.deepcopy(eq)
        new_eq["equipment_id"] = new_id
        new_eq["product_line_id"] = target_line_id
        new_eq["name"] = re.sub(r"#\d+", f"#{code}", str(eq.get("name", new_id)))
        if new_eq.get("energy_meter_id"):
            new_eq["energy_meter_id"] = f"EM-{code}-{slot_map.get(new_eq.get('process_step_id', ''), 'X')}"
        data.setdefault("equipment", []).append(new_eq)

        if old_id in ref_profiles:
            prof = copy.deepcopy(ref_profiles[old_id])
            profiles[new_id] = prof
        if eq.get("equipment_type") == "agv_station":
            agv_prof = profiles.setdefault(new_id, {"process_step_id": "stock_in_out", "fields": {}})
            agv_fields = agv_prof.setdefault("fields", {})
            agv_fields.setdefault("status", "idle")
            agv_fields.setdefault("location", f"{target_line_id}-BUFFER")

    ref_eq_ids = set(id_map.keys())
    ref_dps = [
        d for d in data.get("data_points", [])
        if d.get("equipment_id") in ref_eq_ids
        and (not d.get("product_line_id") or d.get("product_line_id") == ref_line_id)
    ]
    for dp in ref_dps:
        new_dp = copy.deepcopy(dp)
        old_eid = dp["equipment_id"]
        new_eid = id_map[old_eid]
        field_id = dp.get("field_id") or dp.get("data_point_id", "")
        new_dp["equipment_id"] = new_eid
        new_dp["product_line_id"] = target_line_id
        new_dp["field_id"] = field_id
        new_dp["data_point_id"] = f"{new_eid}_{field_id}"
        data.setdefault("data_points", []).append(new_dp)

    ref_twin = copy.deepcopy(data.get("twin_layouts", {}).get(ref_line_id, {}))
    if ref_twin:
        ref_twin["twin_3d_ready"] = True
        ref_twin["detail_level"] = "full"
        steps = ref_twin.get("steps", {})
        for step_id, step_cfg in steps.items():
            if not isinstance(step_cfg, dict):
                continue
            old_eid = step_cfg.get("equipment_id")
            if old_eid in id_map:
                step_cfg["equipment_id"] = id_map[old_eid]
            pos = step_cfg.get("position_3d")
            if isinstance(pos, dict) and "x" in pos:
                line_num = int(re.search(r"(\d+)$", target_line_id).group(1)) if re.search(r"(\d+)$", target_line_id) else 0
                pos["x"] = float(pos["x"]) + (line_num % 5) * 0.15
        data.setdefault("twin_layouts", {})[target_line_id] = ref_twin

    primary_recipe_id = PRIMARY_RECIPE_BY_CATEGORY.get(category)
    ref_recipe = next(
        (r for r in data.get("recipes", []) if r.get("recipe_id") == primary_recipe_id),
        None,
    )
    if ref_recipe is None:
        ref_recipe = next(
            (r for r in data.get("recipes", []) if r.get("product_line_id") == ref_line_id),
            None,
        )
    if ref_recipe:
        new_recipe = copy.deepcopy(ref_recipe)
        new_rid = f"{code}-{ref_recipe['recipe_id'].split('-', 1)[-1]}" if "-" in ref_recipe["recipe_id"] else f"{code}-V1"
        new_recipe["recipe_id"] = new_rid
        new_recipe["product_line_id"] = target_line_id
        new_recipe["display_name"] = f"{target.get('name', target_line_id)} · {ref_recipe.get('display_name', new_rid)}"
        data.setdefault("recipes", []).append(new_recipe)
    else:
        new_rid = f"{code}-V1"
        logs.append(f"WARN {target_line_id} 未找到参考配方，使用 {new_rid}")

    order_id = f"PO-202506-{code}-DEMO"
    data.setdefault("production_orders", []).append({
        "production_order_id": order_id,
        "factory_id": target.get("factory_id", "JQHC-PLANT-01"),
        "customer_order_id": f"SO-{code}-DEMO",
        "product_id": PRODUCT_ID_BY_CATEGORY.get(category, f"PROD-{code}"),
        "product_category": category,
        "grade": ref_recipe.get("grade", "HY-830MPa") if ref_recipe else "standard",
        "recipe_id": new_rid,
        "assigned_line_ids": [target_line_id],
        "priority": "normal",
        "planned_quantity_t": 80,
        "released_quantity_t": 40,
        "due_date": "2025-06-30T12:00:00Z",
        "status": "released",
        "order_type": "regular",
        "delivery_sla_days": 22,
        "customer_segment": "国内工程",
        "is_export": False,
        "remark": f"登记线升级仿真 · {target_line_id}",
        "plan_year": 2025,
    })

    batch_id = f"{code}-20250605-B1"
    data.setdefault("product_batches", []).append({
        "batch_id": batch_id,
        "factory_id": target.get("factory_id", "JQHC-PLANT-01"),
        "workshop_id": target.get("workshop_id"),
        "product_line_id": target_line_id,
        "production_order_id": order_id,
        "product_category": category,
        "recipe_id": new_rid,
        "grade": ref_recipe.get("grade", "standard") if ref_recipe else "standard",
        "shift": "day",
        "status": "in_progress",
        "started_at": "2025-06-05T08:00:00Z",
        "quantity_kg": 1200,
        "parent_batches": ["STRIP-20250604-A1", "FLUX-20250605-A1"] if category == "flux_core_wire" else [],
    })

    defaults = data.setdefault("simulation_defaults", {})
    defaults.setdefault("line_agv_ids", {})[target_line_id] = AGV_BY_CATEGORY.get(category, "AGV-05")
    ws = target.get("workshop_id", "WS-WIRE-01")
    line_num = int(re.search(r"(\d+)$", target_line_id).group(1)) if re.search(r"(\d+)$", target_line_id) else 1
    defaults.setdefault("line_fg_locations", {})[target_line_id] = f"WH-FG-{ws[-2:]}-{line_num:02d}"
    defaults.setdefault("line_batch_output_kg", {})[target_line_id] = 1200
    all_ids = set(defaults.get("all_line_ids", []))
    all_ids.add(target_line_id)
    defaults["all_line_ids"] = sorted(all_ids)

    for registry_key in ("product_lines", "line_registry"):
        for line in data.get(registry_key, []):
            if line.get("product_line_id") == target_line_id:
                line["simulation_enabled"] = True
                line["detail_level"] = "full"
                line["twin_3d_ready"] = True

    logs.append(f"OK {target_line_id} ← {ref_line_id} ({len(id_map)} 设备, recipe={new_rid})")
    return logs


def collect_targets(
    data: dict[str, Any],
    *,
    line: str | None,
    category: str | None,
    all_lines: bool,
    repair: bool = False,
) -> list[str]:
    if line:
        return [line]
    lines = data.get("product_lines", [])
    if repair:
        return sorted(
            l["product_line_id"] for l in lines
            if l.get("simulation_enabled") and l["product_line_id"] not in REFERENCE_SIM_LINES
        )
    if category:
        return sorted(
            l["product_line_id"] for l in lines
            if l.get("product_category") == category and not l.get("simulation_enabled")
        )
    if all_lines:
        return sorted(l["product_line_id"] for l in lines if not l.get("simulation_enabled"))
    return []


def main() -> int:
    parser = argparse.ArgumentParser(description="升级登记产线为 full 仿真")
    parser.add_argument("--line", help="单条产线 ID")
    parser.add_argument("--category", help="按品类批量升级")
    parser.add_argument("--all", action="store_true", help="升级全部 39 条登记线")
    parser.add_argument("--repair", action="store_true", help="修复已升级登记线的 data_points（需配合 --force）")
    parser.add_argument("--force", action="store_true", help="强制覆盖已有仿真数据")
    parser.add_argument("--dry-run", action="store_true", help="仅预览不写文件")
    parser.add_argument("--master", type=Path, default=MASTER_PATH)
    args = parser.parse_args()

    if not args.line and not args.category and not args.all and not args.repair:
        parser.error("请指定 --line、--category、--all 或 --repair")
    if args.repair and not args.force:
        parser.error("--repair 需配合 --force")

    data = load_master(args.master)
    targets = collect_targets(
        data,
        line=args.line,
        category=args.category,
        all_lines=args.all,
        repair=args.repair,
    )
    if not targets:
        print("没有待升级产线")
        return 0

    all_logs: list[str] = []
    for lid in targets:
        all_logs.extend(upgrade_line(data, lid, force=args.force))

    for msg in all_logs:
        print(msg)

    sim_count = sum(1 for l in data["product_lines"] if l.get("simulation_enabled"))
    eq_count = len(data.get("equipment", []))
    print(f"\n汇总: simulation_enabled={sim_count}/42, equipment={eq_count}")

    if args.dry_run:
        print("(dry-run，未写入文件)")
        return 0

    save_master(data, args.master)
    print(f"已写入 {args.master}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
