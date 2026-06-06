"""Push telemetry/events to 智造数据台 Ingest API."""

from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Any

import httpx

log = logging.getLogger(__name__)


class DataplatformClient:
    def __init__(self, base_url: str, source_instance: str = "sim-fcw-07-001"):
        self.base_url = base_url.rstrip("/")
        self.source = "line-simulator"
        self.source_instance = source_instance
        # trust_env=False: bypass macOS/system HTTP proxy for localhost ingest
        self._client = httpx.Client(timeout=10.0, trust_env=False)

    def close(self) -> None:
        self._client.close()

    def _envelope(self, records: list[dict[str, Any]] | None = None, events: list[dict[str, Any]] | None = None,
                  batches: list[dict[str, Any]] | None = None) -> dict[str, Any]:
        body: dict[str, Any] = {
            "source": self.source,
            "source_instance": self.source_instance,
            "sent_at": _iso_now(),
        }
        if records is not None:
            body["records"] = records
        if events is not None:
            body["events"] = events
        if batches is not None:
            body["batches"] = batches
        return body

    def push_telemetry(self, records: list[dict[str, Any]]) -> dict[str, Any]:
        resp = self._client.post(
            f"{self.base_url}/v1/ingest/telemetry",
            json=self._envelope(records=records),
            headers={"X-Source": self.source, "X-Source-Instance": self.source_instance},
        )
        resp.raise_for_status()
        return resp.json()

    def push_events(self, events: list[dict[str, Any]]) -> dict[str, Any]:
        resp = self._client.post(
            f"{self.base_url}/v1/ingest/events",
            json=self._envelope(events=events),
        )
        resp.raise_for_status()
        return resp.json()

    def push_batches(self, batches: list[dict[str, Any]]) -> dict[str, Any]:
        resp = self._client.post(
            f"{self.base_url}/v1/ingest/batches",
            json=self._envelope(batches=batches),
        )
        resp.raise_for_status()
        return resp.json()

    def heartbeat(
        self,
        product_line_id: str,
        scenario_id: str,
        speed_multiplier: float,
        runtime: dict[str, Any] | None = None,
    ) -> None:
        body: dict[str, Any] = {
            "source": self.source,
            "source_instance": self.source_instance,
            "timestamp": _iso_now(),
            "product_line_id": product_line_id,
            "scenario_id": scenario_id,
            "speed_multiplier": speed_multiplier,
        }
        if runtime:
            body["runtime"] = runtime
        resp = self._client.post(
            f"{self.base_url}/v1/ingest/heartbeat",
            json=body,
        )
        resp.raise_for_status()

    def create_order(self, payload: dict[str, Any]) -> dict[str, Any]:
        resp = self._client.post(
            f"{self.base_url}/v1/meta/orders",
            json=payload,
        )
        resp.raise_for_status()
        return resp.json()

    def patch_order_status(
        self,
        production_order_id: str,
        status: str,
        remark: str | None = None,
    ) -> dict[str, Any]:
        body: dict[str, Any] = {"status": status}
        if remark:
            body["remark"] = remark
        resp = self._client.patch(
            f"{self.base_url}/v1/meta/orders/{production_order_id}",
            json=body,
        )
        resp.raise_for_status()
        return resp.json()

    def fetch_orders(self, factory_id: str | None = None) -> list[dict[str, Any]]:
        params = {"factory_id": factory_id} if factory_id else None
        resp = self._client.get(f"{self.base_url}/v1/orders", params=params)
        resp.raise_for_status()
        data = resp.json()
        return data if isinstance(data, list) else []

    def wait_until_ready(
        self,
        *,
        timeout_sec: float = 60.0,
        initial_delay_sec: float = 0.5,
        max_delay_sec: float = 5.0,
    ) -> None:
        """等待 dataplatform-api 可访问（指数退避）."""
        import time

        deadline = time.monotonic() + timeout_sec
        delay = initial_delay_sec
        attempt = 0
        last_err: Exception | None = None
        while time.monotonic() < deadline:
            attempt += 1
            try:
                resp = self._client.get(f"{self.base_url}/v1/meta/factory", timeout=3.0)
                if resp.status_code == 200:
                    log.info("dataplatform-api ready after %s attempt(s)", attempt)
                    return
                last_err = RuntimeError(f"HTTP {resp.status_code}")
            except Exception as exc:
                last_err = exc
            time.sleep(delay)
            delay = min(delay * 1.6, max_delay_sec)
        raise ConnectionError(
            f"dataplatform-api not ready at {self.base_url} within {timeout_sec}s: {last_err}"
        )

    def reseed_master_data(self) -> dict[str, Any]:
        """清空 SQLite 并从 jqhc-factory-master-data.json 全量重灌（API SeedService）."""
        url = f"{self.base_url}/v1/admin/reseed"
        resp = self._client.post(url)
        if resp.status_code == 404:
            raise ReseedNotAvailableError(
                f"POST {url} 返回 404：当前 dataplatform-api 未包含 AdminController。"
                "请在 dataplatform-api 目录执行 mvn clean package -DskipTests，"
                "并用新 jar 重启 API（端口 3001）后再启动模拟器。"
            )
        resp.raise_for_status()
        return resp.json()


class ReseedNotAvailableError(RuntimeError):
    """dataplatform-api 未提供 /v1/admin/reseed（通常为 API 未重新编译/重启）."""


def _iso_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
