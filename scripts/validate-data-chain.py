#!/usr/bin/env python3
"""五方对账：订单 ↔ 批次 ↔ WIP ↔ 成品 ↔ 原料."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MASTER_PATH = ROOT / "schemas/智造数据台/presets/jqhc-factory-master-data.json"


def load() -> dict:
    with MASTER_PATH.open(encoding="utf-8") as f:
        return json.load(f)


def main() -> int:
    md = load()
    errors: list[str] = []
    warnings: list[str] = []

    orders = {o["production_order_id"]: o for o in md.get("production_orders", [])}
    batches = {b["batch_id"]: b for b in md.get("product_batches", [])}
    material_batches = {m["batch_id"] for m in md.get("material_batches", [])}
    products = {p["product_id"]: p for p in md.get("products", [])}
    lines = {l["product_line_id"]: l for l in md.get("product_lines", [])}

    snapshots = md.get("inventory_snapshots", {})
    wip_list = snapshots.get("wip", [])
    finished_list = snapshots.get("finished", [])

    # 1. 订单 → 产品 / 产线
    for oid, order in orders.items():
        pid = order.get("product_id")
        if pid and pid not in products:
            errors.append(f"订单 {oid} 引用未知产品 {pid}")
        for lid in order.get("assigned_line_ids") or []:
            if lid not in lines:
                errors.append(f"订单 {oid} 引用未知产线 {lid}")

    # 2. 批次 → 订单 / 产线 / 配方
    for bid, batch in batches.items():
        oid = batch.get("production_order_id")
        if oid and oid not in orders:
            errors.append(f"批次 {bid} 引用未知订单 {oid}")
        lid = batch.get("product_line_id")
        if lid and lid not in lines:
            errors.append(f"批次 {bid} 引用未知产线 {lid}")
        for parent in batch.get("parent_batches") or []:
            if parent not in material_batches and parent not in batches:
                warnings.append(f"批次 {bid} 父批次 {parent} 未在原料/成品批登记")

    # 3. WIP → in_progress 批次
    ip_ids = {b["batch_id"] for b in batches.values() if b.get("status") == "in_progress"}
    for wip in wip_list:
        pb = wip.get("product_batch")
        if pb not in batches:
            errors.append(f"WIP {wip.get('wip_id')} 引用未知批次 {pb}")
        elif pb not in ip_ids:
            warnings.append(f"WIP {wip.get('wip_id')} 批次 {pb} 非 in_progress")
        step = wip.get("process_step_id")
        lid = wip.get("location", "").split("/")[0] if wip.get("location") else ""
        if lid and step and lid in lines and step not in lines[lid].get("process_steps", []):
            errors.append(f"WIP {wip.get('wip_id')} 工序 {step} 不在产线 {lid} 工序链")

    # 4. 成品库 → 已完成批次
    completed_ids = {
        b["batch_id"] for b in batches.values()
        if b.get("status") in ("completed", "released")
    }
    for fg in finished_list:
        bid = fg.get("batch_id")
        if bid not in batches:
            errors.append(f"成品库 {bid} 无对应 product_batches 主档")
        elif bid not in completed_ids:
            warnings.append(f"成品库 {bid} 对应批次状态非 completed/released")
        batch = batches.get(bid, {})
        if batch and fg.get("grade") and batch.get("grade") != fg.get("grade"):
            errors.append(f"成品库 {bid} grade {fg.get('grade')} ≠ 批次 {batch.get('grade')}")

    # 5. 订单交期年份与演示轴（2025）
    for oid, order in orders.items():
        due = str(order.get("due_date", ""))
        if due.startswith("2026-"):
            errors.append(f"订单 {oid} due_date 仍为 2026: {due}")

    # 6. 批次号年份（禁止 202606 残留）
    for bid in list(batches) + [w.get("product_batch") for w in wip_list] + [f.get("batch_id") for f in finished_list]:
        if bid and "202606" in str(bid):
            errors.append(f"批次 ID 仍含 202606: {bid}")

    # 7. 订单 ↔ 批次释放量（每条订单至少有一条关联批次或已释放量>0）
    batches_by_order: dict[str, list[str]] = {}
    for bid, batch in batches.items():
        oid = batch.get("production_order_id")
        if oid:
            batches_by_order.setdefault(oid, []).append(bid)
    for oid, order in orders.items():
        linked = batches_by_order.get(oid, [])
        released = float(order.get("released_quantity_t") or 0)
        if not linked and released <= 0 and order.get("status") not in ("draft", "cancelled"):
            warnings.append(f"订单 {oid} 无关联批次且 released_quantity_t=0")

    # 8. 设计产能加总 ↔ factory_profile（焊丝10万/焊条30万/合计40万）
    profile = md.get("factory_profile", {})
    wire_cats = {"flux_core_wire", "solid_wire", "submerged_arc_wire"}
    wire_cap = sum(
        l.get("design_capacity_t_per_year", 0)
        for l in md.get("product_lines", [])
        if l.get("product_category") in wire_cats
    )
    rod_cap = sum(
        l.get("design_capacity_t_per_year", 0)
        for l in md.get("product_lines", [])
        if l.get("product_category") == "welding_rod"
    )
    target_wire = profile.get("wire_annual_capacity_t", 100_000)
    target_rod = profile.get("rod_annual_capacity_t", 300_000)
    target_total = profile.get("annual_capacity_t", 400_000)
    if abs(wire_cap - target_wire) >= 500:
        errors.append(f"焊丝设计产能加总 {wire_cap:.0f} ≠ 目标 {target_wire}")
    if abs(rod_cap - target_rod) >= 500:
        errors.append(f"焊条设计产能加总 {rod_cap:.0f} ≠ 目标 {target_rod}")
    if abs(wire_cap + rod_cap - target_total) >= 1000:
        errors.append(f"全厂设计产能 {wire_cap + rod_cap:.0f} ≠ 目标 {target_total}")
    else:
        print(f"  产能加总：焊丝 {wire_cap:.0f}t + 焊条 {rod_cap:.0f}t = {wire_cap + rod_cap:.0f}t（目标 {target_total}t）")

    # 9. 埋弧示范链
    saw_orders = [o for o in orders.values() if o.get("product_category") == "submerged_arc_wire"]
    if not saw_orders:
        warnings.append("无埋弧焊丝示范订单")
    for o in saw_orders:
        if not batches_by_order.get(o["production_order_id"]):
            warnings.append(f"埋弧订单 {o['production_order_id']} 无关联批次")

    print("\n=== 五方数据链对账 ===")
    print(f"订单 {len(orders)} · 批次 {len(batches)} · WIP {len(wip_list)} · 成品 {len(finished_list)}")
    print(f"错误 {len(errors)} · 警告 {len(warnings)}\n")
    for e in errors:
        print(f"  ✗ {e}")
    for w in warnings:
        print(f"  ⚠ {w}")
    if errors:
        return 1
    print("\n✓ 五方对账通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
