<script setup>
import { useRouter } from 'vue-router'
import { fmtPct } from '../../utils/format'
import { lineStatusBadge } from '../../utils/lineCatalog'

const props = defineProps({
  lines: { type: Array, default: () => [] },
  showWorkshop: { type: Boolean, default: true }
})

const router = useRouter()

function goLine(lineId) {
  router.push({ path: `/lines/${lineId}`, query: { tab: 'overview' } })
}

function confBadge(line) {
  if (line.data_confidence === 'telemetry' || line.simulation_enabled) return '实时仿真'
  return '估算'
}

function confClass(line) {
  if (line.data_confidence === 'telemetry' || line.simulation_enabled) return 'telemetry'
  return 'registry_estimate'
}
</script>

<template>
  <div class="line-grid">
    <div
      v-for="line in lines"
      :key="line.product_line_id"
      class="line-card"
      :class="{
        telemetry: line.simulation_enabled || line.data_confidence === 'telemetry',
        maintenance: line.status === 'maintenance',
        inactive: line.status === 'inactive'
      }"
      @click="goLine(line.product_line_id)"
    >
      <div class="lc-head">
        <span class="lc-id">{{ line.product_line_id?.replace('-LINE-', '#') }}</span>
        <span class="lc-badge" :class="confClass(line)">{{ confBadge(line) }}</span>
      </div>
      <div class="lc-name">{{ line.line_name }}</div>
      <div class="lc-metrics">
        <span>OEE {{ fmtPct(line.oee_pct) }}%</span>
        <span v-if="line.utilization_pct != null">利用 {{ fmtPct(line.utilization_pct) }}%</span>
        <span v-if="line.pending_alarms > 0" class="alarm">⚠ {{ line.pending_alarms }}</span>
      </div>
      <div v-if="line.bottleneck_step" class="lc-bn">约束 {{ line.bottleneck_step }}</div>
      <div class="lc-foot">
        <span v-if="showWorkshop && line.workshop_id" class="lc-ws">
          {{ line.workshop_id === 'WS-WIRE-01' ? '焊丝' : line.workshop_id === 'WS-ROD-01' ? '焊条' : line.workshop_id }}
        </span>
        <span v-if="line.status && line.status !== 'active'" class="lc-status" :class="lineStatusBadge(line).cls">
          {{ lineStatusBadge(line).label }}
        </span>
      </div>
    </div>
    <div v-if="!lines.length" class="empty">暂无产线数据</div>
  </div>
</template>

<style scoped>
.line-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(148px, 1fr));
  gap: 8px;
}
.line-card {
  padding: 8px 10px;
  background: var(--bg-panel-elevated);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  cursor: pointer;
  transition: border-color 0.15s;
}
.line-card:hover { border-color: var(--accent-cyan); }
.line-card.telemetry { border-top: 2px solid var(--accent-cyan); }
.line-card.maintenance { border-top-color: var(--accent-amber); }
.line-card.inactive { opacity: 0.55; }
.lc-head { display: flex; justify-content: space-between; align-items: center; }
.lc-id { font-family: var(--font-mono); font-size: 11px; font-weight: 600; }
.lc-badge { font-size: 9px; padding: 1px 4px; border-radius: 2px; background: var(--bg-root); color: var(--text-dim); }
.lc-badge.telemetry { color: var(--accent-cyan); background: rgba(34, 211, 238, 0.08); }
.lc-name { font-size: 10px; color: var(--text-secondary); margin: 4px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.lc-metrics { font-size: 10px; font-family: var(--font-mono); color: var(--text-dim); display: flex; flex-wrap: wrap; gap: 6px; }
.lc-metrics .alarm { color: var(--status-alarm); }
.lc-bn { font-size: 9px; color: var(--accent-amber); margin-top: 4px; }
.lc-foot { display: flex; justify-content: space-between; margin-top: 4px; }
.lc-ws { font-size: 9px; color: var(--text-dim); }
.lc-status { font-size: 9px; padding: 1px 4px; border-radius: 2px; }
.lc-status.manual { color: var(--accent-amber); }
.lc-status.stopped { color: var(--text-dim); }
.empty { grid-column: 1 / -1; text-align: center; color: var(--text-dim); padding: 20px; font-size: 12px; }
</style>
