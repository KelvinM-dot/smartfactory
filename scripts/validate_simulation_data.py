#!/usr/bin/env python3
"""全链路模拟数据校验：静态主数据 + 域配置 + 动态运行时一致性."""

from __future__ import annotations

import json
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.error import URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
MASTER_PATH = ROOT / "schemas/智造数据台/presets/jqhc-factory-master-data.json"
DOMAIN_PATH = ROOT / "schemas/智造数据台/presets/jqhc-manufacturing-config.json"
ENUMS_PATH = ROOT / "schemas/智造数据台/enums.json"

API_BASE = "http://127.0.0.1:3001"
SIM_BASE = "http://127.0.0.1:3002"

SKIP_FIELDS = {
    "status", "location", "recipe_id", "run_mode", "alarm_code", "finished_batch_id",
}
CRITICAL_TREND_FIELDS: dict[str, str] = {
    "flux_core_wire": "fill_ratio_pct,forming_pressure_MPa,coating_thickness_top_um,tension_kN",
    "solid_wire": "tension_kN,coating_thickness_top_um,motor_rpm",
    "submerged_arc_wire": "tension_kN,coating_thickness_top_um,motor_rpm",
    "welding_rod": "tension_kN,coating_thickness_mm,actual_temp_C",
}
MATERIAL_EVENT_TYPES = {
    "LINE_ON", "LINE_OFF", "TRANSFER", "STOCK_IN", "STOCK_OUT",
    "AGV_DISPATCH", "AGV_ARRIVE", "BATCH_CREATE", "BATCH_CLOSE",
}


@dataclass
class Report:
    passed: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    failures: list[str] = field(default_factory=list)

    def ok(self, msg: str) -> None:
        self.passed.append(msg)

    def warn(self, msg: str) -> None:
        self.warnings.append(msg)

    def fail(self, msg: str) -> None:
        self.failures.append(msg)

    def section(self, title: str) -> None:
        print(f"\n{'=' * 60}\n{title}\n{'=' * 60}")


def load_json(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def http_get(url: str, timeout: float = 8.0) -> Any:
    req = Request(url, headers={"Accept": "application/json"})
    with urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode())


def resolve_telemetry_field_id(dp: dict[str, Any]) -> str:
    return str(dp.get("field_id") or dp.get("data_point_id"))


def build_step_bindings(config: dict[str, Any]) -> dict[str, set[str]]:
    bindings: dict[str, set[str]] = defaultdict(set)
    for b in config.get("step_field_bindings", []):
        bindings[b["step_id"]].add(b["field_id"])
    return dict(bindings)


def is_full_line(line: dict[str, Any]) -> bool:
    """精细产线：有完整设备/遥测/3D；登记产线仅校验拓扑."""
    return line.get("detail_level") == "full" or line.get("simulation_enabled") is True


