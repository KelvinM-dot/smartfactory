/** 订单与交付模块统一文案与样式映射 */

export const PRODUCT_CATEGORY_LABELS = {
  flux_core_wire: '药芯焊丝',
  solid_wire: '实心焊丝',
  submerged_arc_wire: '埋弧焊丝',
  welding_rod: '焊条'
}

export const ORDER_STATUS_LABELS = {
  released: '已下达',
  in_progress: '执行中',
  blocked: '受阻',
  ready_to_ship: '待发运',
  completed: '已完成'
}

export const ORDER_RISK_LABELS = {
  low: '低',
  medium: '中',
  high: '高',
  critical: '严重'
}

export const ORDER_TYPE_LABELS = {
  regular: '常规',
  export: '出口',
  custom: '定制'
}

export const PRIORITY_LABELS = {
  low: '低',
  normal: '普通',
  high: '高'
}

export const QUALITY_DECISION_LABELS = {
  pass: '放行',
  hold: 'Hold',
  rework: '返工'
}

export const LOGISTICS_STATUS_LABELS = {
  created: '已创建',
  pending: '待执行',
  transporting: '运输中',
  completed: '已完成',
  failed: '失败'
}

export const TIMELINE_EVENT_LABELS = {
  batch_started: '批次启动',
  batch_completed: '批次完工',
  quality_gate: '质量门',
  logistics_task: '物流任务'
}

/** 产线设计产能折减系数（实际运行非满产） */
export const LINE_ACTUAL_UTILIZATION = 0.62

export function productCategoryLabel(value) {
  return PRODUCT_CATEGORY_LABELS[value] || value || '—'
}

export function orderStatusLabel(value) {
  return ORDER_STATUS_LABELS[value] || value || '—'
}

export function orderRiskLabel(value) {
  return ORDER_RISK_LABELS[value] || value || '—'
}

export function orderTypeLabel(value) {
  return ORDER_TYPE_LABELS[value] || value || '—'
}

export function priorityLabel(value) {
  return PRIORITY_LABELS[value] || value || '—'
}

export function qualityDecisionLabel(value) {
  return QUALITY_DECISION_LABELS[value] || value || '—'
}

export function logisticsStatusLabel(value) {
  return LOGISTICS_STATUS_LABELS[value] || value || '—'
}

export function timelineEventLabel(value) {
  return TIMELINE_EVENT_LABELS[value] || value || '—'
}

export function orderBadgeTone(status) {
  if (status === 'completed' || status === 'pass' || status === 'low') return 'running'
  if (status === 'ready_to_ship' || status === 'medium' || status === 'rework' || status === 'in_progress' || status === 'released') return 'manual'
  if (status === 'blocked' || status === 'hold' || status === 'high' || status === 'critical') return 'stopped'
  return 'manual'
}

export function fmtQtyT(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return `${Number(value).toFixed(1)} t`
}

export function fmtDaysToDue(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  const days = Number(value)
  if (days < 0) return `已逾期 ${Math.abs(days).toFixed(0)} 天`
  if (days <= 2) return `剩余 ${days.toFixed(0)} 天（紧迫）`
  return `剩余 ${days.toFixed(0)} 天`
}
