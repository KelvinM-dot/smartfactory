<script setup>
import { computed } from 'vue'
import {
  twinLayoutsRef, getTwinLayout, buildFlowPathD, statusColor
} from '../../config/twinLayout'
import { useTwinPipeline } from '../../composables/useTwinPipeline'
import { useAppContext } from '../../composables/useAppContext'
import { statusLabel } from '../../utils/format'

const props = defineProps({
  lineId: { type: String, default: 'FCW-LINE-07' },
  pipeline: { type: Array, default: () => [] },
  equipment: { type: Array, default: () => [] },
  batch: { type: Object, default: null },
  selectedStep: { type: String, default: null }
})

const emit = defineEmits(['select-step', 'select-equipment'])

const layout = computed(() => {
  void twinLayoutsRef.value
  return getTwinLayout(props.lineId)
})
const hasLayout = computed(() => Boolean(layout.value))
const viewBox = computed(() => layout.value?.viewBox || { w: 1280, h: 620 })
const nodes = computed(() => layout.value?.nodes || {})
const flowPath = computed(() => layout.value?.flowPath || [])
const keyFields = computed(() => layout.value?.keyFields || {})
const fieldLabels = computed(() => layout.value?.fieldLabels || {})

const pathD = computed(() => buildFlowPathD(nodes.value, flowPath.value))

const { lines } = useAppContext()

const lineTitle = computed(() => {
  const line = lines.value.find(l => l.product_line_id === props.lineId)
  if (line?.name) return `${line.name} · ${props.lineId}`
  return props.lineId
})

const {
  equipByStep,
  lineRunning,
  hasAlarm,
  stepStatus,
  primaryEquip,
  liveMetricText
} = useTwinPipeline(
  () => props.pipeline,
  () => props.equipment,
  keyFields,
  fieldLabels
)

function stepColor(stepId) {
  return statusColor(stepStatus(stepId))
}

function liveMetric(stepId) {
  const text = liveMetricText(stepId)
  return text || null
}

function nodeClass(stepId) {
  const s = stepStatus(stepId)
  return {
      'twin-node': true,
      selected: props.selectedStep === stepId,
      running: s === 'running',
      alarm: s === 'alarm',
      manual: s === 'manual',
      stopped: s === 'stopped'
    }
}

function onNodeClick(stepId) {
  emit('select-step', stepId)
  const eq = primaryEquip(stepId)
  if (eq) emit('select-equipment', eq)
}
</script>

