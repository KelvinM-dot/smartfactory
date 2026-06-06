# FCW-LINE-07 设备级采集点位对照表

> **产线**：药芯焊丝 7 号线（`FCW-LINE-07`）  
> **定位**：POC 精细仿真样板线（`detail_level: full`），前端默认产线，全链路数据对齐基准  
> **品类**：药芯焊丝（flux_core_wire）  
> **关联文档**：[全品类设备采集点位对照表](./全品类设备采集点位对照表.md) · [原始数据源需求清单](./原始数据源需求清单.md)  
> **主数据来源**：`jqhc-factory-master-data.json` · `jqhc-manufacturing-config.json`

---

## 1. 产线概要

| 属性 | 值 |
|------|-----|
| 产线 ID | `FCW-LINE-07` |
| 名称 | 药芯焊丝 7 号线 |
| 车间 | `WS-WIRE-01` 焊丝智能制造车间 |
| 产品大类 | `flux_core_wire`（药芯焊丝） |
| 工艺模板 | `flux_core_wire`（9 工序） |
| 设计产能 | 5500 t/年 · 18 t/日 |
| 班次 | day + night |
| 3D 孪生 | `twin_3d_ready: true` |
| 设备数 | **9 台**（8 台工艺设备 + 1 台 AGV 接驳站） |
| 数据点数 | **27 个**（含通用 status/power_kw） |
| 边缘网关实例 | 建议 `gw-fcw-line-07` |
| 关联 AGV | `AGV-05` |
| 成品库位 | `WH-FG-A-03-12` |
| 标准批量 | 1200 kg/批 |

### 1.1 工序链

```
裁带 → 配粉搅拌 → 填充成型 → 粗拔 → 细拔 → 镀铜 → 层绕 → 包装 → 出入库
 cut    powder      filling    rough   fine    copper   wind   pack   stock
 strip  mixing      forming    draw    draw    plating
```

### 1.2 当前批次与订单（POC 种子）

| 批次 ID | 状态 | 牌号 | 配方 | 关联订单 |
|---------|------|------|------|----------|
| `FCW-20250605-B2` | **in_progress** | HY-830MPa | FCW-HY830-V3 | PO-20260606-FCW-EXPORT-HY830 |
| `FCW-20250606-B3` | released | HY-960MPa | FCW-HY960-V1 | PO-20260606-FCW-LNG-CUSTOM |
| `FCW-20250604-B2` | completed | HY-830MPa | FCW-HY830-V3 | PO-20260606-FCW-EXPORT-HY830 |
| `FCW-20250604-B1` | completed | HY-780MPa | FCW-HY780-V2 | PO-20260606-FCW-DOM-HY780 |

原料追溯批次：`STRIP-20250604-A1`（钢带）、`FLUX-20250605-A1`（药粉）

---

## 2. 设备总览

| 序号 | 设备 ID | 设备名称 | 类型 | 工序 | 协议 | 电表 ID | 孪生关键参数 | 3D 类型 |
|------|---------|----------|------|------|------|---------|--------------|---------|
| 1 | `STRIP-01` | 裁带机 #1 | strip_cutter | cut_strip | modbus_tcp | EM-FCW-01 | line_speed_m_per_min | cutter |
| 2 | `MIXER-01` | 配粉搅拌机 #1 | mixer | powder_mixing | modbus_tcp | EM-FCW-02 | mixing_rpm | mixer |
| 3 | `FILLER-01` | 填充成型机 #1 | filler | filling_forming | modbus_tcp | EM-FCW-03 | fill_ratio_pct | filler |
| 4 | `DRAW-ROUGH-01` | 粗拔机 #1 | wire_drawer | rough_drawing | modbus_tcp | EM-FCW-04 | tension_kN | drawer(rough) |
| 5 | `DRAW-FINE-01` | 细拔机 #1 | wire_drawer | fine_drawing | modbus_tcp | EM-FCW-05 | tension_kN | drawer(fine) |
| 6 | `PLATING-02` | 镀铜线 #2 | plater | copper_plating | modbus_tcp | EM-FCW-06 | coating_thickness_top_um | plater |
| 7 | `WIND-01` | 层绕机 #1 | winder | winding | modbus_tcp | EM-FCW-07 | motor_rpm | winder |
| 8 | `PACK-01` | 包装线 #1 | packer | packaging | none | EM-FCW-08 | spool_count | packer |
| 9 | `AGV-STATION-01` | AGV 接驳站 | agv_station | stock_in_out | mqtt | — | location | stock |

