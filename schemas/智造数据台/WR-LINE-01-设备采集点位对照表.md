# WR-LINE-01 设备级采集点位对照表

> **品类**：焊条（welding_rod）  
> **关联**：[全品类设备采集点位对照表](./全品类设备采集点位对照表.md) · [原始数据源需求清单](./原始数据源需求清单.md)

---

## 1. 产线概要

| 属性 | 值 |
|------|-----|
| 产线 ID | `WR-LINE-01` |
| 名称 | 焊条 1 号线 |
| 车间 | WS-ROD-01 |
| 模板 | welding_rod（7 工序） |
| 产能 | 15000 t/年 · 50 t/日 |
| 班次 | day（单班） |
| 网关 | `gw-wr-line-01` |
| AGV | AGV-01 |
| 成品库位 | WH-FG-C-02-05 |
| 标准批量 | 800 kg |

### 工序链

```
拔丝 → 切丝 → 配粉搅拌 → 压涂 → 烘干 → 包装 → 出入库
```

### 当前批次

| 批次 ID | 状态 | 牌号 | 配方 | 订单 |
|---------|------|------|------|------|
| WR01-20250606-B1 | in_progress | E7014 | WR-E7014-V1 | PO-20260606-WR-HYDRO-E7014 |
| WR-20260528-B2 | completed | E7014 | WR-E7014-V1 | PO-20260606-WR-HYDRO-E7014 |

原料追溯：`ROD-20260520-C1`（盘条）+ `FLUX-WR-20260501-A1`（药粉）

---

## 2. 设备总览

| 序号 | 设备 ID | 名称 | 工序 | 协议 | 电表 | 孪生 key_field | 3D type |
|------|---------|------|------|------|------|----------------|---------|
| 1 | DRAW-WR-01 | 焊条拔丝 #1 | wire_drawing | modbus_tcp | EM-WR-01 | line_speed_m_per_min | drawer |
| 2 | CUT-WR-01 | 焊条切断 #1 | cutting | modbus_tcp | EM-WR-02 | length_mm | cutter |
| 3 | MIXER-WR-01 | 焊条配粉 #1 | powder_mixing | modbus_tcp | EM-WR-03 | mixing_rpm | mixer |
| 4 | COAT-WR-01 | 焊条涂药 #1 | coating | modbus_tcp | EM-WR-04 | coating_thickness_mm | coater |
| 5 | DRY-WR-01 | 焊条烘干 #1 | drying | modbus_tcp | EM-WR-05 | actual_temp_C | dryer |
| 6 | PACK-WR-01 | 焊条包装 #1 | packaging | none | EM-WR-06 | package_count | packer |
| 7 | AGV-WR-01 | 焊条 AGV 接驳 | stock_in_out | mqtt | — | location | stock |

---

## 3. 逐设备采集点位

