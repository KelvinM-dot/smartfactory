"""订单驱动生产场景剧本：export_rush / custom_insert 等."""

from __future__ import annotations

import uuid
from datetime import datetime, timedelta, timezone
from typing import Any

ORDER_SCENARIO_CONFIG: dict[str, dict[str, Any]] = {
    "normal_shift": {
        "arrival_rate_multiplier": 1.0,
        "export_order_weight": 0.22,
        "release_batches_per_shift_multiplier": 1.0,
    },
    "export_rush": {
        "arrival_rate_multiplier": 2.8,
        "export_order_weight": 0.62,
        "release_batches_per_shift_multiplier": 1.6,
        "description": "出口订单集中到达，交期 15–22 天，产线负荷升高",
    },
    "custom_insert": {
        "arrival_rate_multiplier": 0.7,
        "export_order_weight": 0.15,
        "release_batches_per_shift_multiplier": 1.2,
        "inject_on_start": [
            {
                "line_id": "FCW-LINE-07",
                "suffix": "FCW-INSERT-HY960",
                "product_id": "PROD-FCW-HY960-16",
                "product_category": "flux_core_wire",
                "grade": "HY-960MPa",
                "recipe_id": "FCW-HY960-V1",
                "planned_quantity_t": 28,
                "priority": "high",
                "order_type": "custom",
                "delivery_sla_days": 35,
                "customer_segment": "LNG储罐",
                "remark": "场景插单 · 高牌号定制 HY-960",
            }
        ],
    },
    "rod_peak_season": {
        "arrival_rate_multiplier": 1.8,
        "export_order_weight": 0.18,
        "release_batches_per_shift_multiplier": 1.4,
        "category_arrival_bias": {"welding_rod": 0.75},
    },
    "hydro_project_custom": {
        "arrival_rate_multiplier": 0.5,
        "export_order_weight": 0.35,
        "release_batches_per_shift_multiplier": 1.1,
    },
}


def scenario_config(scenario_id: str) -> dict[str, Any]:
    return ORDER_SCENARIO_CONFIG.get(scenario_id, ORDER_SCENARIO_CONFIG["normal_shift"])


def _iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def build_inject_order(template: dict[str, Any], base_date: datetime | None = None) -> dict[str, Any]:
    now = base_date or datetime.now(timezone.utc)
    suffix = f"{template['suffix']}-{uuid.uuid4().hex[:6].upper()}"
    sla = int(template.get("delivery_sla_days", 30))
    return {
        "production_order_id": f"PO-{now.strftime('%Y%m%d')}-{suffix}",
        "factory_id": "JQHC-PLANT-01",
        "customer_order_id": f"SO-{template.get('order_type', 'CUSTOM').upper()}-{suffix}",
        "product_id": template["product_id"],
        "product_category": template["product_category"],
        "grade": template["grade"],
        "recipe_id": template["recipe_id"],
        "assigned_line_ids": [template["line_id"]],
        "priority": template.get("priority", "high"),
        "planned_quantity_t": float(template.get("planned_quantity_t", 20)),
        "released_quantity_t": 0.0,
        "due_date": _iso(now + timedelta(days=sla)),
        "status": "released",
        "order_type": template.get("order_type", "custom"),
        "delivery_sla_days": sla,
        "customer_segment": template.get("customer_segment", "定制"),
        "is_export": template.get("order_type") == "export",
        "remark": template.get("remark", "scenario_inject"),
        "plan_year": now.year,
        "scenario_injected": True,
    }


def apply_scenario_on_start(plant: Any, scenario_id: str) -> list[dict[str, Any]]:
    """启动/切换场景时注入插单等到产线队列."""
    cfg = scenario_config(scenario_id)
    injected: list[dict[str, Any]] = []
    for template in cfg.get("inject_on_start") or []:
        order = build_inject_order(template)
        line_id = template["line_id"]
        eng = plant.engines.get(line_id)
        if eng is None:
            continue
        oid = order["production_order_id"]
        eng.process.order_queue.insert(0, order)
        eng.process.order_aps_pool_kg[oid] = order["planned_quantity_t"] * 1000.0 * 0.4
        eng.process.order_remaining_kg[oid] = order["planned_quantity_t"] * 1000.0 * 0.6
        injected.append(order)
    return injected