> **一致性要求**：上表 `equipment_id` 须与 PLC 点位表、MES 设备编码、`twin_layouts.FCW-LINE-07.steps.*.equipment_id`、遥测 `equipment_id` **完全一致**。

---

## 3. 逐设备采集点位详表

### 图例

| 标记 | 含义 |
|------|------|
| ★ | 前端 CRITICAL_FIELDS，趋势页必显 |
| ● | 域配置 required（mandatory） |
| P0 | 第一期必须接入 |
| P1 | 第二期增强 |

**PLC 寄存器地址列**：实施时由自动化工程师填写，POC 阶段留空待填。

---

### 3.1 STRIP-01 · 裁带机 #1

| 工序 | cut_strip（裁带） |
|------|-------------------|
| 协议 | Modbus TCP |
| 电表 | EM-FCW-01 |
| 额定产能 | 1250 kg/h |
| 质检点 | 钢带来料检验（strip_qc） |

| data_point_id | field_id | 显示名 | 单位 | 规格限 (LSL/USL/Target) | 优先级 | 采集 | 来源 | 频率 | PLC 地址（待填） |
|---------------|----------|--------|------|-------------------------|--------|------|------|------|------------------|
| — | `status` | 设备状态 | — | RUNNING/STOPPED/ALARM… | ● P0 | 必采 | PLC | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | 电表 EM-FCW-01 | 0.2Hz | |
| — | `alarm_code` | 报警码 | — | E101 等 | ● P0 | 事件触发 | PLC | 实时 | |
| `line_speed_m_per_min` | `line_speed_m_per_min` | 线速度 | m/min | 40 / 60 / **50** | ★● P0 | 必采 | PLC | 0.2Hz | |
| `strip_thickness_mm` | `strip_thickness_mm` | 钢带厚度 | mm | 1.8 / 2.2 / **2.0** | P0 | 必采 | 激光测厚仪 | 0.1Hz | |

**POC 仿真范围**：线速度 45–55 m/min，厚度 1.95–2.05 mm

---

### 3.2 MIXER-01 · 配粉搅拌机 #1

| 工序 | powder_mixing（配粉搅拌） |
|------|---------------------------|
| 协议 | Modbus TCP |
| 电表 | EM-FCW-02 |
| 额定产能 | 1200 kg/h |
| 质检点 | 配粉检验（flux_qc） |

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 采集 | 来源 | 频率 | PLC 地址（待填） |
|---------------|----------|--------|------|--------|--------|------|------|------|------------------|
| — | `status` | 设备状态 | — | — | ● P0 | 必采 | PLC | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | EM-FCW-02 | 0.2Hz | |
| — | `alarm_code` | 报警码 | — | E401 搅拌不均匀 | ● P0 | 事件 | PLC | 实时 | |
| — | `batch_id` | 配料批次 | — | — | ● P1 | MES 下发 | MES | 批次切换 | |
| — | `recipe_id` | 配方 ID | — | FCW-HY830-V3 等 | ● P1 | MES 下发 | MES | 批次切换 | |
| `mixing_rpm` | `mixing_rpm` | 搅拌转速 | rpm | 25 / 35 / **30** | ★ P0 | 必采 | PLC | 0.2Hz | |
| `mixing_uniformity` | `mixing_uniformity` | 混合均匀度 | — | 0.7 / 1.0 / **0.85** | P1 | 在线检测 | 分析仪 | 0.05Hz | |