def validate_static_master(report: Report, master: dict[str, Any], config: dict[str, Any]) -> None:
    report.section("一、静态主数据 (jqhc-factory-master-data.json)")

    lines = {l["product_line_id"]: l for l in master.get("product_lines", [])}
    full_line_ids = {lid for lid, l in lines.items() if is_full_line(l)}
    registry_count = len(lines) - len(full_line_ids)
    equipment_all = [e for e in master.get("equipment", []) if e.get("enabled", True)]
    equipment = [e for e in equipment_all if e["product_line_id"] in full_line_ids]
    profiles = master.get("equipment_telemetry_profiles", {})
    data_points = master.get("data_points", [])
    batches = master.get("product_batches", [])
    recipes = {r["recipe_id"]: r for r in master.get("recipes", [])}
    material_batches = {m["batch_id"] for m in master.get("material_batches", [])}
    bindings = build_step_bindings(config)
    enums = load_json(ENUMS_PATH)
    valid_event_types = set(enums.get("$defs", {}).get("MaterialEventType", {}).get("enum", MATERIAL_EVENT_TYPES))

    report.ok(
        f"产线 {len(lines)}（精细 {len(full_line_ids)} · 登记 {registry_count}）"
        f" · 精细设备 {len(equipment)} · 遥测 profile {len(profiles)} · 数据点 {len(data_points)}"
    )

    # 1. 精细产线：设备 ↔ profile 一一对应
    eq_ids = {e["equipment_id"] for e in equipment}
    prof_ids = set(profiles.keys())
    missing_prof = sorted(eq_ids - prof_ids)
    orphan_prof = sorted(prof_ids - eq_ids)
    if missing_prof:
        report.fail(f"设备缺少 telemetry profile: {missing_prof}")
    else:
        report.ok("全部 enabled 设备均有 telemetry profile")
    for oid in orphan_prof:
        report.warn(f"孤立 telemetry profile（无对应设备）: {oid}")

    # 2. 精细产线：设备工序归属产线工序链
    for eq in equipment:
        eid = eq["equipment_id"]
        lid = eq["product_line_id"]
        step = eq.get("process_step_id")
        line_steps = lines[lid].get("process_steps", [])
        if step not in line_steps:
            report.fail(f"{eid} 工序 {step} 不在产线 {lid} 工序链中")
        prof_step = profiles[eid].get("process_step_id")
        if prof_step and prof_step != step:
            report.fail(f"{eid} profile 工序 {prof_step} 与设备工序 {step} 不一致")

    covered_steps = defaultdict(set)
    for eq in equipment:
        covered_steps[eq["product_line_id"]].add(eq["process_step_id"])
    for lid in full_line_ids:
        line = lines[lid]
        steps = set(line.get("process_steps", []))
        missing = steps - covered_steps[lid]
        if missing:
            report.fail(f"精细产线 {lid} 缺少设备覆盖工序: {sorted(missing)}")
        else:
            report.ok(f"精细产线 {lid} 工序链设备覆盖完整 ({len(steps)} 步)")
    if registry_count:
        report.ok(f"登记产线 {registry_count} 条跳过设备/遥测强制校验")

    # 3. 精细产线：profile 字段 ↔ 域配置 step_field_bindings
    unbound: list[str] = []
    for eq in equipment:
        eid = eq["equipment_id"]
        step = eq["process_step_id"]
        allowed = bindings.get(step, set()) | SKIP_FIELDS
        for fid in profiles[eid].get("fields", {}):
            if fid not in allowed:
                unbound.append(f"{eid}@{step}:{fid}")
    if unbound:
        for item in unbound:
            report.fail(f"遥测字段未在域配置绑定: {item}")
    else:
        report.ok("全部 profile 遥测字段已在域配置 step_field_bindings 注册")

    # 4. 精细产线：data_points ↔ profile 字段对齐
    dp_by_eq: dict[str, list[dict]] = defaultdict(list)
    for dp in data_points:
        if dp.get("product_line_id") in full_line_ids or any(
            e["equipment_id"] == dp["equipment_id"] for e in equipment
        ):
            dp_by_eq[dp["equipment_id"]].append(dp)
    for eq in equipment:
        eid = eq["equipment_id"]
        prof_fields = set(profiles[eid].get("fields", {})) - SKIP_FIELDS
        dp_fields = {resolve_telemetry_field_id(dp) for dp in dp_by_eq.get(eid, [])}
        missing_in_profile = dp_fields - prof_fields
        missing_in_dp = prof_fields - dp_fields
        for f in missing_in_profile:
            report.warn(f"{eid} data_points 有 {f} 但 profile 未模拟")
        for f in missing_in_dp:
            report.warn(f"{eid} profile 有 {f} 但 data_points 未定义")

    # 5. 批次 / 配方 / 父批次引用
    for b in batches:
        bid = b["batch_id"]
        lid = b.get("product_line_id")
        if lid not in lines:
            report.fail(f"批次 {bid} 引用未知产线 {lid}")
        rid = b.get("recipe_id")
        if rid and rid not in recipes:
            report.fail(f"批次 {bid} 引用未知配方 {rid}")
        elif rid and recipes[rid].get("product_line_id") != lid:
            report.fail(f"批次 {bid} 配方 {rid} 不属于产线 {lid}")
        for parent in b.get("parent_batches") or []:
            if parent not in material_batches and not any(x["batch_id"] == parent for x in batches):
                report.warn(f"批次 {bid} 父批次 {parent} 未在 material_batches 中登记")

    in_progress = [b for b in batches if b.get("status") == "in_progress"]
    report.ok(f"产品批次 {len(batches)} 条，进行中 {len(in_progress)} 条")

    # 6. WIP 快照 ↔ 进行中批次
    wip_list = master.get("inventory_snapshots", {}).get("wip", [])
    wip_batches = {w["product_batch"] for w in wip_list}
    ip_ids = {b["batch_id"] for b in in_progress}
    for w in wip_list:
        batch_id = w["product_batch"]
        step = w["process_step_id"]
        loc = w.get("location", "")
        if batch_id not in ip_ids:
            report.warn(f"WIP {w.get('wip_id')} 批次 {batch_id} 非 in_progress 状态")
        batch_rec = next((b for b in batches if b["batch_id"] == batch_id), None)
        if batch_rec:
            expected_loc = f"{batch_rec['product_line_id']}/{step}"
            if loc != expected_loc:
                report.fail(f"WIP {w.get('wip_id')} location {loc} 应为 {expected_loc}")
    for bid in ip_ids:
        if bid not in wip_batches:
            report.warn(f"进行中批次 {bid} 无 WIP 库存快照（模拟器从 initial_wip_step 补偿）")

    # 7. simulation_defaults
    defaults = master.get("simulation_defaults", {})
    for key in ("primary_line_id", "ticks_per_process_step", "default_scenario_id"):
        if key not in defaults:
            report.fail(f"simulation_defaults 缺少 {key}")
    if defaults.get("primary_line_id") not in lines:
        report.fail("simulation_defaults.primary_line_id 无效")
    else:
        report.ok("simulation_defaults 配置完整")

    if valid_event_types >= MATERIAL_EVENT_TYPES:
        report.ok(f"物料事件类型枚举覆盖 {len(MATERIAL_EVENT_TYPES)} 种")
    else:
        report.warn(f"枚举缺少事件类型: {MATERIAL_EVENT_TYPES - valid_event_types}")


