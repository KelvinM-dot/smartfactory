<script setup>
import { computed } from 'vue'
import { computeTriageCounts } from '../../utils/orderTriage'

const props = defineProps({
  orders: { type: Array, default: () => [] },
  contextLabel: { type: String, default: '全厂' }
})

const counts = computed(() => computeTriageCounts(props.orders))
const mainCount = computed(() => (counts.value.mustHandle > 0 ? counts.value.mustHandle : counts.value.active))
const mainLabel = computed(() => (counts.value.mustHandle > 0 ? '今日需关注' : '在制订单'))
</script>

<template>
  <div class="order-triage-strip">
    <div class="panel">
      <div class="panel-body compact summary-card order-triage-main">
        <div class="summary-label">{{ contextLabel }} · {{ mainLabel }}</div>
        <div class="summary-value">{{ mainCount }} 单</div>
        <div v-if="counts.mustHandle > 0 && counts.active > counts.mustHandle" class="summary-hint">
          在制共 {{ counts.active }} 单
        </div>
      </div>
    </div>
    <div class="panel" v-for="card in [
      { label: '受阻', value: counts.blocked, tone: 'stopped' },
      { label: '高风险', value: counts.highRisk, tone: 'stopped' },
      { label: '7日内交期', value: counts.dueSoon, tone: 'manual' },
      { label: '待发运', value: counts.readyShip, tone: 'manual' }
    ]" :key="card.label">
      <div class="panel-body compact summary-card order-triage-sub" :class="card.tone">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }} 单</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.summary-card { min-height: 72px; display: flex; flex-direction: column; justify-content: center; }
.summary-label { font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
.summary-value { font-size: 22px; font-weight: 600; color: var(--accent-cyan); }
.summary-card.stopped .summary-value { color: var(--status-stopped); }
.summary-card.manual .summary-value { color: var(--status-warning); }
.summary-hint { font-size: 11px; color: var(--text-secondary); margin-top: 4px; }
</style>
