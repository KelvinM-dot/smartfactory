"""综合能源动态模型：光伏日曲线 + 风电波动 + 余热回收."""

from __future__ import annotations

import math
import random
from datetime import datetime, timezone
from typing import Any


def _pv_curve(hour: float) -> float:
    """6:00–18:00 钟形光伏出力，0–1."""
    if hour < 6 or hour > 18:
        return 0.0
    x = (hour - 12) / 6.0
    return max(0.0, math.cos(x * math.pi / 2) ** 2)


def _wind_factor(seed: int | None = None) -> float:
    rng = random.Random(seed)
    return 0.35 + rng.random() * 0.45


def _parse_plant_green_shift(scenario_id: str | None) -> float | None:
    if not scenario_id or not scenario_id.startswith("plant_green_shift_"):
        return None
    suffix = scenario_id.removeprefix("plant_green_shift_")
    if suffix in {"custom", "dynamic"}:
        return None
    try:
        return float(suffix)
    except ValueError:
        return None


def green_power_ratio(
    master: dict[str, Any],
    *,
    scenario_id: str | None = None,
    process_step_id: str | None = None,
    at: datetime | None = None,
    green_shift_pct: float | None = None,
) -> float:
    """返回当前时刻绿电占产线用电比例 0–1."""
    defaults = master.get("simulation_defaults", {})
    assets = master.get("energy_assets", [])
    profile = master.get("factory_profile", {})
    delivery = profile.get("delivery_sla", {})

    pv_kw = sum(float(a.get("rated_power_kw") or 0) for a in assets if a.get("asset_type") == "pv")
    wind_kw = sum(float(a.get("rated_power_kw") or 0) for a in assets if a.get("asset_type") == "wind")
    total_gen_kw = max(pv_kw + wind_kw, 1.0)

    now = at or datetime.now(timezone.utc)
    local_hour = (now.hour + 8) % 24  # 天津 UTC+8 近似
    pv_share = pv_kw / total_gen_kw * _pv_curve(float(local_hour))
    wind_share = wind_kw / total_gen_kw * _wind_factor(now.timetuple().tm_yday)

    base_ratio = float(defaults.get("default_green_power_ratio_pct", 60.0)) / 100.0
    dynamic = min(0.92, max(0.18, pv_share + wind_share + base_ratio * 0.35))

    if scenario_id == "clean_energy_noon" and 11 <= local_hour <= 14:
        dynamic = min(0.95, dynamic + 0.22)

    shift_pct = green_shift_pct
    if shift_pct is None:
        shift_pct = _parse_plant_green_shift(scenario_id)
    if shift_pct is not None:
        # 全厂绿电排产偏移：50% 为基准，每 +10% 偏移约 +2.2% 绿电占比
        dynamic = min(0.96, dynamic + (shift_pct - 50.0) / 100.0 * 0.22)

    whr_assets = [a for a in assets if a.get("asset_type") == "waste_heat"]
    if whr_assets and process_step_id == "drying":
        saving = float(whr_assets[0].get("energy_saving_pct", 30)) / 100.0
        dynamic = min(0.96, dynamic + saving * 0.15)

    target_coverage = float(profile.get("market", {}).get("export_share_pct", 40)) / 100.0
    _ = target_coverage  # 保留扩展：出口旺季微调
    _ = delivery
    return round(dynamic, 4)

