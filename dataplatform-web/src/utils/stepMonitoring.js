import { twinLayoutsRef } from '../config/twinLayout'
import { buildDataPointLookup } from './dataPoints'
import { inSpec } from './format'

function mergeKeyFields() {
  const merged = {}
  for (const layout of Object.values(twinLayoutsRef.value)) {
    Object.assign(merged, layout.keyFields || {})
  }
  return merged
}

function fieldLabelsMap() {
  return Object.values(twinLayoutsRef.value).reduce(
    (acc, l) => ({ ...acc, ...(l.fieldLabels || {}) }),
    { location: '库位', status: '状态' }
  )
}

/** 登记产线设计基准（用于无遥测时的工序卡片展示） */
const REGISTRY_BENCHMARKS = {
  line_speed_m_per_min: { name: '线速度', unit: 'm/min', target: 52 },
  mixing_rpm: { name: '搅拌转速', unit: 'rpm', target: 30 },
  fill_ratio_pct: { name: '填充率', unit: '%', target: 92 },
  tension_kN: { name: '张力', unit: 'kN', target: 5.2 },
  coating_thickness_top_um: { name: '镀层厚度', unit: 'μm', target: 18 },
  coating_thickness_mm: { name: '药皮厚度', unit: 'mm', target: 1.2 },
  motor_rpm: { name: '转速', unit: 'rpm', target: 1200 },
  spool_count: { name: '包装盘数', unit: '', target: 48 },
  package_count: { name: '包装数', unit: '', target: 120 },
  actual_temp_C: { name: '烘干温度', unit: '°C', target: 185 },
  length_mm: { name: '切丝长度', unit: 'mm', target: 350 },
  location: { name: '库位', unit: '', target: 'WH-登记' }
}

export function equipmentForStep(equipment, stepId) {
  return (equipment || []).find(eq => eq.process_step_id === stepId) || null
}

function formatParam(fieldId, value, dpMap, equipmentId) {
  const dp = dpMap[`${equipmentId}:${fieldId}`]
  const limits = dp?.spec_limits
  const labels = fieldLabelsMap()
  return {
    fieldId,
    name: dp?.display_name || labels[fieldId] || fieldId,
    value,
    unit: dp?.unit || '',
    spec: inSpec(value, limits),
    live: true
  }
}

/** 提取工序卡片上的设备名与关键运行参数（实时遥测） */
export function stepMonitorSnapshot(stepId, equipment, dataPoints, maxParams = 2) {
  const eq = equipmentForStep(equipment, stepId)
  const dpMap = buildDataPointLookup(dataPoints)
  const values = eq?.latest?.values || {}
  const params = []
  const used = new Set()

  const preferred = mergeKeyFields()[stepId]
  if (preferred && values[preferred] != null && preferred !== 'status') {
    params.push(formatParam(preferred, values[preferred], dpMap, eq.equipment_id))
    used.add(preferred)
  }

  const numericEntries = Object.entries(values)
    .filter(([k, v]) => k !== 'status' && k !== 'run_mode' && !used.has(k) && v != null && v !== '')
    .sort((a, b) => {
      const priA = a[0].includes('ratio') || a[0].includes('thickness') ? 0 : 1
      const priB = b[0].includes('ratio') || b[0].includes('thickness') ? 0 : 1
      return priA - priB
    })

  for (const [fieldId, value] of numericEntries) {
    if (params.length >= maxParams) break
    params.push(formatParam(fieldId, value, dpMap, eq.equipment_id))
  }

  return {
    equipmentId: eq?.equipment_id || null,
    equipmentName: eq?.name || null,
    equipmentStatus: values.status || null,
    params,
    registry: false
  }
}

function shiftLabel(pattern) {
  if (!Array.isArray(pattern) || !pattern.length) return '—'
  const map = { day: '白班', night: '夜班' }
  return pattern.map(s => map[s] || s).join('+')
}

/** 登记产线：从孪生布局 + 产线主数据生成可读的监控快照 */
export function registryStepMonitorSnapshot(stepId, line, stepIndex, maxParams = 2) {
  const layout = twinLayoutsRef.value[line?.product_line_id]
  const node = layout?.nodes?.[stepId]
  const keyField = layout?.keyFields?.[stepId]
  const params = []
  const bench = keyField ? REGISTRY_BENCHMARKS[keyField] : null

  if (bench) {
    params.push({
      fieldId: keyField,
      name: bench.name,
      value: bench.target,
      unit: bench.unit,
      spec: true,
      registry: true
    })
  }

  if (stepIndex === 0 && line?.design_capacity_t_per_day != null) {
    params.push({
      fieldId: 'design_capacity_t_per_day',
      name: '设计产能',
      value: line.design_capacity_t_per_day,
      unit: 't/d',
      spec: true,
      registry: true
    })
  } else if (stepIndex === 1 && line?.planned_shift_pattern) {
    params.push({
      fieldId: 'planned_shift_pattern',
      name: '计划班次',
      value: shiftLabel(line.planned_shift_pattern),
      unit: '',
      spec: true,
      registry: true
    })
  }

  const statusMap = {
    active: 'PLANNED',
    maintenance: 'MAINTENANCE',
    inactive: 'STOPPED'
  }

  return {
    equipmentId: null,
    equipmentName: node?.label || stepId,
    equipmentStatus: statusMap[line?.status] || 'PLANNED',
    params: params.slice(0, maxParams),
    registry: true
  }
}

export function enrichPipelineSteps(pipeline, equipment, dataPoints, maxParams = 2, line = null) {
  const isSim = line?.simulation_enabled === true
  return (pipeline || []).map((step, index) => {
    const live = stepMonitorSnapshot(step.process_step_id, equipment, dataPoints, maxParams)
    if (live.equipmentId || live.params.length) {
      return { ...step, monitor: live }
    }
    if (isSim) {
      return { ...step, monitor: live }
    }
    if (line) {
      return { ...step, monitor: registryStepMonitorSnapshot(step.process_step_id, line, index, maxParams) }
    }
    return { ...step, monitor: live }
  })
}

export function formatParamValue(param) {
  if (param.value == null) return '—'
  if (typeof param.value === 'number') {
    const n = Math.abs(param.value) >= 100 ? param.value.toFixed(0) : param.value.toFixed(1)
    return param.unit ? `${n} ${param.unit}` : String(n)
  }
  return String(param.value)
}
