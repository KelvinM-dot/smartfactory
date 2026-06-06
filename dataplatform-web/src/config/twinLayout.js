/**
 * 产线数字孪生布局（2D SVG + 3D Three.js）
 * 权威数据源：jqhc-factory-master-data.json → GET /v1/meta/twin-layouts
 * FALLBACK 由 scripts/generate-twin-fallback.mjs 从 master-data 生成。
 */

import { shallowRef } from 'vue'
import { FALLBACK_TWIN_LAYOUTS, FALLBACK_TWIN_3D_READY } from './twinFallback.generated.js'

export { FALLBACK_TWIN_LAYOUTS }

/** 响应式布局表，API 加载后自动更新 */
export const twinLayoutsRef = shallowRef({ ...FALLBACK_TWIN_LAYOUTS })

/** 产线 3D 就绪标记（来自 master-data twin_3d_ready） */
export const twin3dReadyRef = shallowRef({ ...FALLBACK_TWIN_3D_READY })

let loadPromise = null

function extractTwin3dReady(apiLayouts) {
  const ready = {}
  for (const [lineId, layout] of Object.entries(apiLayouts || {})) {
    if (layout?.twin3dReady === true) ready[lineId] = true
  }
  return ready
}

export function applyTwinLayoutsFromApi(apiLayouts) {
  if (!apiLayouts || typeof apiLayouts !== 'object' || !Object.keys(apiLayouts).length) return
  twinLayoutsRef.value = { ...FALLBACK_TWIN_LAYOUTS, ...apiLayouts }
  const ready = extractTwin3dReady(apiLayouts)
  twin3dReadyRef.value = {
    ...FALLBACK_TWIN_3D_READY,
    ...ready
  }
}

export async function ensureTwinLayoutsLoaded(fetcher) {
  if (!loadPromise) {
    loadPromise = (async () => {
      try {
        const data = await fetcher()
        applyTwinLayoutsFromApi(data)
      } catch {
        /* 保留 FALLBACK */
      }
    })()
  }
  return loadPromise
}

export function getTwinLayout(lineId) {
  return twinLayoutsRef.value[lineId] || null
}

export function supportsTwinVisual(lineId) {
  return lineId in twinLayoutsRef.value
}

/** 3D 场景已验收产线；仅 master-data twin_3d_ready 标记为 true 时启用 */
export function supportsTwin3D(lineId) {
  return twin3dReadyRef.value[lineId] === true
}

/** 根据节点序列生成 SVG 折线路径 d */
export function buildFlowPathD(nodes, flowPath) {
  const pts = flowPath.map(id => nodes[id]).filter(Boolean)
  if (pts.length < 2) return ''
  let d = `M ${pts[0].x} ${pts[0].y}`
  for (let i = 1; i < pts.length; i++) {
    const prev = pts[i - 1]
    const cur = pts[i]
    if (Math.abs(cur.y - prev.y) > 50) {
      const midY = (prev.y + cur.y) / 2
      d += ` L ${prev.x} ${midY} L ${cur.x} ${midY} L ${cur.x} ${cur.y}`
    } else {
      d += ` L ${cur.x} ${cur.y}`
    }
  }
  return d
}

export function statusColor(status) {
  const map = {
    running: '#34d399',
    alarm: '#f87171',
    manual: '#fbbf24',
    stopped: '#64748b',
    unknown: '#475569'
  }
  return map[status] || map.unknown
}

export function equipStatusRaw(values) {
  if (!values) return 'UNKNOWN'
  return values.status || values.run_mode || 'UNKNOWN'
}

export function mapAggregateToTwin(aggregate) {
  const m = { running: 'running', alarm: 'alarm', manual: 'manual', stopped: 'stopped' }
  return m[aggregate] || 'unknown'
}
