<script setup>
import { computed } from 'vue'
import { fmtDateTime } from '../../utils/format'
import {
  fmtDaysToDue,
  orderBadgeTone,
  orderRiskLabel,
  orderStatusLabel,
  orderTypeLabel,
  productCategoryLabel
} from '../../utils/orderLabels'
import { lineLoadHint } from '../../utils/orderTriage'
import { lineShortId } from '../../utils/lineCatalog'
import OrderProgressFunnel from './OrderProgressFunnel.vue'
import OrderBlockReason from './OrderBlockReason.vue'

const props = defineProps({
  order: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  allOrders: { type: Array, default: () => [] },
  lines: { type: Array, default: () => [] },
  showBlockReason: { type: Boolean, default: false }
})

defineEmits(['select', 'open'])

const rowClass = computed(() => ({
  selected: props.selected,
  'critical-row': ['high', 'critical'].includes(props.order.delivery_risk_level)
    || props.order.order_status === 'blocked'
}))

const loadHint = computed(() => lineLoadHint(props.order, props.allOrders, props.lines))

function badgeTone(status) {
  return orderBadgeTone(status)
}
</script>

<template>
  <tr
    class="click-row order-risk-row"
    :class="rowClass"
    @click="$emit('select', order.production_order_id)"
    @dblclick="$emit('open', order.production_order_id)"
  >
    <td>
      <div class="order-row-main">{{ order.customer_order_id || order.production_order_id }}</div>
      <div class="order-row-sub">{{ order.customer_segment || orderTypeLabel(order.order_type) }} · {{ order.grade }}</div>
      <div class="order-row-sub mono">{{ order.production_order_id }}</div>
      <div class="order-row-sub">
        {{ productCategoryLabel(order.product_category) }}
        · {{ (order.assigned_line_ids || []).map(lineShortId).join('、') || '—' }}
      </div>
      <div v-if="loadHint" class="order-row-hint">{{ loadHint }}</div>
    </td>
    <td>{{ orderTypeLabel(order.order_type) }}{{ order.is_export ? ' · 出口' : '' }}</td>
    <td>
      <span class="line-badge" :class="badgeTone(order.order_status)">{{ orderStatusLabel(order.order_status) }}</span>
    </td>
    <td>
      <span class="line-badge" :class="badgeTone(order.delivery_risk_level)">{{ orderRiskLabel(order.delivery_risk_level) }}</span>
    </td>
    <td><OrderProgressFunnel :order="order" compact /></td>
    <td>
      <div>{{ fmtDateTime(order.due_date) }}</div>
      <div class="order-row-sub">{{ fmtDaysToDue(order.days_to_due) }}</div>
    </td>
    <td v-if="showBlockReason">
      <OrderBlockReason :order="order" inline />
    </td>
  </tr>
</template>

<style scoped>
.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(248,113,113,0.15); color: var(--status-alarm, #f87171); }
.mono { font-family: var(--font-mono, monospace); font-size: 10px; }
</style>
