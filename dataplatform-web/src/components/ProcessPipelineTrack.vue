<script setup>
import { computed } from 'vue'
import { statusClass, statusLabel } from '../utils/format'
import { enrichPipelineSteps, formatParamValue } from '../utils/stepMonitoring'

const props = defineProps({
  pipeline: { type: Array, default: () => [] },
  equipment: { type: Array, default: () => [] },
  dataPoints: { type: Array, default: () => [] },
  line: { type: Object, default: null },
  compact: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false },
  maxParams: { type: Number, default: 2 }
})

defineEmits(['select-step'])

const steps = computed(() =>
  enrichPipelineSteps(
    props.pipeline,
    props.equipment,
    props.dataPoints,
    props.maxParams,
    props.line
  )
)

function paramClass(spec) {
  if (spec === true) return 'param-good'
  if (spec === false) return 'param-bad'
  return ''
}
</script>

<template>
  <div class="pipeline-track" :class="{ compact, monitor: true }">
    <div
      v-for="(step, i) in steps"
      :key="step.process_step_id"
      class="pipeline-step"
      :class="[statusClass(step.aggregate_status), { clickable }]"
      @click="clickable && $emit('select-step', step.process_step_id)"
    >
      <div class="node">
        <div class="name">{{ step.display_name }}</div>
        <div class="status-text">{{ statusLabel(step.aggregate_status) }}</div>
        <div v-if="step.monitor.equipmentName || step.monitor.equipmentId" class="step-eq">
          <span class="eq-name">{{ step.monitor.equipmentName || step.monitor.equipmentId }}</span>
          <span v-if="step.monitor.equipmentStatus" class="eq-status" :class="{ registry: step.monitor.registry }">
            {{ step.monitor.equipmentStatus }}
          </span>
        </div>
        <div v-if="step.monitor.params.length" class="step-params">
          <div
            v-for="p in step.monitor.params"
            :key="p.fieldId"
            class="step-param"
            :class="[paramClass(p.spec), { registry: p.registry }]"
          >
            <span class="param-name">{{ p.name }}</span>
            <span class="param-val">{{ formatParamValue(p) }}</span>
          </div>
        </div>
        <div v-else-if="step.monitor.equipmentName" class="step-params empty">暂无实时参数</div>
      </div>
      <div v-if="i < steps.length - 1" class="pipeline-connector"></div>
    </div>
  </div>
</template>
