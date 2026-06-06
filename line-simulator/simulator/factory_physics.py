"""工艺物理与工厂运行启发式模型（配方、产能、班次、质量判定）."""

from __future__ import annotations

import random
from typing import Any

FIELD_RECIPE_ALIASES: dict[str, str] = {
    "coating_thickness_mm": "coating_thickness_um",
    "actual_temp_C": "drying_temp_C",
    "line_speed_m_per_min": "drawing_speed_m_min",
}


def recipe_target(recipe: dict[str, Any] | None, field_id: str) -> float | None:
    if not recipe:
        return None
    params = recipe.get("target_parameters") or {}
    if field_id in params:
        return float(params[field_id])
    alias = FIELD_RECIPE_ALIASES.get(field_id)
    if alias and alias in params:
        raw = float(params[alias])
        if field_id == "coating_thickness_mm" and alias == "coating_thickness_um":
            return raw / 1000.0 if raw > 5 else raw
        return raw
    return None


def ticks_for_step(
    quantity_kg: float,
    rated_capacity_per_hour: float | None,
    base_ticks: int = 25,
) -> int:
    """批次产量与设备额定产能决定工序驻留 tick 数."""
    cap = float(rated_capacity_per_hour or 1000)
    load = quantity_kg / max(cap, 50.0)
    return max(8, min(150, int(base_ticks * (0.65 + load * 14))))


