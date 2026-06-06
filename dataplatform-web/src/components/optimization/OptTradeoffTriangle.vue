<script setup>
import { computed } from 'vue'

const props = defineProps({
  current: { type: Object, default: null },
  optimal: { type: Object, default: null },
  width: { type: Number, default: 320 },
  height: { type: Number, default: 280 }
})

const pad = 36
const W = computed(() => props.width)
const H = computed(() => props.height)

/** 顶点：效率(上)、成本(左下)、质量(右下) */
const vertices = computed(() => ({
  efficiency: { x: W.value / 2, y: pad },
  cost: { x: pad, y: H.value - pad },
  quality: { x: W.value - pad, y: H.value - pad }
}))

function barycentricToPixel(coords) {
  if (!coords) return null
  let e = coords.efficiency_norm ?? 0.33
  let c = coords.cost_norm ?? 0.33
  let q = coords.quality_norm ?? 0.34
  const sum = e + c + q
  if (sum > 0) { e /= sum; c /= sum; q /= sum }
  const v = vertices.value
  return {
    x: e * v.efficiency.x + c * v.cost.x + q * v.quality.x,
    y: e * v.efficiency.y + c * v.cost.y + q * v.quality.y
  }
}

const currentPt = computed(() => barycentricToPixel(props.current?.triangle_coords))
const optimalPt = computed(() => barycentricToPixel(props.optimal?.triangle_coords))

const trianglePath = computed(() => {
  const v = vertices.value
  return `M ${v.efficiency.x} ${v.efficiency.y} L ${v.cost.x} ${v.cost.y} L ${v.quality.x} ${v.quality.y} Z`
})
</script>

<template>
  <div class="triangle-wrap">
    <svg :width="W" :height="H" class="triangle-svg">
      <path :d="trianglePath" class="tri-bg" />
      <line
        :x1="vertices.efficiency.x" :y1="vertices.efficiency.y"
        :x2="vertices.cost.x" :y2="vertices.cost.y"
        class="tri-grid"
      />
      <line
        :x1="vertices.efficiency.x" :y1="vertices.efficiency.y"
        :x2="vertices.quality.x" :y2="vertices.quality.y"
        class="tri-grid"
      />
      <line
        :x1="vertices.cost.x" :y1="vertices.cost.y"
        :x2="vertices.quality.x" :y2="vertices.quality.y"
        class="tri-grid"
      />
      <text :x="vertices.efficiency.x" :y="vertices.efficiency.y - 8" class="tri-label" text-anchor="middle">效率</text>
      <text :x="vertices.cost.x - 4" :y="vertices.cost.y + 16" class="tri-label" text-anchor="start">成本</text>
      <text :x="vertices.quality.x + 4" :y="vertices.quality.y + 16" class="tri-label" text-anchor="end">质量</text>

      <line
        v-if="currentPt && optimalPt"
        :x1="currentPt.x" :y1="currentPt.y"
        :x2="optimalPt.x" :y2="optimalPt.y"
        class="tri-link"
      />
      <circle
        v-if="currentPt"
        :cx="currentPt.x" :cy="currentPt.y" r="7"
        class="pt-current"
      />
      <circle
        v-if="optimalPt"
        :cx="optimalPt.x" :cy="optimalPt.y" r="7"
        class="pt-optimal"
      />
    </svg>
    <div class="legend">
      <span class="leg-item"><i class="dot current" /> 当前策略</span>
      <span class="leg-item"><i class="dot optimal" /> 最佳平衡点</span>
    </div>
  </div>
</template>

<style scoped>
.triangle-wrap { display: flex; flex-direction: column; align-items: center; }
.triangle-svg { display: block; }
.tri-bg { fill: rgba(34, 211, 238, 0.04); stroke: var(--border-accent); stroke-width: 1.5; }
.tri-grid { stroke: var(--border-dim); stroke-width: 0.5; stroke-dasharray: 4 4; }
.tri-label { font-size: 11px; fill: var(--text-secondary); font-family: var(--font-ui); }
.tri-link { stroke: var(--accent-amber); stroke-width: 1; stroke-dasharray: 3 3; opacity: 0.7; }
.pt-current { fill: var(--accent-cyan); stroke: var(--bg-root); stroke-width: 2; }
.pt-optimal { fill: var(--status-running); stroke: var(--bg-root); stroke-width: 2; }
.legend { display: flex; gap: 16px; margin-top: 8px; font-size: 11px; color: var(--text-dim); }
.leg-item { display: flex; align-items: center; gap: 6px; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot.current { background: var(--accent-cyan); }
.dot.optimal { background: var(--status-running); }
</style>
