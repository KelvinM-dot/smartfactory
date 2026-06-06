/**
 * 三极致平衡点分析元数据：杠杆说明、权重解读、使用指引。
 */

export const BALANCE_PRINCIPLE_SECTIONS = [
  {
    title: '平衡点分析做什么',
    body: '以全厂 42 线当前 KPI 快照为基线，通过拖动四类「生产杠杆」滑杆，实时投影效率 · 成本 · 质量三指数的变化，并在离散网格上搜索 Pareto 前沿与加权综合分最高的最佳平衡点。'
  },
  {
    title: '计算方式（纸面推演）',
    body: '后端基于当前三极致指数，按固定灵敏度公式推算杠杆变化后的指数（非改写 line-simulator）。综合分 = 效率×权重 + 成本×权重 + 质量×权重。网格覆盖批次 6 档 × 线速 7 档 × 绿电 5 档 × 规格 4 档，共 840 种组合。'
  },
  {
    title: '与能碳/场景实验室联动',
    body: '能碳模块可通过 ?green_shift_pct= 跳转本页并同步绿电偏移滑杆；验证后可前往场景实验室应用 plant_green_shift_* 场景做仿真对比。采纳最佳杠杆仅更新滑杆，不下发模拟器。'
  }
]

export const LEVER_CATEGORIES = {
  capacity: {
    key: 'capacity',
    label: '产能杠杆',
    color: 'cyan',
    desc: '直接影响产出节拍与批次经济规模'
  },
  energy: {
    key: 'energy',
    label: '能源杠杆',
    color: 'amber',
    desc: '绿电窗口排产偏移，影响能源成本结构'
  },
  planning: {
    key: 'planning',
    label: '排产杠杆',
    color: 'teal',
    desc: '同规格订单集中度，影响换型损失与质量稳定性'
  }
}

export const LEVER_CATALOG = [
  {
    key: 'batch_size_kg',
    label: '批次产量',
    category: 'capacity',
    unit: 'kg',
    min: 650,
    max: 1500,
    step: 50,
    default: 1200,
    desc: '单批次产出重量，反映规模经济。基准 1200kg，药芯线典型区间 800–1500kg。',
    businessMeaning: '扩大批次可减少换型次数、摊薄准备时间，适合订单稳定、规格单一的产线。',
    tradeoff: '↑ 效率、↓ 单位成本 · ↓ 质量（波动容忍度下降）',
    sensitivityKey: 'batch_size_kg'
  },
  {
    key: 'line_speed_pct',
    label: '线速倍率',
    category: 'capacity',
    unit: '%',
    min: 85,
    max: 115,
    step: 1,
    default: 100,
    desc: '相对标准工艺节拍的线速缩放。100% 为设计节拍，±15% 为安全调节带。',
    businessMeaning: '旺季满产时适度提速；质量敏感期或新规格试产时降速保稳。',
    tradeoff: '↑ 效率、↓ 成本 · ↓↓ 质量（提速 1% 质量约 -0.4）',
    sensitivityKey: 'line_speed_pct'
  },
  {
    key: 'green_shift_pct',
    label: '绿电排产偏移',
    category: 'energy',
    unit: '%',
    min: 0,
    max: 100,
    step: 5,
    default: 50,
    desc: '高能耗工序向光伏/风电出力高峰时段偏移的比例。0% 不偏移，100% 全力跟随绿电曲线。',
    businessMeaning: '配合厂区 1.2MW 光伏 + 0.8MW 风电，午间烘干、镀铜等高耗工序优先排入绿电窗口。',
    tradeoff: '↑ 成本指数（绿电占比提升）· 效率微增 · 质量影响极小',
    sensitivityKey: 'green_shift_pct'
  },
  {
    key: 'grade_concentration_pct',
    label: '同规格集中度',
    category: 'planning',
    unit: '%',
    min: 0,
    max: 100,
    step: 5,
    default: 60,
    desc: '同一牌号/规格订单连续排产的比例。越高则换型次数越少，工艺参数越稳定。',
    businessMeaning: '出口集中交付期可提高集中度；多品种小批量订单则需降低。',
    tradeoff: '↑ 效率、↑ 质量 · 成本略升（备货与库存策略调整）',
    sensitivityKey: 'grade_concentration_pct'
  }
]

export const WEIGHT_CATALOG = [
  {
    key: 'efficiency',
    label: '效率权重',
    color: 'cyan',
    desc: 'OEE、产能利用率、交付周期等效率类指标在综合分中的占比。'
  },
  {
    key: 'cost',
    label: '成本权重',
    color: 'amber',
    desc: '能源成本、单位产出成本、绿电占比等成本类指标在综合分中的占比。'
  },
  {
    key: 'quality',
    label: '质量权重',
    color: 'teal',
    desc: '合格率、规格符合度、追溯完整性等质量类指标在综合分中的占比。'
  }
]

export const TRIANGLE_GUIDE = [
  '三角形三个顶点分别代表效率（上）、成本（左下）、质量（右下）',
  '蓝点 = 当前滑杆位置投影的三指数归一化重心',
  '绿点 = 网格搜索得到的加权最佳平衡点',
  '两点间虚线距离 ≈ 策略调整空间；越短说明已接近最优'
]

export const PARETO_GUIDE = 'Pareto 前沿：在效率-成本-质量三维中，无法在不牺牲某一维的情况下同时提升其他维的杠杆组合样本。展示综合分最高的 12 个非支配解，供多目标权衡参考。'

/** 灵敏度字段 → 展示标签 */
export const SENSITIVITY_LABELS = {
  line_speed_pct: {
    label: '线速倍率',
    unit: '每 +1%',
    fields: [
      { key: 'efficiency_per_1pct', label: '效率' },
      { key: 'quality_per_1pct', label: '质量' },
      { key: 'cost_per_1pct', label: '成本' }
    ]
  },
  batch_size_kg: {
    label: '批次产量',
    unit: '每 +100kg',
    fields: [
      { key: 'efficiency_per_100kg', label: '效率' },
      { key: 'quality_per_100kg', label: '质量' },
      { key: 'cost_per_100kg', label: '成本' }
    ]
  },
  green_shift_pct: {
    label: '绿电排产偏移',
    unit: '每 +1%',
    fields: [
      { key: 'efficiency_per_1pct', label: '效率' },
      { key: 'quality_per_1pct', label: '质量' },
      { key: 'cost_per_1pct', label: '成本' }
    ]
  },
  grade_concentration_pct: {
    label: '同规格集中度',
    unit: '每 +1%',
    fields: [
      { key: 'efficiency_per_1pct', label: '效率' },
      { key: 'quality_per_1pct', label: '质量' },
      { key: 'cost_per_1pct', label: '成本' }
    ]
  }
}

export function fmtSensValue(v) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  const n = Number(v)
  return (n >= 0 ? '+' : '') + n.toFixed(2)
}

export function sensClass(v) {
  const n = Number(v)
  if (n > 0.05) return 'up'
  if (n < -0.05) return 'down'
  return 'neutral'
}
