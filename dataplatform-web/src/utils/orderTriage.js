/** 订单交付工作台：筛选、排序、阻塞归因、进度分段 */

import { LINE_ACTUAL_UTILIZATION } from './orderLabels'

export const VIEW_MODES = {
  workbench: 'workbench',
  all: 'all'
}

export const SCENARIO_FILTERS = [
  { id: '', label: '全部场景' },
  { id: 'export', label: '出口集中' },
  { id: 'custom', label: '定制插单' },
  { id: 'regular', label: '常规' }
]

const RISK_WEIGHT = { critical: 4, high: 3, medium: 2, low: 1 }

export function isActiveOrder(order) {
  if (!order) return false
  const status = order.order_status || order.status
  return !!status && status !== 'completed'
}

/** 紧急需介入：受阻 / 高风险 / 7 日内交期等 */
export function isMustHandleOrder(order) {
  if (!order) return false
  const status = order.order_status || order.status
  if (status === 'completed') return false
  const risk = order.delivery_risk_level
  const days = Number(order.days_to_due)
  return status === 'blocked'
    || status === 'ready_to_ship'
    || order.delivery_blocked
    || risk === 'high'
    || risk === 'critical'
    || (!Number.isNaN(days) && days <= 7)
}

/** 工作台/驾驶舱展示队列：紧急优先，否则取在制订单 */
export function pickMustHandleOrders(orders, limit = 5) {
  const sorted = sortOrdersForDelivery(orders || [])
  const urgent = sorted.filter(isMustHandleOrder)
  if (urgent.length) return urgent.slice(0, limit)
  return sorted.filter(isActiveOrder).slice(0, limit)
}

export function riskSortScore(order) {
  const status = order.order_status || order.status
  let score = RISK_WEIGHT[order.delivery_risk_level] || 0
  if (status === 'blocked') score += 10
  if (order.delivery_blocked) score += 6
  if (status === 'ready_to_ship') score += 4
  const days = Number(order.days_to_due)
  if (!Number.isNaN(days)) {
    if (days < 0) score += 8
    else if (days <= 3) score += 5
    else if (days <= 7) score += 2
  }
  return score
}

export function sortOrdersForDelivery(orders) {
  return [...(orders || [])].sort((a, b) => {
    const diff = riskSortScore(b) - riskSortScore(a)
    if (diff !== 0) return diff
    const da = Number(a.days_to_due)
    const db = Number(b.days_to_due)
    if (!Number.isNaN(da) && !Number.isNaN(db) && da !== db) return da - db
    return String(a.production_order_id).localeCompare(String(b.production_order_id))
  })
}

export function lineBelongsToWorkshop(lineId, lines, workshop) {
  if (!workshop || workshop === 'all' || !lineId) return true
  const line = (lines || []).find(l => l.product_line_id === lineId)
  if (!line) return true
  if (workshop === 'wire') return line.workshop_id === 'WS-WIRE-01'
  if (workshop === 'rod') return line.workshop_id === 'WS-ROD-01'
  return true
}

export function orderMatchesLine(order, lineId) {
  if (!lineId) return true
  const ids = order.assigned_line_ids || []
  return ids.includes(lineId)
}

export function filterOrdersByContext(orders, opts = {}) {
  const {
    workshop = 'all',
    lineId = '',
    scenarioType = '',
    statusFilter = '',
    riskFilter = '',
    viewMode = VIEW_MODES.workbench,
    lines = []
  } = opts

  let result = orders || []

  if (workshop && workshop !== 'all') {
    result = result.filter(o =>
      (o.assigned_line_ids || []).some(lid => lineBelongsToWorkshop(lid, lines, workshop))
    )
  }
  if (lineId) {
    result = result.filter(o => orderMatchesLine(o, lineId))
  }

  if (scenarioType) {
    result = result.filter(o => (o.order_type || 'regular') === scenarioType)
  }
  if (statusFilter) {
    result = result.filter(o => (o.order_status || o.status) === statusFilter)
  }
  if (riskFilter) {
    result = result.filter(o => o.delivery_risk_level === riskFilter)
  }
  if (viewMode === VIEW_MODES.workbench) {
    result = result.filter(isActiveOrder)
  }

  return sortOrdersForDelivery(result)
}

export function computeTriageCounts(orders) {
  const list = orders || []
  const open = list.filter(o => (o.order_status || o.status) !== 'completed')
  return {
    active: open.length,
    mustHandle: open.filter(isMustHandleOrder).length,
    blocked: open.filter(o => (o.order_status || o.status) === 'blocked').length,
    highRisk: open.filter(o => ['high', 'critical'].includes(o.delivery_risk_level)).length,
    dueSoon: open.filter(o => {
      const d = Number(o.days_to_due)
      return !Number.isNaN(d) && d <= 7
    }).length,
    readyShip: open.filter(o => (o.order_status || o.status) === 'ready_to_ship').length,
    total: list.length
  }
}

