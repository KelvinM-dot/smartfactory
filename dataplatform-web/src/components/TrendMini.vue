<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { buildTrendOption, buildSparkOption } from '../utils/chartTheme'

const props = defineProps({
  series: { type: Object, default: null },
  title: { type: String, default: '' },
  compact: { type: Boolean, default: false },
  spark: { type: Boolean, default: false }
})

const el = ref(null)
let chart = null

function render() {
  if (!el.value || !props.series) return
  if (!chart) chart = echarts.init(el.value, null, { renderer: 'canvas' })
  const opt = props.spark
    ? buildSparkOption(props.series.points, '#22d3ee', props.series.spec_limits)
    : buildTrendOption([props.series])
  chart.setOption(opt, true)
}

watch(() => props.series, render, { deep: true })
onMounted(() => {
  render()
  window.addEventListener('resize', () => chart?.resize())
})
onUnmounted(() => chart?.dispose())
</script>

<template>
  <div class="panel" :class="{ compact: compact }">
    <div v-if="title" class="panel-head">
      <h3>{{ title }}</h3>
      <span v-if="series?.unit" class="meta">{{ series.field_id }} · {{ series.unit }}</span>
    </div>
    <div class="panel-body flush">
      <div ref="el" :class="spark ? 'chart-box-sm' : 'chart-box'"></div>
    </div>
  </div>
</template>
