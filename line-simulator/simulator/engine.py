"""产线数据生成引擎（主数据驱动 + 全过程 WIP 模拟）."""

from __future__ import annotations

import random
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any

from simulator.factory_physics import (
    coupling_delta,
    equipment_power_kw,
    pick_alarm_from_catalog,
    recipe_target,
    resolve_shift,
)
from simulator.master_data import (
    equipment_by_step,
    get_alarm_catalog,
    get_factory_id,
    get_line,
    get_line_context,
    get_line_equipment,
    get_line_order,
    get_recipe,
    get_simulation_defaults,
    get_telemetry_profiles,
    load_master_data,
)
from simulator.energy_model import green_power_ratio
from simulator.order_scenarios import scenario_config
from simulator.config import (
    TELEMETRY_IDLE_EQUIPMENT_INTERVAL,
    TELEMETRY_SPARSE_ENABLED,
)
from simulator.process_simulator import ProcessState


@dataclass
class SimState:
    running: bool = False
    scenario_id: str = "normal_shift"
    speed_multiplier: float = 1.0
    line_id: str = "FCW-LINE-07"
    tick: int = 0
    records_sent: int = 0
    events_sent: int = 0
    plating_alarm_active: bool = False
    fill_drift: bool = False
    trace_gap: bool = False
    quality_hold: bool = False
    equipment_maintenance: bool = False
    field_state: dict[str, float] = field(default_factory=dict)
    active_equipment_alarms: dict[str, dict[str, Any]] = field(default_factory=dict)
    equipment_runtime: dict[str, dict[str, Any]] = field(default_factory=dict)
    ticks_per_shift: int = 400
    maintenance_dwell_ticks: int = 60
    green_shift_pct: float | None = None