def resolve_shift(sim_tick: int, ticks_per_shift: int = 400) -> str:
    bucket = max(int(ticks_per_shift), 50)
    return "night" if (sim_tick // bucket) % 2 else "day"


def quality_decision(
    pass_rate_pct: float,
    *,
    force_hold: bool = False,
    spec_violation: bool = False,
) -> str:
    if force_hold:
        return "hold"
    rate = float(pass_rate_pct)
    if spec_violation:
        rate = max(0.0, rate - 35.0)
    roll = random.random() * 100.0
    if roll < rate:
        return "pass"
    if roll < rate + (100.0 - rate) * 0.55:
        return "hold"
    return "rework"


def pick_alarm_from_catalog(
    catalog: list[dict[str, Any]],
    *,
    equipment_type: str | None = None,
    step_id: str | None = None,
) -> dict[str, Any]:
    pool = catalog or []
    if equipment_type:
        typed = [
            item for item in pool
            if equipment_type in (item.get("equipment_types") or [])
            or step_id in (item.get("process_steps") or [])
        ]
        if typed:
            pool = typed
    if not pool:
        return {"code": "E101", "message": "断丝", "severity": "critical"}
    return dict(random.choice(pool))


# 设备状态 → 功率系数（精益能耗 / OEE 基础）
STATUS_POWER_FACTOR: dict[str, float] = {
    "RUNNING": 1.0,
    "ALARM": 0.35,
    "MANUAL": 0.25,
    "STOPPED": 0.08,
    "IDLE": 0.12,
}

# 设备类型基础功率 kW（无额定产能时的回退）
EQUIPMENT_BASE_KW: dict[str, float] = {
    "strip_cutter": 18.0,
    "mixer": 22.0,
    "filler": 28.0,
    "wire_drawer": 35.0,
    "plater": 42.0,
    "winder": 15.0,
    "packer": 8.0,
    "agv_station": 2.5,
    "cutter": 20.0,
    "coater": 26.0,
    "dryer": 38.0,
}


def equipment_power_kw(
    equipment_type: str,
    status: str,
    *,
    rated_capacity_per_hour: float | None = None,
    is_active_step: bool = False,
) -> float:
    """按设备类型、运行状态与产能负载估算瞬时功率."""
    base = EQUIPMENT_BASE_KW.get(equipment_type, 12.0)
    cap = float(rated_capacity_per_hour or 1000)
    load_factor = min(1.15, 0.55 + cap / 1800.0)
    if is_active_step and status == "RUNNING":
        load_factor = min(1.25, load_factor * 1.08)
    factor = STATUS_POWER_FACTOR.get(status, 0.1)
    return round(base * load_factor * factor, 2)


def mechanical_quality_results(
    recipe: dict[str, Any] | None,
    *,
    spec_violation: bool = False,
    mechanical_spec: dict[str, Any] | None = None,
) -> dict[str, float]:
    """根据配方目标生成终检力学性能（Rm/KV2/延伸率/I值）."""
    params = (recipe or {}).get("target_parameters") or {}
    ms = mechanical_spec or {}
    jitter = random.uniform(-0.04, 0.04) if not spec_violation else random.uniform(-0.12, -0.02)
    rm = float(params.get("rm_mpa_target") or ms.get("rm_mpa_min") or 610)
    kv2 = float(params.get("impact_kv2_j_target") or ms.get("impact_kv2_j_min") or 69)
    elong = float(params.get("elongation_pct_target") or ms.get("elongation_pct_min") or 20.0)
    i_val = float(params.get("corrosion_i_value_target") or ms.get("corrosion_i_value_min") or 6.0)
    if spec_violation:
        rm *= 0.96
        kv2 *= 0.92
        elong *= 0.94
        i_val *= 0.97
    return {
        "rm_mpa": round(rm * (1 + jitter), 1),
        "impact_kv2_j": round(kv2 * (1 + jitter * 0.5), 1),
        "elongation_pct": round(elong * (1 + jitter * 0.3), 2),
        "corrosion_i_value": round(i_val * (1 + jitter * 0.2), 2),
    }


def evaluate_mechanical_against_spec(
    mech: dict[str, float],
    mechanical_spec: dict[str, Any] | None,
) -> tuple[bool, list[str]]:
    """对照产品 mechanical_spec 全项判定终检是否合格."""
    if not mechanical_spec:
        return True, []
    failures: list[str] = []
    checks = [
        ("rm_mpa", "rm_mpa_min", "抗拉强度"),
        ("impact_kv2_j", "impact_kv2_j_min", "冲击功"),
        ("elongation_pct", "elongation_pct_min", "延伸率"),
        ("corrosion_i_value", "corrosion_i_value_min", "I值"),
    ]
    for actual_key, min_key, label in checks:
        actual = mech.get(actual_key)
        minimum = mechanical_spec.get(min_key)
        if actual is None or minimum is None:
            continue
        if float(actual) < float(minimum):
            failures.append(f"{label} {actual} < {minimum}")
    return len(failures) == 0, failures


def coupling_delta(
    field_id: str,
    step_id: str,
    active_step: str,
    field_state: dict[str, float],
    *,
    step_equipment: dict[str, str] | None = None,
) -> float:
    """上游工序实测值对当前工序参数的微弱耦合."""
    if step_id != active_step:
        return 0.0
    step_equipment = step_equipment or {}
    # (上游工序, 上游字段, 目标字段, 增益系数, 归一化中心)
    links: list[tuple[str, str, str, float, float]] = [
        ("powder_mixing", "mixing_rpm", "fill_ratio_pct", 0.015, 30.0),
        ("filling_forming", "fill_ratio_pct", "tension_kN", 0.02, 18.5),
        ("rough_drawing", "tension_kN", "tension_kN", 0.03, 5.5),
        ("coating", "coating_thickness_mm", "moisture_pct", -0.08, 1.2),
        ("drying", "actual_temp_C", "moisture_pct", -0.012, 185.0),
    ]
    delta = 0.0
    for up_step, up_field, target_field, gain, center in links:
        if target_field != field_id:
            continue
        up_eq = step_equipment.get(up_step)
        if not up_eq:
            continue
        key = f"{up_eq}:{up_field}"
        up_val = field_state.get(key)
        if up_val is None:
            continue
        delta += gain * (float(up_val) - center) / max(abs(center), 1.0)
    return delta