**POC 仿真范围**：转速 28–32 rpm，均匀度 0.78–0.92

---

### 3.3 FILLER-01 · 填充成型机 #1

| 工序 | filling_forming（填充成型） |
|------|-------------------------------|
| 协议 | Modbus TCP |
| 电表 | EM-FCW-03 |
| 额定产能 | 1100 kg/h |
| 质检点 | 填充率 SPC（fill_ratio_qc） |

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 采集 | 来源 | 频率 | PLC 地址（待填） |
|---------------|----------|--------|------|--------|--------|------|------|------|------------------|
| — | `status` | 设备状态 | — | — | ● P0 | 必采 | PLC | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | EM-FCW-03 | 0.2Hz | |
| — | `alarm_code` | 报警码 | — | **E205 填充率偏离** | ● P0 | 事件 | PLC | 实时 | |
| `fill_ratio_pct` | `fill_ratio_pct` | 填充率 | % | 17 / 20 / **18.5** | ★● P0 | **最关键** | XRF/称重 | 0.2Hz | |
| `forming_pressure_MPa` | `forming_pressure_MPa` | 合缝压力 | MPa | 10 / 15 / **12** | ★ P0 | 必采 | PLC | 0.2Hz | |
| `fill_rate_kg_h` | `fill_rate_kg_h` | 填充速率 | kg/h | 70 / 95 / **85** | P1 | 必采 | PLC/称重 | 0.1Hz | |

**POC 仿真范围**：填充率 17.5–19.5%（可 drift），压力 11–13 MPa

---

### 3.4 DRAW-ROUGH-01 · 粗拔机 #1

| 工序 | rough_drawing（粗拔） |
|------|------------------------|
| 协议 | Modbus TCP |
| 电表 | EM-FCW-04 |
| 额定产能 | 1000 kg/h |

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 采集 | 来源 | 频率 | PLC 地址（待填） |
|---------------|----------|--------|------|--------|--------|------|------|------|------------------|
| — | `status` | 设备状态 | — | — | ● P0 | 必采 | PLC | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | EM-FCW-04 | 0.2Hz | |
| — | `alarm_code` | 报警码 | — | **E101 断丝** / E501 张力超限 | ● P0 | 事件 | PLC | 实时 | |
| `rough_tension_kN` | `tension_kN` | 拉拔力(粗) | kN | 4 / 8 / **6** | ★ P0 | 必采 | 张力传感器 | 0.2Hz | |
| `DRAW-ROUGH-01_line_speed_m_per_min` | `line_speed_m_per_min` | 线速度 | m/min | 60–75 / **68** | ★ P0 | 必采 | PLC | 0.2Hz | |

**POC 仿真范围**：张力 5–7 kN，线速度 60–75 m/min

---

### 3.5 DRAW-FINE-01 · 细拔机 #1

| 工序 | fine_drawing（细拔） |
|------|------------------------|
| 协议 | Modbus TCP |
| 电表 | EM-FCW-05 |
| 额定产能 | 950 kg/h |

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 采集 | 来源 | 频率 | PLC 地址（待填） |
|---------------|----------|--------|------|--------|--------|------|------|------|------------------|
| — | `status` | 设备状态 | — | — | ● P0 | 必采 | PLC | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | EM-FCW-05 | 0.2Hz | |
| — | `alarm_code` | 报警码 | — | E101 断丝 | ● P0 | 事件 | PLC | 实时 | |
| `fine_tension_kN` | `tension_kN` | 拉拔力(细) | kN | 2 / 8 / **4.5** | ★ P0 | 必采 | 张力传感器 | 0.2Hz | |
| `DRAW-FINE-01_outlet_diameter_um` | `outlet_diameter_um` | 出口线径 | μm | — / **1600** | ● P1 | 必采 | 激光测径 | 0.1Hz | |

**POC 仿真范围**：张力 4–5.5 kN，线径 1580–1620 μm

---

### 3.6 PLATING-02 · 镀铜线 #2

