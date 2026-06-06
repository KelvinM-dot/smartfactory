# SAW-LINE-01 设备级采集点位对照表

> **品类**：埋弧焊丝（submerged_arc_wire）  
> **关联**：[全品类设备采集点位对照表](./全品类设备采集点位对照表.md) · [原始数据源需求清单](./原始数据源需求清单.md)

---

## 1. 产线概要

| 属性 | 值 |
|------|-----|
| 产线 ID | `SAW-LINE-01` |
| 名称 | 埋弧焊丝 1 号线 |
| 车间 | WS-WIRE-01 |
| 模板 | submerged_arc_wire（6 工序） |
| 产能 | 4950 t/年 · 15.5 t/日 |
| 网关 | `gw-saw-line-01` |
| AGV | AGV-03 |
| 成品库位 | WH-FG-01-01 |
| 标准批量 | 1200 kg |

> 工艺链路与实心焊丝相同，差异在产品应用（埋弧焊）和设备命名前缀。

### 工序链

```
粗拔 → 细拔 → 镀铜 → 层绕 → 包装 → 出入库
```

### 当前批次

| 批次 ID | 状态 | 牌号 | 配方 |
|---------|------|------|------|
| SAW01-20260606-B1 | in_progress | ER50-6 | SAW01-ER50-V2 |

原料追溯：`ROD-20260520-C1`（盘条）

---

## 2. 设备总览

| 序号 | 设备 ID | 名称 | 工序 | 协议 | 电表 | 孪生 key_field |
|------|---------|------|------|------|------|----------------|
| 1 | SAW01-DRAW-R | 粗拔 #SAW01 | rough_drawing | modbus_tcp | EM-SAW01-DRAW-R | tension_kN |
| 2 | SAW01-DRAW-F | 细拔 #SAW01 | fine_drawing | modbus_tcp | EM-SAW01-DRAW-F | tension_kN |
| 3 | SAW01-PLATING | 镀铜 #SAW01 | copper_plating | modbus_tcp | EM-SAW01-PLATING | coating_thickness_top_um |
| 4 | SAW01-WIND | 层绕 #SAW01 | winding | modbus_tcp | EM-SAW01-WIND | motor_rpm |
| 5 | SAW01-PACK | 包装 #SAW01 | packaging | none | EM-SAW01-PACK | package_count |
| 6 | SAW01-AGV | AGV 接驳 | stock_in_out | mqtt | — | location |

---

## 3. 逐设备采集点位

### 3.1 SAW01-DRAW-R · 粗拔

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 |
|---------------|----------|--------|------|--------|--------|------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 | PLC |
| SAW01-DRAW-R_tension_kN | tension_kN | 拉拔力(粗) | kN | 4.5/6.5/**5.5** | ★ P0 | 张力传感器 |
| SAW01-DRAW-R_line_speed_m_per_min | line_speed_m_per_min | 线速 | m/min | 55–65/**60** | ★ P0 | PLC |

### 3.2 SAW01-DRAW-F · 细拔

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 |
|---------------|----------|--------|------|--------|--------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 |
| SAW01-DRAW-F_tension_kN | tension_kN | 拉拔力(细) | kN | 4/6/**5.0** | ★ P0 |
| SAW01-DRAW-F_line_speed_m_per_min | line_speed_m_per_min | 线速 | m/min | 50–60/**55** | P1 |
| SAW01-DRAW-F_outlet_diameter_um | outlet_diameter_um | 出口线径 | μm | 1180–1220/**1200** | P0 |

### 3.3 SAW01-PLATING · 镀铜

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 |
|---------------|----------|--------|------|--------|--------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 |
| SAW01-PLATING_coating_thickness_top_um | coating_thickness_top_um | 镀层厚度(上) | μm | 6/10/**7.5** | ★ P0 |
| SAW01-PLATING_bath_temp_C | bath_temp_C | 镀液温度 | °C | 26–30/**28** | P0 |

### 3.4 SAW01-WIND · 层绕

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 |
|---------------|----------|--------|------|--------|--------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 |
| SAW01-WIND_motor_rpm | motor_rpm | 层绕转速 | rpm | 700/900/**800** | ★ P0 |

### 3.5 SAW01-PACK · 包装

| field_id | 显示名 | 优先级 |
|----------|--------|--------|
| status / power_kw | 通用 | P0 |
| package_count | 包装盘数 | P0 |

### 3.6 SAW01-AGV · 出入库

| data_point_id | field_id | 优先级 | 来源 |
|---------------|----------|--------|------|
| SAW01-AGV_status | status | P0 | AGV |
| SAW01-AGV_location | location | P0 | WMS |
| SAW01-AGV_quantity_kg | quantity_kg | P1 | 地磅 |

---

## 4. 与实心焊丝 (SW-LINE-02) 的对照

| 维度 | SAW-LINE-01 | SW-LINE-02 |
|------|-------------|------------|
| 工序链 | 6 步（相同） | 6 步 |
| 采集字段 | 完全相同 | 完全相同 |
| 设备命名 | `SAW01-*` 前缀 | `DRAW-SW-*` 短 ID |
| 标准批量 | 1200 kg | 1000 kg |
| 产线数 | 2 条 | 8 条 |
| 质量门 | 相同集合 | 相同集合 |

---

## 5. 质量门

| 工序 | qc_id | 显示名 |
|------|-------|--------|
| rough_drawing | raw_inspection | 检验工艺 |
| rough_drawing | rough_wire_process | 粗拔丝工艺 |
| fine_drawing | fine_wire_process | 细拔丝工艺 |
| copper_plating | degrease_process | 脱脂工艺 |
| copper_plating | plating_process | 镀铜工艺 |
| winding | winding_process | 层绕工艺 |

---

## 6. 网关配置模板

```yaml
source_instance: gw-saw-line-01
product_line_id: SAW-LINE-01
template_id: submerged_arc_wire
devices:
  - equipment_id: SAW01-DRAW-R
    energy_meter_id: EM-SAW01-DRAW-R
    tags: [tension_kN, line_speed_m_per_min, status, alarm_code, power_kw]
  - equipment_id: SAW01-DRAW-F
    energy_meter_id: EM-SAW01-DRAW-F
    tags: [tension_kN, outlet_diameter_um, line_speed_m_per_min, status, alarm_code, power_kw]
  - equipment_id: SAW01-PLATING
    energy_meter_id: EM-SAW01-PLATING
    tags: [coating_thickness_top_um, bath_temp_C, status, alarm_code, power_kw]
  - equipment_id: SAW01-WIND
    energy_meter_id: EM-SAW01-WIND
    tags: [motor_rpm, status, alarm_code, power_kw]
  - equipment_id: SAW01-PACK
    energy_meter_id: EM-SAW01-PACK
    tags: [package_count, status, power_kw]
  - equipment_id: SAW01-AGV
    protocol: mqtt
    tags: [status, location, quantity_kg]
```

---

## 7. 复制到 SAW-LINE-02

设备前缀替换为 `SAW02-*`，字段清单不变。