class SimulationEngine:
    def __init__(
        self,
        master_path: str | None = None,
        line_id: str | None = None,
        line_mode: str | None = None,
    ) -> None:
        self.master = load_master_data(master_path)
        defaults = self.master.get("simulation_defaults", {})
        self.line_id = line_id or str(defaults.get("primary_line_id", "FCW-LINE-07"))
        line = get_line(self.line_id, self.master)
        self.context = get_line_context(self.line_id, self.master)
        self.order = get_line_order(self.line_id, self.master) or {}
        if line_mode is None:
            status = str(line.get("status", "active"))
            if status == "maintenance":
                line_mode = "maintenance"
            elif status == "inactive":
                line_mode = "static"
            else:
                line_mode = "active"
        self.line_mode = line_mode
        self.template_id = str(line.get("template_id", "flux_core_wire"))
        self.state = SimState(line_id=self.line_id)
        self.process = ProcessState.from_master(self.line_id, self.master)
        self._step_eq_map = {
            str(e.get("process_step_id")): str(e.get("equipment_id"))
            for e in get_line_equipment(self.line_id, self.master)
            if e.get("process_step_id")
        }
        if self.line_mode == "maintenance":
            self.process.ticks_per_step = int(defaults.get("maintenance_ticks_per_step", 80))
        elif self.line_mode == "static":
            self.process.ticks_per_step = 999_999
        self.profiles = get_telemetry_profiles(self.master)
        self.equipment = get_line_equipment(self.line_id, self.master)
        self._equipment_by_step = equipment_by_step(self.line_id, self.master)
        defaults = get_simulation_defaults(self.master)
        self.state.ticks_per_shift = int(defaults.get("ticks_per_shift", 400))
        self.state.maintenance_dwell_ticks = int(defaults.get("maintenance_dwell_ticks", 60))
        self._init_field_state()

    def _init_field_state(self) -> None:
        for eq_id, profile in self.profiles.items():
            for field_id, spec in profile.get("fields", {}).items():
                if isinstance(spec, dict) and "target" in spec:
                    self.state.field_state[f"{eq_id}:{field_id}"] = float(spec["target"])

    def configure(
        self,
        scenario_id: str,
        speed_multiplier: float,
        line_id: str | None = None,
        green_shift_pct: float | None = None,
    ) -> None:
        if line_id and line_id != self.line_id:
            self.line_id = line_id
            self.state.line_id = line_id
            line = get_line(line_id, self.master)
            self.context = get_line_context(line_id, self.master)
            self.order = get_line_order(line_id, self.master) or {}
            status = str(line.get("status", "active"))
            if status == "maintenance":
                self.line_mode = "maintenance"
            elif status == "inactive":
                self.line_mode = "static"
            else:
                self.line_mode = "active"
            self.template_id = str(line.get("template_id", "flux_core_wire"))
            self.process = ProcessState.from_master(line_id, self.master)
            defaults = self.master.get("simulation_defaults", {})
            if self.line_mode == "maintenance":
                self.process.ticks_per_step = int(defaults.get("maintenance_ticks_per_step", 80))
            elif self.line_mode == "static":
                self.process.ticks_per_step = 999_999
            self.equipment = get_line_equipment(line_id, self.master)
            self._equipment_by_step = equipment_by_step(line_id, self.master)
            self._init_field_state()

        self.state.scenario_id = scenario_id
        self.state.speed_multiplier = speed_multiplier
        self.state.green_shift_pct = green_shift_pct
        self.state.fill_drift = scenario_id == "fill_ratio_drift"
        self.state.trace_gap = scenario_id == "trace_gap"
        self.state.quality_hold = scenario_id == "quality_hold"
        self.state.equipment_maintenance = scenario_id == "equipment_maintenance"
        self.state.plating_alarm_active = False
        self.process.force_quality_hold = scenario_id == "quality_hold"
        self.process.spec_violation_active = scenario_id in {"fill_ratio_drift", "quality_hold"}
        self.process.raw_material_low = scenario_id == "raw_material_low"
        if scenario_id == "raw_material_low":
            self.process.apply_raw_material_low_scenario()
        if scenario_id == "equipment_maintenance":
            self.process.enter_maintenance_dwell(self.state.tick, "场景切换 · 计划维保")

        if scenario_id == "batch_handover":
            today = datetime.now(timezone.utc).strftime("%Y%m%d")
            self.process.batch_id = f"FCW-{today}-B{self.process.batch_seq + 1}"
            self.process.step_index = 0
            self.process.step_tick = 0
        if scenario_id == "ramp_up_fcw_2025" and self.line_id.startswith("FCW"):
            self.process.default_batch_output_kg = min(self.process.default_batch_output_kg * 1.15, 1600)
            self.process.quantity_kg = self.process.default_batch_output_kg
        if scenario_id == "rod_peak_season" and self.line_id.startswith("WR"):
            self.process.ticks_per_step = max(8, int(self.process.base_ticks_per_step * 0.75))
        order_scenario = scenario_config(scenario_id)
        release_mult = float(order_scenario.get("release_batches_per_shift_multiplier", 1.0))
        base_release = float(
            get_simulation_defaults(self.master).get("order_release", {}).get("release_batches_per_shift", 3.0)
        )
        self.process.release_batches_per_shift = round(base_release * release_mult, 2)
        if scenario_id == "export_rush":
            self.process.logistics_lead_ticks = max(4, int(self.process.logistics_lead_ticks * 0.6))
        if scenario_id == "hydro_project_custom":
            self.process.spec_violation_active = False
            self.process.grade = "HY-960MPa"
            self.process.recipe_id = "FCW-HY960-V1"

    def start(self) -> list[dict[str, Any]]:
        self.state.running = True
        self.state.tick = 0
        if self.line_mode == "static":
            return []
        self.process._initialized = False
        return self.process.initialize()

    def stop(self) -> None:
        self.state.running = False

    def inject_alarm(
        self,
        *,
        equipment_id: str | None = None,
        alarm_code: str | None = None,
        alarm_message: str | None = None,
        severity: str | None = None,
        duration_ticks: int = 30,
    ) -> dict[str, Any]:
        eq = equipment_id or self.process.current_equipment_id(self.master)
        if not eq:
            eq = self._default_alarm_equipment()
        catalog = get_alarm_catalog(self.master)
        picked = pick_alarm_from_catalog(
            catalog,
            equipment_type=str(self._equipment_by_step.get(self.process.current_step, {}).get("equipment_type", "")),
            step_id=self.process.current_step,
        )
        code = alarm_code or picked.get("code", "E101")
        message = alarm_message or picked.get("message", "设备异常")
        sev = severity or picked.get("severity", "warning")
        event = _alarm_event(self.line_id, self.process.batch_id, eq, code, message, sev)
        until_tick = self.state.tick + max(int(duration_ticks), 5)
        self.state.active_equipment_alarms[eq] = {
            "until_tick": until_tick,
            "event": event,
            "post_maintenance": True,
        }
        return event

    def _default_alarm_equipment(self) -> str:
        defaults = {
            "FCW-LINE-07": "PLATING-02",
            "SW-LINE-02": "PLATING-SW-01",
            "WR-LINE-01": "DRY-WR-01",
        }
        return defaults.get(self.line_id, "PLATING-02")

    def _alarms_from_material_events(self, material_events: list[dict[str, Any]]) -> list[dict[str, Any]]:
        alarms: list[dict[str, Any]] = []
        for ev in material_events:
            if ev.get("event_type") != "MATERIAL_SHORTAGE":
                continue
            eq = self.process.current_equipment_id(self.master) or self._default_alarm_equipment()
            if eq in self.state.active_equipment_alarms:
                continue
            remark = str(ev.get("remark") or "原料库存不足")
            alarm = _alarm_event(
                self.line_id,
                self.process.batch_id,
                eq,
                "E701",
                remark.replace("原料缺料 · ", "") if "原料缺料" in remark else "原料库存不足",
                "critical",
            )
            self.state.active_equipment_alarms[eq] = {
                "until_tick": self.state.tick + max(self.process.material_recovery_ticks, 15),
                "event": alarm,
                "post_maintenance": False,
            }
            alarms.append(alarm)
        return alarms

    def heartbeat_runtime(self) -> dict[str, Any]:
        energy = self.energy_snapshot()
        return {
            "dwell_mode": self.process.dwell_mode,
            "dwell_reason": self.process.dwell_reason,
            "dwell_until_tick": self.process.dwell_until_tick,
            "active_power_kw": energy.get("active_power_kw"),
            "running_equipment_count": energy.get("running_equipment_count"),
            "tick": self.state.tick,
            "current_step": self.process.current_step,
            "raw_material_low": self.process.raw_material_low,
        }

    def _clear_expired_alarms(self) -> list[dict[str, Any]]:
        events: list[dict[str, Any]] = []
        expired = [
            (eq, meta)
            for eq, meta in self.state.active_equipment_alarms.items()
            if self.state.tick >= meta["until_tick"]
        ]
        for eq, meta in expired:
            del self.state.active_equipment_alarms[eq]
            if meta.get("post_maintenance"):
                self.state.equipment_runtime[eq] = {
                    "mode": "MAINTENANCE",
                    "until_tick": self.state.tick + self.state.maintenance_dwell_ticks,
                    "reason": "报警后维保",
                }
                if not self.process.dwell_mode:
                    events.extend(
                        self.process.enter_maintenance_dwell(
                            self.state.tick,
                            f"设备 {eq} 报警后维保",
                        )
                    )
        self._clear_expired_equipment_runtime()
        return events

    def _clear_expired_equipment_runtime(self) -> None:
        expired = [
            eq for eq, meta in self.state.equipment_runtime.items()
            if self.state.tick >= meta.get("until_tick", 0)
        ]
        for eq in expired:
            del self.state.equipment_runtime[eq]

    def tick_records(self) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        if not self.state.running:
            return [], []

        self.state.tick += 1
        records: list[dict[str, Any]] = []
        events: list[dict[str, Any]] = []
        ts = _iso_now()

        if self.state.scenario_id == "plating_alarm" and self.state.tick == 30:
            alarm_eq = "PLATING-02" if self.line_id == "FCW-LINE-07" else (
                self.process.current_equipment_id(self.master) or "PLATING-SW-01"
            )
            events.append(_alarm_event(self.line_id, self.process.batch_id, alarm_eq, "E101", "断丝", "critical"))
            self.state.plating_alarm_active = True
        if self.state.plating_alarm_active and self.state.tick > 60:
            self.state.plating_alarm_active = False
        events.extend(self._clear_expired_alarms())
        if self.state.equipment_maintenance and self.state.tick == 20 and not self.process.dwell_mode:
            events.extend(self.process.enter_maintenance_dwell(self.state.tick, "场景触发 · 工序维保"))

        material_events: list[dict[str, Any]] = []
        if self.line_mode != "static":
            material_events = self.process.tick(self.state.tick)
        events.extend(material_events)
        events.extend(self._alarms_from_material_events(material_events))

        current_step = self.process.current_step
        batch_updates: list[dict[str, Any]] = []
        if material_events:
            for ev in material_events:
                if ev.get("event_type") == "BATCH_CLOSE":
                    batch_updates.append({
                        "batch_id": ev.get("material_batch"),
                        "product_line_id": self.line_id,
                        "status": "completed",
                        "ended_at": ts,
                    })
                if ev.get("event_type") == "BATCH_CREATE":
                    batch_updates.append(self.initial_batch())

        for eq in self.equipment:
            eq_id = str(eq.get("equipment_id"))
            profile = self.profiles.get(eq_id)
            if not profile:
                continue
            step_id = str(profile.get("process_step_id", eq.get("process_step_id")))
            is_active = step_id == current_step
            if TELEMETRY_SPARSE_ENABLED and not is_active:
                if self.state.tick % TELEMETRY_IDLE_EQUIPMENT_INTERVAL != 0:
                    continue
                values, meta = self._build_values(
                    eq_id, profile, is_active, eq, sparse_only=True
                )
            else:
                values, meta = self._build_values(eq_id, profile, is_active, eq)
            records.append({
                "record_type": "process_snapshot",
                "config_id": "jqhc-manufacturing",
                "template_id": self.template_id,
                "timestamp": ts,
                "product_line_id": self.line_id,
                "equipment_id": eq_id,
                "process_step_id": step_id,
                "product_batch": self.process.batch_id,
                "recipe_id": self.process.recipe_id,
                "shift": resolve_shift(self.state.tick, self.state.ticks_per_shift),
                "values": values,
                "value_meta": meta,
            })

        self.state.records_sent += len(records)
        if events:
            self.state.events_sent += len(events)
        if batch_updates:
            events.append({"__batch_updates__": batch_updates})  # type: ignore[misc]
        return records, events

    def _resolve_equipment_status(self, eq_id: str, is_active_step: bool) -> str:
        plating_eq = self._default_alarm_equipment()
        if eq_id in self.state.active_equipment_alarms:
            return "ALARM"
        if eq_id == plating_eq and self.state.plating_alarm_active and 30 <= self.state.tick <= 60:
            return "ALARM"
        runtime = self.state.equipment_runtime.get(eq_id)
        if runtime and self.state.tick < runtime.get("until_tick", 0):
            return "MANUAL"
        dwell = self.process.dwell_mode
        if dwell and is_active_step:
            if dwell in {"maintenance", "rework", "hold"}:
                return "MANUAL"
            if dwell == "material_shortage":
                return "STOPPED"
        if self.line_mode == "static":
            return "STOPPED"
        if self.line_mode == "maintenance":
            return "MANUAL" if is_active_step else "STOPPED"
        if not self.state.running:
            return "STOPPED"
        return "RUNNING" if is_active_step else "STOPPED"

    def _build_values(
        self,
        eq_id: str,
        profile: dict[str, Any],
        is_active_step: bool,
        eq_meta: dict[str, Any] | None = None,
        sparse_only: bool = False,
    ) -> tuple[dict[str, Any], dict[str, Any]]:
        values: dict[str, Any] = {}
        meta: dict[str, Any] = {}
        fields = profile.get("fields", {})
        recipe = get_recipe(self.process.recipe_id, self.master)
        eq_meta = eq_meta or {}
        equipment_type = str(eq_meta.get("equipment_type", ""))
        rated_cap = eq_meta.get("rated_capacity_per_hour")

        status = self._resolve_equipment_status(eq_id, is_active_step)
        values["status"] = status
        power_kw = equipment_power_kw(
            equipment_type,
            status,
            rated_capacity_per_hour=float(rated_cap) if rated_cap else None,
            is_active_step=is_active_step,
        )
        values["power_kw"] = power_kw
        meta["power_kw"] = {"data_source": "simulated", "quality": "good"}

        if sparse_only:
            return values, meta

        for field_id, spec in fields.items():
            if field_id in {"status", "location", "recipe_id"}:
                if field_id == "location" and isinstance(spec, str):
                    values[field_id] = spec
                elif field_id == "recipe_id" and isinstance(spec, str):
                    values[field_id] = spec
                continue

            if not isinstance(spec, dict):
                continue

            target_override = recipe_target(recipe, field_id)
            target = float(target_override if target_override is not None else spec.get("target", 0))
            low, high = spec.get("range", [target - 1, target + 1])
            if target_override is not None and isinstance(low, (int, float)) and isinstance(high, (int, float)):
                span = float(high) - float(low)
                mid = target
                low, high = mid - span / 2, mid + span / 2

            key = f"{eq_id}:{field_id}"
            current = self.state.field_state.get(key, target)
            drift = spec.get("drift", False)
            noise = 0.15 if (drift or is_active_step) else 0.05
            if self.line_mode == "static":
                noise = 0.0

            step_id = str(profile.get("process_step_id", ""))
            if field_id == "fill_ratio_pct" and self.state.fill_drift:
                current = min(21.8, current + 0.025)
            elif (
                self.state.fill_drift
                and field_id in {"coating_thickness_top_um", "coating_thickness_mm"}
                and step_id == "coating"
            ):
                current += 0.04
            elif is_active_step:
                current += random.uniform(-noise, noise)
                if self.process.current_step and field_id in {"tension_kN", "coating_thickness_top_um", "mixing_rpm"}:
                    current += random.uniform(-0.03, 0.03)
                current += coupling_delta(
                    field_id,
                    step_id,
                    str(self.process.current_step or ""),
                    self.state.field_state,
                    step_equipment=self._step_eq_map,
                )
            else:
                current += random.uniform(-noise * 0.4, noise * 0.4)

            if isinstance(low, (int, float)) and isinstance(high, (int, float)):
                current = max(float(low), min(float(high), current))

            if field_id == "fill_ratio_pct" and self.state.fill_drift:
                current = min(21.8, current)

            self.state.field_state[key] = current
            values[field_id] = round(current, 2)

            in_spec = isinstance(low, (int, float)) and isinstance(high, (int, float)) and float(low) <= current <= float(high)
            quality = "good" if in_spec else "uncertain"
            if not in_spec and is_active_step:
                quality = "bad"
            meta[field_id] = {
                "data_source": "simulated",
                "quality": quality,
                "recipe_target": target if target_override is not None else None,
            }

        return values, meta

    def initial_batch(self) -> dict[str, Any]:
        if self.line_mode == "static" or not self.process.batch_id:
            return {}
        parents = [] if self.state.trace_gap else self._default_parent_batches()
        return {
            "batch_id": self.process.batch_id,
            "factory_id": str(self.context.get("factory_id") or get_factory_id(self.master)),
            "workshop_id": str(self.context.get("workshop_id") or ""),
            "product_line_id": self.line_id,
            "production_order_id": self.process.production_order_id or self.order.get("production_order_id"),
            "product_category": self.process.product_category,
            "recipe_id": self.process.recipe_id or self._default_recipe_id(),
            "grade": self.process.grade or self._default_grade(),
            "shift": resolve_shift(self.state.tick, self.state.ticks_per_shift),
            "status": "in_progress",
            "started_at": _iso_now(),
            "quantity_kg": self.process.quantity_kg,
            "parent_batches": parents,
        }

    def energy_snapshot(self) -> dict[str, Any]:
        defaults = get_simulation_defaults(self.master)
        green_ratio = green_power_ratio(
            self.master,
            scenario_id=self.state.scenario_id,
            process_step_id=self.process.current_step,
            green_shift_pct=self.state.green_shift_pct,
        )
        tick_interval_h = max(float(defaults.get("push_interval_sec", 1.0)), 0.1) / 3600.0
        speed = max(self.state.speed_multiplier, 0.1) if self.state.running else 0.0

        equipment_breakdown: list[dict[str, Any]] = []
        total_kw = 0.0
        running_count = 0
        current_step = self.process.current_step
        for eq in self.equipment:
            eq_id = str(eq.get("equipment_id"))
            step_id = str(eq.get("process_step_id", ""))
            is_active = step_id == current_step
            status = self._resolve_equipment_status(eq_id, is_active)
            if status == "RUNNING":
                running_count += 1
            kw = equipment_power_kw(
                str(eq.get("equipment_type", "")),
                status,
                rated_capacity_per_hour=eq.get("rated_capacity_per_hour"),
                is_active_step=is_active,
            )
            total_kw += kw
            equipment_breakdown.append({
                "equipment_id": eq_id,
                "status": status,
                "power_kw": kw,
                "is_active_step": is_active,
            })

        power_kwh = round(total_kw * tick_interval_h * speed, 3)
        if power_kwh <= 0 and self.line_mode == "static":
            power_kwh = round(len(self.equipment) * 0.08 * tick_interval_h, 3)
        green_power_kwh = round(power_kwh * green_ratio, 3)
        grid_power_kwh = round(max(power_kwh - green_power_kwh, 0), 3)
        specific_energy = round(
            (total_kw * 3600.0) / max(self.process.quantity_kg, 0.1),
            2,
        )
        return {
            "factory_id": str(self.context.get("factory_id") or get_factory_id(self.master)),
            "workshop_id": str(self.context.get("workshop_id") or ""),
            "product_line_id": self.line_id,
            "product_batch": self.process.batch_id,
            "power_kwh": power_kwh,
            "green_power_kwh": green_power_kwh,
            "grid_power_kwh": grid_power_kwh,
            "specific_energy_kwh_per_t": specific_energy,
            "active_power_kw": round(total_kw, 2),
            "running_equipment_count": running_count,
            "equipment_breakdown": equipment_breakdown,
        }

    def _default_recipe_id(self) -> str:
        defaults = {
            "FCW-LINE-07": "FCW-HY830-V3",
            "SW-LINE-02": "SW-ER50-V2",
            "WR-LINE-01": "WR-E7014-V1",
        }
        return defaults.get(self.line_id, "FCW-HY830-V3")

    def _default_grade(self) -> str:
        defaults = {
            "FCW-LINE-07": "HY-830MPa",
            "SW-LINE-02": "ER50-6",
            "WR-LINE-01": "E7014",
        }
        return defaults.get(self.line_id, "")

    def _default_parent_batches(self) -> list[str]:
        defaults = {
            "FCW-LINE-07": ["STRIP-20250604-A1", "FLUX-20250605-A1"],
            "SW-LINE-02": ["ROD-20260520-C1"],
            "WR-LINE-01": ["ROD-20260520-C1", "FLUX-WR-20260501-A1"],
        }
        return defaults.get(self.line_id, [])

    def status(self) -> dict[str, Any]:
        base = {
            "running": self.state.running,
            "line_mode": self.line_mode,
            "scenario_id": self.state.scenario_id,
            "speed_multiplier": self.state.speed_multiplier,
            "line_id": self.line_id,
            "batch_id": self.process.batch_id,
            "records_sent": self.state.records_sent,
            "events_sent": self.state.events_sent,
            "tick": self.state.tick,
        }
        base.update(self.process.status())
        base["shift"] = resolve_shift(self.state.tick, self.state.ticks_per_shift)
        base["active_equipment_alarms"] = list(self.state.active_equipment_alarms.keys())
        base["quality_hold"] = self.state.quality_hold
        base["logistics_pending"] = len(self.process._logistics_pending)
        base["equipment_runtime"] = {
            eq: meta.get("mode") for eq, meta in self.state.equipment_runtime.items()
        }
        energy = self.energy_snapshot()
        base["energy"] = {
            "active_power_kw": energy.get("active_power_kw"),
            "running_equipment_count": energy.get("running_equipment_count"),
        }
        return base


def _iso_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _alarm_event(
    line_id: str,
    batch_id: str,
    equipment_id: str,
    code: str,
    message: str,
    severity: str,
) -> dict[str, Any]:
    return {
        "alarm_id": f"ALM-{uuid.uuid4().hex[:8]}",
        "equipment_id": equipment_id,
        "product_line_id": line_id,
        "product_batch": batch_id,
        "alarm_code": code,
        "alarm_message": message,
        "severity": severity,
        "triggered_at": _iso_now(),
        "handle_status": "pending",
    }
