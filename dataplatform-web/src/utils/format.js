export function fmtTime(iso) {
  if (!iso) return '—'
  const s = String(iso)
  return s.length >= 19 ? s.slice(11, 19) : s
}

export function fmtDateTime(iso) {
  if (!iso) return '—'
  return String(iso).replace('T', ' ').replace('Z', '').slice(0, 19)
}

export function statusLabel(status) {
  const map = {
    running: '运行',
    alarm: '报警',
    manual: '手动',
    stopped: '停机',
    unknown: '未知',
    in_progress: '进行中',
    completed: '已完成',
    pending: '待处理',
    acknowledged: '已确认',
    resolved: '已解决'
  }
  return map[status] || status
}

export function statusClass(status) {
  if (!status) return 'unknown'
  const s = String(status).toUpperCase()
  if (s === 'RUNNING' || status === 'running') return 'running'
  if (s === 'ALARM' || status === 'alarm') return 'alarm'
  if (s === 'MANUAL' || status === 'manual') return 'manual'
  if (s === 'STOPPED' || status === 'stopped') return 'stopped'
  return 'unknown'
}

/** 百分比展示：避免 4.279999999999999 等浮点尾差 */
export function fmtPct(value, digits = 1) {
  if (value == null || value === '' || Number.isNaN(Number(value))) return '—'
  const factor = 10 ** digits
  return (Math.round(Number(value) * factor) / factor).toFixed(digits)
}

export function inSpec(value, limits) {
  if (value == null || !limits) return null
  if (limits.lsl == null || limits.usl == null) return null
  const v = Number(value)
  return v >= limits.lsl && v <= limits.usl
}

/** 指数差值展示：带符号、一位小数 */
export function fmtDelta(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  const d = Number(value)
  return (d >= 0 ? '+' : '') + d.toFixed(1)
}
