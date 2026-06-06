"""Load jqhc-factory-master-data.json for simulator."""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any


DEFAULT_MASTER_PATH = (
    Path(__file__).resolve().parents[2]
    / "schemas"
    / "智造数据台"
    / "presets"
    / "jqhc-factory-master-data.json"
)


@lru_cache(maxsize=1)
def load_master_data(path: str | None = None) -> dict[str, Any]:
    master_path = Path(path) if path else DEFAULT_MASTER_PATH
    with master_path.open(encoding="utf-8") as f:
        return json.load(f)


def get_line(line_id: str, master: dict[str, Any] | None = None) -> dict[str, Any]:
    data = master or load_master_data()
    for line in data.get("product_lines", []):
        if line.get("product_line_id") == line_id:
            return line
    raise KeyError(f"product line not found: {line_id}")


def get_line_equipment(line_id: str, master: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    data = master or load_master_data()
    return [eq for eq in data.get("equipment", []) if eq.get("product_line_id") == line_id and eq.get("enabled", True)]


def get_line_order(line_id: str, master: dict[str, Any] | None = None) -> dict[str, Any] | None:
    orders = get_line_orders(line_id, master)
    for order in orders:
        if str(order.get("status")) == "in_progress":
            return order
    return orders[0] if orders else None


_PRIORITY_RANK = {"high": 0, "normal": 1, "low": 2}


def get_line_orders(line_id: str, master: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    data = master or load_master_data()
    orders = [dict(order) for order in data.get("production_orders", []) if line_id in order.get("assigned_line_ids", [])]
    return sorted(
        orders,
        key=lambda o: (
            _PRIORITY_RANK.get(str(o.get("priority")), 1),
            str(o.get("due_date", "")),
        ),
    )


def get_factory_id(master: dict[str, Any] | None = None) -> str:
    data = master or load_master_data()
    return str(data.get("factory_id", "JQHC-PLANT-01"))


def get_line_context(line_id: str, master: dict[str, Any] | None = None) -> dict[str, str]:
    line = get_line(line_id, master)
    return {
        "factory_id": str(line.get("factory_id") or get_factory_id(master)),
        "workshop_id": str(line.get("workshop_id") or ""),
    }


def get_telemetry_profiles(master: dict[str, Any] | None = None) -> dict[str, Any]:
    data = master or load_master_data()
    return data.get("equipment_telemetry_profiles", {})


def get_simulation_defaults(master: dict[str, Any] | None = None) -> dict[str, Any]:
    data = master or load_master_data()
    return data.get("simulation_defaults", {})


def get_initial_batch(line_id: str, master: dict[str, Any] | None = None) -> dict[str, Any] | None:
    """优先返回 in_progress，其次 released，避免多批次产线取到历史批."""
    data = master or load_master_data()
    line_batches = [
        b for b in data.get("product_batches", [])
        if b.get("product_line_id") == line_id
    ]
    for status in ("in_progress", "released"):
        for batch in line_batches:
            if batch.get("status") == status:
                return dict(batch)
    return dict(line_batches[0]) if line_batches else None


def get_initial_wip_step(line_id: str, master: dict[str, Any] | None = None) -> str | None:
    data = master or load_master_data()
    snapshots = data.get("inventory_snapshots", {})
    for wip in snapshots.get("wip", []):
        loc = str(wip.get("location", ""))
        if loc.startswith(line_id):
            return wip.get("process_step_id")
    return None


def get_product(product_id: str, master: dict[str, Any] | None = None) -> dict[str, Any] | None:
    master = master or load_master_data()
    for product in master.get("products", []):
        if product.get("product_id") == product_id:
            return product
    return None


def get_product_for_order(order_id: str | None, master: dict[str, Any] | None = None) -> dict[str, Any] | None:
    if not order_id:
        return None
    master = master or load_master_data()
    for order in master.get("production_orders", []):
        if order.get("production_order_id") == order_id:
            pid = order.get("product_id")
            if pid:
                return get_product(str(pid), master)
    return None


def get_recipe(recipe_id: str, master: dict[str, Any] | None = None) -> dict[str, Any] | None:
    data = master or load_master_data()
    for recipe in data.get("recipes", []):
        if recipe.get("recipe_id") == recipe_id:
            return dict(recipe)
    return None


def get_scenarios(master: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    defaults = get_simulation_defaults(master)
    return list(defaults.get("scenarios", []))


def get_alarm_catalog(master: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    defaults = get_simulation_defaults(master)
    return list(defaults.get("alarm_catalog", []))


def equipment_by_step(line_id: str, master: dict[str, Any] | None = None) -> dict[str, dict[str, Any]]:
    mapping: dict[str, dict[str, Any]] = {}
    for eq in get_line_equipment(line_id, master):
        step = eq.get("process_step_id")
        if step and step not in mapping:
            mapping[str(step)] = dict(eq)
    return mapping


def get_raw_material_inventory(master: dict[str, Any] | None = None) -> dict[str, float]:
    """原料批次可用量（batch_id → kg），来自 material_batches."""
    data = master or load_master_data()
    inventory: dict[str, float] = {}
    for batch in data.get("material_batches", []):
        if str(batch.get("material_class", "")) != "raw":
            continue
        batch_id = str(batch.get("batch_id", ""))
        if not batch_id:
            continue
        inventory[batch_id] = float(batch.get("quantity_kg") or 0)
    return inventory


def step_equipment_map(line_id: str, master: dict[str, Any] | None = None) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for eq in get_line_equipment(line_id, master):
        step = eq.get("process_step_id")
        if step and step not in mapping:
            mapping[str(step)] = str(eq.get("equipment_id"))
    return mapping
