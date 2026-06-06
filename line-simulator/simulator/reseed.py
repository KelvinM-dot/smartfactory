"""Trigger dataplatform master-data reseed via API."""

from __future__ import annotations

import logging
from typing import Any

from simulator.client import DataplatformClient, ReseedNotAvailableError
from simulator.config import RESET_DB_BEFORE_START

log = logging.getLogger(__name__)


def maybe_reseed_master_data(
    client: DataplatformClient,
    *,
    force: bool | None = None,
) -> dict[str, Any] | None:
    """
    若 force 或 config.RESET_DB_BEFORE_START 为 True，则调用 API 清库并重灌主数据。
    返回 reseed 响应；未执行时返回 None。
    """
    should_reset = RESET_DB_BEFORE_START if force is None else force
    if not should_reset:
        return None
    log.info("RESET_DB_BEFORE_START=true → wait API then POST /v1/admin/reseed")
    client.wait_until_ready()
    try:
        result = client.reseed_master_data()
    except ReseedNotAvailableError as exc:
        log.error("%s", exc)
        raise
    log.info("Master data reseeded: %s", result.get("summary"))
    return result


def reseed_master_data(client: DataplatformClient) -> dict[str, Any]:
    """无条件清库并重灌主数据."""
    return client.reseed_master_data()