export function resolveBlockReasons(order, detail = null) {
  const reasons = []
  const status = order?.order_status || order?.status || detail?.order?.status
  const risk = detail?.risk || order || {}
  const apiReasons = (risk.blocking_reasons || order?.blocking_reasons || [])
    .filter(r => r && r !== '暂无明显阻塞')
  if (apiReasons.length) reasons.push(...apiReasons)

  if (status === 'blocked' && !reasons.length) {
    reasons.push('订单已标记受阻，待确认恢复条件')
  }
  if (risk.delivery_blocked) reasons.push('交付链路阻塞')
  if ((risk.blocking_quality_gates || 0) > 0) {
    reasons.push(`质量门 Hold ×${risk.blocking_quality_gates}`)
  }
  if ((risk.blocking_logistics_tasks || 0) > 0) {
    reasons.push(`物流待办 ×${risk.blocking_logistics_tasks}`)
  }
  if ((risk.blocked_quantity_t || 0) > 0 && !reasons.length) {
    reasons.push(`待放行 ${Number(risk.blocked_quantity_t).toFixed(1)} t`)
  }
  return [...new Set(reasons)]
}

export function formatProgressPct(pct) {
  const n = Number(pct)
  if (Number.isNaN(n) || n <= 0) return '刚启动'
  if (n < 1) return '<1%'
  return `${n.toFixed(0)}%`
}

export function progressSegments(order) {
  const planned = Math.max(Number(order?.planned_quantity_t) || 0, 0.001)
  const completed = Math.max(Number(order?.completed_quantity_t) || 0, 0)
  const inProg = Math.max(Number(order?.in_progress_quantity_t) || 0, 0)
  const ready = Math.max(Number(order?.ready_to_ship_quantity_t) || 0, 0)
  const released = Math.max(Number(order?.released_quantity_t) || 0, 0)
  const blocked = Math.max(Number(order?.blocked_quantity_t) || 0, 0)
  const accounted = completed + inProg + ready + blocked
  const unreleased = Math.max(planned - Math.max(released, accounted), 0)

  return {
    planned,
    segments: [
      { key: 'completed', label: '已完成', value: completed, tone: 'done' },
      { key: 'ready', label: '待发运', value: ready, tone: 'ready' },
      { key: 'in_progress', label: '执行中', value: inProg, tone: 'active' },
      { key: 'blocked', label: '阻塞', value: blocked, tone: 'blocked' },
      { key: 'unreleased', label: '未释放', value: unreleased, tone: 'pending' }
    ].filter(s => s.value > 0.0001)
  }
}

export function releaseProgress(order) {
  const planned = Number(order?.planned_quantity_t) || 0
  const released = Number(order?.released_quantity_t) || 0
  if (!planned) return { pct: 0, released: 0, planned: 0, remaining: 0 }
  const pct = Math.min(100, (released / planned) * 100)
  return {
    pct,
    released,
    planned,
    remaining: Math.max(planned - released, 0)
  }
}

export function lineBacklogLoadDays(lineId, orders, lineMeta) {
  if (!lineId || !lineMeta) return null
  const design = Number(lineMeta.design_capacity_t_per_day) || 0
  const capacity = design * LINE_ACTUAL_UTILIZATION
  if (!capacity) return null
  const backlog = (orders || [])
    .filter(o => {
      const ids = o.assigned_line_ids || []
      return ids.includes(lineId) && (o.order_status || o.status) !== 'completed'
    })
    .reduce((sum, o) => {
      const planned = Number(o.planned_quantity_t) || 0
      const done = Number(o.completed_quantity_t) || 0
      return sum + Math.max(planned - done, 0)
    }, 0)
  return backlog / capacity
}

export function lineLoadHint(order, allOrders, lines) {
  const lineId = (order.assigned_line_ids || [])[0]
  if (!lineId) return null
  const line = (lines || []).find(l => l.product_line_id === lineId)
  const days = lineBacklogLoadDays(lineId, allOrders, line)
  if (days == null) return null
  const planned = Number(order.planned_quantity_t) || 0
  const capacity = (Number(line?.design_capacity_t_per_day) || 0) * LINE_ACTUAL_UTILIZATION
  const extra = capacity ? planned / capacity : 0
  if (extra < 0.3 && days < 5) return null
  return `负荷约 ${days.toFixed(1)} 天${extra >= 0.3 ? ` · 本单 +${extra.toFixed(1)} 天` : ''}`
}
