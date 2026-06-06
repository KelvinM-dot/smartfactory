"""APS 下达窗口：按班次将计划量分批 release 到产线可执行池."""

from __future__ import annotations

from typing import Any


def init_release_state(
    order_queue: list[dict[str, Any]],
    *,
    release_hold_ratio_released: float = 0.55,
    release_hold_ratio_in_progress: float = 0.15,
) -> tuple[dict[str, float], dict[str, float]]:
    """
    返回 (order_remaining_kg, order_aps_pool_kg).

    - order_remaining_kg: 产线当前可排产量 (kg)
    - order_aps_pool_kg: 计划员尚未下达量 (kg)
    """
    remaining: dict[str, float] = {}
    aps_pool: dict[str, float] = {}
    for item in order_queue:
        oid = str(item.get("production_order_id") or "")
        if not oid:
            continue
        planned_kg = float(item.get("planned_quantity_t") or 0.0) * 1000.0
        erp_released_kg = float(item.get("released_quantity_t") or 0.0) * 1000.0
        not_on_floor_kg = max(planned_kg - erp_released_kg, 0.0)
        status = str(item.get("status") or "released")
        hold_ratio = release_hold_ratio_in_progress if status == "in_progress" else release_hold_ratio_released
        pool_kg = not_on_floor_kg * hold_ratio
        floor_kg = max(not_on_floor_kg - pool_kg, 0.0)
        aps_pool[oid] = pool_kg
        remaining[oid] = floor_kg
    return remaining, aps_pool


def apply_shift_release(
    *,
    order_queue: list[dict[str, Any]],
    order_remaining_kg: dict[str, float],
    order_aps_pool_kg: dict[str, float],
    default_batch_output_kg: float,
    release_batches_per_shift: float,
) -> list[dict[str, Any]]:
    """班次边界释放一批计划量到产线可执行池，返回释放事件元数据."""
    released_events: list[dict[str, Any]] = []
    chunk_kg = max(release_batches_per_shift, 0.5) * default_batch_output_kg
    for item in order_queue:
        oid = str(item.get("production_order_id") or "")
        status = str(item.get("status") or "")
        if not oid or status in {"completed", "blocked"}:
            continue
        pool = order_aps_pool_kg.get(oid, 0.0)
        if pool <= 0:
            continue
        amount = min(pool, chunk_kg)
        order_aps_pool_kg[oid] = max(pool - amount, 0.0)
        order_remaining_kg[oid] = order_remaining_kg.get(oid, 0.0) + amount
        released_events.append({
            "production_order_id": oid,
            "released_kg": round(amount, 1),
            "aps_pool_remaining_kg": round(order_aps_pool_kg[oid], 1),
        })
    return released_events


def resolve_changeover_ticks(
    *,
    prev_recipe: str,
    prev_grade: str,
    new_recipe: str,
    new_grade: str,
    rules: dict[str, Any],
    default_ticks: int,
) -> int:
    if not prev_recipe or prev_recipe == new_recipe:
        return int(rules.get("same_recipe", 0))
    if prev_grade and prev_grade == new_grade:
        return int(rules.get("same_grade", rules.get("diff_recipe", default_ticks)))
    return int(rules.get("diff_grade", default_ticks))