<template>
  <div v-if="!hasLayout" class="twin-canvas-wrap twin-canvas-empty">
    <div class="empty-msg">{{ lineId }} · 孪生布局未配置</div>
  </div>
  <div v-else class="twin-canvas-wrap">
    <svg
      class="twin-svg"
      :viewBox="`0 0 ${viewBox.w} ${viewBox.h}`"
      preserveAspectRatio="xMidYMid meet"
    >
      <defs>
        <linearGradient id="twin-bg-grad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#0a1220" />
          <stop offset="100%" stop-color="#0f1a2e" />
        </linearGradient>
        <filter id="glow-green" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="4" result="blur" />
          <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
        <filter id="glow-red" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="6" result="blur" />
          <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
        <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L0,6 L8,3 z" fill="#334155" />
        </marker>
      </defs>

      <!-- 背景网格 -->
      <rect width="100%" height="100%" fill="url(#twin-bg-grad)" />
      <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
        <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1e293b" stroke-width="0.5" />
      </pattern>
      <rect width="100%" height="100%" fill="url(#grid)" opacity="0.6" />

      <!-- 产线标题 -->
      <text x="640" y="36" text-anchor="middle" class="twin-title">
        {{ lineTitle }}
      </text>
      <text v-if="batch" x="640" y="58" text-anchor="middle" class="twin-subtitle">
        批次 {{ batch.batch_id }} · {{ batch.grade }} · {{ batch.recipe_id }}
      </text>

      <!-- 物料主流向路径 -->
      <path
        :d="pathD"
        class="twin-flow-base"
        marker-end="url(#arrow)"
      />
      <path
        :d="pathD"
        class="twin-flow-active"
        :class="{ active: lineRunning, alarm: hasAlarm }"
      />

      <!-- 流动粒子（运行中） -->
      <circle v-if="lineRunning" r="5" class="twin-particle">
        <animateMotion dur="14s" repeatCount="indefinite" :path="pathD" />
      </circle>
      <circle v-if="lineRunning" r="4" class="twin-particle delay">
        <animateMotion dur="14s" begin="4s" repeatCount="indefinite" :path="pathD" />
      </circle>

      <!-- 工序节点 -->
      <g
        v-for="stepId in flowPath"
        :key="stepId"
        :transform="`translate(${nodes[stepId].x}, ${nodes[stepId].y})`"
        class="twin-node-group"
        @click="onNodeClick(stepId)"
      >
        <!-- 外圈状态环 -->
        <circle
          r="52"
          fill="none"
          :stroke="stepColor(stepId)"
          stroke-width="2"
          :class="nodeClass(stepId)"
          :filter="stepStatus(stepId) === 'alarm' ? 'url(#glow-red)' : stepStatus(stepId) === 'running' ? 'url(#glow-green)' : undefined"
        />
        <!-- 设备本体 -->
        <rect x="-44" y="-36" width="88" height="72" rx="4" class="twin-node-body" />
        <!-- 工序名 -->
        <text y="-18" text-anchor="middle" class="twin-node-label">{{ nodes[stepId].label }}</text>
        <!-- 设备 ID -->
        <text y="-2" text-anchor="middle" class="twin-node-eq">
          {{ primaryEquip(stepId)?.equipment_id || '—' }}
        </text>
        <!-- 状态 -->
        <text y="14" text-anchor="middle" :fill="stepColor(stepId)" class="twin-node-status">
          {{ statusLabel(stepStatus(stepId)) }}
        </text>
        <!-- 实时参数 -->
        <text y="28" text-anchor="middle" class="twin-node-metric">
          {{ liveMetric(stepId) || '—' }}
        </text>
      </g>

      <!-- 图例 -->
      <g transform="translate(40, 540)">
        <text class="twin-legend-title">图例</text>
        <g v-for="(item, i) in [
          { c: '#34d399', t: '运行' },
          { c: '#f87171', t: '报警' },
          { c: '#fbbf24', t: '手动' },
          { c: '#64748b', t: '停机' }
        ]" :key="item.t" :transform="`translate(${i * 90}, 16)`">
          <circle r="5" :fill="item.c" />
          <text x="12" y="4" class="twin-legend-item">{{ item.t }}</text>
        </g>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.twin-canvas-wrap {
  width: 100%;
  height: 100%;
  min-height: 480px;
  background: #070b14;
  border-radius: 4px;
  overflow: hidden;
}

.twin-canvas-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-msg {
  color: var(--text-dim);
  font-size: 13px;
}

.twin-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.twin-title {
  fill: #e2e8f0;
  font-size: 16px;
  font-weight: 600;
  font-family: "PingFang SC", sans-serif;
}

.twin-subtitle {
  fill: #64748b;
  font-size: 11px;
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
}

.twin-flow-base {
  fill: none;
  stroke: #1e3a5f;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.twin-flow-active {
  fill: none;
  stroke: #22d3ee;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-dasharray: 12 8;
  stroke-dashoffset: 0;
  opacity: 0;
  transition: opacity 0.3s;
}

.twin-flow-active.active {
  opacity: 0.85;
  animation: flow-dash 1.2s linear infinite;
}

.twin-flow-active.alarm {
  stroke: #f87171;
}

@keyframes flow-dash {
  to { stroke-dashoffset: -40; }
}

.twin-particle {
  fill: #22d3ee;
  filter: drop-shadow(0 0 6px #22d3ee);
}

.twin-particle.delay { fill: #34d399; opacity: 0.7; }

.twin-node-group { cursor: pointer; }
.twin-node-group:hover .twin-node-body { fill: #1a2744; }

.twin-node-body {
  fill: #111827;
  stroke: #334155;
  stroke-width: 1;
}

.twin-node-group.selected .twin-node-body {
  stroke: #22d3ee;
  stroke-width: 2;
  fill: #152238;
}

.twin-node.running { animation: twin-pulse 2s ease-in-out infinite; }
.twin-node.alarm { animation: twin-alarm-pulse 0.8s ease-in-out infinite; }

@keyframes twin-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.65; }
}

@keyframes twin-alarm-pulse {
  0%, 100% { stroke-width: 2; }
  50% { stroke-width: 4; }
}

.twin-node-label {
  fill: #94a3b8;
  font-size: 11px;
  font-family: "PingFang SC", sans-serif;
}

.twin-node-eq {
  fill: #22d3ee;
  font-size: 10px;
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
}

.twin-node-status {
  font-size: 10px;
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
  font-weight: 600;
}

.twin-node-metric {
  fill: #e2e8f0;
  font-size: 9px;
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
}

.twin-legend-title {
  fill: #64748b;
  font-size: 10px;
}

.twin-legend-item {
  fill: #94a3b8;
  font-size: 10px;
  font-family: "PingFang SC", sans-serif;
}
</style>