| 工序 | copper_plating（镀铜） |
|------|-------------------------|
| 协议 | Modbus TCP |
| 电表 | EM-FCW-06 |
| 额定产能 | 920 kg/h |
| 质检点 | 镀铜工艺（plating_process） |

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 采集 | 来源 | 频率 | PLC 地址（待填） |
|---------------|----------|--------|------|--------|--------|------|------|------|------------------|
| — | `status` | 设备状态 | — | — | ● P0 | 必采 | PLC | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | EM-FCW-06 | 0.2Hz | |
| — | `alarm_code` | 报警码 | — | **E301 镀层厚度异常** | ● P0 | 事件 | PLC | 实时 | |
| `coating_thickness_top_um` | `coating_thickness_top_um` | 镀层厚度(上) | μm | 6 / 12 / **8** | ★● P0 | **最关键** | XRF | 0.2Hz | |
| `coating_thickness_bottom_um` | `coating_thickness_bottom_um` | 镀层厚度(下) | μm | 6 / 12 / **8** | ★ P0 | 必采 | XRF | 0.2Hz | |
| `bath_temp_C` | `bath_temp_C` | 镀液温度 | °C | 25 / 32 / **28** | P0 | 必采 | 温度传感器 | 0.1Hz | |
| `PLATING-02_line_speed_m_per_min` | `line_speed_m_per_min` | 线速度 | m/min | 85–95 / **90** | P1 | 必采 | PLC | 0.2Hz | |
| `PLATING-02_current_density_A_per_dm2` | `current_density_A_per_dm2` | 电流密度 | A/dm² | 2.8–3.6 / **3.2** | P1 | 必采 | 整流器 | 0.1Hz | |
| `PLATING-02_bath_ph` | `bath_ph` | 镀液 pH | — | 8.2–8.8 / **8.5** | P1 | 必采 | pH 计 | 0.05Hz | |

**POC 仿真范围**：上镀层 7.5–8.8 μm，下镀层 7.3–8.6 μm，温度 27–29°C

---

### 3.7 WIND-01 · 层绕机 #1

| 工序 | winding（层绕） |
|------|-----------------|
| 协议 | Modbus TCP |
| 电表 | EM-FCW-07 |
| 额定产能 | 880 kg/h |

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 采集 | 来源 | 频率 | PLC 地址（待填） |
|---------------|----------|--------|------|--------|--------|------|------|------|------------------|
| — | `status` | 设备状态 | — | — | ● P0 | 必采 | PLC | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | EM-FCW-07 | 0.2Hz | |
| — | `alarm_code` | 报警码 | — | — | ● P0 | 事件 | PLC | 实时 | |
| `motor_rpm` | `motor_rpm` | 层绕转速 | rpm | 750 / 950 / **850** | ★ P0 | 必采 | PLC | 0.2Hz | |
| — | `tension_N` | 收线张力 | N | — | P1 | 选采 | 传感器 | 0.1Hz | |
| — | `spool_id` | 线盘 ID | — | — | P1 | MES | MES | 换盘时 | |

**POC 仿真范围**：转速 820–880 rpm

---

### 3.8 PACK-01 · 包装线 #1

| 工序 | packaging（包装） |
|------|-------------------|
| 协议 | none（无 PLC，人工/独立控制系统） |
| 电表 | EM-FCW-08 |
| 额定产能 | 45 spool/h |

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 采集 | 来源 | 频率 | 备注 |
|---------------|----------|--------|------|--------|--------|------|------|------|------|
| — | `status` | 设备状态 | — | — | ● P0 | 必采 | 包装机 PLC/人工 | 0.2Hz | |
| — | `power_kw` | 实时功率 | kW | — | P0 | 必采 | EM-FCW-08 | 0.2Hz | |
| `spool_count` | `spool_count` | 包装盘数 | piece | — | P0 | 必采 | 计数器/MES | 0.1Hz | 孪生 key_field |
| — | `finished_batch_id` | 成品批次号 | — | — | P1 | MES 绑定 | MES | 批次切换 | |
| — | `label_content` | 标签内容 | — | — | P2 | 选采 | 贴标机 | 换批时 | |

