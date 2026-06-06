/** 产线目录：车间分组、筛选、默认产线解析（42 线全仿真） */

export const WORKSHOP_LABELS = {
  'WS-WIRE-01': '焊丝车间',
  'WS-ROD-01': '焊条车间',
  'WS-WH-01': '仓储'
}

export const CATEGORY_LABELS = {
  flux_core_wire: '药芯',
  solid_wire: '实心',
  submerged_arc_wire: '埋弧',
  welding_rod: '焊条'
}

export const WORKSHOP_ORDER = ['WS-WIRE-01', 'WS-ROD-01', 'WS-WH-01']

const LINE_KEY = 'jqhc-last-line'
const FALLBACK_LINE_ID = 'FCW-LINE-07'

export function workshopLabel(workshopId) {
  return WORKSHOP_LABELS[workshopId] || workshopId || '其他'
}

export function categoryLabel(category) {
  return CATEGORY_LABELS[category] || category || ''
}

export function lineShortId(lineId) {
  return String(lineId || '').replace('-LINE-', '#')
}

export function groupLinesByWorkshop(lines) {
  const groups = {}
  for (const line of lines || []) {
    const ws = line.workshop_id || 'OTHER'
    if (!groups[ws]) groups[ws] = []
    groups[ws].push(line)
  }
  for (const ws of Object.keys(groups)) {
    groups[ws].sort((a, b) =>
      String(a.product_line_id).localeCompare(String(b.product_line_id))
    )
  }
  return groups
}

export function orderedWorkshopGroups(lines) {
  const groups = groupLinesByWorkshop(lines)
  const ordered = []
  for (const ws of WORKSHOP_ORDER) {
    if (groups[ws]?.length) ordered.push({ workshopId: ws, lines: groups[ws] })
  }
  for (const [ws, list] of Object.entries(groups)) {
    if (!WORKSHOP_ORDER.includes(ws)) ordered.push({ workshopId: ws, lines: list })
  }
  return ordered
}

export function filterLines(lines, { workshop = 'all', search = '', status = 'all' } = {}) {
  let result = lines || []
  if (workshop === 'wire') {
    result = result.filter(l => l.workshop_id === 'WS-WIRE-01')
  } else if (workshop === 'rod') {
    result = result.filter(l => l.workshop_id === 'WS-ROD-01')
  }
  if (status !== 'all') {
    result = result.filter(l => l.status === status)
  }
  const q = search.trim().toLowerCase()
  if (q) {
    result = result.filter(l =>
      String(l.product_line_id).toLowerCase().includes(q)
      || String(l.name || '').toLowerCase().includes(q)
      || String(l.product_category || '').toLowerCase().includes(q)
    )
  }
  return result
}

export function countSimulationLines(lines) {
  return (lines || []).filter(l => l.simulation_enabled).length
}

export function pickDefaultLineId(lines) {
  const list = lines || []
  const saved = typeof localStorage !== 'undefined' ? localStorage.getItem(LINE_KEY) : null
  if (saved && list.some(l => l.product_line_id === saved)) return saved
  const activeSim = list.find(l => l.simulation_enabled && l.status === 'active')
  if (activeSim) return activeSim.product_line_id
  const active = list.find(l => l.status === 'active')
  if (active) return active.product_line_id
  return list[0]?.product_line_id || FALLBACK_LINE_ID
}

export function lineStatusBadge(line) {
  const map = {
    active: { label: '运行', cls: 'running' },
    maintenance: { label: '维护', cls: 'manual' },
    inactive: { label: '停用', cls: 'stopped' }
  }
  return map[line?.status] || { label: line?.status || '—', cls: 'unknown' }
}

export function kpiModeBadge(card) {
  const mode = card?.overview?.data_source?.kpi_mode
  if (mode === 'telemetry') return { label: '实时遥测', cls: 'running' }
  if (card?.line?.simulation_enabled) return { label: '仿真离线', cls: 'manual' }
  return { label: '估算', cls: 'stopped' }
}
