# SW-LINE-02 设备级采集点位对照表

> **品类**：实心焊丝（solid_wire）  
> **关联**：[全品类设备采集点位对照表](./全品类设备采集点位对照表.md) · [原始数据源需求清单](./原始数据源需求清单.md)

---

## 1. 产线概要

| 属性 | 值 |
|------|-----|
| 产线 ID | `SW-LINE-02` |
| 名称 | 实心焊丝 2 号线 |
| 车间 | WS-WIRE-01 |
| 模板 | solid_wire（6 工序） |
| 产能 | 4800 t/年 · 15 t/日 |
| 网关 | `gw-sw-line-02` |
| AGV | AGV-03 |
| 成品库位 | WH-FG-B-01-08 |
| 标准批量 | 1000 kg |

### 工序链

```
粗拔 → 细拔 → 镀铜 → 层绕 → 包装 → 出入库
```

### 当前批次

| 批次 ID | 状态 | 牌号 | 配方 | 订单 |
|---------|------|------|------|------|
| SW-20250605-B1 | in_progress | ER50-6 | SW-ER50-V2 | PO-20260606-SW-DOM-ER50 |
| SW-20250606-B2 | released | ER70S-6 | SW-ER70-V1 | PO-20260606-SW-EXPORT-ER70 |

原料追溯：`ROD-20260520-C1`（盘条）

---

## 2. 设备总览

| 序号 | 设备 ID | 名称 | 工序 | 协议 | 电表 | 孪生 key_field | 3D type |
|------|---------|------|------|------|------|----------------|---------|
| 1 | DRAW-SW-01 | 实心粗拔 #1 | rough_drawing | modbus_tcp | EM-SW-01 | tension_kN | drawer(rough) |
| 2 | DRAW-SW-02 | 实心细拔 #1 | fine_drawing | modbus_tcp | EM-SW-02 | tension_kN | drawer(fine) |
| 3 | PLATING-SW-01 | 实心镀铜 #1 | copper_plating | modbus_tcp | EM-SW-03 | coating_thickness_top_um | plater |
| 4 | WIND-SW-01 | 实心层绕 #1 | winding | modbus_tcp | EM-SW-04 | motor_rpm | winder |
| 5 | PACK-SW-01 | 实心包装 #1 | packaging | none | EM-SW-05 | package_count | packer |
| 6 | AGV-SW-01 | 实心 AGV 接驳 | stock_in_out | mqtt | — | location | stock |

---

## 3. 逐设备采集点位

### 3.1 DRAW-SW-01 · 粗拔

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 | PLC地址 |
|---------------|----------|--------|------|--------|--------|------|---------|
| — | status | 设备状态 | — | — | ● P0 | PLC | |
| — | power_kw | 实时功率 | kW | — | P0 | EM-SW-01 | |
| — | alarm_code | 报警码 | — | E101 | ● P0 | PLC | |
| sw_rough_tension_kN | tension_kN | 拉拔力(粗) | kN | 4.5/6.5/**5.5** | ★ P0 | 张力传感器 | |
| DRAW-SW-01_line_speed_m_per_min | line_speed_m_per_min | 线速 | m/min | 55–65/**60** | ★ P0 | PLC | |

### 3.2 DRAW-SW-02 · 细拔

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 |
|---------------|----------|--------|------|--------|--------|------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 | PLC |
| sw_fine_tension_kN | tension_kN | 拉拔力(细) | kN | 4/6/**5.0** | ★ P0 | 张力传感器 |
| DRAW-SW-02_line_speed_m_per_min | line_speed_m_per_min | 线速 | m/min | 50–60/**55** | P1 | PLC |
| DRAW-SW-02_outlet_diameter_um | outlet_diameter_um | 出口线径 | μm | 1180–1220/**1200** | P0 | 激光测径 |

### 3.3 PLATING-SW-01 · 镀铜

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 |
|---------------|----------|--------|------|--------|--------|------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 | PLC |
| sw_coating_top_um | coating_thickness_top_um | 镀层厚度(上) | μm | 6/10/**7.5** | ★ P0 | XRF |
| PLATING-SW-01_bath_temp_C | bath_temp_C | 镀液温度 | °C | 26–30/**28** | P0 | 温度传感器 |

### 3.4 WIND-SW-01 · 层绕

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 |
|---------------|----------|--------|------|--------|--------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 |
| sw_motor_rpm | motor_rpm | 层绕转速 | rpm | 700/900/**800** | ★ P0 |

### 3.5 PACK-SW-01 · 包装

| field_id | 显示名 | 优先级 | 来源 |
|----------|--------|--------|------|
| status / power_kw | 通用 | P0 | 包装机 |
| package_count | 包装盘数 | P0 | 计数器 |
| finished_batch_id | 成品批次 | P1 | MES |

### 3.6 AGV-SW-01 · 出入库

| data_point_id | field_id | 优先级 | 来源 |
|---------------|----------|--------|------|
| AGV-SW-01_status | status | P0 | AGV |
| AGV-SW-01_location | location | P0 | WMS |
| AGV-SW-01_quantity_kg | quantity_kg | P1 | 地磅 |

---

## 4. 质量门

| 工序 | qc_id | 显示名 |
|------|-------|--------|
| rough_drawing | raw_inspection | 检验工艺 |
| rough_drawing | rough_wire_process | 粗拔丝工艺 |
| fine_drawing | fine_wire_process | 细拔丝工艺 |
| copper_plating | degrease_process | 脱脂工艺 |
| copper_plating | plating_process | 镀铜工艺 |
| winding | winding_process | 层绕工艺 |

**ER50-6 力学标准**：Rm ≥ 610 · KV₂ ≥ 69 J · A ≥ 22% · I ≥ 6.0

---

## 5. WIP 位置编码

```
SW-LINE-02/rough_drawing
SW-LINE-02/fine_drawing
SW-LINE-02/copper_plating
SW-LINE-02/winding
SW-LINE-02/packaging
SW-LINE-02/stock_in_out
```

---

## 6. 网关配置模板

```yaml
source_instance: gw-sw-line-02
product_line_id: SW-LINE-02
template_id: solid_wire
devices:
  - equipment_id: DRAW-SW-01
    energy_meter_id: EM-SW-01
    tags: [tension_kN, line_speed_m_per_min, status, alarm_code, power_kw]
  - equipment_id: DRAW-SW-02
    energy_meter_id: EM-SW-02
    tags: [tension_kN, outlet_diameter_um, line_speed_m_per_min, status, alarm_code, power_kw]
  - equipment_id: PLATING-SW-01
    energy_meter_id: EM-SW-03
    tags: [coating_thickness_top_um, bath_temp_C, status, alarm_code, power_kw]
  - equipment_id: WIND-SW-01
    energy_meter_id: EM-SW-04
    tags: [motor_rpm, status, alarm_code, power_kw]
  - equipment_id: PACK-SW-01
    energy_meter_id: EM-SW-05
    tags: [package_count, status, power_kw]
  - equipment_id: AGV-SW-01
    protocol: mqtt
    tags: [status, location, quantity_kg]
```

---

## 7. 复制到其他实心线

SW-LINE-01/03~08 设备前缀为 `SW01-`、`SW03-` 等，字段清单与本表相同。  
参考脚本：`line-simulator/tools/upgrade_line_simulation.py`
