<script setup>
import { fmtDateTime } from '../../utils/format'
import {
  orderBadgeTone,
  orderStatusLabel,
  qualityDecisionLabel,
  timelineEventLabel
} from '../../utils/orderLabels'

defineProps({
  events: { type: Array, default: () => [] }
})

defineEmits(['batch'])

function badgeTone(status) {
  return orderBadgeTone(status)
}

function statusLabel(ev) {
  return orderStatusLabel(ev.status) || qualityDecisionLabel(ev.status) || ev.status || '—'
}
</script>

<template>
  <div class="order-timeline-axis">
    <div v-for="ev in events" :key="`${ev.event_type}-${ev.timestamp}-${ev.batch_id}`" class="order-timeline-item">
      <div class="order-timeline-time">{{ fmtDateTime(ev.timestamp) }}</div>
      <div class="order-timeline-track"><div class="order-timeline-dot" /></div>
      <div class="order-timeline-body">
        <div class="order-timeline-title">
          {{ timelineEventLabel(ev.event_type) }}
          <span class="line-badge" :class="badgeTone(ev.status)">{{ statusLabel(ev) }}</span>
        </div>
        <div v-if="ev.batch_id">
          批次：
          <button class="link-btn" @click="$emit('batch', ev.batch_id)">{{ ev.batch_id }}</button>
        </div>
        <div v-if="ev.message" class="dim">{{ ev.message }}</div>
      </div>
    </div>
    <div v-if="!events.length" class="empty-state">暂无订单时间线</div>
  </div>
</template>

<style scoped>
.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; margin-left: 6px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(100,116,139,0.2); color: var(--status-stopped); }
.link-btn { background: none; border: none; color: var(--accent-cyan); cursor: pointer; padding: 0; font: inherit; }
.dim { color: var(--text-secondary); font-size: 11px; margin-top: 4px; }
</style>