def validate_line_twin_and_product_chain(
    report: Report,
    master: dict[str, Any],
    config: dict[str, Any],
) -> None:
    """以产线为单位：孪生布局 ↔ 工序链 ↔ 设备 ↔ 遥测 ↔ 产品数据."""
    report.section("一·补、产线全链路（孪生 ↔ 生产 ↔ 产品）")

    lines = {l["product_line_id"]: l for l in master.get("product_lines", [])}
    twin_layouts = master.get("twin_layouts") or {}
    profiles = master.get("equipment_telemetry_profiles", {})
    equipment_all = [e for e in master.get("equipment", []) if e.get("enabled", True)]
    equipment = [e for e in equipment_all if is_full_line(lines[e["product_line_id"]])]
    step_display = {
        s["step_id"]: s["display_name"] for s in config.get("process_steps", [])
    }
    bindings = build_step_bindings(config)
    recipes_all = master.get("recipes", [])
    batches_all = master.get("product_batches", [])

    if not twin_layouts:
        report.fail("master-data 缺少 twin_layouts（2D/3D 孪生应与产线工序链同源）")
        return

    line_ids = set(lines.keys())
    twin_ids = set(twin_layouts.keys())
    if line_ids != twin_ids:
        report.fail(f"twin_layouts 产线集合 {twin_ids} ≠ product_lines {line_ids}")
    else:
        report.ok(f"{len(line_ids)} 条产线均有 twin_layout 定义")

    for lid, line in lines.items():
        layout = twin_layouts.get(lid, {})
        process_steps = line.get("process_steps", [])
        flow_path = layout.get("flow_path") or []
        full = is_full_line(line)

        if flow_path != process_steps:
            report.fail(
                f"{lid} flow_path 与 process_steps 不一致: "
                f"twin={flow_path} master={process_steps}"
            )
        else:
            level = "精细" if full else "登记"
            report.ok(f"{lid}（{level}）孪生工序链 {len(flow_path)} 步与主数据一致")

        steps = layout.get("steps") or {}
        if set(flow_path) != set(steps.keys()):
            missing = set(flow_path) - set(steps.keys())
            extra = set(steps.keys()) - set(flow_path)
            report.fail(f"{lid} twin steps 键不匹配: 缺 {missing} 多 {extra}")
            continue

        if not full:
            continue

        line_eq_by_step = {
            e["process_step_id"]: e
            for e in equipment_all
            if e["product_line_id"] == lid
        }

        model_ok = True
        for step_id in flow_path:
            step_cfg = steps[step_id]
            eq_id = step_cfg.get("equipment_id")
            key_field = step_cfg.get("key_field_id")
            node = step_cfg.get("node") or {}
            pos3d = step_cfg.get("position_3d") or {}

            eq = line_eq_by_step.get(step_id)
            if not eq:
                report.fail(f"{lid}/{step_id} 主数据无 enabled 设备")
                model_ok = False
            elif eq_id != eq["equipment_id"]:
                report.fail(
                    f"{lid}/{step_id} twin 设备 {eq_id} ≠ 主数据 {eq['equipment_id']}"
                )
                model_ok = False

            if eq_id not in profiles:
                report.fail(f"{lid}/{step_id} 设备 {eq_id} 缺少 telemetry profile")
                model_ok = False
            elif profiles[eq_id].get("process_step_id") != step_id:
                report.fail(f"{lid}/{step_id} profile 工序与 twin 不一致")
                model_ok = False

            if key_field:
                prof_fields = profiles.get(eq_id, {}).get("fields", {})
                if key_field not in prof_fields and key_field not in SKIP_FIELDS:
                    report.fail(f"{lid}/{step_id} key_field {key_field} 不在 profile")
                    model_ok = False
                allowed = bindings.get(step_id, set()) | SKIP_FIELDS
                if key_field not in allowed:
                    report.warn(f"{lid}/{step_id} key_field {key_field} 未在域配置绑定")

            label = node.get("label")
            expected = step_display.get(step_id)
            if expected and label != expected:
                report.fail(f"{lid}/{step_id} 节点标签「{label}」≠ 域配置「{expected}」")
                model_ok = False

            if not pos3d.get("type"):
                report.fail(f"{lid}/{step_id} 缺少 3D position_3d.type")
                model_ok = False

        if model_ok:
            report.ok(f"{lid} 孪生建模完整：{len(flow_path)} 工序均有设备/3D/key_field")

        # 精细产线：产品数据链配方/批次/WIP
        if full:
            line_recipes = [r for r in recipes_all if r.get("product_line_id") == lid]
            line_batches = [b for b in batches_all if b.get("product_line_id") == lid]
            recipe_ids = {r["recipe_id"] for r in line_recipes}
            if not line_recipes:
                report.fail(f"{lid} 无产线配方")
            for b in line_batches:
                rid = b.get("recipe_id")
                if rid and rid not in recipe_ids:
                    report.fail(f"{lid} 批次 {b['batch_id']} 配方 {rid} 不属于该产线")
            if line_recipes:
                report.ok(
                    f"{lid} 产品数据：配方 {len(line_recipes)} · 批次 {len(line_batches)}"
                )

            wip_list = [
                w for w in master.get("inventory_snapshots", {}).get("wip", [])
                if w.get("location", "").startswith(lid)
            ]
            ip_batches = {
                b["batch_id"]
                for b in line_batches
                if b.get("status") == "in_progress"
            }
            for w in wip_list:
                if w.get("product_batch") not in ip_batches:
                    report.warn(
                        f"{lid} WIP {w.get('wip_id')} 批次 {w.get('product_batch')} 非 in_progress"
                    )
                step = w.get("process_step_id")
                if step and step not in process_steps:
                    report.fail(f"{lid} WIP 工序 {step} 不在产线工序链")


