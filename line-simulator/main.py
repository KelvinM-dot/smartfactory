"""FastAPI 控制面 + 后台推送线程."""

from __future__ import annotations

import logging
import threading
import time
from contextlib import asynccontextmanager
from typing import Any, Optional

from fastapi import FastAPI
from pydantic import BaseModel, Field

from simulator.bootstrap import bootstrap_all_lines, bootstrap_runtime_data
from simulator.client import DataplatformClient
from simulator.config import (
    DATAPLATFORM_URL,
    DEFAULT_SCENARIO_ID,
    DEFAULT_SPEED_MULTIPLIER,
    MASTER_DATA_PATH,
    PUSH_INTERVAL_SEC,
    RESET_DB_BEFORE_START,
    SIM_AUTO_START,
    SIMULATOR_HOST,
    SIMULATOR_PORT,
    SOURCE_INSTANCE,
)
from simulator.storage import sqlite_config_public
from simulator.order_scenarios import apply_scenario_on_start
from simulator.plant_engine import PlantSimulationEngine
from simulator.reseed import maybe_reseed_master_data, reseed_master_data

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

plant = PlantSimulationEngine(master_path=MASTER_DATA_PATH)
client = DataplatformClient(DATAPLATFORM_URL, source_instance=SOURCE_INSTANCE)
_loop_stop = threading.Event()
_loop_thread: Optional[threading.Thread] = None
_last_order_sync_ts = 0.0
_sim_elapsed_sec = 0.0


class StartRequest(BaseModel):
    scenario_id: str = "normal_shift"
    speed_multiplier: float = Field(default=1.0, ge=0.1, le=120.0)
    target_line_id: str = "FCW-LINE-07"
    reset_db: Optional[bool] = Field(
        default=None,
        description="为 true 时启动前先清库并重灌主数据；为 null 时使用 config.RESET_DB_BEFORE_START",
    )


class ScenarioRequest(BaseModel):
    scenario_id: str
    green_shift_pct: Optional[float] = Field(
        default=None, ge=0, le=100,
        description="能碳专用：全厂绿电排产偏移 0–100%，覆盖 plant_green_shift_* 场景",
    )


class AlarmRequest(BaseModel):
    line_id: str = "FCW-LINE-07"
    equipment_id: Optional[str] = None
    alarm_code: Optional[str] = None
    alarm_message: Optional[str] = None
    severity: Optional[str] = None
    duration_ticks: int = Field(default=30, ge=5, le=600)