**POC 仿真范围**：盘数 40–48

---

### 3.9 AGV-STATION-01 · AGV 接驳站

| 工序 | stock_in_out（出入库） |
|------|------------------------|
| 协议 | MQTT |
| 关联 AGV | AGV-05 |
| 成品库位 | WH-FG-A-03-12 |

| data_point_id | field_id | 显示名 | 单位 | 优先级 | 采集 | 来源 | 频率 |
|---------------|----------|--------|------|--------|------|------|------|
| `AGV-STATION-01_status` | `status` | 设备状态 | — | P0 | 必采 | AGV 调度/WMS | 0.2Hz |
| `AGV-STATION-01_location` | `location` | 库位 | — | P0 | 必采 | WMS | 事件 |
| `AGV-STATION-01_quantity_kg` | `quantity_kg` | 出入库重量 | kg | P1 | 必采 | 地磅/WMS | 事件 |

**POC 仿真范围**：库位 WH-FG-A-03-12，重量 1000–1300 kg

---

## 4. 采集点位汇总统计

| 设备 | 过程参数 | 通用字段 | 合计 | P0 点位 |
|------|----------|----------|------|---------|
| STRIP-01 | 2 | 3 | 5 | 5 |
| MIXER-01 | 2 (+2 MES) | 3 | 7 | 5 |
| FILLER-01 | 3 | 3 | 6 | 6 |
| DRAW-ROUGH-01 | 2 | 3 | 5 | 5 |
| DRAW-FINE-01 | 2 | 3 | 5 | 4 |
| PLATING-02 | 6 | 3 | 9 | 6 |
| WIND-01 | 1 | 3 | 4 | 4 |
| PACK-01 | 1 | 3 | 4 | 4 |
| AGV-STATION-01 | 3 | 0 | 3 | 2 |
| **合计** | **22** | **24** | **48** | **41** |

> 通用字段 `status` / `power_kw` / `alarm_code` 每台工艺设备各 3 个；AGV 站无电表。

---

## 5. 业务事件采集（MES / WMS / QMS）

除遥测外，以下事件须按工序推进实时推送至 `POST /v1/ingest/events`：

### 5.1 物料与批次事件

| 触发时机 | event_type | 关键字段 | 来源 |
|----------|------------|----------|------|
| 新批次开工 | `BATCH_CREATE` | `material_batch`, `product_line_id`, `production_order_id`, `quantity_kg` | MES |
| 批次完工 | `BATCH_CLOSE` | 同上 + `quantity_kg` 实际产量 | MES |
| 工序开始 | `LINE_ON` | `process_step_id`, `product_line_id`, `material_batch` | MES |
| 工序结束 | `LINE_OFF` | 同上 | MES |
| WIP 转移 | `TRANSFER` | `from_location`, `to_location`, `process_step_id` | MES |
| 原料领用 | `STOCK_OUT` | `material_batch`, `location=WH-RAW`, `quantity_kg` | WMS |
| 成品入库 | `STOCK_IN` | `location=WH-FG-A-03-12`, `finished_batch_id` | WMS |
| 订单下达 | `ORDER_RELEASE` | `production_order_id`, `assigned_line_ids` | APS |
| 原料不足 | `MATERIAL_SHORTAGE` | `remark`, 触发 E701 | WMS |

**WIP 位置编码规范**（POC 使用）：

```
FCW-LINE-07/cut_strip
FCW-LINE-07/powder_mixing
FCW-LINE-07/filling_forming
FCW-LINE-07/rough_drawing
FCW-LINE-07/fine_drawing
FCW-LINE-07/copper_plating
FCW-LINE-07/winding
FCW-LINE-07/packaging
FCW-LINE-07/stock_in_out
```

### 5.2 物流事件

