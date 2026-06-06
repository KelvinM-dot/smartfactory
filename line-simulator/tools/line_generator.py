#!/usr/bin/env python3
"""生成 42 条产线登记并合并进 jqhc-factory-master-data.json."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

_TOOLS_DIR = Path(__file__).resolve().parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))
from master_data_lib import DETAILED_LINES, MASTER_PATH, load_master, save_master

# 登记产线 2D 孪生模板（与精细产线工序链一致，无 position_3d）
_TWIN_STEP = dict[str, Any]

FCW_TWIN_TEMPLATE: list[_TWIN_STEP] = [
    {"step_id": "cut_strip", "slot": "STRIP", "key_field": "line_speed_m_per_min",
     "node": {"x": 90, "y": 160, "label": "裁带", "icon": "▣"}},
    {"step_id": "powder_mixing", "slot": "MIXER", "key_field": "mixing_rpm",
     "node": {"x": 280, "y": 160, "label": "配粉搅拌", "icon": "◉"}},
    {"step_id": "filling_forming", "slot": "FILLER", "key_field": "fill_ratio_pct",
     "node": {"x": 470, "y": 160, "label": "填充成型", "icon": "▤"}},
    {"step_id": "rough_drawing", "slot": "DRAW-R", "key_field": "tension_kN",
     "node": {"x": 660, "y": 160, "label": "粗拔", "icon": "◎"}},
    {"step_id": "fine_drawing", "slot": "DRAW-F", "key_field": "tension_kN",
     "node": {"x": 850, "y": 160, "label": "细拔", "icon": "◎"}},
    {"step_id": "copper_plating", "slot": "PLATING", "key_field": "coating_thickness_top_um",
     "node": {"x": 1040, "y": 160, "label": "镀铜", "icon": "◈"}},
    {"step_id": "winding", "slot": "WIND", "key_field": "motor_rpm",
     "node": {"x": 1040, "y": 380, "label": "层绕", "icon": "◐"}},
    {"step_id": "packaging", "slot": "PACK", "key_field": "spool_count",
     "node": {"x": 780, "y": 380, "label": "包装", "icon": "▦"}},
    {"step_id": "stock_in_out", "slot": "AGV", "key_field": "location",
     "node": {"x": 520, "y": 380, "label": "出入库", "icon": "⬡"}},
]

SW_TWIN_TEMPLATE: list[_TWIN_STEP] = [
    {"step_id": "rough_drawing", "slot": "DRAW-R", "key_field": "tension_kN",
     "node": {"x": 140, "y": 160, "label": "粗拔", "icon": "◎"}},
    {"step_id": "fine_drawing", "slot": "DRAW-F", "key_field": "tension_kN",
     "node": {"x": 380, "y": 160, "label": "细拔", "icon": "◎"}},
    {"step_id": "copper_plating", "slot": "PLATING", "key_field": "coating_thickness_top_um",
     "node": {"x": 620, "y": 160, "label": "镀铜", "icon": "◈"}},
    {"step_id": "winding", "slot": "WIND", "key_field": "motor_rpm",
     "node": {"x": 860, "y": 160, "label": "层绕", "icon": "◐"}},
    {"step_id": "packaging", "slot": "PACK", "key_field": "spool_count",
     "node": {"x": 1040, "y": 380, "label": "包装", "icon": "▦"}},
    {"step_id": "stock_in_out", "slot": "AGV", "key_field": "location",
     "node": {"x": 680, "y": 380, "label": "出入库", "icon": "⬡"}},
]

WR_TWIN_TEMPLATE: list[_TWIN_STEP] = [
    {"step_id": "wire_drawing", "slot": "DRAW", "key_field": "tension_kN",
     "node": {"x": 120, "y": 160, "label": "拔丝", "icon": "◎"}},
    {"step_id": "cutting", "slot": "CUT", "key_field": "length_mm",
     "node": {"x": 300, "y": 160, "label": "切丝", "icon": "▣"}},
    {"step_id": "powder_mixing", "slot": "MIXER", "key_field": "mixing_rpm",
     "node": {"x": 500, "y": 160, "label": "配粉搅拌", "icon": "◉"}},
    {"step_id": "coating", "slot": "COAT", "key_field": "coating_thickness_mm",
     "node": {"x": 700, "y": 160, "label": "压涂", "icon": "▤"}},
    {"step_id": "drying", "slot": "DRY", "key_field": "actual_temp_C",
     "node": {"x": 900, "y": 160, "label": "烘干", "icon": "◈"}},
    {"step_id": "packaging", "slot": "PACK", "key_field": "package_count",
     "node": {"x": 1040, "y": 380, "label": "包装", "icon": "▦"}},
    {"step_id": "stock_in_out", "slot": "AGV", "key_field": "location",
     "node": {"x": 620, "y": 380, "label": "出入库", "icon": "⬡"}},
]

TWIN_TEMPLATE_BY_CATEGORY: dict[str, list[_TWIN_STEP]] = {
    "flux_core_wire": FCW_TWIN_TEMPLATE,
    "solid_wire": SW_TWIN_TEMPLATE,
    "submerged_arc_wire": SW_TWIN_TEMPLATE,
    "welding_rod": WR_TWIN_TEMPLATE,
}

FCW_STEPS = [
    "cut_strip", "powder_mixing", "filling_forming", "rough_drawing",
    "fine_drawing", "copper_plating", "winding", "packaging", "stock_in_out",
]
SW_STEPS = [
    "rough_drawing", "fine_drawing", "copper_plating", "winding", "packaging", "stock_in_out",
]
WR_STEPS = [
    "wire_drawing", "cutting", "powder_mixing", "coating", "drying", "packaging", "stock_in_out",
]


def _stub_line(
    line_id: str,
    *,
    name: str,
    workshop_id: str,
    product_category: str,
    template_id: str,
    capacity_year: float,
    capacity_day: float,
    status: str = "active",
    simulation_enabled: bool = False,
    twin_3d_ready: bool = False,
    process_steps: list[str],
) -> dict[str, Any]:
    return {
        "product_line_id": line_id,
        "factory_id": "JQHC-PLANT-01",
        "workshop_id": workshop_id,
        "name": name,
        "product_category": product_category,
        "template_id": template_id,
        "config_id": "jqhc-manufacturing",
        "design_capacity_t_per_year": capacity_year,
        "design_capacity_t_per_day": capacity_day,
        "planned_shift_pattern": ["day", "night"] if workshop_id == "WS-WIRE-01" else ["day"],
        "status": status,
        "simulation_enabled": simulation_enabled,
        "detail_level": "full" if simulation_enabled else "registry",
        "twin_3d_ready": twin_3d_ready,
        "process_steps": process_steps,
    }


def build_line_registry() -> list[dict[str, Any]]:
    lines: list[dict[str, Any]] = []

    for n in range(1, 13):
        lid = f"FCW-LINE-{n:02d}"
        is_detail = lid in DETAILED_LINES
        lines.append(_stub_line(
            lid,
            name=f"药芯焊丝 {n} 号线",
            workshop_id="WS-WIRE-01",
            product_category="flux_core_wire",
            template_id="flux_core_wire",
            capacity_year=5500 if n == 7 else 4200,
            capacity_day=18.0 if n == 7 else 14.0,
            status="active" if n != 11 else "maintenance",
            simulation_enabled=is_detail,
            twin_3d_ready=is_detail,
            process_steps=FCW_STEPS,
        ))

    for n in range(1, 9):
        lid = f"SW-LINE-{n:02d}"
        is_detail = lid in DETAILED_LINES
        lines.append(_stub_line(
            lid,
            name=f"实心焊丝 {n} 号线",
            workshop_id="WS-WIRE-01",
            product_category="solid_wire",
            template_id="solid_wire",
            capacity_year=4800,
            capacity_day=15.0,
            status="active" if n != 5 else "maintenance",
            simulation_enabled=is_detail,
            twin_3d_ready=is_detail,
            process_steps=SW_STEPS,
        ))

    for n in range(1, 3):
        lid = f"SAW-LINE-{n:02d}"
        lines.append(_stub_line(
            lid,
            name=f"埋弧焊丝 {n} 号线",
            workshop_id="WS-WIRE-01",
            product_category="submerged_arc_wire",
            template_id="submerged_arc_wire",
            capacity_year=4950,
            capacity_day=15.5,
            status="active",
            process_steps=SW_STEPS,
        ))

    for n in range(1, 21):
        lid = f"WR-LINE-{n:02d}"
        is_detail = lid in DETAILED_LINES
        lines.append(_stub_line(
            lid,
            name=f"焊条 {n} 号线",
            workshop_id="WS-ROD-01",
            product_category="welding_rod",
            template_id="welding_rod",
            capacity_year=15000,
            capacity_day=50.0,
            status="active" if n != 18 else "inactive",
            simulation_enabled=is_detail,
            twin_3d_ready=is_detail,
            process_steps=WR_STEPS,
        ))

    return lines


def factory_profile() -> dict[str, Any]:
    return {
        "legal_name": "天津市金桥焊材科技有限公司",
        "brand_name": "金桥焊材",
        "shareholder": "天津市金桥焊材集团股份有限公司",
        "shareholding_pct": 100,
        "total_investment_cny": 750_000_000,
        "building_area_m2": 83_423.15,
        "single_workshop_area_m2": 80_000,
        "workforce_planned": 405,
        "workforce_traditional_baseline": 1200,
        "labor_reduction_pct": 61,
        "annual_labor_saving_cny": 57_000_000,
        "equipment_count": 1033,
        "wire_line_count": 22,
        "rod_line_count": 20,
        "total_production_line_count": 42,
        "capacity_note": "年产40万吨为产能规模，非产线条数；产线为焊丝22条+焊条20条共42条",
        "annual_capacity_t": 400_000,
        "wire_annual_capacity_t": 100_000,
        "rod_annual_capacity_t": 300_000,
        "production_2024": {
            "rod_t": 249_100,
            "wire_t": 2_964,
            "output_value_cny": 1_086_000_000,
        },
        "targets_2025": {
            "sales_t": 120_000,
            "fcw_sales_t": 25_000,
            "output_value_cny": 1_350_000_000,
        },
        "market": {
            "domestic_share_pct": 33,
            "export_share_pct": 40,
            "export_countries": 100,
            "focus_segments": ["海洋工程", "LNG储罐", "超高压管线", "压力容器", "造船"],
        },
        "delivery_sla": {
            "regular_days_min": 15,
            "regular_days_max": 30,
            "custom_days_min": 45,
            "agv_delivery_accuracy_pct": 99.9,
            "automation_efficiency_gain_pct": 15,
            "rod_line_efficiency_gain_pct": 55,
        },
        "honors": ["天津市首批先进级智能工厂", "天津市雏鹰企业"],
    }


def product_catalog() -> list[dict[str, Any]]:
    grades = [
        ("610", 610, 69, 22.0, 6.0),
        ("690", 690, 69, 20.0, 6.0),
        ("780", 780, 69, 18.0, 6.0),
        ("830", 830, 47, 15.0, 6.0),
        ("960", 960, 47, 12.0, 6.0),
    ]
    catalog: list[dict[str, Any]] = []
    categories = [
        ("flux_core_wire", "药芯气保焊丝"),
        ("solid_wire", "实心气保焊丝"),
        ("submerged_arc_wire", "埋弧焊丝"),
        ("self_shielded_fcw", "自保护药芯焊丝"),
        ("welding_tape", "焊带"),
        ("flux", "焊剂"),
        ("welding_rod", "焊条"),
    ]
    for cat_id, cat_name in categories:
        for grade_code, rm, kv2, elong, i_val in grades:
            catalog.append({
                "catalog_id": f"CAT-{cat_id.upper()}-{grade_code}",
                "product_category": cat_id,
                "category_name": cat_name,
                "grade_code": grade_code,
                "rm_mpa_min": rm,
                "impact_kv2_j_min": kv2,
                "elongation_pct_min": elong,
                "corrosion_i_value_min": i_val,
                "application_domains": ["水电", "海工", "LNG", "管线", "钢结构"],
            })
    return catalog


def annual_production_plan() -> dict[str, Any]:
    return {
        "plan_year": 2025,
        "basis": "金桥焊材科技公司公开产销目标与2024实际结构校准",
        "annual_targets_t": {
            "total": 120_000,
            "flux_core_wire": 25_000,
            "solid_wire": 35_000,
            "submerged_arc_wire": 15_000,
            "self_shielded_fcw": 10_000,
            "welding_rod": 30_000,
            "other": 5_000,
        },
        "monthly_release_t": [
            {"month": 1, "rod": 2100, "wire": 850, "fcw": 180},
            {"month": 2, "rod": 2000, "wire": 820, "fcw": 170},
            {"month": 3, "rod": 2400, "wire": 900, "fcw": 200},
            {"month": 4, "rod": 2500, "wire": 950, "fcw": 220},
            {"month": 5, "rod": 2600, "wire": 1000, "fcw": 240},
            {"month": 6, "rod": 2700, "wire": 1050, "fcw": 260},
            {"month": 7, "rod": 2800, "wire": 1100, "fcw": 280},
            {"month": 8, "rod": 2900, "wire": 1150, "fcw": 300},
            {"month": 9, "rod": 3000, "wire": 1200, "fcw": 320},
            {"month": 10, "rod": 3100, "wire": 1250, "fcw": 340},
            {"month": 11, "rod": 3200, "wire": 1300, "fcw": 360},
            {"month": 12, "rod": 3300, "wire": 1360, "fcw": 380},
        ],
        "export_order_ratio": 0.4,
        "custom_order_ratio": 0.15,
    }


def build_registry_twin_layout(line_id: str, process_steps: list[str], category: str) -> dict[str, Any]:
    template = TWIN_TEMPLATE_BY_CATEGORY.get(category, FCW_TWIN_TEMPLATE)
    flow_path: list[str] = []
    steps: dict[str, Any] = {}
    for defn in template:
        sid = defn["step_id"]
        if sid not in process_steps:
            continue
        flow_path.append(sid)
        steps[sid] = {
            "equipment_id": f"{line_id}-{defn['slot']}",
            "key_field_id": defn["key_field"],
            "node": dict(defn["node"]),
        }
    return {
        "twin_3d_ready": False,
        "detail_level": "registry",
        "view_box": {"w": 1280, "h": 620},
        "flow_path": flow_path,
        "steps": steps,
    }


def build_twin_layouts(data: dict[str, Any], lines: list[dict[str, Any]]) -> dict[str, Any]:
    existing = data.get("twin_layouts") or {}
    layouts: dict[str, Any] = {}
    for line in lines:
        lid = line["product_line_id"]
        if lid in DETAILED_LINES and lid in existing:
            layouts[lid] = existing[lid]
            continue
        layouts[lid] = build_registry_twin_layout(
            lid,
            line.get("process_steps") or [],
            line.get("product_category") or "flux_core_wire",
        )
    return layouts


def patch_agv_data_points(data: dict[str, Any]) -> None:
    """精细产线 AGV 出入库：补齐 status / location 数据点定义."""
    specs = [
        ("AGV-STATION-01", "stock_in_out", "FCW-LINE-07"),
        ("AGV-SW-01", "stock_in_out", "SW-LINE-02"),
        ("AGV-WR-01", "stock_in_out", "WR-LINE-01"),
    ]
    points = data.setdefault("data_points", [])
    existing = {(p.get("equipment_id"), p.get("field_id") or p.get("data_point_id")) for p in points}
    for eq_id, step_id, line_id in specs:
        for field_id, display, unit in [
            ("status", "设备状态", ""),
            ("location", "库位", ""),
        ]:
            if (eq_id, field_id) in existing:
                continue
            points.append({
                "data_point_id": f"{eq_id}_{field_id}",
                "field_id": field_id,
                "equipment_id": eq_id,
                "product_line_id": line_id,
                "display_name": display,
                "process_step_id": step_id,
                "unit": unit,
                "data_category": "process_parameter" if field_id != "status" else "equipment_status",
            })
            existing.add((eq_id, field_id))


def patch_sim_stock_key_fields(data: dict[str, Any]) -> None:
    """仿真产线出入库关键字段与 data_points 对齐（优先 location）."""
    for lid in DETAILED_LINES:
        layout = data.get("twin_layouts", {}).get(lid)
        if not isinstance(layout, dict):
            continue
        steps = layout.get("steps") or {}
        stock = steps.get("stock_in_out")
        if isinstance(stock, dict) and stock.get("key_field_id") == "status":
            stock["key_field_id"] = "location"


def patch_master(data: dict[str, Any]) -> dict[str, Any]:
    data["factory_name"] = "金桥焊材科技公司"
    data["factory_profile"] = factory_profile()
    data["product_catalog"] = product_catalog()
    data["line_registry"] = build_line_registry()
    data["annual_production_plan"] = annual_production_plan()

    detailed = {lid: next(l for l in build_line_registry() if l["product_line_id"] == lid)
                for lid in DETAILED_LINES}
    for lid, line in detailed.items():
        line["simulation_enabled"] = True
        line["detail_level"] = "full"
        line["twin_3d_ready"] = True
    detailed["FCW-LINE-07"]["status"] = "active"
    detailed["SW-LINE-02"]["status"] = "active"
    detailed["WR-LINE-01"]["status"] = "active"
    detailed["FCW-LINE-07"]["design_capacity_t_per_year"] = 5500
    detailed["FCW-LINE-07"]["design_capacity_t_per_day"] = 18.0
    detailed["WR-LINE-01"]["design_capacity_t_per_year"] = 15000
    detailed["WR-LINE-01"]["design_capacity_t_per_day"] = 50.0

    data["product_lines"] = build_line_registry()
    data["twin_layouts"] = build_twin_layouts(data, data["product_lines"])
    patch_agv_data_points(data)
    patch_sim_stock_key_fields(data)

    workshops = data.get("workshops", [])
    for ws in workshops:
        if ws.get("workshop_id") == "WS-WIRE-01":
            ws["line_ids"] = [l["product_line_id"] for l in data["product_lines"]
                              if l.get("workshop_id") == "WS-WIRE-01"]
            ws["remark"] = "真实工厂 22 条焊丝一体化产线（12 药芯 + 8 实心 + 2 埋弧），当前 3 条精细仿真"
        if ws.get("workshop_id") == "WS-ROD-01":
            ws["line_ids"] = [l["product_line_id"] for l in data["product_lines"]
                              if l.get("workshop_id") == "WS-ROD-01"]
            ws["remark"] = "真实工厂 20 条焊条一体化产线，当前 1 条精细仿真"

    sim = data.setdefault("simulation_defaults", {})
    sim["all_line_ids"] = [l["product_line_id"] for l in data["product_lines"]
                           if l.get("simulation_enabled")]
    sim["simulation_line_ids"] = list(sim["all_line_ids"])
    sim["registry_line_count"] = len(data["product_lines"])
    sim["equipment_per_line_by_category"] = {
        "flux_core_wire": 9,
        "solid_wire": 6,
        "submerged_arc_wire": 6,
        "welding_rod": 7,
    }
    sim["plant_utilization_pct"] = 78
    sim["default_green_power_ratio_pct"] = 60.0
    sim["agv_success_rate_pct"] = 99.9
    sim["delivery_sla_regular_days"] = 30
    sim["delivery_sla_custom_days"] = 45
    existing_ids = {s.get("id") for s in sim.get("scenarios", []) if isinstance(s, dict)}
    extra = [
        {"id": "ramp_up_fcw_2025", "label": "药芯产能爬坡", "description": "2025 药芯焊丝 2.5 万吨目标爬坡"},
        {"id": "rod_peak_season", "label": "焊条旺季", "description": "焊条产线接近 30 万吨年化节奏"},
        {"id": "export_rush", "label": "出口订单集中", "description": "出口订单占比 40%，交期 15 天压力"},
        {"id": "clean_energy_noon", "label": "午间绿电高峰", "description": "光伏出力高峰，烘干工序能耗下降"},
        {"id": "hydro_project_custom", "label": "海工定制单", "description": "Rm≥830 高牌号 + 45 天交期"},
    ]
    sim["scenarios"] = list(sim.get("scenarios", [])) + [s for s in extra if s["id"] not in existing_ids]

    for asset in data.get("energy_assets", []):
        if asset.get("asset_type") == "pv":
            asset["annual_generation_kwh"] = 7_000_000
            asset["panel_area_m2"] = 112_000
            asset["rated_power_kw"] = 7000
        if asset.get("asset_type") == "wind":
            asset["annual_generation_kwh"] = 3_000_000
            asset["rated_power_kw"] = 2000
        if asset.get("asset_type") == "waste_heat":
            asset["energy_saving_pct"] = 30
            asset["annual_carbon_reduction_t"] = 35_000

    shifts = data.get("shift_calendars", [])
    for sh in shifts:
        if sh.get("workshop_id") == "WS-WIRE-01" and sh.get("shift_type") == "day":
            sh["planned_headcount"] = 112
            sh["actual_headcount"] = 108
        if sh.get("workshop_id") == "WS-WIRE-01" and sh.get("shift_type") == "night":
            sh["planned_headcount"] = 98
            sh["actual_headcount"] = 96
        if sh.get("workshop_id") == "WS-ROD-01":
            sh["planned_headcount"] = 81
            sh["actual_headcount"] = 78

    return data


def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else MASTER_PATH
    data = patch_master(load_master(path))
    try:
        from data_completeness_patch import patch_master as patch_completeness
        data = patch_completeness(data)
    except ImportError:
        import importlib.util
        spec = importlib.util.spec_from_file_location(
            "data_completeness_patch",
            Path(__file__).resolve().parent / "data_completeness_patch.py",
        )
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        data = mod.patch_master(data)
    save_master(data, path)
    twin_count = len(data.get("twin_layouts", {}))
    sim_count = len([l for l in data["product_lines"] if l.get("simulation_enabled")])
    print(f"Updated {path}: {len(data['product_lines'])} lines, "
          f"{sim_count} simulation-enabled, {twin_count} twin_layouts")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