def validate_simulator_logic(report: Report, master: dict[str, Any]) -> None:
    report.section("二、模拟器逻辑（内存 dry-run）")

    sys.path.insert(0, str(ROOT / "line-simulator"))
    from simulator.engine import SimulationEngine  # noqa: WPS433
    from simulator.plant_engine import PlantSimulationEngine  # noqa: WPS433

    plant = PlantSimulationEngine(master_path=str(MASTER_PATH))
    plant.configure("normal_shift", 1.0)
    init_events = plant.start_all()

    # 初始物料事件结构
    for ev in init_events:
        if ev.get("event_type") not in MATERIAL_EVENT_TYPES:
            report.fail(f"未知初始事件类型: {ev.get('event_type')}")
        if not ev.get("event_id") or not ev.get("product_line_id"):
            report.fail(f"初始事件缺少 event_id/product_line_id: {ev}")
    report.ok(f"start_all 产生 {len(init_events)} 条初始物料事件")

    # 多 tick 采样
    batch_ids_seen: dict[str, set[str]] = defaultdict(set)
    step_transitions: dict[str, list[str]] = defaultdict(list)
    for _ in range(30):
        records, events = plant.tick_all()
        material_events, _ = _split_events(events)
        for rec in records:
            lid = rec["product_line_id"]
            eid = rec["equipment_id"]
            if rec.get("record_type") != "process_snapshot":
                report.fail(f"非法 record_type: {rec.get('record_type')}")
            if not rec.get("values"):
                report.fail(f"{eid} 空 values")
            batch_ids_seen[lid].add(rec.get("product_batch"))
        for ev in material_events:
            lid = ev["product_line_id"]
            batch_ids_seen[lid].add(ev.get("material_batch"))
            if ev["event_type"] == "TRANSFER":
                step_transitions[lid].append(f"{ev.get('from_location')}->{ev.get('to_location')}")

    for lid, eng in plant.engines.items():
        mode = eng.line_mode
        if mode == "static":
            _, evs = eng.tick_records()
            mat, _ = _split_events(evs)
            if mat:
                report.fail(f"{lid} static 模式不应产生物料事件")
            else:
                report.ok(f"{lid} static 模式：无物料流转（符合 inactive 产线）")
        elif eng.state.records_sent == 0:
            report.fail(f"{lid} 未产生遥测 records")
        else:
            report.ok(
                f"{lid} ({mode}) 30 tick 产生 records={eng.state.records_sent} "
                f"events={eng.state.events_sent}"
            )

        records, _ = eng.tick_records()
        eq_on_line = {
            e["equipment_id"]
            for e in master.get("equipment", [])
            if e["product_line_id"] == lid
        }
        for rec in records:
            if rec["equipment_id"] not in eq_on_line:
                report.fail(f"{rec['equipment_id']} 不属于产线 {lid}")

    # FCW 应能观察到批次号（可能仍是 B2 或轮转后 B3+）
    fcw_batches = batch_ids_seen.get("FCW-LINE-07", set())
    if not fcw_batches:
        report.fail("FCW-LINE-07 30 tick 内无 product_batch")
    else:
        report.ok(f"FCW-LINE-07 遥测/事件批次号: {sorted(fcw_batches)}")


