import api from './index.js'

export function getOptOverview(factoryId) {
  return api.get('/v1/opt/overview', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOptLines(factoryId, workshopId) {
  return api.get('/v1/opt/lines', {
    params: { factory_id: factoryId, workshop_id: workshopId }
  }).then(r => r.data)
}

export function getOptWorkshops(factoryId) {
  return api.get('/v1/opt/workshops', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOptRecommendations(factoryId, limit = 12) {
  return api.get('/v1/opt/recommendations', {
    params: { factory_id: factoryId, limit }
  }).then(r => r.data)
}

export function getOptEfficiency(factoryId) {
  return api.get('/v1/opt/efficiency', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOptCost(factoryId) {
  return api.get('/v1/opt/cost', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOptQuality(factoryId) {
  return api.get('/v1/opt/quality', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOptScenarios() {
  return api.get('/v1/opt/scenarios').then(r => r.data)
}

export function getOptKpiSnapshot(factoryId) {
  return api.get('/v1/opt/kpi-snapshot', { params: { factory_id: factoryId } }).then(r => r.data)
}

export function getOptSimStatus() {
  return api.get('/v1/opt/sim/status').then(r => r.data)
}

export function applyOptScenario(scenarioId, factoryId) {
  return api.post('/v1/opt/sim/scenario', { scenario_id: scenarioId }, {
    params: { factory_id: factoryId }
  }).then(r => r.data)
}

/** 直连模拟器（vite 代理 /sim → :3002），API 代理失败时回退 */
export function applySimScenarioDirect(scenarioId, extra = {}) {
  return api.post('/sim/scenario', { scenario_id: scenarioId, ...extra }).then(r => r.data)
}

export function compareOptSnapshots(baseline, after) {
  return api.post('/v1/opt/scenario/compare', { baseline, after }).then(r => r.data)
}

export function analyzeOptBalance(params, factoryId) {
  return api.post('/v1/opt/balance', params || {}, {
    params: { factory_id: factoryId }
  }).then(r => r.data)
}

export function getOptEnergyCarbon(factoryId) {
  return api.get('/v1/opt/energy-carbon/overview', {
    params: { factory_id: factoryId }
  }).then(r => r.data)
}

export function projectOptGreenShift(greenShiftPct, factoryId) {
  return api.get('/v1/opt/energy-carbon/green-shift', {
    params: { factory_id: factoryId, green_shift_pct: greenShiftPct }
  }).then(r => r.data)
}

export function getOptEnergyCarbonBatches(factoryId, limit = 30) {
  return api.get('/v1/opt/energy-carbon/batches', {
    params: { factory_id: factoryId, limit }
  }).then(r => r.data)
}

export function applyEnergyCarbonScenario(body, factoryId) {
  return api.post('/v1/opt/energy-carbon/apply-scenario', body || {}, {
    params: { factory_id: factoryId }
  }).then(r => r.data)
}

export function applyOptScenarioWithParams(scenarioId, params, factoryId) {
  return api.post('/v1/opt/sim/scenario', {
    scenario_id: scenarioId,
    ...params
  }, { params: { factory_id: factoryId } }).then(r => r.data)
}
