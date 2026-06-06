#!/usr/bin/env python3
"""按 annual_production_plan 生成/刷新 production_orders（委托 order_simulator）."""

from __future__ import annotations

import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

_TOOLS_DIR = Path(__file__).resolve().parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))
from master_data_lib import MASTER_PATH, load_master, save_master

ORDER_TEMPLATES = [
    {
        "suffix": "FCW-EXPORT-HY830",
        "product_id": "PROD-FCW-HY830-16",
        "product_category": "flux_core_wire",
        "grade": "HY-830MPa",
        "recipe_id": "FCW-HY830-V3",
        "assigned_line_ids": ["FCW-LINE-07"],
        "planned_quantity_t": 180,
        "priority": "high",
        "order_type": "export",
        "delivery_sla_days": 22,
        "customer_segment": "海洋工程",
        "remark": "海工出口订单 · 交期 15-30 天",
    },
    {
        "suffix": "FCW-DOM-HY780",
        "product_id": "PROD-FCW-HY780-16",
        "product_category": "flux_core_wire",
        "grade": "HY-780MPa",
        "recipe_id": "FCW-HY780-V2",
        "assigned_line_ids": ["FCW-LINE-07"],
        "planned_quantity_t": 90,
        "priority": "normal",
        "order_type": "regular",
        "delivery_sla_days": 28,
        "customer_segment": "钢结构",
        "remark": "内销常规订单",
    },
    {
        "suffix": "SW-DOM-ER50",
        "product_id": "PROD-SW-ER50-12",
        "product_category": "solid_wire",
        "grade": "ER50-6",
        "recipe_id": "SW-ER50-V2",
        "assigned_line_ids": ["SW-LINE-02"],
        "planned_quantity_t": 160,
        "priority": "normal",
        "order_type": "regular",
        "delivery_sla_days": 25,
        "customer_segment": "工程机械",
        "remark": "实心焊丝常规订单",
    },
    {
        "suffix": "WR-HYDRO-E7014",
        "product_id": "PROD-WR-E7014-32",
        "product_category": "welding_rod",
        "grade": "E7014",
        "recipe_id": "WR-E7014-V1",
        "assigned_line_ids": ["WR-LINE-01"],
        "planned_quantity_t": 240,
        "priority": "high",
        "order_type": "regular",
        "delivery_sla_days": 30,
        "customer_segment": "水电",
        "remark": "焊条主力订单 · 焊条产能占比高",
    },
    {
        "suffix": "FCW-LNG-CUSTOM",
        "product_id": "PROD-FCW-HY960-16",
        "product_category": "flux_core_wire",
        "grade": "HY-960MPa",
        "recipe_id": "FCW-HY960-V1",
        "assigned_line_ids": ["FCW-LINE-07"],
        "planned_quantity_t": 45,
        "priority": "high",
        "order_type": "custom",
        "delivery_sla_days": 50,
        "customer_segment": "LNG储罐",
        "remark": "高牌号定制单 · 交期 45+ 天",
    },
    {
        "suffix": "SW-EXPORT-ER70",
        "product_id": "PROD-SW-ER70-12",
        "product_category": "solid_wire",
        "grade": "ER70S-6",
        "recipe_id": "SW-ER70-V1",
        "assigned_line_ids": ["SW-LINE-02"],
        "planned_quantity_t": 120,
        "priority": "high",
        "order_type": "export",
        "delivery_sla_days": 18,
        "customer_segment": "出口",
        "remark": "出口订单集中 · 15 天交期压力",
    },
]


