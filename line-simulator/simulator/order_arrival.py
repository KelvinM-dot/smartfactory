"""动态订单到达：Poisson 过程 + 季节性因子."""

from __future__ import annotations

import hashlib
import math
import random
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any

from simulator.order_scenarios import scenario_config

ORDER_TYPE_PROFILES: dict[str, dict[str, Any]] = {
    "regular": {"sla": 28, "priority": "normal", "qty_factor": 10},
    "export": {"sla": 18, "priority": "high", "qty_factor": 12},
    "custom": {"sla": 45, "priority": "high", "qty_factor": 7},
}


class OrderArrivalSimulator:
    def __init__(self, master: dict[str, Any], scenario_id: str = "normal_shift") -> None:
        defaults = master.get("simulation_defaults", {})
        arrival_cfg = defaults.get("order_arrival", {})
        self.base_orders_per_day = float(arrival_cfg.get("base_orders_per_day", 0.35))
        self.enabled = bool(arrival_cfg.get("enabled", True))
        self.max_open_orders_per_line = int(arrival_cfg.get("max_open_orders_per_line", 4))
        self.scenario_id = scenario_id
        self._scenario = scenario_config(scenario_id)
        self._lines = [
            l for l in master.get("product_lines", [])
            if l.get("simulation_enabled")
        ]
        self._recipes_by_line = self._index_recipes(master)
        self._products = {
            (p.get("product_category"), p.get("grade")): p
            for p in master.get("products", [])
        }
        self._rng = random.Random(42)

    @staticmethod
    def _index_recipes(master: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
        out: dict[str, list[dict[str, Any]]] = {}
        for recipe in master.get("recipes", []):
            lid = str(recipe.get("product_line_id") or "")
            if lid:
                out.setdefault(lid, []).append(recipe)
        return out

    def configure(self, scenario_id: str) -> None:
        self.scenario_id = scenario_id
        self._scenario = scenario_config(scenario_id)

    def _seasonal_factor(self, now: datetime) -> float:
        month = now.month
        if month in {3, 4, 9, 10}:
            return 1.25
        if month in {1, 2, 7}:
            return 0.82
        return 1.0

    def _pick_order_type(self) -> str:
        export_w = float(self._scenario.get("export_order_weight", 0.22))
        custom_w = 0.08 if self.scenario_id == "custom_insert" else 0.05
        regular_w = max(1.0 - export_w - custom_w, 0.1)
        r = self._rng.random()
        if r < export_w:
            return "export"
        if r < export_w + custom_w:
            return "custom"
        return "regular"

    def _pick_line(self) -> dict[str, Any] | None:
        if not self._lines:
            return None
        bias = self._scenario.get("category_arrival_bias") or {}
        if bias:
            categories = list(bias.keys())
            weights = [float(bias[c]) for c in categories]
            chosen_cat = self._rng.choices(categories, weights=weights, k=1)[0]
            candidates = [l for l in self._lines if l.get("product_category") == chosen_cat]
            if candidates:
                return self._rng.choice(candidates)
        return self._rng.choice(self._lines)

    def _line_code(self, line_id: str) -> str:
        parts = line_id.split("-")
        if len(parts) >= 3:
            return f"{parts[0]}{parts[2]}"
        return line_id.replace("-", "")

    def maybe_generate(
        self,
        *,
        sim_elapsed_sec: float,
        open_orders_by_line: dict[str, int],
    ) -> dict[str, Any] | None:
        if not self.enabled or sim_elapsed_sec <= 0:
            return None

        now = datetime.now(timezone.utc)
        lambda_per_sec = (
            self.base_orders_per_day
            * self._seasonal_factor(now)
            * float(self._scenario.get("arrival_rate_multiplier", 1.0))
            / 86400.0
        )
        prob = 1.0 - math.exp(-lambda_per_sec * sim_elapsed_sec)
        if self._rng.random() > prob:
            return None

        line = self._pick_line()
        if line is None:
            return None
        line_id = str(line["product_line_id"])
        if open_orders_by_line.get(line_id, 0) >= self.max_open_orders_per_line:
            return None

        recipes = self._recipes_by_line.get(line_id, [])
        if not recipes:
            return None
        recipe = self._rng.choice(recipes)
        category = str(line.get("product_category"))
        grade = str(recipe.get("grade") or "standard")
        product = self._products.get((category, grade))
        product_id = str(product["product_id"]) if product else f"PROD-{self._line_code(line_id)}"

        order_type = self._pick_order_type()
        profile = ORDER_TYPE_PROFILES[order_type]
        daily = float(line.get("design_capacity_t_per_day") or 10.0) * 0.55
        planned_t = round(daily * profile["qty_factor"] * self._rng.uniform(0.85, 1.15), 1)
        planned_t = max(min(planned_t, 180.0), daily * 4)

        suffix = f"{self._line_code(line_id)}-ARR-{uuid.uuid4().hex[:6].upper()}"
        oid = f"PO-{now.strftime('%Y%m%d')}-{suffix}"
        return {
            "production_order_id": oid,
            "factory_id": str(line.get("factory_id") or "JQHC-PLANT-01"),
            "customer_order_id": f"SO-{order_type.upper()}-{suffix}",
            "product_id": product_id,
            "product_category": category,
            "grade": grade,
            "recipe_id": str(recipe["recipe_id"]),
            "assigned_line_ids": [line_id],
            "priority": profile["priority"],
            "planned_quantity_t": planned_t,
            "released_quantity_t": 0.0,
            "due_date": (now + timedelta(days=int(profile["sla"]))).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "status": "released",
            "order_type": order_type,
            "delivery_sla_days": int(profile["sla"]),
            "customer_segment": "动态到达",
            "is_export": order_type == "export",
            "remark": f"动态订单到达 · {self.scenario_id}",
            "dynamic_arrival": True,
        }
