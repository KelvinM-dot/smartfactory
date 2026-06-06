#!/usr/bin/env python3
"""
P0–P3 订单仿真生成器：滚动交期、分布采样、批次对齐、全厂差异化。

设计产能（焊丝 10 万 t / 焊条 30 万 t）为工厂上限；实际运行按 utilization 折减，
订单量与 backlog 按「实际可达产能」而非满产设计。

用法:
  python order_simulator.py
  python order_simulator.py --dry-run
  python order_simulator.py /path/to/master.json
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

_TOOLS_DIR = Path(__file__).resolve().parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))
from master_data_lib import MASTER_PATH, REFERENCE_SIM_LINES, load_master, save_master

# 工厂设计产能（上限，非满产运行）
DESIGN_ANNUAL_T = {
    "wire_total": 100_000,
    "rod_total": 300_000,
}

# 实际运行利用率：设计产能 × 利用率 ≈ 在产负荷
CATEGORY_ACTUAL_UTILIZATION = {
    "flux_core_wire": 0.55,
    "solid_wire": 0.58,
    "submerged_arc_wire": 0.52,
    "welding_rod": 0.68,
}

PRIORITY_RANK = {"high": 0, "normal": 1, "low": 2}

REFERENCE_LINE_ORDERS: list[dict[str, Any]] = [
    {
        "suffix": "FCW-EXPORT-HY830",
        "line_id": "FCW-LINE-07",
        "product_id": "PROD-FCW-HY830-16",
        "product_category": "flux_core_wire",
        "grade": "HY-830MPa",
        "recipe_id": "FCW-HY830-V3",
        "planned_quantity_t": 180,
        "priority": "high",
        "order_type": "export",
        "delivery_sla_days": 22,
        "customer_segment": "海洋工程",
        "is_export": True,
        "remark": "海工出口订单 · 交期 15-30 天",
        "role": "in_progress",
    },
    {
        "suffix": "FCW-DOM-HY780",
        "line_id": "FCW-LINE-07",
        "product_id": "PROD-FCW-HY780-16",
        "product_category": "flux_core_wire",
        "grade": "HY-780MPa",
        "recipe_id": "FCW-HY780-V2",
        "planned_quantity_t": 90,
        "priority": "normal",
        "order_type": "regular",
        "delivery_sla_days": 28,
        "customer_segment": "钢结构",
        "is_export": False,
        "remark": "内销常规订单",
        "role": "released",
    },
    {
        "suffix": "FCW-LNG-CUSTOM",
        "line_id": "FCW-LINE-07",
        "product_id": "PROD-FCW-HY960-16",
        "product_category": "flux_core_wire",
        "grade": "HY-960MPa",
        "recipe_id": "FCW-HY960-V1",
        "planned_quantity_t": 45,
        "priority": "high",
        "order_type": "custom",
        "delivery_sla_days": 50,
        "customer_segment": "LNG储罐",
        "is_export": False,
        "remark": "高牌号定制单 · 交期 45+ 天",
        "role": "released",
    },
    {
        "suffix": "SW-DOM-ER50",
        "line_id": "SW-LINE-02",
        "product_id": "PROD-SW-ER50-12",
        "product_category": "solid_wire",
        "grade": "ER50-6",
        "recipe_id": "SW-ER50-V2",
        "planned_quantity_t": 160,
        "priority": "normal",
        "order_type": "regular",
        "delivery_sla_days": 25,
        "customer_segment": "工程机械",
        "is_export": False,
        "remark": "实心焊丝常规订单",
        "role": "in_progress",
    },
    {
        "suffix": "SW-EXPORT-ER70",
        "line_id": "SW-LINE-02",
        "product_id": "PROD-SW-ER70-12",
        "product_category": "solid_wire",
        "grade": "ER70S-6",
        "recipe_id": "SW-ER70-V1",
        "planned_quantity_t": 120,
        "priority": "high",
        "order_type": "export",
        "delivery_sla_days": 18,
        "customer_segment": "出口",
        "is_export": True,
        "remark": "出口订单集中 · 15 天交期压力",
        "role": "released",
    },
    {
        "suffix": "WR-HYDRO-E7014",
        "line_id": "WR-LINE-01",
        "product_id": "PROD-WR-E7014-32",
        "product_category": "welding_rod",
        "grade": "E7014",
        "recipe_id": "WR-E7014-V1",
        "planned_quantity_t": 240,
        "priority": "high",
        "order_type": "regular",
        "delivery_sla_days": 30,
        "customer_segment": "水电",
        "is_export": False,
        "remark": "焊条主力订单 · 焊条产能占比高",
        "role": "in_progress",
    },
    {
        "suffix": "WR-EXPORT-E7018",
        "line_id": "WR-LINE-01",
        "product_id": "PROD-WR-E7014-32",
        "product_category": "welding_rod",
        "grade": "E7014",
        "recipe_id": "WR-E7014-V1",
        "planned_quantity_t": 160,
        "priority": "high",
        "order_type": "export",
        "delivery_sla_days": 20,
        "customer_segment": "出口",
        "is_export": True,
        "remark": "焊条出口批次",
        "role": "released",
    },
]

# 非参考线：按产线索引轮换的订单画像
LINE_ORDER_PROFILES: dict[str, list[dict[str, Any]]] = {
    "flux_core_wire": [
        {"grade": "HY-780MPa", "order_type": "regular", "priority": "normal", "segment": "钢结构", "sla": 28, "qty_days": 11},
        {"grade": "HY-830MPa", "order_type": "export", "priority": "high", "segment": "海洋工程", "sla": 20, "qty_days": 14},
        {"grade": "HY-960MPa", "order_type": "custom", "priority": "high", "segment": "LNG储罐", "sla": 48, "qty_days": 8},
    ],
    "solid_wire": [
        {"grade": "ER50-6", "order_type": "regular", "priority": "normal", "segment": "工程机械", "sla": 26, "qty_days": 12},
        {"grade": "ER70S-6", "order_type": "export", "priority": "high", "segment": "出口", "sla": 18, "qty_days": 10},
    ],
    "submerged_arc_wire": [
        {"grade": "ER50-6", "order_type": "regular", "priority": "normal", "segment": "桥梁钢构", "sla": 30, "qty_days": 13},
        {"grade": "ER70S-6", "order_type": "regular", "priority": "normal", "segment": "压力容器", "sla": 32, "qty_days": 11},
    ],
    "welding_rod": [
        {"grade": "E7014", "order_type": "regular", "priority": "normal", "segment": "水电", "sla": 30, "qty_days": 14},
        {"grade": "E7014", "order_type": "export", "priority": "high", "segment": "出口", "sla": 22, "qty_days": 12},
        {"grade": "E7014", "order_type": "regular", "priority": "low", "segment": "建筑钢构", "sla": 35, "qty_days": 9},
    ],
}


def _iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _line_seed(line_id: str) -> int:
    return int(hashlib.md5(line_id.encode()).hexdigest()[:8], 16)


def _line_code(line_id: str) -> str:
    m = re.match(r"^([A-Z]+)-LINE-(\d+)$", line_id)
    if not m:
        return line_id.replace("-", "")
    return f"{m.group(1)}{int(m.group(2)):02d}"


def _actual_daily_capacity(line: dict[str, Any]) -> float:
    design = float(line.get("design_capacity_t_per_day") or 10.0)
    category = str(line.get("product_category") or "flux_core_wire")
    util = CATEGORY_ACTUAL_UTILIZATION.get(category, 0.60)
    seed = _line_seed(str(line.get("product_line_id")))
    line_factor = 0.88 + (seed % 13) / 100.0  # 0.88–1.00 产线个体差异
    return round(design * util * line_factor, 2)


def _recipes_by_line(data: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    out: dict[str, list[dict[str, Any]]] = {}
    for recipe in data.get("recipes", []):
        lid = str(recipe.get("product_line_id") or "")
        if lid:
            out.setdefault(lid, []).append(recipe)
    return out


def _products_by_grade(data: dict[str, Any]) -> dict[tuple[str, str], dict[str, Any]]:
    out: dict[tuple[str, str], dict[str, Any]] = {}
    for product in data.get("products", []):
        key = (str(product.get("product_category")), str(product.get("grade")))
        out.setdefault(key, product)
    return out


def _pick_recipe(recipes: list[dict[str, Any]], grade: str) -> dict[str, Any] | None:
    for recipe in recipes:
        if str(recipe.get("grade")) == grade:
            return recipe
    return recipes[0] if recipes else None


def _pick_product(products: dict[tuple[str, str], dict[str, Any]], category: str, grade: str, recipe: dict[str, Any]) -> str:
    product = products.get((category, grade))
    if product:
        return str(product["product_id"])
    return f"PROD-{category.upper()}-{grade}"


def _order_id(base_date: datetime, suffix: str) -> str:
    return f"PO-{base_date.strftime('%Y%m%d')}-{suffix}"


def _build_reference_orders(base_date: datetime) -> list[dict[str, Any]]:
    orders: list[dict[str, Any]] = []
    for tpl in REFERENCE_LINE_ORDERS:
        due = base_date + timedelta(days=int(tpl["delivery_sla_days"]))
        status = "in_progress" if tpl.get("role") == "in_progress" else "released"
        orders.append({
            "production_order_id": _order_id(base_date, tpl["suffix"]),
            "factory_id": "JQHC-PLANT-01",
            "customer_order_id": f"SO-{tpl['order_type'].upper()}-{tpl['suffix']}",
            "product_id": tpl["product_id"],
            "product_category": tpl["product_category"],
            "grade": tpl["grade"],
            "recipe_id": tpl["recipe_id"],
            "assigned_line_ids": [tpl["line_id"]],
            "priority": tpl["priority"],
            "planned_quantity_t": tpl["planned_quantity_t"],
            "released_quantity_t": 0.0,
            "due_date": _iso(due),
            "status": status,
            "order_type": tpl["order_type"],
            "delivery_sla_days": tpl["delivery_sla_days"],
            "customer_segment": tpl["customer_segment"],
            "is_export": tpl.get("is_export", tpl["order_type"] == "export"),
            "remark": tpl["remark"],
            "plan_year": base_date.year,
        })
    return orders


def _build_line_orders(
    line: dict[str, Any],
    *,
    base_date: datetime,
    recipes_by_line: dict[str, list[dict[str, Any]]],
    products_by_grade: dict[tuple[str, str], dict[str, Any]],
) -> list[dict[str, Any]]:
    line_id = str(line["product_line_id"])
    if line_id in REFERENCE_SIM_LINES:
        return []

    category = str(line.get("product_category") or "flux_core_wire")
    profiles = LINE_ORDER_PROFILES.get(category, LINE_ORDER_PROFILES["solid_wire"])
    recipes = recipes_by_line.get(line_id, [])
    if not recipes:
        return []

    daily = _actual_daily_capacity(line)
    seed = _line_seed(line_id)
    code = _line_code(line_id)
    orders: list[dict[str, Any]] = []

    for seq, profile in enumerate(profiles[:2], start=1):
        prof = profiles[(seed + seq) % len(profiles)]
        recipe = _pick_recipe(recipes, prof["grade"])
        if recipe is None:
            continue
        grade = str(recipe.get("grade") or prof["grade"])
        qty_t = round(daily * prof["qty_days"] * (0.92 + (seed % 7) / 100.0), 1)
        qty_t = max(min(qty_t, daily * 21), daily * 5)
        due = base_date + timedelta(days=int(prof["sla"]) + seq * 2)
        role = "in_progress" if seq == 1 else "released"
        suffix = f"{code}-P{seq}"
        orders.append({
            "production_order_id": _order_id(base_date, suffix),
            "factory_id": str(line.get("factory_id") or "JQHC-PLANT-01"),
            "customer_order_id": f"SO-{prof['order_type'].upper()}-{suffix}",
            "product_id": _pick_product(products_by_grade, category, grade, recipe),
            "product_category": category,
            "grade": grade,
            "recipe_id": str(recipe["recipe_id"]),
            "assigned_line_ids": [line_id],
            "priority": prof["priority"],
            "planned_quantity_t": qty_t,
            "released_quantity_t": 0.0,
            "due_date": _iso(due),
            "status": role,
            "order_type": prof["order_type"],
            "delivery_sla_days": prof["sla"],
            "customer_segment": prof["segment"],
            "is_export": prof["order_type"] == "export",
            "remark": f"仿真订单 · {line.get('name', line_id)} · {prof['segment']}",
            "plan_year": base_date.year,
        })
    return orders


def _batch_output_kg(data: dict[str, Any], line_id: str) -> float:
    defaults = data.get("simulation_defaults", {})
    line_output = defaults.get("line_batch_output_kg", {})
    return float(line_output.get(line_id, defaults.get("batch_output_kg", 1200)))


def _parent_batches(category: str) -> list[str]:
    if category == "welding_rod":
        return ["ROD-20260520-C1", "FLUX-WR-20260501-A1"]
    if category == "flux_core_wire":
        return ["STRIP-20250604-A1", "FLUX-20250605-A1"]
    return ["ROD-20260520-C1"]


def _rebuild_reference_batches(
    data: dict[str, Any],
    orders: list[dict[str, Any]],
    base_date: datetime,
) -> list[dict[str, Any]]:
    """参考产线保留多批次历史 + 在制 + 排队."""
    batches: list[dict[str, Any]] = []

    def add_batch(**kwargs: Any) -> None:
        batches.append(kwargs)

    # FCW-LINE-07
    fcw_export = next(o for o in orders if "FCW-EXPORT-HY830" in o["production_order_id"])
    fcw_dom = next(o for o in orders if "FCW-DOM-HY780" in o["production_order_id"])
    fcw_custom = next(o for o in orders if "FCW-LNG-CUSTOM" in o["production_order_id"])
    add_batch(
        batch_id="FCW-20250604-B2", factory_id="JQHC-PLANT-01", workshop_id="WS-WIRE-01",
        product_line_id="FCW-LINE-07", production_order_id=fcw_export["production_order_id"],
        product_category="flux_core_wire", recipe_id="FCW-HY830-V3", grade="HY-830MPa",
        shift="day", status="completed",
        started_at=_iso(base_date - timedelta(days=2, hours=8)),
        ended_at=_iso(base_date - timedelta(days=1, hours=18)),
        quantity_kg=1180, parent_batches=_parent_batches("flux_core_wire"),
    )
    add_batch(
        batch_id="FCW-20250604-B1", factory_id="JQHC-PLANT-01", workshop_id="WS-WIRE-01",
        product_line_id="FCW-LINE-07", production_order_id=fcw_dom["production_order_id"],
        product_category="flux_core_wire", recipe_id="FCW-HY780-V2", grade="HY-780MPa",
        shift="night", status="completed",
        started_at=_iso(base_date - timedelta(days=1, hours=20)),
        ended_at=_iso(base_date - timedelta(hours=4)),
        quantity_kg=1180, parent_batches=_parent_batches("flux_core_wire"),
    )
    add_batch(
        batch_id="FCW-20250605-B2", factory_id="JQHC-PLANT-01", workshop_id="WS-WIRE-01",
        product_line_id="FCW-LINE-07", production_order_id=fcw_export["production_order_id"],
        product_category="flux_core_wire", recipe_id="FCW-HY830-V3", grade="HY-830MPa",
        shift="day", status="in_progress",
        started_at=_iso(base_date - timedelta(hours=8)),
        quantity_kg=1200, parent_batches=_parent_batches("flux_core_wire"),
    )
    add_batch(
        batch_id="FCW-20250606-B3", factory_id="JQHC-PLANT-01", workshop_id="WS-WIRE-01",
        product_line_id="FCW-LINE-07", production_order_id=fcw_custom["production_order_id"],
        product_category="flux_core_wire", recipe_id="FCW-HY960-V1", grade="HY-960MPa",
        shift="day", status="released",
        started_at=_iso(base_date + timedelta(hours=6)),
        quantity_kg=680, parent_batches=_parent_batches("flux_core_wire"),
    )

    # SW-LINE-02
    sw_dom = next(o for o in orders if "SW-DOM-ER50" in o["production_order_id"])
    sw_export = next(o for o in orders if "SW-EXPORT-ER70" in o["production_order_id"])
    add_batch(
        batch_id="SW-20250603-B1", factory_id="JQHC-PLANT-01", workshop_id="WS-WIRE-01",
        product_line_id="SW-LINE-02", production_order_id=sw_dom["production_order_id"],
        product_category="solid_wire", recipe_id="SW-ER50-V2", grade="ER50-6",
        shift="night", status="completed",
        started_at=_iso(base_date - timedelta(days=3, hours=4)),
        ended_at=_iso(base_date - timedelta(days=2, hours=20)),
        quantity_kg=1050, parent_batches=_parent_batches("solid_wire"),
    )
    add_batch(
        batch_id="SW-20250605-B1", factory_id="JQHC-PLANT-01", workshop_id="WS-WIRE-01",
        product_line_id="SW-LINE-02", production_order_id=sw_dom["production_order_id"],
        product_category="solid_wire", recipe_id="SW-ER50-V2", grade="ER50-6",
        shift="day", status="in_progress",
        started_at=_iso(base_date - timedelta(hours=6)),
        quantity_kg=1000, parent_batches=_parent_batches("solid_wire"),
    )
    add_batch(
        batch_id="SW-20250606-B2", factory_id="JQHC-PLANT-01", workshop_id="WS-WIRE-01",
        product_line_id="SW-LINE-02", production_order_id=sw_export["production_order_id"],
        product_category="solid_wire", recipe_id="SW-ER70-V1", grade="ER70S-6",
        shift="day", status="released",
        started_at=_iso(base_date + timedelta(hours=4)),
        quantity_kg=920, parent_batches=_parent_batches("solid_wire"),
    )

    # WR-LINE-01 — P0 修复：补 in_progress 批次
    wr_main = next(o for o in orders if "WR-HYDRO-E7014" in o["production_order_id"])
    wr_export = next(o for o in orders if "WR-EXPORT-E7018" in o["production_order_id"])
    add_batch(
        batch_id="WR-20260528-B2", factory_id="JQHC-PLANT-01", workshop_id="WS-ROD-01",
        product_line_id="WR-LINE-01", production_order_id=wr_main["production_order_id"],
        product_category="welding_rod", recipe_id="WR-E7014-V1", grade="E7014",
        shift="day", status="completed",
        started_at=_iso(base_date - timedelta(days=9, hours=8)),
        ended_at=_iso(base_date - timedelta(days=9)),
        quantity_kg=800, parent_batches=_parent_batches("welding_rod"),
    )
    add_batch(
        batch_id="WR01-20250606-B1", factory_id="JQHC-PLANT-01", workshop_id="WS-ROD-01",
        product_line_id="WR-LINE-01", production_order_id=wr_main["production_order_id"],
        product_category="welding_rod", recipe_id="WR-E7014-V1", grade="E7014",
        shift="day", status="in_progress",
        started_at=_iso(base_date - timedelta(hours=5)),
        quantity_kg=800, parent_batches=_parent_batches("welding_rod"),
    )
    add_batch(
        batch_id="WR01-20250606-B2", factory_id="JQHC-PLANT-01", workshop_id="WS-ROD-01",
        product_line_id="WR-LINE-01", production_order_id=wr_export["production_order_id"],
        product_category="welding_rod", recipe_id="WR-E7014-V1", grade="E7014",
        shift="day", status="released",
        started_at=_iso(base_date + timedelta(hours=8)),
        quantity_kg=760, parent_batches=_parent_batches("welding_rod"),
    )
    return batches


def _rebuild_standard_line_batches(
    data: dict[str, Any],
    line: dict[str, Any],
    line_orders: list[dict[str, Any]],
    base_date: datetime,
) -> list[dict[str, Any]]:
    line_id = str(line["product_line_id"])
    code = _line_code(line_id)
    category = str(line.get("product_category"))
    output_kg = _batch_output_kg(data, line_id)
    batches: list[dict[str, Any]] = []
    primary = next((o for o in line_orders if o.get("status") == "in_progress"), line_orders[0] if line_orders else None)
    if primary is None:
        return batches

    seed = _line_seed(line_id)
    completed_count = 1 if seed % 3 != 0 else 0
    if completed_count:
        batches.append({
            "batch_id": f"{code}-{base_date.strftime('%Y%m%d')}-B0",
            "factory_id": str(line.get("factory_id") or "JQHC-PLANT-01"),
            "workshop_id": line.get("workshop_id"),
            "product_line_id": line_id,
            "production_order_id": primary["production_order_id"],
            "product_category": category,
            "recipe_id": primary["recipe_id"],
            "grade": primary["grade"],
            "shift": "night",
            "status": "completed",
            "started_at": _iso(base_date - timedelta(days=1, hours=10)),
            "ended_at": _iso(base_date - timedelta(hours=14)),
            "quantity_kg": output_kg * 0.95,
            "parent_batches": _parent_batches(category),
        })

    batches.append({
        "batch_id": f"{code}-{base_date.strftime('%Y%m%d')}-B1",
        "factory_id": str(line.get("factory_id") or "JQHC-PLANT-01"),
        "workshop_id": line.get("workshop_id"),
        "product_line_id": line_id,
        "production_order_id": primary["production_order_id"],
        "product_category": category,
        "recipe_id": primary["recipe_id"],
        "grade": primary["grade"],
        "shift": "day",
        "status": "in_progress",
        "started_at": _iso(base_date - timedelta(hours=4 + seed % 5)),
        "quantity_kg": output_kg,
        "parent_batches": _parent_batches(category),
    })
    return batches


def _sync_released_from_batches(orders: list[dict[str, Any]], batches: list[dict[str, Any]]) -> None:
    """P0: released_quantity_t = 在制+完成+待发运批次吨数（与 IngestService 一致）."""
    progress_statuses = {"in_progress", "completed", "ready_to_ship"}
    kg_by_order: dict[str, float] = {}
    for batch in batches:
        oid = str(batch.get("production_order_id") or "")
        status = str(batch.get("status") or "")
        if not oid or status not in progress_statuses:
            continue
        kg_by_order[oid] = kg_by_order.get(oid, 0.0) + float(batch.get("quantity_kg") or 0)

    for order in orders:
        oid = str(order.get("production_order_id"))
        order["released_quantity_t"] = round(kg_by_order.get(oid, 0.0) / 1000.0, 2)


def _sort_orders(orders: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return sorted(
        orders,
        key=lambda o: (
            PRIORITY_RANK.get(str(o.get("priority")), 1),
            str(o.get("due_date", "")),
        ),
    )


def _patch_simulation_defaults(data: dict[str, Any]) -> None:
    defaults = data.setdefault("simulation_defaults", {})
    defaults["changeover_dwell_ticks"] = int(defaults.get("changeover_dwell_ticks", 35))
    defaults["changeover_rules"] = defaults.get("changeover_rules") or {
        "same_recipe": 0,
        "same_grade": 22,
        "diff_recipe": 30,
        "diff_grade": 45,
    }
    defaults["order_release"] = defaults.get("order_release") or {
        "enabled": True,
        "release_batches_per_shift": 3.0,
        "hold_ratio_released": 0.55,
        "hold_ratio_in_progress": 0.15,
        "note": "APS 按班次分批下达，非一次性释放全部计划量",
    }
    defaults["order_arrival"] = defaults.get("order_arrival") or {
        "enabled": True,
        "base_orders_per_day": 0.35,
        "max_open_orders_per_line": 4,
        "note": "Poisson 动态到达；设计产能非满产，到达率按实际负荷校准",
    }
    defaults["order_generation"] = {
        "design_annual_t": DESIGN_ANNUAL_T,
        "category_actual_utilization": CATEGORY_ACTUAL_UTILIZATION,
        "note": "设计产能为工厂上限；订单按实际利用率折减，非满产运行",
    }
    scenarios = defaults.setdefault("scenarios", [])
    existing_ids = {s.get("id") for s in scenarios if isinstance(s, dict)}
    if "custom_insert" not in existing_ids:
        scenarios.append({
            "id": "custom_insert",
            "label": "定制插单",
            "description": "运行中向 FCW-07 插入 HY-960 高优先级定制单",
        })


def validate_order_chain(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    lines = {
        l["product_line_id"]: l
        for l in data.get("product_lines", [])
        if l.get("simulation_enabled")
    }
    orders = data.get("production_orders", [])
    batches = data.get("product_batches", [])

    for lid in sorted(lines):
        line_orders = [o for o in orders if lid in o.get("assigned_line_ids", [])]
        if not line_orders:
            errors.append(f"{lid}: 无 assigned 订单")
            continue
        ip_batches = [
            b for b in batches
            if b.get("product_line_id") == lid and b.get("status") == "in_progress"
        ]
        if not ip_batches:
            errors.append(f"{lid}: 无 in_progress 批次 (D03)")

    for order in orders:
        oid = order.get("production_order_id")
        if not order.get("delivery_sla_days"):
            errors.append(f"{oid}: 缺少 delivery_sla_days")
        due = order.get("due_date")
        if due:
            try:
                due_dt = datetime.fromisoformat(str(due).replace("Z", "+00:00"))
                if due_dt <= datetime.now(timezone.utc):
                    errors.append(f"{oid}: due_date 已过期")
            except ValueError:
                errors.append(f"{oid}: due_date 格式错误")

    return errors


def patch_master(data: dict[str, Any], base_date: datetime | None = None) -> dict[str, Any]:
    now = base_date or datetime.now(timezone.utc)
    recipes_by_line = _recipes_by_line(data)
    products_by_grade = _products_by_grade(data)

    orders: list[dict[str, Any]] = []
    orders.extend(_build_reference_orders(now))
    sim_lines = [
        l for l in data.get("product_lines", [])
        if l.get("simulation_enabled")
    ]
    for line in sorted(sim_lines, key=lambda l: str(l.get("product_line_id"))):
        orders.extend(_build_line_orders(line, base_date=now, recipes_by_line=recipes_by_line, products_by_grade=products_by_grade))

    orders = _sort_orders(orders)
    batches: list[dict[str, Any]] = []
    batches.extend(_rebuild_reference_batches(data, orders, now))
    for line in sim_lines:
        lid = str(line["product_line_id"])
        if lid in REFERENCE_SIM_LINES:
            continue
        line_orders = [o for o in orders if lid in o.get("assigned_line_ids", [])]
        batches.extend(_rebuild_standard_line_batches(data, line, line_orders, now))

    _sync_released_from_batches(orders, batches)
    _patch_simulation_defaults(data)
    data["production_orders"] = orders
    data["product_batches"] = batches
    data["updated_at"] = _iso(now)
    return data


def main() -> int:
    parser = argparse.ArgumentParser(description="生成/刷新 production_orders 与对齐批次")
    parser.add_argument("master_path", nargs="?", default=str(MASTER_PATH))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    path = Path(args.master_path)
    data = patch_master(load_master(path))
    errors = validate_order_chain(data)
    if errors:
        print("VALIDATION WARNINGS:")
        for err in errors:
            print(f"  - {err}")
    if args.dry_run:
        print(f"[dry-run] orders={len(data['production_orders'])}, batches={len(data['product_batches'])}")
        return 0 if not errors else 1
    save_master(data, path)
    print(
        f"Updated {path.name}: orders={len(data['production_orders'])}, "
        f"batches={len(data['product_batches'])}, sim_lines="
        f"{sum(1 for l in data.get('product_lines', []) if l.get('simulation_enabled'))}"
    )
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
