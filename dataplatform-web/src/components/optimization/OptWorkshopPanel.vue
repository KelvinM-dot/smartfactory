<script setup>
import { fmtPct } from '../../utils/format'

defineProps({
  workshop: { type: Object, required: true }
})

function workshopShort(id) {
  if (id === 'WS-WIRE-01') return '焊丝车间'
  if (id === 'WS-ROD-01') return '焊条车间'
  return id
}
</script>

<template>
  <div class="ws-panel">
    <div class="ws-head">
      <div>
        <div class="ws-name">{{ workshop.workshop_name || workshopShort(workshop.workshop_id) }}</div>
        <div class="ws-id">{{ workshop.workshop_id }}</div>
      </div>
      <div class="ws-stat">
        <span class="num">{{ workshop.line_count }}</span>
        <span class="lbl">产线</span>
      </div>
    </div>
    <div class="ws-metrics">
      <div class="metric">
        <span class="m-val">{{ workshop.active_lines ?? '—' }}</span>
        <span class="m-lbl">运行中</span>
      </div>
      <div class="metric">
        <span class="m-val">{{ workshop.simulation_lines ?? workshop.telemetry_lines ?? 0 }}</span>
        <span class="m-lbl">仿真线</span>
      </div>
      <div class="metric">
        <span class="m-val">{{ fmtPct(workshop.avg_oee_pct) }}%</span>
        <span class="m-lbl">均 OEE</span>
      </div>
      <div class="metric">
        <span class="m-val">{{ workshop.design_capacity_t_per_year ? (workshop.design_capacity_t_per_year / 10000).toFixed(1) + '万t' : '—' }}</span>
        <span class="m-lbl">设计产能</span>
      </div>
    </div>
    <div v-if="workshop.bottleneck_step" class="ws-bottleneck">
      主约束：<strong>{{ workshop.bottleneck_step }}</strong>
    </div>
    <div v-if="workshop.consumption_kwh" class="ws-energy">
      能耗窗口 {{ workshop.consumption_kwh }} kWh
    </div>
  </div>
</template>

<style scoped>
.ws-panel {
  background: var(--bg-panel);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  padding: 12px 14px;
}
.ws-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.ws-name { font-size: 14px; font-weight: 600; }
.ws-id { font-size: 10px; font-family: var(--font-mono); color: var(--text-dim); }
.ws-stat { text-align: right; }
.ws-stat .num { font-size: 22px; font-weight: 700; font-family: var(--font-mono); color: var(--accent-cyan); }
.ws-stat .lbl { display: block; font-size: 10px; color: var(--text-dim); }
.ws-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.metric { text-align: center; padding: 6px; background: var(--bg-panel-elevated); border-radius: 3px; }
.m-val { display: block; font-family: var(--font-mono); font-size: 13px; font-weight: 600; }
.m-lbl { font-size: 10px; color: var(--text-dim); }
.ws-bottleneck, .ws-energy { margin-top: 8px; font-size: 11px; color: var(--text-secondary); }
.ws-bottleneck strong { color: var(--accent-amber); }
</style>