def _split_events(events: list[dict]) -> tuple[list[dict], list[dict]]:
    material: list[dict] = []
    batch_updates: list[dict] = []
    for ev in events:
        if "__batch_updates__" in ev:
            batch_updates.extend(ev["__batch_updates__"])
        else:
            material.append(ev)
    return material, batch_updates


EVENT_TYPE_ORDER = {
    "BATCH_CREATE": 0,
    "LINE_ON": 1,
    "TRANSFER": 2,
    "LINE_OFF": 3,
    "STOCK_OUT": 4,
    "AGV_DISPATCH": 5,
    "STOCK_IN": 6,
    "AGV_ARRIVE": 7,
    "BATCH_CLOSE": 8,
}


def _step_before(step: str, pivot: str, line_id: str, master: dict[str, Any]) -> bool:
    for line in master.get("product_lines", []):
        if line.get("product_line_id") != line_id:
            continue
        steps = line.get("process_steps", [])
        if step in steps and pivot in steps:
            return steps.index(step) < steps.index(pivot)
    return False


def validate_material_event_sequence(
    report: Report,
    events: list[dict[str, Any]],
    line_id: str,
    master: dict[str, Any],
) -> None:
    """按批次校验物料事件状态机."""
    by_batch: dict[str, list[dict]] = defaultdict(list)
    for ev in events:
        if ev.get("product_line_id") != line_id:
            continue
        by_batch[ev.get("material_batch") or ""].append(ev)

    for batch_id, evs in by_batch.items():
        if not batch_id:
            continue
        evs.sort(key=lambda e: (
            e.get("timestamp", ""),
            EVENT_TYPE_ORDER.get(e.get("event_type"), 99),
            e.get("event_id", ""),
        ))
        open_steps: Counter[str] = Counter()
        wip_resume_step: str | None = None
        for ev in evs:
            et = ev.get("event_type")
            step = ev.get("process_step_id")
            if et == "LINE_ON" and wip_resume_step is None and "WIP 恢复" in str(ev.get("remark", "")):
                wip_resume_step = step
            if et == "BATCH_CREATE":
                if open_steps:
                    report.warn(f"{line_id}/{batch_id} BATCH_CREATE 时仍有未关闭工序")
            elif et == "LINE_ON":
                open_steps[step] += 1
            elif et == "LINE_OFF":
                if open_steps[step] <= 0:
                    # WIP 从中途恢复：当前工序之前的 LINE_ON/OFF 不在事件链中
                    if wip_resume_step and step and _step_before(step, wip_resume_step, line_id, master):
                        continue
                    report.fail(f"{line_id}/{batch_id} LINE_OFF 无对应 LINE_ON @ {step}")
                open_steps[step] -= 1
            elif et == "TRANSFER":
                if not ev.get("from_location") or not ev.get("to_location"):
                    report.fail(f"{line_id}/{batch_id} TRANSFER 缺少 from/to location")
            elif et == "BATCH_CLOSE":
                if any(v > 0 for v in open_steps.values()):
                    report.warn(f"{line_id}/{batch_id} BATCH_CLOSE 时仍有未 LINE_OFF 工序: {dict(open_steps)}")

        # 完工链：STOCK_OUT → AGV → STOCK_IN
        types = [e.get("event_type") for e in evs]
        if "BATCH_CLOSE" in types:
            for required in ("STOCK_OUT", "STOCK_IN"):
                if required not in types:
                    report.fail(f"{line_id}/{batch_id} 完工链缺少 {required}")


