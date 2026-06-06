<script setup>
import { computed } from 'vue'
import { fmtQtyT } from '../../utils/orderLabels'
import { releaseProgress } from '../../utils/orderTriage'

const props = defineProps({
  order: { type: Object, default: null },
  risk: { type: Object, default: null }
})

const merged = computed(() => ({ ...props.order, ...props.risk }))
const release = computed(() => releaseProgress(merged.value))
const windowHint = computed(() => {
  const released = Number(merged.value.released_quantity_t) || 0
  const planned = Number(merged.value.planned_quantity_t) || 0
  if (!planned) return '计划量未设定'
  if (released <= 0) return '尚未按班次/窗口下达释放量'
  if (release.value.pct >= 99) return '订单量已全部释放，进入全量执行'
  return `APS 分窗释放：已释放 ${release.value.pct.toFixed(0)}%，剩余 ${fmtQtyT(release.value.remaining)} 待后续班次下达`
})
</script>

<template>
  <div class="order-release-panel">
    <div class="order-release-bar">
      <div class="order-release-fill" :style="{ width: `${release.pct}%` }" />
    </div>
    <div class="order-release-legend">
      <span>已释放 {{ fmtQtyT(release.released) }}</span>
      <span>计划 {{ fmtQtyT(release.planned) }}</span>
      <span>待释放 {{ fmtQtyT(release.remaining) }}</span>
    </div>
    <div class="summary-line">{{ windowHint }}</div>
  </div>
</template>

<style scoped>
.summary-line { font-size: 12px; color: var(--text-secondary); }
</style>