def _iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def build_orders(base_date: datetime | None = None) -> list[dict[str, Any]]:
    now = base_date or datetime.now(timezone.utc)
    plan = annual_plan_ratio()
    orders: list[dict[str, Any]] = []
    for idx, tpl in enumerate(ORDER_TEMPLATES):
        due = now + timedelta(days=int(tpl["delivery_sla_days"]))
        released_ratio = 0.55 if tpl["order_type"] == "export" else 0.42
        if idx == 0:
            status = "in_progress"
            released_ratio = 0.67
        elif tpl["order_type"] == "custom":
            status = "released"
            released_ratio = 0.15
        else:
            status = "released"
        orders.append({
            "production_order_id": f"PO-2025{now.strftime('%m%d')}-{tpl['suffix']}",
            "factory_id": "JQHC-PLANT-01",
            "customer_order_id": f"SO-{tpl['order_type'].upper()}-{tpl['suffix']}",
            "product_id": tpl["product_id"],
            "product_category": tpl["product_category"],
            "grade": tpl["grade"],
            "recipe_id": tpl["recipe_id"],
            "assigned_line_ids": tpl["assigned_line_ids"],
            "priority": tpl["priority"],
            "planned_quantity_t": tpl["planned_quantity_t"],
            "released_quantity_t": round(tpl["planned_quantity_t"] * released_ratio, 1),
            "due_date": _iso(due),
            "status": status,
            "order_type": tpl["order_type"],
            "delivery_sla_days": tpl["delivery_sla_days"],
            "customer_segment": tpl["customer_segment"],
            "is_export": tpl["order_type"] == "export",
            "remark": tpl["remark"],
            "plan_year": 2025,
            "plan_share_of_annual_wire_t": round(tpl["planned_quantity_t"] / max(plan.get("wire", 1), 1), 4),
        })
    return orders


def annual_plan_ratio() -> dict[str, float]:
    return {"wire": 100_000, "rod": 300_000, "fcw": 25_000}


def extra_products() -> list[dict[str, Any]]:
    return [
        {
            "product_id": "PROD-FCW-HY780-16",
            "display_name": "药芯焊丝 HY-780MPa Φ1.6",
            "product_category": "flux_core_wire",
            "grade": "HY-780MPa",
            "spec": "Φ1.6mm",
            "package_spec": "20kg/盘",
            "quality_standard": "JQHC-FCW-HY780",
            "mechanical_spec": {"rm_mpa_min": 780, "impact_kv2_j_min": 69, "elongation_pct_min": 18.0, "corrosion_i_value_min": 6.0},
            "default_recipe_ids": ["FCW-HY780-V2"],
            "allowed_line_ids": ["FCW-LINE-07"],
        },
        {
            "product_id": "PROD-FCW-HY960-16",
            "display_name": "药芯焊丝 HY-960MPa Φ1.6",
            "product_category": "flux_core_wire",
            "grade": "HY-960MPa",
            "spec": "Φ1.6mm",
            "package_spec": "20kg/盘",
            "quality_standard": "JQHC-FCW-HY960",
            "mechanical_spec": {"rm_mpa_min": 960, "impact_kv2_j_min": 47, "elongation_pct_min": 12.0, "corrosion_i_value_min": 6.0},
            "default_recipe_ids": ["FCW-HY960-V1"],
            "allowed_line_ids": ["FCW-LINE-07"],
        },
        {
            "product_id": "PROD-SW-ER70-12",
            "display_name": "实心焊丝 ER70S-6 Φ1.2",
            "product_category": "solid_wire",
            "grade": "ER70S-6",
            "spec": "Φ1.2mm",
            "package_spec": "20kg/盘",
            "quality_standard": "AWS-ER70S-6",
            "mechanical_spec": {"rm_mpa_min": 690, "impact_kv2_j_min": 69, "elongation_pct_min": 20.0, "corrosion_i_value_min": 6.0},
            "default_recipe_ids": ["SW-ER70-V1"],
            "allowed_line_ids": ["SW-LINE-02"],
        },
    ]


