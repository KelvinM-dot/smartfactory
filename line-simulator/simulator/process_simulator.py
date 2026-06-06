"""WIP progression and material event generation."""

from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any

import random

from simulator.factory_physics import (
    evaluate_mechanical_against_spec,
    mechanical_quality_results,
    quality_decision,
    ticks_for_step,
)
from simulator.order_release import (
    apply_shift_release,
    init_release_state,
    resolve_changeover_ticks,
)
from simulator.config import REDUCE_LOGISTICS_EVENTS
from simulator.master_data import (
    equipment_by_step,
    get_factory_id,
    get_initial_batch,
    get_initial_wip_step,
    get_line,
    get_line_context,
    get_line_order,
    get_line_orders,
    get_raw_material_inventory,
    get_recipe,
    get_simulation_defaults,
    step_equipment_map,
)


def batch_prefix(line_id: str) -> str:
    parts = line_id.split("-")
    return parts[0] if parts else "BATCH"


def _iso_now_ms(offset_ms: int = 0) -> str:
    base = datetime.now(timezone.utc)
    return base.strftime("%Y-%m-%dT%H:%M:%S") + f".{offset_ms:03d}Z"


@dataclass
class ProcessState:
    line_id: str
    process_steps: list[str]
    factory_id: str = "JQHC-PLANT-01"
    workshop_id: str = ""
    step_index: int = 0
    step_tick: int = 0
    ticks_per_step: int = 25
    batch_id: str = ""
    recipe_id: str = ""
    grade: str = ""
    quantity_kg: float = 1200.0
    product_category: str = "flux_core_wire"
    production_order_id: str = ""
    order_queue: list[dict[str, Any]] = field(default_factory=list)
    order_index: int = 0
    order_remaining_kg: dict[str, float] = field(default_factory=dict)
    order_aps_pool_kg: dict[str, float] = field(default_factory=dict)
    default_batch_output_kg: float = 1200.0
    ticks_per_shift: int = 400
    release_batches_per_shift: float = 3.0
    release_enabled: bool = True
    changeover_rules: dict[str, Any] = field(default_factory=dict)
    pending_order_status_updates: list[dict[str, Any]] = field(default_factory=list)
    order_blocked_reason: dict[str, str] = field(default_factory=dict)
    agv_id: str = "AGV-05"
    fg_location: str = "WH-FG-A-03-12"
    quality_hold_location: str = "WH-HOLD-01"
    batch_seq: int = 2
    events_sent: int = 0
    batches_completed: int = 0
    quality_pass_rate_pct: float = 97.2
    logistics_lead_ticks: int = 10
    base_ticks_per_step: int = 25
    force_quality_hold: bool = False
    spec_violation_active: bool = False
    raw_warehouse_id: str = "WH-RAW-01"
    raw_inventory: dict[str, float] = field(default_factory=dict)
    raw_material_low: bool = False
    dwell_mode: str | None = None
    dwell_until_tick: int = 0
    dwell_reason: str = ""
    rework_dwell_ticks: int = 40
    maintenance_dwell_ticks: int = 60
    hold_dwell_ticks: int = 25
    changeover_dwell_ticks: int = 35
    material_recovery_ticks: int = 80
    raw_material_low_threshold_kg: float = 150.0
    _equipment_by_step: dict[str, dict[str, Any]] = field(default_factory=dict)
    _initialized: bool = False
    _event_seq: int = 0
    _pending_events: list[dict[str, Any]] = field(default_factory=list)
    _logistics_pending: list[dict[str, Any]] = field(default_factory=list)

    @classmethod
    def from_master(cls, line_id: str, master: dict[str, Any]) -> "ProcessState":
        defaults = get_simulation_defaults(master)
        line = get_line(line_id, master)
        context = get_line_context(line_id, master)
        order = get_line_order(line_id, master) or {}
        order_queue = get_line_orders(line_id, master)
        steps = list(line.get("process_steps", []))
        batch = get_initial_batch(line_id, master) or {}
        wip_step = get_initial_wip_step(line_id, master)

        line_agv = defaults.get("line_agv_ids", {})
        line_fg = defaults.get("line_fg_locations", {})
        line_output = defaults.get("line_batch_output_kg", {})
        prefix = batch_prefix(line_id)
        default_batch_output_kg = float(line_output.get(line_id, defaults.get("batch_output_kg", 1200)))

        step_index = 0
        if wip_step and wip_step in steps:
            step_index = steps.index(wip_step)

        batch_id = str(batch.get("batch_id", f"{prefix}-{datetime.now(timezone.utc).strftime('%Y%m%d')}-B1"))
        seq_part = batch_id.rsplit("-", 1)[-1]
        if seq_part.startswith("B") and seq_part[1:].isdigit():
            batch_seq = int(seq_part[1:])

        state = cls(
            line_id=line_id,
            factory_id=str(context.get("factory_id") or get_factory_id(master)),
            workshop_id=str(context.get("workshop_id") or ""),
            process_steps=steps,
            step_index=step_index,
            batch_id=batch_id,
            recipe_id=str(batch.get("recipe_id", "")),
            grade=str(batch.get("grade", "")),
            product_category=str(line.get("product_category", "flux_core_wire")),
            production_order_id=str(batch.get("production_order_id") or order.get("production_order_id") or ""),
            order_queue=order_queue,
            quantity_kg=float(batch.get("quantity_kg", default_batch_output_kg)),
            default_batch_output_kg=default_batch_output_kg,
            ticks_per_step=int(defaults.get("ticks_per_process_step", 25)),
            agv_id=str(line_agv.get(line_id, defaults.get("agv_id", "AGV-05"))),
            fg_location=str(line_fg.get(line_id, "WH-FG-A-03-12")),
            quality_hold_location=str(defaults.get("quality_hold_location_id", "WH-HOLD-01")),
            quality_pass_rate_pct=float(defaults.get("default_quality_gate_pass_rate_pct", 97.2)),
            logistics_lead_ticks=int(defaults.get("logistics_lead_ticks", 10)),
            base_ticks_per_step=int(defaults.get("ticks_per_process_step", 25)),
            raw_warehouse_id=str(defaults.get("default_raw_warehouse_id", "WH-RAW-01")),
            raw_inventory=get_raw_material_inventory(master),
            rework_dwell_ticks=int(defaults.get("rework_dwell_ticks", 40)),
            maintenance_dwell_ticks=int(defaults.get("maintenance_dwell_ticks", 60)),
            hold_dwell_ticks=int(defaults.get("hold_dwell_ticks", 25)),
            changeover_dwell_ticks=int(defaults.get("changeover_dwell_ticks", 35)),
            material_recovery_ticks=int(defaults.get("material_recovery_ticks", 80)),
            raw_material_low_threshold_kg=float(defaults.get("raw_material_low_threshold_kg", 150)),
            ticks_per_shift=int(defaults.get("ticks_per_shift", 400)),
            release_batches_per_shift=float(
                defaults.get("order_release", {}).get("release_batches_per_shift", 3.0)
            ),
            release_enabled=bool(defaults.get("order_release", {}).get("enabled", True)),
            changeover_rules=dict(defaults.get("changeover_rules", {})),
        )
        state._equipment_by_step = equipment_by_step(line_id, master)
        state._refresh_step_duration()
        release_cfg = defaults.get("order_release", {})
        state.order_remaining_kg, state.order_aps_pool_kg = init_release_state(
            order_queue,
            release_hold_ratio_released=float(release_cfg.get("hold_ratio_released", 0.55)),
            release_hold_ratio_in_progress=float(release_cfg.get("hold_ratio_in_progress", 0.15)),
        )
        if state.production_order_id and state.production_order_id in state.order_remaining_kg:
            seeded_qty = float(batch.get("quantity_kg", 0) or 0)
            state.order_remaining_kg[state.production_order_id] = max(
                state.order_remaining_kg[state.production_order_id] - seeded_qty,
                0.0,
            )
        if state.production_order_id:
            for idx, item in enumerate(order_queue):
                if str(item.get("production_order_id")) == state.production_order_id:
                    state.order_index = idx
                    break
        state.batch_seq = batch_seq
        return state

    def initialize(self) -> list[dict[str, Any]]:
        """Resume WIP at current step; batch already exists in seed/API — no duplicate BATCH_CREATE."""
        if self._initialized:
            return []
        self._initialized = True
        step = self.current_step
        remark = f"WIP 恢复 · {step}" if self.step_index > 0 else f"WIP 进入 {step}"
        events = [
            self._material_event(
                "LINE_ON",
                process_step_id=step,
                location=self._wip_location(step),
                remark=remark,
            ),
        ]
        if self.step_index == 0:
            events.extend(self._emit_raw_material_issue())
        self.events_sent += len(events)
        return events

    @staticmethod
    def _priority_rank(priority: str | None) -> int:
        return {"high": 0, "normal": 1, "low": 2}.get(str(priority or "normal"), 1)

    @staticmethod
    def _order_type_rank(order_type: str | None) -> int:
        return {"export": 0, "custom": 1, "regular": 2}.get(str(order_type or "regular"), 2)

    def _eligible_orders(self) -> list[tuple[int, dict[str, Any]]]:
        eligible: list[tuple[int, dict[str, Any]]] = []
        for idx, candidate in enumerate(self.order_queue):
            candidate_id = str(candidate.get("production_order_id") or "")
            candidate_status = str(candidate.get("status") or "")
            if (
                candidate_id
                and self.order_remaining_kg.get(candidate_id, 0.0) > 0
                and candidate_status not in {"blocked", "completed"}
            ):
                eligible.append((idx, candidate))
        return eligible

    def _select_next_order(self) -> dict[str, Any] | None:
        if not self.order_queue:
            return None

        current_order = self.order_queue[self.order_index] if 0 <= self.order_index < len(self.order_queue) else None
        current_order_id = str(current_order.get("production_order_id")) if current_order else ""
        current_status = str(current_order.get("status") or "") if current_order else ""
        if current_order_id and self.order_remaining_kg.get(current_order_id, 0.0) > 0 and current_status not in {"blocked", "completed"}:
            return current_order

        eligible = self._eligible_orders()
        if not eligible:
            return None

        def sort_key(item: tuple[int, dict[str, Any]]) -> tuple[int, int, str]:
            _, order = item
            return (
                self._priority_rank(order.get("priority")),
                self._order_type_rank(order.get("order_type")),
                str(order.get("due_date") or ""),
            )

        idx, candidate = min(eligible, key=sort_key)
        self.order_index = idx
        return candidate

    @property
    def current_step(self) -> str:
        return self.process_steps[self.step_index]

    def _wip_location(self, step_id: str) -> str:
        return f"{self.line_id}/{step_id}"

    def _material_event(
        self,
        event_type: str,
        *,
        process_step_id: str | None = None,
        location: str | None = None,
        from_location: str | None = None,
        to_location: str | None = None,
        quantity_kg: float | None = None,
        remark: str | None = None,
        material_batch: str | None = None,
    ) -> dict[str, Any]:
        self._event_seq += 1
        return {
            "event_id": f"MAT-{uuid.uuid4().hex[:10]}",
            "event_type": event_type,
            "timestamp": _iso_now_ms(self._event_seq % 1000),
            "material_batch": material_batch or self.batch_id,
            "factory_id": self.factory_id,
            "workshop_id": self.workshop_id,
            "product_category": self.product_category,
            "product_line_id": self.line_id,
            "production_order_id": self.production_order_id or None,
            "process_step_id": process_step_id,
            "quantity_kg": quantity_kg if quantity_kg is not None else self.quantity_kg,
            "quantity_unit": "kg",
            "location": location,
            "from_location": from_location,
            "to_location": to_location,
            "agv_id": self.agv_id if event_type in {"AGV_DISPATCH", "AGV_ARRIVE"} else None,
            "remark": remark,
        }

    def _logistics_event(
        self,
        task_type: str,
        *,
        task_id: str | None = None,
        source_location: str | None = None,
        target_location: str | None = None,
        status: str = "created",
        remark: str | None = None,
    ) -> dict[str, Any]:
        event = self._material_event(
            "LOGISTICS_TASK",
            process_step_id=self.current_step,
            from_location=source_location,
            to_location=target_location,
            location=target_location or source_location,
            remark=remark,
        )
        event["task_id"] = task_id or f"LT-{uuid.uuid4().hex[:8]}"
        event["task_type"] = task_type
        event["task_status"] = status
        return event

    def _refresh_step_duration(self) -> None:
        eq = self._equipment_by_step.get(self.current_step, {})
        cap = eq.get("rated_capacity_per_hour")
        self.ticks_per_step = ticks_for_step(self.quantity_kg, cap, self.base_ticks_per_step)

    def apply_raw_material_low_scenario(self) -> None:
        """将关键原料压至阈值以下，下一批次领料将触发缺料停留."""
        from simulator.master_data import load_master_data

        self.raw_material_low = True
        recipe = get_recipe(self.recipe_id, load_master_data()) if self.recipe_id else None
        if not recipe:
            return
        for ref in recipe.get("raw_material_refs") or []:
            batch_id = str(ref.get("batch_id") or "")
            if batch_id:
                self.raw_inventory[batch_id] = min(
                    self.raw_inventory.get(batch_id, 0.0),
                    max(self.raw_material_low_threshold_kg * 0.6, 30.0),
                )

    def enter_maintenance_dwell(self, sim_tick: int, reason: str = "计划维保") -> list[dict[str, Any]]:
        return self._enter_dwell("maintenance", sim_tick, self.maintenance_dwell_ticks, reason)

    def _in_dwell(self, sim_tick: int) -> bool:
        return bool(self.dwell_mode) and sim_tick < self.dwell_until_tick

    def _dwell_ticks(self, mode: str) -> int:
        mapping = {
            "rework": self.rework_dwell_ticks,
            "maintenance": self.maintenance_dwell_ticks,
            "hold": self.hold_dwell_ticks,
            "material_shortage": self.material_recovery_ticks,
            "changeover": self.changeover_dwell_ticks,
        }
        return mapping.get(mode, 30)

    def _queue_order_status(self, order_id: str, status: str, remark: str = "") -> None:
        if not order_id:
            return
        self.pending_order_status_updates.append({
            "production_order_id": order_id,
            "status": status,
            "remark": remark,
        })

    def _mark_order_blocked(self, reason: str) -> None:
        if not self.production_order_id:
            return
        oid = self.production_order_id
        self.order_blocked_reason[oid] = reason
        for item in self.order_queue:
            if str(item.get("production_order_id") or "") == oid:
                item["status"] = "blocked"
                break
        self._queue_order_status(oid, "blocked", reason)

    def _recover_order_if_blocked(self) -> None:
        if not self.production_order_id:
            return
        oid = self.production_order_id
        if oid not in self.order_blocked_reason:
            return
        del self.order_blocked_reason[oid]
        target_status = "in_progress"
        for item in self.order_queue:
            if str(item.get("production_order_id") or "") == oid:
                item["status"] = target_status
                break
        self._queue_order_status(oid, target_status, "受阻恢复 · 产线恢复运行")

    def drain_order_status_updates(self) -> list[dict[str, Any]]:
        updates = list(self.pending_order_status_updates)
        self.pending_order_status_updates.clear()
        return updates

    def register_order(self, order: dict[str, Any]) -> None:
        oid = str(order.get("production_order_id") or "")
        if not oid or oid in self.order_remaining_kg:
            return
        remaining, aps_pool = init_release_state([order])
        self.order_remaining_kg[oid] = remaining.get(oid, 0.0)
        self.order_aps_pool_kg[oid] = aps_pool.get(oid, 0.0)

    def _resolve_changeover_ticks(self, prev_recipe: str, prev_grade: str, new_recipe: str, new_grade: str) -> int:
        return resolve_changeover_ticks(
            prev_recipe=prev_recipe,
            prev_grade=prev_grade,
            new_recipe=new_recipe,
            new_grade=new_grade,
            rules=self.changeover_rules,
            default_ticks=self.changeover_dwell_ticks,
        )

    def _maybe_shift_release(self, sim_tick: int) -> list[dict[str, Any]]:
        if not self.release_enabled or self.ticks_per_shift <= 0:
            return []
        if sim_tick <= 0 or sim_tick % self.ticks_per_shift != 0:
            return []
        released = apply_shift_release(
            order_queue=self.order_queue,
            order_remaining_kg=self.order_remaining_kg,
            order_aps_pool_kg=self.order_aps_pool_kg,
            default_batch_output_kg=self.default_batch_output_kg,
            release_batches_per_shift=self.release_batches_per_shift,
        )
        events: list[dict[str, Any]] = []
        for item in released:
            oid = str(item["production_order_id"])
            evt = self._material_event(
                "ORDER_RELEASE",
                remark=(
                    f"班次下达 {item['released_kg']}kg · "
                    f"APS余量 {item['aps_pool_remaining_kg']}kg"
                ),
            )
            evt["production_order_id"] = oid
            events.append(evt)
            self._queue_order_status(
                str(item["production_order_id"]),
                "released",
                f"APS 班次下达 {item['released_kg']}kg",
            )
        return events

    def _enter_dwell(self, mode: str, sim_tick: int, ticks: int, reason: str) -> list[dict[str, Any]]:
        self.dwell_mode = mode
        self.dwell_until_tick = sim_tick + max(int(ticks), 5)
        self.dwell_reason = reason
        if mode in {"material_shortage", "hold", "rework"}:
            self._mark_order_blocked(reason)
        return [
            self._material_event(
                "LINE_OFF",
                process_step_id=self.current_step,
                location=self._wip_location(self.current_step),
                remark=f"{mode.upper()} 停留开始 · {reason}",
            ),
        ]

    def _exit_dwell(self) -> list[dict[str, Any]]:
        mode = self.dwell_mode or "dwell"
        reason = self.dwell_reason
        if mode == "material_shortage":
            from simulator.master_data import load_master_data

            recipe = get_recipe(self.recipe_id, load_master_data())
            if recipe:
                for ref in recipe.get("raw_material_refs") or []:
                    batch_id = str(ref.get("batch_id") or "")
                    qty = float(ref.get("qty_kg") or 0)
                    if batch_id and qty > 0:
                        self.raw_inventory[batch_id] = max(
                            self.raw_inventory.get(batch_id, 0.0),
                            qty * 2.5,
                        )
        self.dwell_mode = None
        self.dwell_until_tick = 0
        self.dwell_reason = ""
        if mode in {"material_shortage", "hold", "rework"}:
            self._recover_order_if_blocked()
        return [
            self._material_event(
                "LINE_ON",
                process_step_id=self.current_step,
                location=self._wip_location(self.current_step),
                remark=f"{mode.upper()} 停留结束 · {reason}",
            ),
        ]

    def _maybe_exit_dwell(self, sim_tick: int) -> list[dict[str, Any]]:
        if self.dwell_mode and sim_tick >= self.dwell_until_tick:
            return self._exit_dwell()
        return []

    def _roll_quality_gate(self, process_step_id: str) -> tuple[str, str]:
        decision = quality_decision(
            self.quality_pass_rate_pct,
            force_hold=self.force_quality_hold and process_step_id in {"packaging", "stock_in_out"},
            spec_violation=self.spec_violation_active,
        )
        reasons = {
            "pass": f"质量门放行 · {process_step_id}",
            "hold": f"参数波动 Hold · {process_step_id}",
            "rework": f"判定返工 · {process_step_id}",
        }
        return decision, reasons.get(decision, decision)

    def _schedule_logistics(self, event: dict[str, Any], sim_tick: int) -> None:
        self._logistics_pending.append({
            "due_tick": sim_tick + self.logistics_lead_ticks,
            "event": event,
        })

    def _flush_logistics(self, sim_tick: int, master: dict[str, Any] | None = None) -> list[dict[str, Any]]:
        from simulator.master_data import get_simulation_defaults, load_master_data

        defaults = get_simulation_defaults(master or load_master_data())
        success_rate = float(defaults.get("agv_success_rate_pct", 99.9)) / 100.0
        ready: list[dict[str, Any]] = []
        pending: list[dict[str, Any]] = []
        for item in self._logistics_pending:
            if sim_tick >= item["due_tick"]:
                event = item["event"]
                is_logistics = event.get("task_id") or str(event.get("event_type", "")).startswith("LOGISTICS")
                if is_logistics and random.random() > success_rate:
                    retry = dict(event)
                    retry["status"] = "failed"
                    retry["remark"] = (retry.get("remark") or "") + " · AGV 超时重试"
                    ready.append(retry)
                    item["due_tick"] = sim_tick + max(self.logistics_lead_ticks // 2, 3)
                    pending.append(item)
                    continue
                ready.append(event)
            else:
                pending.append(item)
        self._logistics_pending = pending
        return ready

    def _emit_raw_material_issue(
        self,
        master: dict[str, Any] | None = None,
        *,
        sim_tick: int = 0,
    ) -> list[dict[str, Any]]:
        from simulator.master_data import load_master_data

        recipe = get_recipe(self.recipe_id, master or load_master_data()) if self.recipe_id else None
        if not recipe:
            return []
        events: list[dict[str, Any]] = []
        shortages: list[str] = []
        planned: list[tuple[str, float, str]] = []
        for ref in recipe.get("raw_material_refs") or []:
            qty = float(ref.get("qty_kg") or 0)
            batch_id = str(ref.get("batch_id") or "")
            material_type = str(ref.get("material_type", "raw"))
            if qty <= 0 or not batch_id:
                continue
            available = self.raw_inventory.get(batch_id, 0.0)
            if available < qty:
                shortages.append(f"{material_type}({batch_id}) 需{qty}kg/剩{available:.0f}kg")
            else:
                planned.append((batch_id, qty, material_type))

        if shortages:
            events.append(
                self._material_event(
                    "MATERIAL_SHORTAGE",
                    process_step_id=self.current_step,
                    location=self.raw_warehouse_id,
                    remark="原料缺料 · " + "; ".join(shortages),
                )
            )
            events.extend(
                self._enter_dwell(
                    "material_shortage",
                    sim_tick,
                    self.material_recovery_ticks,
                    shortages[0],
                )
            )
            return events

        for batch_id, qty, material_type in planned:
            self.raw_inventory[batch_id] = max(self.raw_inventory.get(batch_id, 0.0) - qty, 0.0)
            events.append(
                self._material_event(
                    "STOCK_OUT",
                    material_batch=batch_id,
                    process_step_id=self.current_step,
                    location=self.raw_warehouse_id,
                    from_location=self.raw_warehouse_id,
                    quantity_kg=qty,
                    remark=f"原料领用 · {material_type}",
                )
            )
        return events

    def _quality_gate_event(
        self,
        decision: str,
        *,
        process_step_id: str,
        reason_text: str,
    ) -> dict[str, Any]:
        event = self._material_event(
            "QUALITY_GATE",
            process_step_id=process_step_id,
            location=self.quality_hold_location if decision != "pass" else self._wip_location(process_step_id),
            remark=reason_text,
        )
        event["gate_event_id"] = f"QG-{uuid.uuid4().hex[:8]}"
        event["gate_type"] = "in_process"
        event["decision"] = decision
        event["reason_text"] = reason_text
        return event

    def _apply_quality_dwell(
        self,
        decision: str,
        process_step_id: str,
        reason: str,
        sim_tick: int,
    ) -> list[dict[str, Any]]:
        if decision == "rework":
            return self._enter_dwell("rework", sim_tick, self.rework_dwell_ticks, reason)
        if decision == "hold":
            return self._enter_dwell("hold", sim_tick, self.hold_dwell_ticks, reason)
        return []

    def tick(self, sim_tick: int = 0) -> list[dict[str, Any]]:
        events: list[dict[str, Any]] = list(self._pending_events)
        self._pending_events.clear()
        events.extend(self._maybe_shift_release(sim_tick))
        events.extend(self._flush_logistics(sim_tick))
        events.extend(self._maybe_exit_dwell(sim_tick))

        if self._in_dwell(sim_tick):
            self.events_sent += len(events)
            return events

        self.step_tick += 1
        if self.step_tick < self.ticks_per_step:
            return events

        self.step_tick = 0
        old_step = self.current_step
        events.append(
            self._material_event(
                "LINE_OFF",
                process_step_id=old_step,
                location=self._wip_location(old_step),
                remark=f"工序完成 · {old_step}",
            )
        )

        if self.step_index >= len(self.process_steps) - 1:
            events.extend(self._complete_batch(old_step, sim_tick))
            return events

        self.step_index += 1
        new_step = self.current_step
        events.append(
            self._material_event(
                "TRANSFER",
                process_step_id=new_step,
                from_location=self._wip_location(old_step),
                to_location=self._wip_location(new_step),
                remark=f"WIP 转移 {old_step} → {new_step}",
            )
        )
        events.append(
            self._logistics_event(
                "transfer_wip",
                source_location=self._wip_location(old_step),
                target_location=self._wip_location(new_step),
                status="completed",
                remark=f"物流任务完成 {old_step} → {new_step}",
            )
        )
        events.append(
            self._material_event(
                "LINE_ON",
                process_step_id=new_step,
                location=self._wip_location(new_step),
                remark=f"WIP 进入 {new_step}",
            )
        )
        if new_step in {"packaging", "stock_in_out", "coating", "drying"}:
            decision, reason = self._roll_quality_gate(new_step)
            events.append(
                self._quality_gate_event(
                    decision,
                    process_step_id=new_step,
                    reason_text=reason,
                )
            )
            events.extend(self._apply_quality_dwell(decision, new_step, reason, sim_tick))
        self._refresh_step_duration()
        self.events_sent += len(events)
        return events

    def _complete_batch(self, last_step: str, sim_tick: int = 0) -> list[dict[str, Any]]:
        events: list[dict[str, Any]] = []
        from_loc = self._wip_location(last_step)
        stock_in_task_id = f"LT-{uuid.uuid4().hex[:8]}"

        events.append(
            self._material_event(
                "STOCK_OUT",
                process_step_id=last_step,
                location=from_loc,
                remark="成品下线",
            )
        )
        events.append(
            self._material_event(
                "AGV_DISPATCH",
                process_step_id=last_step,
                from_location=from_loc,
                to_location=self.fg_location,
                remark="AGV 搬运成品入库",
            )
        )
        pending_task = self._logistics_event(
            "stock_in",
            task_id=stock_in_task_id,
            source_location=from_loc,
            target_location=self.fg_location,
            status="pending",
            remark="成品入库物流任务已创建",
        )
        events.append(pending_task)
        if not REDUCE_LOGISTICS_EVENTS:
            events.append(
                self._logistics_event(
                    "stock_in",
                    task_id=stock_in_task_id,
                    source_location=from_loc,
                    target_location=self.fg_location,
                    status="transporting",
                    remark="成品入库物流任务执行中",
                )
            )
        self._schedule_logistics(
            self._logistics_event(
                "stock_in",
                task_id=stock_in_task_id,
                source_location=from_loc,
                target_location=self.fg_location,
                status="completed",
                remark="成品入库物流任务完成",
            ),
            sim_tick=sim_tick,
        )
        self._schedule_logistics(
            self._material_event(
                "STOCK_IN",
                location=self.fg_location,
                to_location=self.fg_location,
                remark="成品入库",
            ),
            sim_tick=sim_tick,
        )
        self._schedule_logistics(
            self._material_event(
                "AGV_ARRIVE",
                location=self.fg_location,
                to_location=self.fg_location,
                remark="AGV 到达成品库",
            ),
            sim_tick=sim_tick,
        )
        from simulator.master_data import get_product_for_order, load_master_data

        master = load_master_data()
        recipe = get_recipe(self.recipe_id, master) if self.recipe_id else None
        product = get_product_for_order(self.production_order_id, master)
        mech_spec = (product or {}).get("mechanical_spec")
        mech = mechanical_quality_results(
            recipe,
            spec_violation=self.spec_violation_active,
            mechanical_spec=mech_spec,
        )
        passed, failures = evaluate_mechanical_against_spec(mech, mech_spec)
        lab_event = self._material_event(
            "QUALITY_LAB",
            process_step_id=last_step,
            location=self.fg_location,
            remark=(
                f"力学终检 Rm={mech['rm_mpa']} KV2={mech['impact_kv2_j']} "
                f"A={mech['elongation_pct']}% I={mech['corrosion_i_value']}"
            ),
        )
        lab_event.update(mech)
        lab_event["grade"] = self.grade
        lab_event["mechanical_pass"] = passed
        if failures:
            lab_event["mechanical_failures"] = failures
        events.append(lab_event)

        q_decision, q_reason = self._roll_quality_gate(last_step)
        if not passed:
            q_decision = "hold"
            q_reason = "力学性能未达产品标准 · " + "; ".join(failures[:2])
        events.append(
            self._quality_gate_event(
                q_decision,
                process_step_id=last_step,
                reason_text=q_reason if q_decision != "pass" else "终检通过，允许入库",
            )
        )
        events.extend(
            self._apply_quality_dwell(
                q_decision,
                last_step,
                q_reason if q_decision != "pass" else "终检复核",
                sim_tick,
            )
        )
        events.append(
            self._material_event(
                "BATCH_CLOSE",
                process_step_id=last_step,
                location=self.fg_location,
                remark="批次完工",
            )
        )

        self.batches_completed += 1
        self.batch_seq += 1
        if self.production_order_id:
            self.order_remaining_kg[self.production_order_id] = max(
                self.order_remaining_kg.get(self.production_order_id, 0.0) - self.quantity_kg,
                0.0,
            )
            if self.order_remaining_kg.get(self.production_order_id, 0.0) <= 0:
                for item in self.order_queue:
                    if str(item.get("production_order_id") or "") == self.production_order_id:
                        item["status"] = "completed"
                        break

        next_order = self._select_next_order()
        if next_order:
            prev_recipe = self.recipe_id
            prev_grade = self.grade
            self.production_order_id = str(next_order.get("production_order_id") or self.production_order_id)
            self.recipe_id = str(next_order.get("recipe_id") or self.recipe_id)
            self.grade = str(next_order.get("grade") or self.grade)
            changeover_ticks = self._resolve_changeover_ticks(
                prev_recipe, prev_grade, self.recipe_id, self.grade
            )
            if changeover_ticks > 0:
                events.extend(
                    self._enter_dwell(
                        "changeover",
                        sim_tick,
                        changeover_ticks,
                        f"换型 {prev_grade} → {self.grade}",
                    )
                )
            remaining_kg = self.order_remaining_kg.get(self.production_order_id, self.default_batch_output_kg)
            if remaining_kg > 0:
                self.quantity_kg = min(remaining_kg, self.default_batch_output_kg)
            else:
                self.quantity_kg = self.default_batch_output_kg
        else:
            self.step_index = 0
            self.step_tick = 0
            self.events_sent += len(events)
            return events
        today = datetime.now(timezone.utc).strftime("%Y%m%d")
        prefix = batch_prefix(self.line_id)
        new_batch_id = f"{prefix}-{today}-B{self.batch_seq}"
        self.batch_id = new_batch_id
        self.step_index = 0
        self.step_tick = 0

        events.append(
            self._material_event(
                "BATCH_CREATE",
                material_batch=new_batch_id,
                process_step_id=self.current_step,
                remark="新批次启动",
            )
        )
        raw_events = self._emit_raw_material_issue(sim_tick=sim_tick)
        events.extend(raw_events)
        if self._in_dwell(sim_tick):
            self.events_sent += len(events)
            return events
        self._refresh_step_duration()
        events.append(
            self._material_event(
                "LINE_ON",
                material_batch=new_batch_id,
                process_step_id=self.current_step,
                location=self._wip_location(self.current_step),
                remark=f"WIP 进入 {self.current_step}",
            )
        )
        self.events_sent += len(events)
        return events

    def current_equipment_id(self, master: dict[str, Any]) -> str | None:
        mapping = step_equipment_map(self.line_id, master)
        return mapping.get(self.current_step)

    def status(self) -> dict[str, Any]:
        return {
            "current_step": self.current_step,
            "step_index": self.step_index,
            "step_tick": self.step_tick,
            "ticks_per_step": self.ticks_per_step,
            "factory_id": self.factory_id,
            "workshop_id": self.workshop_id,
            "batch_id": self.batch_id,
            "production_order_id": self.production_order_id,
            "batches_completed": self.batches_completed,
            "material_events_sent": self.events_sent,
            "dwell_mode": self.dwell_mode,
            "dwell_until_tick": self.dwell_until_tick,
            "dwell_reason": self.dwell_reason,
            "raw_material_low": self.raw_material_low,
            "raw_inventory": {k: round(v, 1) for k, v in self.raw_inventory.items()},
            "order_aps_pool_kg": {k: round(v, 1) for k, v in self.order_aps_pool_kg.items()},
            "release_batches_per_shift": self.release_batches_per_shift,
        }
