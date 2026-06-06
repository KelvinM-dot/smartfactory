"""Push baseline master data snapshots to dataplatform on sim start."""

from __future__ import annotations

import logging
from typing import Any

from simulator.client import DataplatformClient
from simulator.master_data import load_master_data

log = logging.getLogger(__name__)


def bootstrap_runtime_data(client: DataplatformClient, line_id: str) -> dict[str, Any]:
    """Ensure active batch exists on dataplatform; meta is seeded by Java API."""
    master = load_master_data()
    batches = [
        b for b in master.get("product_batches", [])
        if b.get("product_line_id") == line_id and b.get("status") == "in_progress"
    ]
    if not batches:
        return {"pushed_batches": 0, "line_id": line_id}

    try:
        resp = client.push_batches(batches)
        log.info("Bootstrap pushed %s in-progress batch(es) for %s", len(batches), line_id)
        return {"pushed_batches": len(batches), "line_id": line_id, "response": resp}
    except Exception as e:
        log.warning("Bootstrap batch push failed for %s: %s", line_id, e)
        return {"pushed_batches": 0, "line_id": line_id, "error": str(e)}


def bootstrap_all_lines(client: DataplatformClient) -> dict[str, Any]:
    master = load_master_data()
    batches = [
        b for b in master.get("product_batches", [])
        if b.get("status") == "in_progress"
    ]
    if not batches:
        return {"pushed_batches": 0, "lines": []}

    try:
        resp = client.push_batches(batches)
        line_ids = sorted({str(b.get("product_line_id")) for b in batches})
        log.info("Bootstrap pushed %s in-progress batch(es) for lines %s", len(batches), line_ids)
        return {"pushed_batches": len(batches), "lines": line_ids, "response": resp}
    except Exception as e:
        log.warning("Bootstrap all-lines batch push failed: %s", e)
        return {"pushed_batches": 0, "error": str(e)}