def validate_runtime_api(report: Report, master: dict[str, Any]) -> None:
    report.section("三、动态运行时（API + 模拟器状态）")

    try:
        sim = http_get(f"{SIM_BASE}/sim/status")
    except URLError as e:
        report.fail(f"模拟器不可达 ({SIM_BASE}): {e}")
        return

    if not sim.get("running"):
        report.fail("模拟器 plant.running=false，未推送动态数据")
    else:
        report.ok(f"模拟器运行中 scenario={sim.get('scenario_id')} speed={sim.get('speed_multiplier')}×")

    sim_line_ids = set(master.get("simulation_defaults", {}).get("simulation_line_ids") or [])
    if not sim_line_ids:
        sim_line_ids = {
            l["product_line_id"]
            for l in master.get("product_lines", [])
            if l.get("simulation_enabled")
        }

    lines = master.get("product_lines", [])
    for line in lines:
        lid = line["product_line_id"]
        line_status = line.get("status")
        full = is_full_line(line)
        is_sim = lid in sim_line_ids

        try:
            overview = http_get(f"{API_BASE}/v1/state/lines/{lid}/overview")
        except URLError as e:
            if is_sim:
                report.fail(f"API overview 不可达 ({lid}): {e}")
            continue

        ds = overview.get("data_source", {})
        if is_sim:
            if not ds.get("connected"):
                report.fail(f"{lid} data_source.connected=false（heartbeat 超时或未推送）")
            else:
                report.ok(f"{lid} 在线 · {ds.get('scenario_id')} · {ds.get('speed_multiplier')}×")
        elif not full:
            report.ok(f"{lid} 登记产线跳过在线状态强检")
            continue

        sim_line = sim.get("lines", {}).get(lid, {})
        api_batch = (overview.get("current_batch") or {}).get("batch_id")
        sim_batch = sim_line.get("batch_id")
        if api_batch and sim_batch and api_batch != sim_batch:
            report.warn(f"{lid} API 当前批次 {api_batch} ≠ 模拟器 {sim_batch}（API 重启后可能短暂不一致）")
        elif api_batch:
            report.ok(f"{lid} 当前批次 {api_batch}")

        # 物料事件序列
        try:
            events = http_get(f"{API_BASE}/v1/material-events?line_id={lid}")
            if line_status == "inactive":
                if events:
                    report.warn(f"{lid} inactive 但有 {len(events)} 条物料事件")
                else:
                    report.ok(f"{lid} inactive 无物料事件（符合预期）")
            elif not events:
                report.warn(f"{lid} 尚无物料事件（模拟刚启动？稍后重试）")
            else:
                report.ok(f"{lid} 物料事件 {len(events)} 条")
                validate_material_event_sequence(report, events, lid, master)
                types = sorted({e.get("event_type") for e in events})
                report.ok(f"{lid} 事件类型: {', '.join(types)}")
        except URLError as e:
            report.fail(f"{lid} material-events 查询失败: {e}")

        if not full and line_status != "active":
            report.ok(f"{lid} 登记产线跳过运行时遥测/趋势细检")
            continue

        # 精细/活跃产线：设备 latest 遥测新鲜度
        try:
            equipment = http_get(f"{API_BASE}/v1/meta/lines/{lid}/equipment")
            if not equipment and not full:
                report.ok(f"{lid} 登记产线无设备元数据（符合预期）")
                continue
            stale = 0
            empty = 0
            for eq in equipment:
                eid = eq["equipment_id"]
                try:
                    latest = http_get(f"{API_BASE}/v1/state/equipment/{eid}/latest")
                except URLError:
                    empty += 1
                    continue
                values = latest.get("values") or {}
                if not values:
                    empty += 1
                ts = latest.get("timestamp")
                if ts and ds.get("connected"):
                    try:
                        t = datetime.fromisoformat(str(ts).replace("Z", "+00:00"))
                        age = (datetime.now(timezone.utc) - t).total_seconds()
                        if age > 120:
                            stale += 1
                    except ValueError:
                        pass
            if empty == len(equipment):
                report.fail(f"{lid} 全部设备无 latest 遥测")
            elif empty > 0:
                report.warn(f"{lid} {empty}/{len(equipment)} 台设备 latest 为空")
            else:
                report.ok(f"{lid} {len(equipment)} 台设备 latest 遥测正常")
            if stale > 0:
                report.warn(f"{lid} {stale} 台设备 timestamp 超过 120s")

        except URLError as e:
            report.fail(f"{lid} equipment/latest 查询失败: {e}")

        # 趋势数据（关键字段，仅仿真线）
        if is_sim and line_status == "active":
            category = line.get("product_category", "flux_core_wire")
            critical = CRITICAL_TREND_FIELDS.get(category, CRITICAL_TREND_FIELDS["flux_core_wire"])
            try:
                trends = http_get(
                    f"{API_BASE}/v1/trends?line_id={lid}&field_ids={critical}&range=buffer"
                )
                total_pts = sum(len(s.get("points", [])) for s in trends.get("series", []))
                if total_pts == 0:
                    report.fail(f"{lid} 关键趋势无数据点")
                else:
                    report.ok(f"{lid} 关键趋势共 {total_pts} 点")
            except URLError as e:
                report.fail(f"{lid} trends 查询失败: {e}")


