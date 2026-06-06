/**
 * 场景实验室元数据：分类、仿真机理、预期 KPI 方向、使用说明。
 * 与 master-data simulation_defaults.scenarios 的 id 一一对应。
 */

export const SCENARIO_CATEGORIES = {
  normal: {
    key: 'normal',
    label: '正常运行',
    color: 'green',
    desc: '标准当班或计划内工序切换，无异常注入'
  },
  abnormal: {
    key: 'abnormal',
    label: '异常工况',
    color: 'red',
    desc: '质量、设备、物料或追溯类异常，用于验证韧性'
  },
  business: {
    key: 'business',
    label: '经营策略',
    color: 'amber',
    desc: '产能爬坡、旺季节奏、出口交期、绿电排产等经营场景'
  }
}

/** @type {Record<string, object>} */
export const SCENARIO_CATALOG = {
  normal_shift: {
    category: 'normal',
    summary: '全厂 42 线按标准工艺节拍推进 WIP，作为一切对比的默认基线。',
    effects: [
      '不注入额外报警、Hold 或缺料事件',
      '工艺参数在规格带内正常波动',
      '批次、工序、遥测按模板连续 tick'
    ],
    affectedLines: '全部 42 线',
    kpiHint: { efficiency: '基准', cost: '基准', quality: '基准' },
    useCase: '恢复常态、作为其他场景对比前的参考态'
  },
  batch_handover: {
    category: 'normal',
    summary: '模拟班次/批次交接：重置当前批次号与工序索引，WIP 从首工序重新计数。',
    effects: [
      '生成新批次号（FCW-日期-B 序号）',
      'step_index / step_tick 归零',
      '各线工序状态重新初始化'
    ],
    affectedLines: '全部仿真线',
    kpiHint: { efficiency: '短期↓', cost: '—', quality: '—' },
    useCase: '验证交接窗口对 OEE、在制品堆积的影响'
  },
  fill_ratio_drift: {
    category: 'abnormal',
    summary: '药芯填充率持续上浮并逼近规格上限，触发工艺越限与质量风险。',
    effects: [
      'fill_ratio_pct 字段持续漂移上浮',
      'spec_violation_active 开启，可能触发质控关注',
      '药芯线（FCW）填充工序数据点异常率上升'
    ],
    affectedLines: '药芯焊丝 FCW 线为主',
    kpiHint: { efficiency: '↓', cost: '↑', quality: '↓↓' },
    useCase: '验证填充率失控时质量指数与合格率变化'
  },
  quality_hold: {
    category: 'abnormal',
    summary: '包装/终检工序强制质量门 Hold，产出暂存待判区，产线出现等待。',
    effects: [
      'force_quality_hold 在 packaging / stock_in_out 生效',
      '不合格或待判批次进入 WH-HOLD-01',
      '工序 dwell 增加，有效产出下降'
    ],
    affectedLines: '全部仿真线终检段',
    kpiHint: { efficiency: '↓↓', cost: '↑', quality: '↑/↓' },
    useCase: '评估质门 Hold 对交期与三极致权衡的冲击'
  },
  plating_alarm: {
    category: 'abnormal',
    summary: '镀铜工序在第 30 tick 自动注入「断丝」Critical 报警，模拟镀层异常停线。',
    effects: [
      'tick=30 时注入 E101 断丝报警（镀铜设备）',
      '报警持续约 30 tick，可能触发后续维保 dwell',
      '镀铜段 active_power 与 running_equipment 骤降'
    ],
    affectedLines: '含镀铜工序的 FCW / SW 线',
    kpiHint: { efficiency: '↓↓', cost: '↑', quality: '↓' },
    useCase: '演练镀铜断丝应急响应与产能损失量化'
  },
  trace_gap: {
    category: 'abnormal',
    summary: '新批次无 parent_batches 关联，制造追溯链断点，影响合规与召回能力。',
    effects: [
      '批次 parent_batches 置空',
      '追溯查询返回缺口',
      '不影响物理产出但拉低质量合规评分'
    ],
    affectedLines: '全部仿真线新批次',
    kpiHint: { efficiency: '—', cost: '—', quality: '↓' },
    useCase: '验证 MES/追溯断点对质量指数与审计风险的影响'
  },
  raw_material_low: {
    category: 'abnormal',
    summary: '关键原料库存不足，产线进入等料 dwell 并可能触发 E701 缺料报警。',
    effects: [
      'raw_material_low 标志开启，工序等待补料',
      'MATERIAL_SHORTAGE 事件 → Critical 报警',
      '恢复需 material_recovery_ticks 个 tick'
    ],
    affectedLines: '全部仿真线',
    kpiHint: { efficiency: '↓↓', cost: '↑', quality: '—' },
    useCase: '评估供应链中断时的停线时长与 OEE 损失'
  },
  equipment_maintenance: {
    category: 'abnormal',
    summary: '当前工序设备进入计划维保 MAINTENANCE 停留，产能阶段性归零。',
    effects: [
      '切换时立即 enter_maintenance_dwell',
      'tick=20 可能二次触发工序维保',
      'running_equipment_count 下降，能耗降低'
    ],
    affectedLines: '全部仿真线当前工序',
    kpiHint: { efficiency: '↓↓', cost: '↓/↑', quality: '—' },
    useCase: '排产维保窗口对产能与能源成本的权衡分析'
  },
  ramp_up_fcw_2025: {
    category: 'business',
    summary: '2025 药芯焊丝 2.5 万吨目标下的产能爬坡：批次产量提升约 15%。',
    effects: [
      'FCW 线 default_batch_output_kg ×1.15（上限 1600kg）',
      'quantity_kg 同步上调',
      '线速不变，靠批次放大提产能'
    ],
    affectedLines: 'FCW-* 药芯线',
    kpiHint: { efficiency: '↑', cost: '↓', quality: '↓' },
    useCase: '验证产能爬坡目标下的效率-质量平衡点'
  },
  rod_peak_season: {
    category: 'business',
    summary: '焊条产线旺季节奏：工序节拍加快约 25%，逼近 30 万吨年化产能。',
    effects: [
      'WR 线 ticks_per_step 降至 base ×0.75',
      '单工序停留时间缩短，产出加快',
      '设备负荷与质量波动风险同步上升'
    ],
    affectedLines: 'WR-* 焊条线',
    kpiHint: { efficiency: '↑↑', cost: '↓', quality: '↓' },
    useCase: '旺季满产策略下的质量风险预警'
  },
  export_rush: {
    category: 'business',
    summary: '出口订单集中交付：物流提前期缩短 40%，交期压力 15 天。',
    effects: [
      'logistics_lead_ticks ×0.6',
      '订单履约周期压缩',
      '高交期压力订单占比上升'
    ],
    affectedLines: '全部仿真线订单流',
    kpiHint: { efficiency: '↑', cost: '↑', quality: '—' },
    useCase: '出口旺季交期与成本指数联动评估'
  },
  clean_energy_noon: {
    category: 'business',
    summary: '午间光伏出力高峰（11:00–14:00）：绿电占比提升，烘干工序能耗优化。',
    effects: [
      '11–14 点绿电比例 +22%',
      '烘干工序叠加余热回收节能',
      '全厂 green_power_ratio_pct 上升'
    ],
    affectedLines: '全厂能源模型',
    kpiHint: { efficiency: '—', cost: '↑', quality: '—' },
    useCase: '绿电排产策略对成本指数与碳排的贡献验证'
  },
  plant_green_shift_60: {
    category: 'business',
    summary: '能碳决策专用：全厂高耗能工序向绿电窗口偏移 60%，提升绿电消纳比例。',
    effects: [
      '全厂绿电占比提升约 +2.2%（相对 50% 基准）',
      '电网购电与 Scope2 碳排同比下降',
      '不改变产线工艺，仅调整能源结构投影'
    ],
    affectedLines: '全厂 42 线能源模型',
    kpiHint: { efficiency: '—', cost: '↑', quality: '—' },
    useCase: '验证中等绿电偏移对成本指数与碳排强度的影响'
  },
  plant_green_shift_80: {
    category: 'business',
    summary: '能碳决策专用：全厂绿电排产偏移 80%，最大化光伏/风电消纳，激进减碳策略。',
    effects: [
      '全厂绿电占比提升约 +6.6%（相对 50% 基准）',
      '电网购电显著下降，净碳排大幅降低',
      '需配合 11:00–14:00 绿电窗口排产效果最佳'
    ],
    affectedLines: '全厂 42 线能源模型',
    kpiHint: { efficiency: '—', cost: '↑↑', quality: '—' },
    useCase: '能碳模块「应用并对比」首选场景，验证激进绿电策略'
  },
  hydro_project_custom: {
    category: 'business',
    summary: '海工高牌号定制单：Rm≥830 / HY-960MPa 规格，45 天交期专项排产。',
    effects: [
      'grade 切换为 HY-960MPa，recipe FCW-HY960-V1',
      '关闭通用 spec_violation 干扰',
      '高规格工艺路径，质量要求更严'
    ],
    affectedLines: 'FCW 海工定制产线',
    kpiHint: { efficiency: '↓', cost: '↓', quality: '↑' },
    useCase: '高附加值定制单的质量溢价与产能占用分析'
  }
}