### 3.1 DRAW-WR-01 · 拔丝

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 | PLC地址 |
|---------------|----------|--------|------|--------|--------|------|---------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 | PLC | |
| wr_drawing_speed_m_min | line_speed_m_per_min | 拔丝线速度 | m/min | 30/50/**40** | ★ P0 | PLC | |
| — | outlet_diameter_um | 出口线径 | μm | — | P1 | 激光测径 | |
| — | tension_kN | 张力 | kN | — | P1 | 传感器 | |

质检点：rod_inspection（盘条检验）

### 3.2 CUT-WR-01 · 切丝

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 |
|---------------|----------|--------|------|--------|--------|------|
| — | status / alarm_code | 通用 | — | — | P0 | PLC |
| wr_cut_length_mm | length_mm | 切断长度 | mm | 350/370/**360** | ★ P0 | PLC |
| CUT-WR-01_cutting_speed_cuts_per_min | cutting_speed_cuts_per_min | 切丝速度 | 根/min | — | P1 | PLC |
| — | fault_code | 故障码 | — | — | ● P0 | PLC |

质检点：cut_length_qc（切丝长度 SPC）

### 3.3 MIXER-WR-01 · 配粉搅拌

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 |
|---------------|----------|--------|------|--------|--------|------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 | PLC |
| wr_mixing_rpm | mixing_rpm | 配粉转速 | rpm | 22/34/**28** | ★ P0 | PLC |
| — | batch_id / recipe_id | 批次/配方 | — | — | P1 | MES |

### 3.4 COAT-WR-01 · 压涂

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 |
|---------------|----------|--------|------|--------|--------|------|
| — | status / power_kw / alarm_code | 通用 | — | — | P0 | PLC |
| wr_coating_thickness_mm | coating_thickness_mm | 涂药厚度 | mm | 1.2/2.0/**1.6** | ★ P0 | 测厚仪 |
| COAT-WR-01_coating_pressure_MPa | coating_pressure_MPa | 压涂压力 | MPa | 10/14/**12** | P1 | PLC |
| — | core_speed_m_per_min | 钢芯速度 | m/min | — | P1 | PLC |

### 3.5 DRY-WR-01 · 烘干

| data_point_id | field_id | 显示名 | 单位 | 规格限 | 优先级 | 来源 |
|---------------|----------|--------|------|--------|--------|------|
| — | status / alarm_code | 通用 | — | — | P0 | PLC |
| wr_drying_temp_C | actual_temp_C | 烘干温度 | °C | 170/190/**180** | ★ P0 | 温控 |
| DRY-WR-01_moisture_pct | moisture_pct | 含水率 | % | —/**0.15** | ★ P0 | 含水率仪 |
| — | belt_speed_m_per_min | 带速 | m/min | — | P1 | PLC |
| — | humidity_pct | 环境湿度 | % | — | P2 | 传感器 |

质检点：moisture_qc（含水率终检）

### 3.6 PACK-WR-01 · 包装

| data_point_id | field_id | 显示名 | 优先级 |
|---------------|----------|--------|--------|
| — | status / power_kw | 通用 | P0 |
| wr_package_count | package_count | 包装箱数 | P0 |
| — | finished_batch_id | 成品批次 | P1 |

### 3.7 AGV-WR-01 · 出入库

| data_point_id | field_id | 优先级 | 来源 |
|---------------|----------|--------|------|
| AGV-WR-01_status | status | P0 | AGV |
| AGV-WR-01_location | location | P0 | WMS |
| AGV-WR-01_quantity_kg | quantity_kg | P1 | 地磅 |

---

## 4. 采集点位汇总

| 设备 | 过程参数 | 通用字段 | 合计 | P0 |
|------|----------|----------|------|-----|
| DRAW-WR-01 | 1 (+2选采) | 3 | 6 | 4 |
| CUT-WR-01 | 2 | 2 | 4 | 4 |
| MIXER-WR-01 | 1 | 3 | 4 | 4 |
| COAT-WR-01 | 2 | 3 | 5 | 4 |
| DRY-WR-01 | 2 | 2 | 4 | 4 |
| PACK-WR-01 | 1 | 2 | 3 | 3 |
| AGV-WR-01 | 3 | 0 | 3 | 2 |
| **合计** | **12** | **15** | **29** | **25** |

---

## 5. 质量门与力学标准

| 工序 | qc_id | 说明 |
|------|-------|------|
| wire_drawing | rod_inspection | 盘条来料检验 |
| cutting | cut_length_qc | 切丝长度 SPC |
| drying | moisture_qc | 含水率终检 |

**E7014 力学标准**：Rm ≥ 610 MPa · KV₂ ≥ 69 J · A ≥ 22% · I ≥ 6.0

---

## 6. WIP 位置编码

```
WR-LINE-01/wire_drawing
WR-LINE-01/cutting
WR-LINE-01/powder_mixing
WR-LINE-01/coating
WR-LINE-01/drying
WR-LINE-01/packaging
WR-LINE-01/stock_in_out
```

---

## 7. 网关配置模板

```yaml
source_instance: gw-wr-line-01
product_line_id: WR-LINE-01
template_id: welding_rod
devices:
  - equipment_id: DRAW-WR-01
    energy_meter_id: EM-WR-01
    tags: [line_speed_m_per_min, status, alarm_code, power_kw]
  - equipment_id: CUT-WR-01
    energy_meter_id: EM-WR-02
    tags: [length_mm, cutting_speed_cuts_per_min, fault_code, status, alarm_code]
  - equipment_id: MIXER-WR-01
    energy_meter_id: EM-WR-03
    tags: [mixing_rpm, status, alarm_code, power_kw]
  - equipment_id: COAT-WR-01
    energy_meter_id: EM-WR-04
    tags: [coating_thickness_mm, coating_pressure_MPa, status, alarm_code, power_kw]
  - equipment_id: DRY-WR-01
    energy_meter_id: EM-WR-05
    tags: [actual_temp_C, moisture_pct, status, alarm_code]
  - equipment_id: PACK-WR-01
    energy_meter_id: EM-WR-06
    tags: [package_count, status, power_kw]
  - equipment_id: AGV-WR-01
    protocol: mqtt
    tags: [status, location, quantity_kg]
```

---

## 8. 复制到其他焊条线

WR-LINE-02~20 设备前缀为 `WR02-`、`WR15-` 等，字段清单与本表相同。  
焊条车间共 20 条产线，单班运行。
