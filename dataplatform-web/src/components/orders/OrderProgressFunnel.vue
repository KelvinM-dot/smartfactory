<script setup>
import { computed } from 'vue'
import { fmtQtyT } from '../../utils/orderLabels'
import { formatProgressPct, progressSegments } from '../../utils/orderTriage'

const props = defineProps({
  order: { type: Object, required: true },
  compact: { type: Boolean, default: false }
})

const data = computed(() => progressSegments(props.order))
const pctLabel = computed(() => formatProgressPct(props.order?.progress_pct))
</script>

<template>
  <div class="order-progress-funnel" :class="{ compact }">
    <div class="order-progress-bar">
      <div
        v-for="seg in data.segments"
        :key="seg.key"
        class="order-progress-seg"
        :class="seg.tone"
        :style="{ width: `${(seg.value / data.planned) * 100}%` }"
        :title="`${seg.label} ${seg.value.toFixed(1)} t`"
      />
    </div>
    <div class="order-progress-meta">
      <span>{{ fmtQtyT(order.completed_quantity_t) }} / {{ fmtQtyT(order.planned_quantity_t) }}</span>
      <span>{{ pctLabel }}</span>
    </div>
  </div>
</template>
