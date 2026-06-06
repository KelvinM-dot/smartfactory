<script setup>
import { computed } from 'vue'
import { orderBadgeTone, orderRiskLabel, orderStatusLabel } from '../../utils/orderLabels'
import { pickMustHandleOrders } from '../../utils/orderTriage'
import OrderBlockReason from './OrderBlockReason.vue'

const props = defineProps({
  orders: { type: Array, default: () => [] },
  limit: { type: Number, default: 5 },
  showProgress: { type: Boolean, default: true }
})

const emit = defineEmits(['select'])

const items = computed(() => pickMustHandleOrders(props.orders || [], props.limit))

function riskBadge(level) {
  return orderBadgeTone(level)
}
</script>

<template>
  <div class="order-must-handle-list">
    <div v-if="items.length" class="compact-table">
      <div class="compact-row head risk-row">
        <span>订单</span>
        <span v-if="showProgress">进度</span>
        <span>风险</span>
        <span>阻塞原因</span>
      </div>
      <div
        v-for="item in items"
        :key="item.production_order_id"
        class="compact-row risk-row clickable"
        @click="emit('select', item.production_order_id)"
      >
        <span class="mono">{{ item.production_order_id }}</span>
        <span v-if="showProgress">{{ item.completed_quantity_t ?? 0 }} / {{ item.planned_quantity_t }} t</span>
        <span>
          <span class="line-badge" :class="riskBadge(item.delivery_risk_level)">{{ orderRiskLabel(item.delivery_risk_level) }}</span>
          <small class="dim" v-if="item.order_status"> · {{ orderStatusLabel(item.order_status) }}</small>
        </span>
        <span><OrderBlockReason :order="item" inline /></span>
      </div>
    </div>
    <div v-else class="empty-state">暂无高风险或受阻订单</div>
  </div>
</template>

<style scoped>
.compact-row.risk-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.9fr 1.4fr;
  gap: 8px;
  padding: 6px 0;
  font-size: 12px;
  border-bottom: 1px solid var(--border-dim);
}
.compact-row.head { color: var(--text-secondary); font-size: 11px; }
.compact-row.clickable { cursor: pointer; }
.compact-row.clickable:hover { background: rgba(34, 211, 238, 0.05); }
.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(248,113,113,0.15); color: var(--status-alarm); }
.mono { font-family: var(--font-mono, monospace); font-size: 11px; }
.dim { color: var(--text-secondary); }
</style>