| 触发时机 | event_type / 对象 | 关键字段 | 来源 |
|----------|-------------------|----------|------|
| AGV 派车 | `AGV_DISPATCH` | `agv_id=AGV-05`, `from_location`, `to_location` | AGV 调度 |
| AGV 到达 | `AGV_ARRIVE` | `agv_id`, `to_location` | AGV 调度 |
| 物流任务 | `LOGISTICS_TASK` | `task_id`, `task_type`, `status`, `source/target_location_id` | WMS |

### 5.3 质量事件

| 触发时机 | 类型 | 关键字段 | 来源 | 对应质检点 |
|----------|------|----------|------|------------|
| 来料检验 | `QUALITY_GATE` | `gate_type=incoming`, `decision` | QMS | strip_qc |
| 配粉检验 | `QUALITY_GATE` | `process_step_id=powder_mixing` | QMS | flux_qc |
| 填充率 SPC | `QUALITY_GATE` | `process_step_id=filling_forming` | QMS/SPC | fill_ratio_qc |
| 镀铜过程 | `QUALITY_GATE` | `process_step_id=copper_plating` | QMS | plating_process |
| 力学终检 | `QUALITY_LAB` | `rm_mpa`, `impact_kv2_j`, `elongation_pct`, `corrosion_i_value`, `mechanical_pass` | LIMS | 成品放行 |

**HY-830 力学标准**（`FCW-HY830-V3`）：Rm ≥ 830 MPa · KV₂ ≥ 50 J · A ≥ 16% · I ≥ 6.2

---

## 6. 边缘网关配置模板

```yaml
# gw-fcw-line-07 建议配置
source: edge-gateway
source_instance: gw-fcw-line-07
product_line_id: FCW-LINE-07
template_id: flux_core_wire
config_id: jqhc-manufacturing

push:
  telemetry_interval_sec: 5      # 关键参数
  heartbeat_interval_sec: 15
  sparse_mode: true              # 非活跃工序仅推 status + power_kw

devices:
  - equipment_id: STRIP-01
    protocol: modbus_tcp
    host: <待填>
    port: 502
    energy_meter_id: EM-FCW-01
    tags: [line_speed_m_per_min, strip_thickness_mm, status, alarm_code, power_kw]

  - equipment_id: MIXER-01
    protocol: modbus_tcp
    energy_meter_id: EM-FCW-02
    tags: [mixing_rpm, mixing_uniformity, status, alarm_code, power_kw]

  - equipment_id: FILLER-01
    protocol: modbus_tcp
    energy_meter_id: EM-FCW-03
    tags: [fill_ratio_pct, forming_pressure_MPa, fill_rate_kg_h, status, alarm_code, power_kw]

  - equipment_id: DRAW-ROUGH-01
    protocol: modbus_tcp
    energy_meter_id: EM-FCW-04
    tags: [tension_kN, line_speed_m_per_min, status, alarm_code, power_kw]

  - equipment_id: DRAW-FINE-01
    protocol: modbus_tcp
    energy_meter_id: EM-FCW-05
    tags: [tension_kN, outlet_diameter_um, status, alarm_code, power_kw]

  - equipment_id: PLATING-02
    protocol: modbus_tcp
    energy_meter_id: EM-FCW-06
    tags: [coating_thickness_top_um, coating_thickness_bottom_um, bath_temp_C,
           line_speed_m_per_min, current_density_A_per_dm2, bath_ph,
           status, alarm_code, power_kw]

  - equipment_id: WIND-01
    protocol: modbus_tcp
    energy_meter_id: EM-FCW-07
    tags: [motor_rpm, status, alarm_code, power_kw]

  - equipment_id: PACK-01
    protocol: http            # 或独立采集模块
    energy_meter_id: EM-FCW-08
    tags: [spool_count, status, power_kw]

  - equipment_id: AGV-STATION-01
    protocol: mqtt
    broker: <待填>
    topic: agv/fcw07/#
    tags: [status, location, quantity_kg]

context_from_mes:              # 每条遥测 record 须携带
  - product_batch               # 当前批次，如 FCW-20250605-B2
  - recipe_id                   # 当前配方，如 FCW-HY830-V3
  - shift                       # day / night
```