def validate_cross_layer(report: Report, master: dict[str, Any], config: dict[str, Any]) -> None:
    report.section("四、跨层一致性（主数据 ↔ 种子 ↔ 模拟）")

    config_id_master = master.get("config_id")
    config_id_domain = config.get("config_id")
    if config_id_master != config_id_domain:
        report.fail(f"config_id 不一致: master={config_id_master} domain={config_id_domain}")
    else:
        report.ok(f"config_id 一致: {config_id_master}")

    try:
        factory = http_get(f"{API_BASE}/v1/meta/factory")
        api_lines = http_get(f"{API_BASE}/v1/meta/lines")
    except URLError as e:
        report.fail(f"Meta API 不可达: {e}")
        return

    if factory.get("factory_id") != master.get("factory_id"):
        report.fail("API factory_id 与 master 不一致")
    else:
        report.ok(f"工厂 {factory.get('factory_name')} 种子一致")

    master_line_ids = {l["product_line_id"] for l in master.get("product_lines", [])}
    api_line_ids = {l["product_line_id"] for l in api_lines}
    if master_line_ids != api_line_ids:
        report.fail(f"产线 ID 集合不一致 master={master_line_ids} api={api_line_ids}")
    else:
        report.ok(f"产线元数据 {len(api_line_ids)} 条与 master 一致")

    full_line_ids = {
        l["product_line_id"]
        for l in master.get("product_lines", [])
        if is_full_line(l)
    }
    for lid in master_line_ids:
        if lid not in full_line_ids:
            continue
        try:
            api_eq = http_get(f"{API_BASE}/v1/meta/lines/{lid}/equipment")
            master_eq = [
                e for e in master.get("equipment", [])
                if e["product_line_id"] == lid and e.get("enabled", True)
            ]
            if len(api_eq) != len(master_eq):
                report.fail(f"{lid} 设备数 API={len(api_eq)} master={len(master_eq)}")
        except URLError:
            report.fail(f"{lid} equipment meta 查询失败")
    if len(master_line_ids) - len(full_line_ids) > 0:
        report.ok(f"登记产线 {len(master_line_ids) - len(full_line_ids)} 条跳过 API 设备数对账")

    # twin-layout API ↔ master-data
    try:
        api_twins = http_get(f"{API_BASE}/v1/meta/twin-layouts")
        master_twins = master.get("twin_layouts") or {}
        if set(api_twins.keys()) != set(master_twins.keys()):
            report.fail(
                f"twin-layouts API 产线 {set(api_twins.keys())} ≠ master {set(master_twins.keys())}"
            )
        else:
            for lid in master_twins:
                master_flow = (master_twins[lid].get("flow_path") or [])
                api_flow = (api_twins.get(lid) or {}).get("flowPath") or []
                if master_flow != api_flow:
                    report.fail(f"{lid} API twin flowPath 与 master flow_path 不一致")
                elif len(api_twins[lid].get("nodes") or {}) != len(master_flow):
                    report.fail(f"{lid} API twin nodes 数量与工序链不一致")
                else:
                    report.ok(f"{lid} API twin-layout 与 master-data 一致")
    except URLError as e:
        report.warn(f"twin-layouts API 不可达（跳过 API 孪生校验）: {e}")

    # 进行中批次 API ↔ master（重启后模拟可能已轮转）
    try:
        for b in master.get("product_batches", []):
            if b.get("status") != "in_progress":
                continue
            lid = b["product_line_id"]
            overview = http_get(f"{API_BASE}/v1/state/lines/{lid}/overview")
            api_batch = (overview.get("current_batch") or {}).get("batch_id")
            if api_batch == b["batch_id"]:
                report.ok(f"{lid} 种子进行中批次 {b['batch_id']} 仍为 API 当前批次")
            else:
                report.warn(
                    f"{lid} 种子批次 {b['batch_id']} → API 当前 {api_batch}（模拟轮转或 ingest 更新，属正常动态行为）"
                )
    except URLError:
        pass


