#!/usr/bin/env python3
"""P0-P3 数据完备性补丁：配方力学、批次、时间轴、产能、遥测扩展、孪生元数据."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

_TOOLS_DIR = Path(__file__).resolve().parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))
from master_data_lib import DETAILED_LINES, MASTER_PATH, load_master, save_master

# 仿真线需扩展的 telemetry profile 字段（域配置 required 核心子集）
PROFILE_FIELD_PATCHES: dict[str, dict[str, dict[str, Any]]] = {
    "DRAW-FINE-01": {
        "outlet_diameter_um": {"range": [1580, 1620], "target": 1600},
    },
    "PLATING-02": {
        "current_density_A_per_dm2": {"range": [2.8, 3.6], "target": 3.2},
        "bath_ph": {"range": [8.2, 8.8], "target": 8.5},
    },
    "PACK-01": {
        "finished_batch_id": "FCW-20250605-B2",
    },
    "PACK-SW-01": {
        "finished_batch_id": "SW-20250605-B1",
    },
    "PACK-WR-01": {
        "finished_batch_id": "WR-20250528-B2",
        "package_count": {"range": [110, 130], "target": 120},
    },
    "DRY-WR-01": {
        "moisture_pct": {"range": [0.08, 0.35], "target": 0.15},
        "actual_temp_C": {"range": [175, 190], "target": 185},
    },
    "CUT-WR-01": {
        "cutting_speed_cuts_per_min": {"range": [1750, 1850], "target": 1800},
        "length_mm": {"range": [340, 360], "target": 350},
    },
    "COAT-WR-01": {
        "coating_thickness_mm": {"range": [1.1, 1.3], "target": 1.2},
        "coating_pressure_MPa": {"range": [10, 14], "target": 12},
    },
    "DRAW-SW-02": {
        "outlet_diameter_um": {"range": [1180, 1220], "target": 1200},
    },
    "AGV-STATION-01": {
        "quantity_kg": {"range": [1000, 1300], "target": 1200},
    },
    "AGV-SW-01": {
        "quantity_kg": {"range": [900, 1100], "target": 1000},
    },
    "AGV-WR-01": {
        "quantity_kg": {"range": [700, 900], "target": 800},
    },
}

RECIPE_MECHANICAL_PATCHES: dict[str, dict[str, Any]] = {
    "SW-ER50-V2": {
        "rm_mpa_target": 610,
        "impact_kv2_j_target": 69,
        "elongation_pct_target": 22.0,
        "corrosion_i_value_target": 6.0,
        "line_speed_m_per_min": 58,
    },
    "WR-E7014-V1": {
        "rm_mpa_target": 610,
        "impact_kv2_j_target": 69,
        "elongation_pct_target": 22.0,
        "corrosion_i_value_target": 6.0,
    },
}


def _ensure_data_point(
    points: list[dict[str, Any]],
    *,
    equipment_id: str,
    field_id: str,
    process_step_id: str,
    display_name: str,
    unit: str,
    product_line_id: str | None = None,
    spec_limits: dict[str, Any] | None = None,
) -> None:
    key = (equipment_id, field_id)
    existing = {
        (p.get("equipment_id"), p.get("field_id") or p.get("data_point_id"))
        for p in points
    }
    if key in existing:
        return
    row: dict[str, Any] = {
        "data_point_id": f"{equipment_id}_{field_id}",
        "field_id": field_id,
        "equipment_id": equipment_id,
        "display_name": display_name,
        "process_step_id": process_step_id,
        "unit": unit,
        "data_category": "process_parameter",
    }
    if product_line_id:
        row["product_line_id"] = product_line_id
    if spec_limits:
        row["spec_limits"] = spec_limits
    points.append(row)


METADATA_PROFILE_FIELDS = frozenset({
    "status", "location", "recipe_id", "run_mode", "alarm_code", "finished_batch_id",
})

FIELD_LABELS: dict[str, tuple[str, str]] = {
    "outlet_diameter_um": ("出口线径", "μm"),
    "current_density_A_per_dm2": ("电流密度", "A/dm²"),
    "bath_ph": ("镀液pH", ""),
    "bath_temp_C": ("镀液温度", "℃"),
    "line_speed_m_per_min": ("线速", "m/min"),
    "moisture_pct": ("含水率", "%"),
    "cutting_speed_cuts_per_min": ("切丝速度", "根/min"),
    "coating_pressure_MPa": ("压涂压力", "MPa"),
    "quantity_kg": ("出入库重量", "kg"),
    "package_count": ("包装数", ""),
}


def patch_profile_datapoint_alignment(data: dict[str, Any]) -> None:
    """仿真线：profile 数值字段与 data_points 双向补齐."""
    profiles = data.get("equipment_telemetry_profiles", {})
    points = data.setdefault("data_points", [])
    eq_by_id = {e["equipment_id"]: e for e in data.get("equipment", []) if e.get("enabled", True)}
    detailed = DETAILED_LINES

    for eq_id, prof in profiles.items():
        eq = eq_by_id.get(eq_id)
        if not eq or eq.get("product_line_id") not in detailed:
            continue
        step = prof.get("process_step_id") or eq.get("process_step_id", "")
        line_id = eq.get("product_line_id")
        for fid, spec in prof.get("fields", {}).items():
            if fid in METADATA_PROFILE_FIELDS or not isinstance(spec, dict):
                continue
            name, unit = FIELD_LABELS.get(fid, (fid, ""))
            limits = {k: spec[k] for k in ("lsl", "usl", "target", "range") if k in spec}
            _ensure_data_point(
                points,
                equipment_id=eq_id,
                field_id=fid,
                process_step_id=step,
                display_name=name,
                unit=unit,
                product_line_id=line_id,
                spec_limits=limits or None,
            )


def patch_telemetry_and_datapoints(data: dict[str, Any]) -> None:
    profiles = data.setdefault("equipment_telemetry_profiles", {})
    points = data.setdefault("data_points", [])
    eq_by_id = {e["equipment_id"]: e for e in data.get("equipment", [])}

    labels = FIELD_LABELS

    for eq_id, patch_fields in PROFILE_FIELD_PATCHES.items():
        prof = profiles.setdefault(eq_id, {"fields": {}})
        fields = prof.setdefault("fields", {})
        for fid, spec in patch_fields.items():
            fields[fid] = spec
        eq = eq_by_id.get(eq_id)
        step = prof.get("process_step_id") or (eq.get("process_step_id") if eq else "")
        line_id = eq.get("product_line_id") if eq else None
        for fid in patch_fields:
            if fid in {"finished_batch_id"}:
                continue
            name, unit = labels.get(fid, (fid, ""))
            _ensure_data_point(
                points,
                equipment_id=eq_id,
                field_id=fid,
                process_step_id=step,
                display_name=name,
                unit=unit,
                product_line_id=line_id,
            )


def patch_recipes(data: dict[str, Any]) -> None:
    recipes: list[dict[str, Any]] = data.setdefault("recipes", [])
    ids = {r["recipe_id"] for r in recipes}
    if "SW-ER70-V1" not in ids:
        recipes.append({
            "recipe_id": "SW-ER70-V1",
            "display_name": "实心 ER70S-6 出口配方",
            "product_category": "solid_wire",
            "grade": "ER70S-6",
            "product_line_id": "SW-LINE-02",
            "version": "V1",
            "target_parameters": {
                "tension_kN": 5.2,
                "coating_thickness_top_um": 8.0,
                "rm_mpa_target": 690,
                "impact_kv2_j_target": 69,
                "elongation_pct_target": 20.0,
                "corrosion_i_value_target": 6.0,
            },
            "raw_material_refs": [
                {"material_type": "wire_rod", "batch_id": "ROD-20260520-C1", "qty_kg": 920},
            ],
        })
    for recipe in recipes:
        rid = recipe.get("recipe_id")
        if rid in RECIPE_MECHANICAL_PATCHES:
            tp = recipe.setdefault("target_parameters", {})
            tp.update(RECIPE_MECHANICAL_PATCHES[rid])


# 成品/在制批次 ID：202606 → 202506（演示时间轴 2025-06）
PRODUCT_BATCH_RENAMES: dict[str, str] = {
    "FCW-20260605-B2": "FCW-20250605-B2",
    "SW-20260605-B1": "SW-20250605-B1",
    "SW-20260603-B1": "SW-20250603-B1",
    "FCW-20260604-B1": "FCW-20250604-B2",
}

SHIFT_RENAMES: dict[str, str] = {
    "SHIFT-20260605-DAY": "SHIFT-20250605-DAY",
    "SHIFT-20260605-NIGHT": "SHIFT-20250605-NIGHT",
    "SHIFT-20260605-ROD-DAY": "SHIFT-20250605-ROD-DAY",
}


def _apply_renames(obj: Any, rename: dict[str, str]) -> Any:
    if isinstance(obj, dict):
        return {k: _apply_renames(v, rename) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_apply_renames(v, rename) for v in obj]
    if isinstance(obj, str) and obj in rename:
        return rename[obj]
    return obj


def _normalize_demo_year(text: str) -> str:
    """将演示时间串中的 2026-06 归一到 2025-06."""
    return text.replace("2026-06-", "2025-06-").replace("2026-07-", "2025-07-")


def patch_material_batch_ids(data: dict[str, Any]) -> None:
    """原料批次 ID 与 2025 年时间轴对齐（202606 → 202506）."""
    rename = {
        "STRIP-20260604-A1": "STRIP-20250604-A1",
        "FLUX-20260605-A1": "FLUX-20250605-A1",
    }

    patched = _apply_renames(data, rename)
    data.clear()
    data.update(patched)

    for mb in data.get("material_batches", []):
        bid = mb.get("batch_id")
        if bid == "STRIP-20250604-A1":
            mb["received_at"] = "2025-06-04T06:00:00Z"
        if bid == "FLUX-20250605-A1":
            mb["received_at"] = "2025-06-05T08:00:00Z"


def patch_product_and_shift_ids(data: dict[str, Any]) -> None:
    """在制/完成批次、班次、finished_batch_id 等全量迁 2025-06 口径."""
    patched = _apply_renames(data, PRODUCT_BATCH_RENAMES)
    data.clear()
    data.update(patched)

    for shift in data.get("shift_calendars", []):
        old_id = shift.get("shift_id")
        if old_id in SHIFT_RENAMES:
            shift["shift_id"] = SHIFT_RENAMES[old_id]
        for key in ("start_time", "end_time"):
            if shift.get(key):
                shift[key] = _normalize_demo_year(str(shift[key]))

    # 成品库 HY-830 批次（原 FCW-20260604-B1）补主档
    batches: list[dict[str, Any]] = data.setdefault("product_batches", [])
    by_id = {b["batch_id"]: b for b in batches}
    if "FCW-20250604-B2" not in by_id:
        batches.append({
            "batch_id": "FCW-20250604-B2",
            "factory_id": "JQHC-PLANT-01",
            "workshop_id": "WS-WIRE-01",
            "product_line_id": "FCW-LINE-07",
            "production_order_id": "PO-20250606-FCW-EXPORT-HY830",
            "product_category": "flux_core_wire",
            "recipe_id": "FCW-HY830-V3",
            "grade": "HY-830MPa",
            "shift": "day",
            "status": "completed",
            "started_at": "2025-06-04T08:00:00Z",
            "ended_at": "2025-06-04T18:00:00Z",
            "quantity_kg": 1180,
            "parent_batches": ["STRIP-20250604-A1", "FLUX-20250605-A1"],
        })

    for prof in (data.get("equipment_telemetry_profiles") or {}).values():
        fields = prof.get("fields") or {}
        for fid, spec in list(fields.items()):
            if fid == "finished_batch_id" and isinstance(spec, str):
                fields[fid] = PRODUCT_BATCH_RENAMES.get(spec, spec)


def patch_product_batches_and_timeline(data: dict[str, Any]) -> None:
    """补齐订单批次、成品批主档、统一 2025 年时间轴."""
    batches: list[dict[str, Any]] = data.setdefault("product_batches", [])
    by_id = {b["batch_id"]: b for b in batches}

    new_batches = [
        {
            "batch_id": "FCW-20250604-B1",
            "factory_id": "JQHC-PLANT-01",
            "workshop_id": "WS-WIRE-01",
            "product_line_id": "FCW-LINE-07",
            "production_order_id": "PO-20250606-FCW-DOM-HY780",
            "product_category": "flux_core_wire",
            "recipe_id": "FCW-HY780-V2",
            "grade": "HY-780MPa",
            "shift": "night",
            "status": "completed",
            "started_at": "2025-06-04T20:00:00Z",
            "ended_at": "2025-06-05T04:30:00Z",
            "quantity_kg": 1180,
            "parent_batches": ["STRIP-20250604-A1", "FLUX-20250605-A1"],
        },
        {
            "batch_id": "FCW-20250606-B3",
            "factory_id": "JQHC-PLANT-01",
            "workshop_id": "WS-WIRE-01",
            "product_line_id": "FCW-LINE-07",
            "production_order_id": "PO-20250606-FCW-LNG-CUSTOM",
            "product_category": "flux_core_wire",
            "recipe_id": "FCW-HY960-V1",
            "grade": "HY-960MPa",
            "shift": "day",
            "status": "released",
            "started_at": "2025-06-06T08:00:00Z",
            "quantity_kg": 680,
            "parent_batches": ["STRIP-20250604-A1", "FLUX-20250605-A1"],
        },
        {
            "batch_id": "SW-20250606-B2",
            "factory_id": "JQHC-PLANT-01",
            "workshop_id": "WS-WIRE-01",
            "product_line_id": "SW-LINE-02",
            "production_order_id": "PO-20250606-SW-EXPORT-ER70",
            "product_category": "solid_wire",
            "recipe_id": "SW-ER70-V1",
            "grade": "ER70S-6",
            "shift": "day",
            "status": "released",
            "started_at": "2025-06-06T06:00:00Z",
            "quantity_kg": 920,
            "parent_batches": ["ROD-20260520-C1"],
        },
    ]
    for nb in new_batches:
        if nb["batch_id"] not in by_id:
            batches.append(nb)
            by_id[nb["batch_id"]] = nb

    # 统一既有批次时间为 2025-06（批次 ID 已在 patch_product_and_shift_ids 中迁移）
    time_map = {
        "FCW-20250605-B2": ("2025-06-05T08:00:00Z", None),
        "SW-20250605-B1": ("2025-06-05T06:30:00Z", None),
        "SW-20250603-B1": ("2025-06-03T20:00:00Z", "2025-06-04T04:30:00Z"),
        "WR-20260528-B2": ("2025-05-28T08:00:00Z", "2025-05-28T16:00:00Z"),
        "FCW-20250604-B2": ("2025-06-04T08:00:00Z", "2025-06-04T18:00:00Z"),
    }
    for bid, (started, ended) in time_map.items():
        if bid in by_id:
            by_id[bid]["started_at"] = started
            if ended:
                by_id[bid]["ended_at"] = ended

    # 订单交期对齐 2025-06/07（演示时间轴）
    due_by_order = {
        "PO-20250606-FCW-EXPORT-HY830": "2025-06-28T12:00:00Z",
        "PO-20250606-FCW-DOM-HY780": "2025-07-04T12:00:00Z",
        "PO-20250606-SW-DOM-ER50": "2025-07-01T12:00:00Z",
        "PO-20250606-WR-HYDRO-E7014": "2025-07-06T12:00:00Z",
        "PO-20250606-FCW-LNG-CUSTOM": "2025-07-26T12:00:00Z",
        "PO-20250606-SW-EXPORT-ER70": "2025-06-24T12:00:00Z",
    }
    for order in data.get("production_orders", []):
        oid = order.get("production_order_id", "")
        if oid in due_by_order:
            order["due_date"] = due_by_order[oid]

    # 成品库存批与 product_batches 对齐
    finished = data.get("inventory_snapshots", {}).get("finished", [])
    for item in finished:
        bid = item.get("batch_id")
        if bid in by_id:
            item["grade"] = by_id[bid].get("grade", item.get("grade"))


def patch_wire_capacity(data: dict[str, Any]) -> None:
    """焊丝侧设计产能加总对齐 10 万吨（+2300t 调至埋弧线）."""
    for line in data.get("product_lines", []) + data.get("line_registry", []):
        if line.get("product_category") == "submerged_arc_wire":
            line["design_capacity_t_per_year"] = 4950
            line["design_capacity_t_per_day"] = 15.5
        if line.get("product_line_id", "").startswith("SAW-"):
            line["template_id"] = "submerged_arc_wire"
    fp = data.setdefault("factory_profile", {})
    fp["wire_capacity_sum_note"] = (
        "焊丝 22 条线设计产能加总 10 万吨；埋弧 2 线各 4950t/年计入焊丝产能池"
    )


QC_ANNOTATIONS_BY_LINE: dict[str, dict[str, list[dict[str, str]]]] = {
    "SW-LINE-02": {
        "rough_drawing": [
            {"qc_id": "raw_inspection", "display_name": "检验工艺", "role": "quality_gate"},
            {"qc_id": "rough_wire_process", "display_name": "粗拔丝工艺", "role": "process_support"},
        ],
        "fine_drawing": [
            {"qc_id": "fine_wire_process", "display_name": "细拔丝工艺", "role": "process_support"},
        ],
        "copper_plating": [
            {"qc_id": "degrease_process", "display_name": "脱脂工艺", "role": "process_support"},
            {"qc_id": "plating_process", "display_name": "镀铜工艺", "role": "process_support"},
        ],
        "winding": [
            {"qc_id": "winding_process", "display_name": "层绕工艺", "role": "process_support"},
        ],
    },
    "FCW-LINE-07": {
        "cut_strip": [
            {"qc_id": "strip_qc", "display_name": "钢带来料检验", "role": "quality_gate"},
        ],
        "powder_mixing": [
            {"qc_id": "flux_qc", "display_name": "配粉检验", "role": "quality_gate"},
        ],
        "filling_forming": [
            {"qc_id": "fill_ratio_qc", "display_name": "填充率 SPC", "role": "quality_gate"},
        ],
        "copper_plating": [
            {"qc_id": "plating_process", "display_name": "镀铜工艺", "role": "process_support"},
        ],
    },
    "WR-LINE-01": {
        "wire_drawing": [
            {"qc_id": "rod_inspection", "display_name": "盘条检验", "role": "quality_gate"},
        ],
        "cutting": [
            {"qc_id": "cut_length_qc", "display_name": "切丝长度 SPC", "role": "quality_gate"},
        ],
        "drying": [
            {"qc_id": "moisture_qc", "display_name": "含水率终检", "role": "quality_gate"},
        ],
    },
}


def patch_twin_qc_annotations(data: dict[str, Any]) -> None:
    """精细产线孪生工序补充 QC 支撑工艺注释（不参与仿真推送）."""
    layouts = data.get("twin_layouts", {})
    for lid, step_map in QC_ANNOTATIONS_BY_LINE.items():
        layout = layouts.get(lid)
        if not layout:
            continue
        steps = layout.setdefault("steps", {})
        for step_id, annotations in step_map.items():
            if step_id in steps and isinstance(steps[step_id], dict):
                steps[step_id]["qc_annotations"] = annotations
        layout["process_narrative_note"] = (
            "药芯域模型将资料「拉拔」拆为粗拔+细拔+镀铜；QC 注释标注资料中的质量控制支撑点"
            if lid == "FCW-LINE-07"
            else "质量控制点以注释展示，与《科技公司详情》实心焊丝工艺图一致"
        )


def patch_saw_demo_data(data: dict[str, Any]) -> None:
    """埋弧品类：示范产品 / 配方 / 订单 / 批次."""
    products: list[dict[str, Any]] = data.setdefault("products", [])
    product_ids = {p["product_id"] for p in products}
    if "PROD-SAW-H10Mn2-40" not in product_ids:
        products.append({
            "product_id": "PROD-SAW-H10Mn2-40",
            "display_name": "埋弧焊丝 H10Mn2 Φ4.0",
            "product_category": "submerged_arc_wire",
            "grade": "H10Mn2",
            "spec": "Φ4.0mm",
            "package_spec": "25kg/盘",
            "quality_standard": "GB-T-H10Mn2",
            "default_recipe_ids": ["SAW-H10Mn2-V1"],
            "allowed_line_ids": ["SAW-LINE-01", "SAW-LINE-02"],
            "mechanical_spec": {
                "rm_mpa_min": 610,
                "impact_kv2_j_min": 69,
                "elongation_pct_min": 22.0,
                "corrosion_i_value_min": 6.0,
            },
        })

    recipes: list[dict[str, Any]] = data.setdefault("recipes", [])
    recipe_ids = {r["recipe_id"] for r in recipes}
    if "SAW-H10Mn2-V1" not in recipe_ids:
        recipes.append({
            "recipe_id": "SAW-H10Mn2-V1",
            "display_name": "埋弧 H10Mn2 管线钢配方",
            "product_category": "submerged_arc_wire",
            "grade": "H10Mn2",
            "product_line_id": "SAW-LINE-01",
            "version": "V1",
            "target_parameters": {
                "tension_kN": 5.8,
                "coating_thickness_top_um": 7.5,
                "rm_mpa_target": 610,
                "impact_kv2_j_target": 69,
                "elongation_pct_target": 22.0,
                "line_speed_m_per_min": 72,
            },
            "raw_material_refs": [
                {"material_type": "wire_rod", "batch_id": "ROD-20260520-C1", "qty_kg": 1100},
            ],
        })

    orders: list[dict[str, Any]] = data.setdefault("production_orders", [])
    order_ids = {o["production_order_id"] for o in orders}
    if "PO-20250606-SAW-PIPE-610" not in order_ids:
        orders.append({
            "production_order_id": "PO-20250606-SAW-PIPE-610",
            "factory_id": "JQHC-PLANT-01",
            "customer_order_id": "SO-REGULAR-SAW-PIPE-610",
            "product_id": "PROD-SAW-H10Mn2-40",
            "product_category": "submerged_arc_wire",
            "grade": "H10Mn2",
            "recipe_id": "SAW-H10Mn2-V1",
            "assigned_line_ids": ["SAW-LINE-01"],
            "priority": "normal",
            "planned_quantity_t": 85.0,
            "released_quantity_t": 28.0,
            "due_date": "2025-07-08T12:00:00Z",
            "status": "released",
            "remark": "超高压管线埋弧焊丝常规订单",
            "order_type": "regular",
            "delivery_sla_days": 28,
            "customer_segment": "管线钢",
            "is_export": False,
        })
        data.setdefault("production_orders", orders)

    batches: list[dict[str, Any]] = data.setdefault("product_batches", [])
    batch_ids = {b["batch_id"] for b in batches}
    if "SAW-20250605-B1" not in batch_ids:
        batches.append({
            "batch_id": "SAW-20250605-B1",
            "factory_id": "JQHC-PLANT-01",
            "workshop_id": "WS-WIRE-01",
            "product_line_id": "SAW-LINE-01",
            "production_order_id": "PO-20250606-SAW-PIPE-610",
            "product_category": "submerged_arc_wire",
            "recipe_id": "SAW-H10Mn2-V1",
            "grade": "H10Mn2",
            "shift": "day",
            "status": "released",
            "started_at": "2025-06-05T10:00:00Z",
            "quantity_kg": 950,
            "parent_batches": ["ROD-20260520-C1"],
        })


def patch_twin_metadata(data: dict[str, Any]) -> None:
    layouts = data.get("twin_layouts", {})
    for lid in DETAILED_LINES:
        if lid in layouts and isinstance(layouts[lid], dict):
            layouts[lid]["detail_level"] = "full"
    sim = data.setdefault("simulation_defaults", {})
    sim["default_quality_gate_pass_rate_pct"] = 97.2
    sim["kpi_quality_rate_unified_pct"] = 97.2


def patch_equipment_metadata(data: dict[str, Any]) -> None:
    for eq in data.get("equipment", []):
        if eq.get("equipment_id") == "CUT-WR-01":
            eq["rated_capacity_per_hour"] = 1800
            eq["capacity_unit"] = "cuts_per_min"


def patch_master(data: dict[str, Any]) -> dict[str, Any]:
    patch_material_batch_ids(data)
    patch_product_and_shift_ids(data)
    patch_recipes(data)
    patch_product_batches_and_timeline(data)
    patch_wire_capacity(data)
    patch_saw_demo_data(data)
    patch_telemetry_and_datapoints(data)
    patch_profile_datapoint_alignment(data)
    patch_twin_metadata(data)
    patch_twin_qc_annotations(data)
    patch_equipment_metadata(data)
    return data


def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else MASTER_PATH
    data = patch_master(load_master(path))
    save_master(data, path)
    print(f"Patched {path}: batches={len(data.get('product_batches', []))}, "
          f"data_points={len(data.get('data_points', []))}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