export const SCENARIO_GROUPS = [
  {
    label: '正常运行',
    category: 'normal',
    ids: ['normal_shift', 'batch_handover']
  },
  {
    label: '异常工况',
    category: 'abnormal',
    ids: [
      'fill_ratio_drift', 'quality_hold', 'plating_alarm', 'trace_gap',
      'raw_material_low', 'equipment_maintenance'
    ]
  },
  {
    label: '经营策略',
    category: 'business',
    ids: [
      'ramp_up_fcw_2025', 'rod_peak_season', 'export_rush',
      'hydro_project_custom'
    ]
  },
  {
    label: '能碳策略',
    category: 'business',
    ids: ['plant_green_shift_60', 'plant_green_shift_80', 'clean_energy_noon']
  }
]

/**
 * 合并 API 场景与本地 catalog，API 字段优先保留。
 */
export function enrichScenario(apiScenario) {
  const cat = SCENARIO_CATALOG[apiScenario.id] || {}
  const category = SCENARIO_CATEGORIES[cat.category] || SCENARIO_CATEGORIES.normal
  return {
    ...apiScenario,
    category: cat.category || 'normal',
    categoryLabel: category.label,
    categoryColor: category.color,
    summary: cat.summary || apiScenario.description || '',
    effects: cat.effects || [],
    affectedLines: cat.affectedLines || '—',
    kpiHint: cat.kpiHint || {},
    useCase: cat.useCase || ''
  }
}

export function enrichScenarios(list) {
  return (list || []).map(enrichScenario)
}

export function kpiHintClass(hint) {
  if (!hint || hint === '—' || hint === '基准') return 'neutral'
  if (hint.includes('↑↑')) return 'up-strong'
  if (hint.includes('↑')) return 'up'
  if (hint.includes('↓↓')) return 'down-strong'
  if (hint.includes('↓')) return 'down'
  return 'neutral'
}