def print_summary(report: Report) -> int:
    report.section("校验摘要")
    print(f"  PASS: {len(report.passed)}")
    print(f"  WARN: {len(report.warnings)}")
    print(f"  FAIL: {len(report.failures)}")

    if report.warnings:
        print("\n--- 警告 ---")
        for w in report.warnings[:20]:
            print(f"  ⚠ {w}")
        if len(report.warnings) > 20:
            print(f"  ... 另有 {len(report.warnings) - 20} 条")

    if report.failures:
        print("\n--- 失败 ---")
        for f in report.failures:
            print(f"  ✗ {f}")
        return 1

    print("\n✓ 全部关键校验通过")
    return 0


def validate_data_chain_inline(report: Report) -> None:
    """调用五方对账脚本（内联 import）."""
    import importlib.util
    spec = importlib.util.spec_from_file_location(
        "validate_data_chain",
        ROOT / "scripts/validate-data-chain.py",
    )
    if spec is None or spec.loader is None:
        report.warn("五方对账脚本未找到，跳过")
        return
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    md = mod.load()
    # 复用核心检查：仅将错误/警告汇入 report
    import io
    from contextlib import redirect_stdout
    buf = io.StringIO()
    with redirect_stdout(buf):
        code = mod.main()
    out = buf.getvalue()
    if code == 0:
        report.ok("五方数据链对账通过")
    else:
        for line in out.splitlines():
            if line.strip().startswith("✗"):
                report.fail(line.strip().lstrip("✗ ").strip())
    for line in out.splitlines():
        if "产能加总" in line:
            report.ok(line.strip())


def main() -> int:
    report = Report()
    master = load_json(MASTER_PATH)
    config = load_json(DOMAIN_PATH)

    validate_data_chain_inline(report)
    validate_static_master(report, master, config)
    validate_line_twin_and_product_chain(report, master, config)
    validate_simulator_logic(report, master)
    validate_runtime_api(report, master)
    validate_cross_layer(report, master, config)

    return print_summary(report)


if __name__ == "__main__":
    sys.exit(main())
