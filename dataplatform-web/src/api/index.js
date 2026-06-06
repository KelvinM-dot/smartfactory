import axios from 'axios'

const api = axios.create({ baseURL: '' })

export const LINE_ID = 'FCW-LINE-07'
export const BATCH_ID = 'FCW-20250605-B2'

export const CRITICAL_FIELDS = [
  'fill_ratio_pct',
  'forming_pressure_MPa',
  'coating_thickness_top_um',
  'coating_thickness_bottom_um',
  'tension_kN',
  'line_speed_m_per_min',
  'mixing_rpm',
  'motor_rpm'
]

export function getFactory() {
  return api.get('/v1/meta/factory').then(r => r.data)
}

export function getFactoryEnergy(factoryId) {
  return api.get('/v1/factory/energy', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getFactoryKpis(factoryId) {
  return api.get('/v1/factory/kpis', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOrderRisk(factoryId) {
  return api.get('/v1/orders/risk', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOrderSummary(factoryId) {
  return api.get('/v1/orders/summary', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOrders(factoryId, status, riskLevel) {
  return api.get('/v1/orders', { params: { factory_id: factoryId, status, risk_level: riskLevel } }).then(r => r.data)
}

export function getOrderDetail(orderId) {
  return api.get(`/v1/orders/${orderId}`).then(r => r.data)
}

export function getOrderTimeline(orderId) {
  return api.get(`/v1/orders/${orderId}/timeline`).then(r => r.data)
}

export function createOrder(payload) {
  return api.post('/v1/meta/orders', payload).then(r => r.data)
}

export function getLogisticsTasks(factoryId, status) {
  return api.get('/v1/logistics/tasks', { params: { factory_id: factoryId, status } }).then(r => r.data)
}

export function getQualityGates(factoryId, batchId, decision) {
  return api.get('/v1/quality/gates', {
    params: { factory_id: factoryId, batch_id: batchId, decision }
  }).then(r => r.data)
}

export function getOverview(lineId) {
  return api.get(`/v1/state/lines/${lineId}/overview`).then(r => r.data)
}

export function getTrends(lineId, fieldIds, range = 'buffer', batchId) {
  return api.get('/v1/trends', {
    params: { line_id: lineId, field_ids: fieldIds.join(','), range, batch_id: batchId }
  }).then(r => r.data)
}

export function getAlarms(lineId, status) {
  return api.get('/v1/alarms', {
    params: { line_id: lineId || undefined, status }
  }).then(r => r.data)
}

/** 全厂报警（不传 line_id） */
export function getPlantAlarms(status) {
  return getAlarms(null, status)
}

export function acknowledgeAlarm(alarmId, payload) {
  return api.post(`/v1/alarms/${alarmId}/acknowledge`, payload).then(r => r.data)
}

export function resolveAlarm(alarmId, payload) {
  return api.post(`/v1/alarms/${alarmId}/resolve`, payload).then(r => r.data)
}

/** 全厂报警聚合（按产线并行拉取） */
export async function getAllAlarms(lineIds, status) {
  const lists = await Promise.all(
    (lineIds || []).map(async id => {
      const list = await getAlarms(id, status).catch(() => [])
      return list.map(a => ({ ...a, product_line_id: a.product_line_id || id }))
    })
  )
  return lists.flat()
}

export function getBatchTimeline(batchId) {
  return api.get(`/v1/compute/batches/${batchId}/timeline`).then(r => r.data)
}

export function getMaterialEvents(lineId, batchId, limit = 50) {
  return api.get('/v1/material-events', {
    params: {
      line_id: lineId || undefined,
      batch_id: batchId || undefined,
      limit
    }
  }).then(r => r.data)
}

/** 全厂最近物料事件 */
export async function getRecentMaterialEvents(limit = 30) {
  try {
    const events = await getMaterialEvents(null, null, limit)
    if (events.length || !limit) return events
  } catch {
    /* 旧版 API 要求 line_id，回退按产线聚合 */
  }
  const lines = await getLines().catch(() => [])
  const lists = await Promise.all(
    lines.map(l => getMaterialEvents(l.product_line_id, null, limit).catch(() => []))
  )
  return lists.flat()
    .sort((a, b) => String(b.timestamp).localeCompare(String(a.timestamp)))
    .slice(0, limit)
}

export function getBatches(lineId) {
  return api.get('/v1/meta/batches', {
    params: lineId ? { line_id: lineId } : {}
  }).then(r => r.data)
}

export function getLines() {
  return api.get('/v1/meta/lines').then(r => r.data)
}

export function getProducts() {
  return api.get('/v1/meta/products').then(r => r.data)
}

export function getInventory(lineId) {
  return api.get('/v1/meta/inventory', { params: lineId ? { line_id: lineId } : {} }).then(r => r.data)
}

export function getRecipes(lineId) {
  return api.get('/v1/meta/recipes', { params: lineId ? { line_id: lineId } : {} }).then(r => r.data)
}

export function getEquipment(lineId) {
  return api.get(`/v1/meta/lines/${lineId}/equipment`).then(r => r.data)
}

export function getDataPoints(lineId) {
  return api.get('/v1/meta/datapoints', { params: { line_id: lineId } }).then(r => r.data)
}

export function getTwinLayouts() {
  return api.get('/v1/meta/twin-layouts').then(r => r.data)
}

export function getStepDetail(lineId, stepId) {
  return api.get(`/v1/state/lines/${lineId}/steps/${stepId}`).then(r => r.data)
}

export function getEquipmentLatest(equipmentId) {
  return api.get(`/v1/state/equipment/${equipmentId}/latest`).then(r => r.data)
}

export function getOee(lineId) {
  return api.get('/v1/compute/oee', { params: { line_id: lineId } }).then(r => r.data)
}

/** 并行拉取全部设备最新值 */
export async function getAllEquipmentLatest(lineId) {
  const equipment = await getEquipment(lineId)
  const results = await Promise.all(
    equipment.map(eq => getEquipmentLatest(eq.equipment_id).catch(() => null))
  )
  return equipment.map((eq, i) => ({
    ...eq,
    latest: results[i]
  }))
}

/** 工厂驾驶舱：服务端聚合（单次请求替代 120+ 次 HTTP） */
export async function getFactoryDashboard(factoryId) {
  const data = await api.get('/v1/factory/dashboard', {
    params: factoryId ? { factory_id: factoryId } : undefined
  }).then(r => r.data)
  return {
    factory: data.factory,
    lines: (data.lines || []).map(card => ({
      line: card.line,
      overview: card.overview,
      equipment: card.equipment,
      dataPoints: card.data_points || card.dataPoints || [],
      error: card.error
    })),
    energy: data.energy,
    logisticsTasks: data.logistics_tasks || data.logisticsTasks || [],
    qualityGates: data.quality_gates || data.qualityGates || [],
    kpis: data.kpis,
    orderRisk: data.order_risk || data.orderRisk || []
  }
}

export default api