---

## 7. 数字孪生对齐检查表

| 工序 | twin equipment_id | 实际 equipment_id | key_field_id | 3D type | 状态 |
|------|-------------------|-------------------|--------------|---------|------|
| cut_strip | STRIP-01 | STRIP-01 | line_speed_m_per_min | cutter | ✓ 一致 |
| powder_mixing | MIXER-01 | MIXER-01 | mixing_rpm | mixer | ✓ 一致 |
| filling_forming | FILLER-01 | FILLER-01 | fill_ratio_pct | filler | ✓ 一致 |
| rough_drawing | DRAW-ROUGH-01 | DRAW-ROUGH-01 | tension_kN | drawer(rough) | ✓ 一致 |
| fine_drawing | DRAW-FINE-01 | DRAW-FINE-01 | tension_kN | drawer(fine) | ✓ 一致 |
| copper_plating | PLATING-02 | PLATING-02 | coating_thickness_top_um | plater | ✓ 一致 |
| winding | WIND-01 | WIND-01 | motor_rpm | winder | ✓ 一致 |
| packaging | PACK-01 | PACK-01 | spool_count | packer | ✓ 一致 |
| stock_in_out | AGV-STATION-01 | AGV-STATION-01 | location | stock | ✓ 一致 |

---

## 8. 产线级验收清单

### 8.1 遥测验收

- [ ] 9 台设备 `equipment_id` 全部可采集 `status`
- [ ] 8 台工艺设备 `power_kw` 有实时值（EM-FCW-01 ~ EM-FCW-08）
- [ ] 8 个 CRITICAL_FIELDS 在活跃工序有 ≥ 0.2Hz 数据
- [ ] `fill_ratio_pct` 规格带 17–20% 合规判断正常
- [ ] `coating_thickness_top/bottom_um` 规格带 6–12 μm 合规判断正常
- [ ] 遥测 record 携带正确 `product_batch` + `recipe_id`
- [ ] 网关心跳 `gw-fcw-line-07` 周期 ≤ 30s

### 8.2 事件验收

- [ ] 批次 `FCW-20250605-B2` 可走完 9 工序 TRANSFER 链
- [ ] `parent_batches` 含 `STRIP-*` + `FLUX-*`，追溯无 gap
- [ ] AGV-05 的 DISPATCH/ARRIVE 事件可查询
- [ ] 质量门在 strip_qc / flux_qc / fill_ratio_qc 可触发
- [ ] 力学终检 QUALITY_LAB 含 Rm/KV₂/A/I 四项

### 8.3 功能验收

- [ ] 产线工作台 3 Tab（总览/孪生/参数表）数据完整
- [ ] 趋势页 8 条曲线有连续数据
- [ ] WebSocket 实时推送 overview 正常
- [ ] 报警 E205（填充率）可触发并走 acknowledge/resolve 流程
- [ ] 订单 PO-20260606-FCW-EXPORT-HY830 关联批次可追踪

---

## 9. 复制到其他产线的说明

FCW-LINE-07 为 **`detail_level: full`** 精细样板。其余 41 条产线（如 `FCW-LINE-01`）使用带产线前缀的设备 ID（如 `FCW01-STRIP`），结构相同但：

1. 将本文档中所有 `equipment_id` 替换为对应产线前缀版本
2. `twin_layouts.{lineId}` 中核对 equipment_id 映射
3. `energy_meter_id` 替换为 `EM-FCW01-*` 系列
4. 工序链、字段清单、事件类型**完全相同**（药芯焊丝模板）

可使用脚本 `line-simulator/tools/upgrade_line_simulation.py` 从 FCW-LINE-07 批量派生其他产线配置。

---

*本文档由 `jqhc-factory-master-data.json` 中 FCW-LINE-07 段自动梳理，PLC 地址列留空供现场实施填写。*
