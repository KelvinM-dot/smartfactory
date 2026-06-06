import { computed, toValue } from 'vue'
import { mapAggregateToTwin } from '../config/twinLayout'

/** 孪生 2D/3D 共用的产线管道状态逻辑 */
export function useTwinPipeline(pipelineRef, equipmentRef, keyFieldsRef, fieldLabelsRef) {
  const pipelineMap = computed(() =>
    Object.fromEntries((toValue(pipelineRef) || []).map(s => [s.process_step_id, s]))
  )

  const equipByStep = computed(() => {
    const m = {}
    for (const eq of toValue(equipmentRef) || []) {
      if (!m[eq.process_step_id]) m[eq.process_step_id] = []
      m[eq.process_step_id].push(eq)
    }
    return m
  })

  const equipByStepFirst = computed(() => {
    const m = {}
    for (const eq of toValue(equipmentRef) || []) {
      if (!m[eq.process_step_id]) m[eq.process_step_id] = eq
    }
    return m
  })

  const lineRunning = computed(() =>
    (toValue(pipelineRef) || []).some(s => s.aggregate_status === 'running')
  )

  const hasAlarm = computed(() =>
    (toValue(pipelineRef) || []).some(s => s.aggregate_status === 'alarm')
  )

  function stepStatus(stepId) {
    const p = pipelineMap.value[stepId]
    return p ? mapAggregateToTwin(p.aggregate_status) : 'unknown'
  }

  function primaryEquip(stepId) {
    return (equipByStep.value[stepId] || [])[0]
  }

  function liveMetricText(stepId) {
    const eq = equipByStepFirst.value[stepId] || primaryEquip(stepId)
    const keyFields = toValue(keyFieldsRef) || {}
    const fieldLabels = toValue(fieldLabelsRef) || {}
    const field = keyFields[stepId]
    const val = eq?.latest?.values?.[field]
    if (val == null) return ''
    const label = fieldLabels[field] || field
    if (field === 'status') return `${label}: ${val}`
    return typeof val === 'number' ? `${label}: ${val.toFixed(1)}` : `${label}: ${val}`
  }

  return {
    pipelineMap,
    equipByStep,
    equipByStepFirst,
    lineRunning,
    hasAlarm,
    stepStatus,
    primaryEquip,
    liveMetricText
  }
}