def _extract_batch_updates(events: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    material_events: list[dict[str, Any]] = []
    batch_updates: list[dict[str, Any]] = []
    for event in events:
        if "__batch_updates__" in event:
            batch_updates.extend(event["__batch_updates__"])
        else:
            material_events.append(event)
    return material_events, batch_updates


def _start_simulation(
    *,
    scenario_id: str,
    speed_multiplier: float,
    target_line_id: str | None = None,
    reset_db: bool | None = None,
) -> dict[str, Any]:
    """统一启动流程：reseed → configure → bootstrap → start → demo alarms."""
    plant.stop()
    reseed_result = maybe_reseed_master_data(client, force=reset_db)
    if target_line_id:
        plant.configure(scenario_id, speed_multiplier, target_line_id)
        bootstrap = bootstrap_runtime_data(client, target_line_id)
        plant.scenario_injected_orders = apply_scenario_on_start(plant, scenario_id)
        _push_scenario_orders_to_api(plant.scenario_injected_orders)
        init_events = plant.start_line(target_line_id)
        plant.running = True
        if init_events:
            try:
                client.push_events(init_events)
                plant.get_engine(target_line_id).state.events_sent += len(init_events)
            except Exception as e:
                log.warning("Initial material events push failed: %s", e)
    else:
        plant.configure(scenario_id, speed_multiplier)
        bootstrap = bootstrap_all_lines(client)
        init_events = plant.start_all()
        _push_scenario_orders_to_api(plant.scenario_injected_orders)
        if init_events:
            try:
                client.push_events(init_events)
            except Exception as e:
                log.warning("Initial material events push failed: %s", e)
    demo_alarms = _push_demo_alarms()
    return {
        "ok": True,
        "reseed": reseed_result,
        "bootstrap": bootstrap,
        "demo_alarms_pushed": demo_alarms,
        "status": plant.status(),
    }


def _push_demo_alarms() -> int:
    """启动后为活跃产线注入演示报警，保证报警中心有初始数据."""
    demo_configs = [
        ("FCW-LINE-07", "FILLER-01", "E205", "填充率超规格", "warning"),
        ("SW-LINE-02", "PLATING-SW-01", "E101", "镀铜断丝", "critical"),
    ]
    pushed = 0
    for line_id, equipment_id, code, message, severity in demo_configs:
        eng = plant.engines.get(line_id)
        if eng is None or not eng.state.running:
            continue
        try:
            event = eng.inject_alarm(
                equipment_id=equipment_id,
                alarm_code=code,
                alarm_message=message,
                severity=severity,
            )
            client.push_events([event])
            pushed += 1
        except Exception as e:
            log.warning("Demo alarm push failed for %s: %s", line_id, e)
    return pushed


def _push_scenario_orders_to_api(orders: list[dict[str, Any]]) -> None:
    for order in orders:
        try:
            client.create_order(order)
        except Exception as exc:
            log.warning("Scenario order push failed for %s: %s", order.get("production_order_id"), exc)


def _sync_order_status_updates() -> None:
    for upd in plant.drain_order_status_updates():
        try:
            client.patch_order_status(
                str(upd.get("production_order_id")),
                str(upd.get("status")),
                str(upd.get("remark") or "") or None,
            )
        except Exception as exc:
            log.warning("Order status patch failed for %s: %s", upd.get("production_order_id"), exc)


def _maybe_dynamic_order_arrival(sim_elapsed_sec: float) -> None:
    new_order = plant.order_arrival.maybe_generate(
        sim_elapsed_sec=sim_elapsed_sec,
        open_orders_by_line=plant.open_order_counts(),
    )
    if not new_order:
        return
    try:
        client.create_order(new_order)
        log.info(
            "Dynamic order arrived: %s → %s (%.1ft)",
            new_order.get("production_order_id"),
            new_order.get("assigned_line_ids"),
            float(new_order.get("planned_quantity_t") or 0),
        )
    except Exception as exc:
        log.warning("Dynamic order create failed: %s", exc)


def _push_loop() -> None:
    global _last_order_sync_ts, _sim_elapsed_sec
    while not _loop_stop.is_set():
        interval = PUSH_INTERVAL_SEC / max(plant.speed_multiplier, 0.1)
        if not plant.running:
            time.sleep(0.2)
            continue
        try:
            now_ts = time.time()
            if now_ts - _last_order_sync_ts >= 5.0:
                try:
                    orders = client.fetch_orders("JQHC-PLANT-01")
                    orders_by_id = {
                        str(item.get("production_order_id")): item
                        for item in orders
                        if item.get("production_order_id")
                    }
                    for eng in plant.engines.values():
                        line_id = eng.process.line_id
                        refreshed_queue = []
                        for item in eng.process.order_queue:
                            order_id = str(item.get("production_order_id") or "")
                            merged = {**item, **orders_by_id.get(order_id, {})}
                            planned_t = float(merged.get("planned_quantity_t") or 0.0)
                            released_t = float(merged.get("released_quantity_t") or 0.0)
                            if order_id and order_id not in eng.process.order_remaining_kg:
                                eng.process.order_remaining_kg[order_id] = max((planned_t - released_t) * 1000.0, 0.0)
                            refreshed_queue.append(merged)
                        queued_ids = {
                            str(item.get("production_order_id") or "")
                            for item in refreshed_queue
                            if item.get("production_order_id")
                        }
                        for item in orders:
                            order_id = str(item.get("production_order_id") or "")
                            assigned_lines = item.get("assigned_line_ids") or []
                            if order_id and order_id not in queued_ids and line_id in assigned_lines:
                                order_doc = dict(item)
                                eng.process.register_order(order_doc)
                                refreshed_queue.append(order_doc)
                        refreshed_queue.sort(
                            key=lambda o: (
                                {"high": 0, "normal": 1, "low": 2}.get(str(o.get("priority")), 1),
                                str(o.get("due_date") or ""),
                            )
                        )
                        eng.process.order_queue = refreshed_queue
                    _last_order_sync_ts = now_ts
                except Exception as sync_error:
                    log.warning("Order sync failed: %s", sync_error)

            _sim_elapsed_sec += interval * max(plant.speed_multiplier, 0.1)
            _maybe_dynamic_order_arrival(interval * max(plant.speed_multiplier, 0.1))

            records, events = plant.tick_all()
            material_events, batch_updates = _extract_batch_updates(events)
            _sync_order_status_updates()

            if records:
                client.push_telemetry(records)
            if material_events:
                client.push_events(material_events)
            if batch_updates:
                client.push_batches(batch_updates)

            for line_id, eng in plant.engines.items():
                if eng.state.running:
                    client.heartbeat(
                        line_id,
                        plant.scenario_id,
                        plant.speed_multiplier,
                        runtime=eng.heartbeat_runtime(),
                    )
        except Exception as e:
            log.warning("Push failed: %s", e)
        time.sleep(interval)


def _start_loop() -> None:
    global _loop_thread
    if _loop_thread and _loop_thread.is_alive():
        return
    _loop_stop.clear()
    _loop_thread = threading.Thread(target=_push_loop, daemon=True)
    _loop_thread.start()


def _auto_start_simulation() -> None:
    """POC: start all lines after boot when config or master-data 允许自动启动."""
    from simulator.master_data import get_simulation_defaults, load_master_data

    defaults = get_simulation_defaults(load_master_data(MASTER_DATA_PATH))
    if not SIM_AUTO_START and not defaults.get("auto_start_on_boot", False):
        return

    scenario_id = str(defaults.get("default_scenario_id", DEFAULT_SCENARIO_ID))
    speed_multiplier = float(defaults.get("default_speed_multiplier", DEFAULT_SPEED_MULTIPLIER))

    deadline = time.monotonic() + 60.0
    delay = 0.5
    attempt = 0
    while time.monotonic() < deadline:
        attempt += 1
        try:
            client.wait_until_ready(timeout_sec=min(15.0, deadline - time.monotonic()))
            result = _start_simulation(
                scenario_id=scenario_id,
                speed_multiplier=speed_multiplier,
            )
            log.info(
                "Auto-started simulation: scenario=%s speed=%sx bootstrap=%s",
                scenario_id,
                speed_multiplier,
                result.get("bootstrap"),
            )
            return
        except Exception as e:
            log.warning("Auto-start attempt %s failed: %s", attempt, e)
            plant.stop()
            time.sleep(delay)
            delay = min(delay * 1.6, 5.0)
    log.error("Auto-start gave up after 60s; ensure API on :3001 then POST /sim/start-all")


def _schedule_auto_start() -> None:
    threading.Thread(target=_auto_start_simulation, daemon=True).start()


@asynccontextmanager
async def lifespan(app: FastAPI):
    _start_loop()
    _schedule_auto_start()
    log.info(
        "Simulator ready (push loop active). Auto-start enabled by master-data/env will run in background; manual start also available: POST /sim/start-all or /sim/start"
    )
    yield
    _loop_stop.set()
    client.close()


app = FastAPI(title="Line Simulator", version="0.3.0", lifespan=lifespan)


@app.post("/sim/start")
def sim_start(req: StartRequest) -> dict[str, Any]:
    return _start_simulation(
        scenario_id=req.scenario_id,
        speed_multiplier=req.speed_multiplier,
        target_line_id=req.target_line_id,
        reset_db=req.reset_db,
    )


@app.post("/sim/start-all")
def sim_start_all(req: StartRequest) -> dict[str, Any]:
    return _start_simulation(
        scenario_id=req.scenario_id,
        speed_multiplier=req.speed_multiplier,
        reset_db=req.reset_db,
    )


@app.post("/sim/reseed")
def sim_reseed() -> dict[str, Any]:
    """清库并重灌 jqhc-factory-master-data.json（不启动仿真）."""
    plant.stop()
    result = reseed_master_data(client)
    return {"ok": True, "reseed": result, "status": plant.status()}


@app.post("/sim/stop")
def sim_stop() -> dict[str, Any]:
    plant.stop()
    return {"ok": True, "status": plant.status()}


@app.post("/sim/scenario")
def sim_scenario(req: ScenarioRequest) -> dict[str, Any]:
    plant.configure(
        req.scenario_id,
        plant.speed_multiplier,
        green_shift_pct=req.green_shift_pct,
    )
    injected = apply_scenario_on_start(plant, req.scenario_id)
    if injected:
        plant.scenario_injected_orders.extend(injected)
        _push_scenario_orders_to_api(injected)
    return {
        "ok": True,
        "scenario_id": req.scenario_id,
        "green_shift_pct": req.green_shift_pct,
        "scenario_orders_injected": len(injected),
        "status": plant.status(),
    }


@app.post("/sim/inject-alarm")
def sim_inject_alarm(req: AlarmRequest = AlarmRequest()) -> dict[str, Any]:
    line_id = req.line_id
    try:
        event = plant.inject_alarm(
            line_id,
            equipment_id=req.equipment_id,
            alarm_code=req.alarm_code,
            alarm_message=req.alarm_message,
            severity=req.severity,
            duration_ticks=req.duration_ticks,
        )
        client.push_events([event])
        plant.get_engine(line_id).state.events_sent += 1
    except KeyError as e:
        return {"ok": False, "error": str(e)}
    except Exception as e:
        return {"ok": False, "error": str(e)}
    return {"ok": True, "event": event}


@app.get("/sim/scenarios")
def sim_scenarios() -> dict[str, Any]:
    from simulator.master_data import get_alarm_catalog, get_scenarios, load_master_data

    master = load_master_data(MASTER_DATA_PATH)
    return {
        "scenarios": get_scenarios(master),
        "alarm_catalog": get_alarm_catalog(master),
    }


@app.get("/sim/status")
def sim_status() -> dict[str, Any]:
    status = plant.status()
    status["dataplatform_url"] = DATAPLATFORM_URL
    status["storage"] = sqlite_config_public()
    return status


@app.get("/sim/config")
def sim_config() -> dict[str, Any]:
    from simulator.config import resolve_master_data_path

    return {
        "dataplatform_url": DATAPLATFORM_URL,
        "push_interval_sec": PUSH_INTERVAL_SEC,
        "source_instance": SOURCE_INSTANCE,
        "master_data_path": str(resolve_master_data_path()),
        "sim_auto_start": SIM_AUTO_START,
        "default_scenario_id": DEFAULT_SCENARIO_ID,
        "default_speed_multiplier": DEFAULT_SPEED_MULTIPLIER,
        "reset_db_before_start": RESET_DB_BEFORE_START,
        "storage": sqlite_config_public(),
    }


@app.get("/sim/master-data/summary")
def master_data_summary() -> dict[str, Any]:
    from simulator.master_data import load_master_data

    master = load_master_data(MASTER_DATA_PATH)
    return {
        "factory_id": master.get("factory_id"),
        "product_lines": len(master.get("product_lines", [])),
        "equipment": len(master.get("equipment", [])),
        "data_points": len(master.get("data_points", [])),
        "recipes": len(master.get("recipes", [])),
        "simulation_defaults": master.get("simulation_defaults", {}),
    }


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "dataplatform_url": DATAPLATFORM_URL,
        "storage": sqlite_config_public(),
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host=SIMULATOR_HOST, port=SIMULATOR_PORT, reload=False)