def extra_recipes() -> list[dict[str, Any]]:
    return [
        {
            "recipe_id": "FCW-HY780-V2",
            "display_name": "药芯 HY-780 标准配方",
            "product_category": "flux_core_wire",
            "grade": "HY-780MPa",
            "product_line_id": "FCW-LINE-07",
            "version": "V2",
            "target_parameters": {
                "fill_ratio_pct": 18.0,
                "coating_thickness_top_um": 8.0,
                "tension_kN": 4.2,
                "mixing_rpm": 30,
                "rm_mpa_target": 780,
                "impact_kv2_j_target": 72,
                "elongation_pct_target": 19.0,
                "corrosion_i_value_target": 6.2,
            },
            "raw_material_refs": [
                {"material_type": "steel_strip", "batch_id": "STRIP-20250604-A1", "qty_kg": 850},
                {"material_type": "flux_powder", "batch_id": "FLUX-20250605-A1", "qty_kg": 120},
            ],
        },
        {
            "recipe_id": "FCW-HY960-V1",
            "display_name": "药芯 HY-960 高牌号配方",
            "product_category": "flux_core_wire",
            "grade": "HY-960MPa",
            "product_line_id": "FCW-LINE-07",
            "version": "V1",
            "target_parameters": {
                "fill_ratio_pct": 17.8,
                "coating_thickness_top_um": 9.0,
                "tension_kN": 5.2,
                "mixing_rpm": 28,
                "rm_mpa_target": 960,
                "impact_kv2_j_target": 50,
                "elongation_pct_target": 13.0,
                "corrosion_i_value_target": 6.5,
            },
            "raw_material_refs": [
                {"material_type": "steel_strip", "batch_id": "STRIP-20250604-A1", "qty_kg": 900},
                {"material_type": "flux_powder", "batch_id": "FLUX-20250605-A1", "qty_kg": 140},
            ],
        },
        {
            "recipe_id": "SW-ER70-V1",
            "display_name": "实心 ER70S-6 配方",
            "product_category": "solid_wire",
            "grade": "ER70S-6",
            "product_line_id": "SW-LINE-02",
            "version": "V1",
            "target_parameters": {
                "tension_kN": 5.2,
                "coating_thickness_top_um": 7.8,
                "rm_mpa_target": 690,
                "impact_kv2_j_target": 75,
                "elongation_pct_target": 21.0,
                "corrosion_i_value_target": 6.1,
            },
            "raw_material_refs": [
                {"material_type": "wire_rod", "batch_id": "ROD-20260520-C1", "qty_kg": 1000},
            ],
        },
    ]


def _patch_products_recipes(data: dict[str, Any]) -> None:
    existing_products = {p["product_id"]: p for p in data.get("products", [])}
    for p in extra_products():
        existing_products[p["product_id"]] = p
    for p in existing_products.values():
        if "mechanical_spec" not in p and p.get("grade") == "HY-830MPa":
            p["mechanical_spec"] = {
                "rm_mpa_min": 830, "impact_kv2_j_min": 47,
                "elongation_pct_min": 15.0, "corrosion_i_value_min": 6.0,
            }
        if p.get("grade") == "ER50-6":
            p["mechanical_spec"] = {
                "rm_mpa_min": 610, "impact_kv2_j_min": 69,
                "elongation_pct_min": 22.0, "corrosion_i_value_min": 6.0,
            }
        if p.get("grade") == "E7014":
            p["mechanical_spec"] = {
                "rm_mpa_min": 610, "impact_kv2_j_min": 69,
                "elongation_pct_min": 22.0, "corrosion_i_value_min": 6.0,
            }
    data["products"] = list(existing_products.values())

    existing_recipes = {r["recipe_id"]: r for r in data.get("recipes", [])}
    for r in extra_recipes():
        existing_recipes[r["recipe_id"]] = r
    fcw = existing_recipes.get("FCW-HY830-V3")
    if fcw:
        tp = fcw.setdefault("target_parameters", {})
        tp.setdefault("rm_mpa_target", 830)
        tp.setdefault("impact_kv2_j_target", 50)
        tp.setdefault("elongation_pct_target", 16.0)
        tp.setdefault("corrosion_i_value_target", 6.2)
    data["recipes"] = list(existing_recipes.values())


def patch_master(data: dict[str, Any]) -> dict[str, Any]:
    _patch_products_recipes(data)
    from order_simulator import patch_master as simulate_orders

    return simulate_orders(data)


def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else MASTER_PATH
    data = patch_master(load_master(path))
    save_master(data, path)
    print(f"Updated orders: {len(data['production_orders'])}, products: {len(data['products'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
