export const EC_TABS = [
  { key: 'overview', label: '能碳总览' },
  { key: 'supply', label: '能源结构' },
  { key: 'demand', label: '用能诊断' },
  { key: 'batches', label: '批次碳足迹' },
  { key: 'optimize', label: '能碳优化' }
]

export const BATCH_FOOTPRINT_GUIDE = '按产线单吨能耗 × 批次产量（吨）分摊能耗，再按当前全厂绿电占比计算 Scope2 与绿电抵扣。适用于在制品/近完成批次碳强度对比，非 CBAM 认证口径。'

export const ENERGY_SCENARIO_IDS = {
  plant_green_shift_60: { greenShift: 60, label: '全厂绿电偏移 60%' },
  plant_green_shift_80: { greenShift: 80, label: '全厂绿电偏移 80%' },
  clean_energy_noon: { greenShift: null, label: '午间绿电高峰' }
}

export const EC_PRINCIPLE_SECTIONS = [
  {
    title: '能碳决策做什么',
    body: '在全厂 42 线仿真基线上，统一呈现实时能耗、绿电结构、碳排强度与减碳进度，并输出可执行的绿电排产建议。区别于工厂总览的运营监控，本模块聚焦决策与优化。'
  },
  {
    title: '碳核算口径（POC）',
    body: 'Scope2 = 电网购电 kWh × 排放因子（默认 0.5704 kg/kWh）；绿电抵扣 = 绿电 kWh × 排放因子 × 抵扣系数（0.8）；净碳排 = Scope2 − 抵扣。碳排强度 = 净碳排 ÷ 估算产量（吨）。非 CBAM 认证口径。'
  },
  {
    title: '三模块深度联动',
    body: '① 本页快调绿电偏移（纸面）→ ② 跳转平衡点带 green_shift_pct 验证四杠杆 → ③ 应用能碳场景跳转场景实验室自动对比。回跳时通过 URL 参数 from=energy-carbon 保持上下文。'
  }
]

export const REC_CATEGORIES = {
  energy: { label: '能源', color: 'amber' },
  carbon: { label: '碳排', color: 'teal' },
  cost: { label: '成本', color: 'cyan' }
}

export const ANOMALY_LABELS = {
  high_consumption: { label: '能耗偏高', color: 'red' },
  dwell_active: { label: '工序等待', color: 'amber' },
  maint_energy: { label: '维保耗能', color: 'amber' },
  low_output: { label: '产出偏低', color: 'cyan' }
}

export const ASSET_TYPE_COLORS = {
  pv: 'amber',
  wind: 'cyan',
  grid: 'red',
  waste_heat: 'teal'
}
