"""全厂多产线并行仿真引擎."""

from __future__ import annotations

from typing import Any

from simulator.config import POC_LIGHTWEIGHT_MODE, POC_SIM_LINE_IDS
from simulator.engine import SimulationEngine
from simulator.master_data import get_line, load_master_data
from simulator.order_arrival import OrderArrivalSimulator
from simulator.order_scenarios import apply_scenario_on_start


class PlantSimulationEngine:
    def __init__(self, master_path: str | None = None) -> None:
        self.master_path = master_path
        self.master = load_master_data(master_path)
        self.scenario_id = "normal_shift"
        self.speed_multiplier = 1.0
        self.green_shift_pct: float | None = None
        self.running = False
        self.engines: dict[str, SimulationEngine] = {}
        self.order_arrival = OrderArrivalSimulator(self.master, self.scenario_id)
        self.scenario_injected_orders: list[dict[str, Any]] = []
        for line in self.master.get("product_lines", []):
            if not line.get("simulation_enabled", False):
                continue
            line_id = str(line.get("product_line_id"))
            if POC_LIGHTWEIGHT_MODE and line_id not in POC_SIM_LINE_IDS:
                continue
            self.engines[line_id] = SimulationEngine(
                master_path=master_path,
                line_id=line_id,
            )

    def configure(
        self,
        scenario_id: str,
        speed_multiplier: float,
        target_line_id: str | None = None,
        green_shift_pct: float | None = None,
    ) -> None:
        self.scenario_id = scenario_id
        self.speed_multiplier = speed_multiplier
        self.green_shift_pct = green_shift_pct
        self.order_arrival.configure(scenario_id)
        targets = [target_line_id] if target_line_id else list(self.engines.keys())
        for line_id in targets:
            if line_id in self.engines:
                self.engines[line_id].configure(
                    scenario_id, speed_multiplier, line_id, green_shift_pct=green_shift_pct
                )

    def start_line(self, line_id: str) -> list[dict[str, Any]]:
        eng = self.engines.get(line_id)
        if eng is None:
            raise KeyError(f"unknown line: {line_id}")
        return eng.start()

    def start_all(self) -> list[dict[str, Any]]:
        self.running = True
        self.scenario_injected_orders = apply_scenario_on_start(self, self.scenario_id)
        events: list[dict[str, Any]] = []
        for line_id, eng in self.engines.items():
            events.extend(eng.start())
        return events

    def open_order_counts(self) -> dict[str, int]:
        counts: dict[str, int] = {}
        for eng in self.engines.values():
            line_id = eng.process.line_id
            open_n = sum(
                1 for item in eng.process.order_queue
                if str(item.get("status") or "") not in {"completed"}
            )
            counts[line_id] = open_n
        return counts

    def drain_order_status_updates(self) -> list[dict[str, Any]]:
        updates: list[dict[str, Any]] = []
        for eng in self.engines.values():
            updates.extend(eng.process.drain_order_status_updates())
        return updates

    def stop(self) -> None:
        self.running = False
        for eng in self.engines.values():
            eng.stop()

    def tick_all(self) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        records: list[dict[str, Any]] = []
        events: list[dict[str, Any]] = []
        for eng in self.engines.values():
            if not eng.state.running:
                continue
            recs, evs = eng.tick_records()
            records.extend(recs)
            events.extend(evs)
        return records, events

    def inject_alarm(self, line_id: str | None = None, **kwargs: Any) -> dict[str, Any]:
        target = line_id or "FCW-LINE-07"
        if target not in self.engines:
            raise KeyError(f"unknown line: {target}")
        eng = self.engines[target]
        return eng.inject_alarm(**kwargs)

    def get_engine(self, line_id: str) -> SimulationEngine:
        return self.engines[line_id]

    def status(self) -> dict[str, Any]:
        energy_breakdown = [eng.energy_snapshot() for eng in self.engines.values()]
        total_consumption = round(sum(item.get("power_kwh", 0.0) for item in energy_breakdown), 2)
        total_green = round(sum(item.get("green_power_kwh", 0.0) for item in energy_breakdown), 2)
        total_grid = round(sum(item.get("grid_power_kwh", 0.0) for item in energy_breakdown), 2)
        return {
            "running": self.running,
            "poc_lightweight_mode": POC_LIGHTWEIGHT_MODE,
            "active_sim_lines": len(self.engines),
            "scenario_id": self.scenario_id,
            "speed_multiplier": self.speed_multiplier,
            "green_shift_pct": self.green_shift_pct,
            "factory_energy": {
                "total_consumption_kwh": total_consumption,
                "green_power_kwh": total_green,
                "grid_power_kwh": total_grid,
                "green_power_ratio_pct": round((total_green / total_consumption * 100.0), 1) if total_consumption else 0.0,
                "line_breakdown": energy_breakdown,
            },
            "lines": {
                line_id: {
                    **eng.status(),
                    "line_name": get_line(line_id, self.master).get("name"),
                    "line_status": get_line(line_id, self.master).get("status"),
                }
                for line_id, eng in self.engines.items()
            },
        }
