# 数据结构目录

智造数据台采用 **配置驱动**：工序与字段通过 `presets/*.json` 定义，运行时记录用通用结构 + `field_id`。

详细原则见 [数据定义说明.md](./数据定义说明.md)。

---

## 智造数据台（PC）

| 优先级 | 文件 | 说明 |
|--------|------|------|
| ★ | `presets/jqhc-manufacturing-config.json` | 完整默认配置（13 工序 / 61 字段 / 3 产品模板） |
| ★ | `presets/jqhc-factory-master-data.json` | POC 工厂主数据（产线 / 设备 / 配方 / 库存 / 仿真 profile） |
| ★ | `config-model.json` | 配置 schema（DomainConfig） |
| ★ | `telemetry-record.json` | 通用运行时遥测（`values` + `field_id`） |
| | `entities.json` | 产线、设备、数据点、批次、看板 |
| | `runtime-analytics.json` | 物料事件、报警、OEE、SPC、追溯 |
| | `ingest-api.json` | POC：模拟器/网关 ↔ 数据台 Ingest & Query 契约 |
| | `enums.json` | 枚举 |
| | `examples.json` | 配置驱动示例 |
| | `telemetry-payloads.json` | 遗留类型化参考（deprecated） |

---

## 使用顺序（开发/产品设计）

1. 阅读 `数据定义说明.md`
2. 加载 `presets/jqhc-manufacturing-config.json` 与 `presets/jqhc-factory-master-data.json` 作为领域基线
3. 按 `config-model.json` 扩展工序或字段
4. 运行时读写 `telemetry-record.json` 实例
5. 元数据实体（设备、批次、任务）引用 `config_id` + `template_id`

---

## 统计

| 工序 | 字段 | 绑定 | 模板 |
|------|------|------|------|
| 13 | 61 | 126 | 3 |
