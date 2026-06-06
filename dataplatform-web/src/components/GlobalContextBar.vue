<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppContext } from '../composables/useAppContext'
import LinePickerBar from './LinePickerBar.vue'
import { categoryLabel, filterLines } from '../utils/lineCatalog'

const router = useRouter()
const route = useRoute()
const {
  lineId, batchId, timeRange, lines, batches, source, computedAt, overview, currentBatch,
  setLineId, setBatchId, setTimeRange, refreshMeta
} = useAppContext()

const showContext = computed(() => route.path.startsWith('/lines'))
const workshopFilter = ref('all')

const currentLine = computed(() =>
  lines.value.find(l => l.product_line_id === lineId.value)
)

const activeBatchId = computed(() =>
  batchId.value || currentBatch.value?.batch_id || null
)

const activeOrderId = computed(() =>
  currentBatch.value?.production_order_id || null
)

const sourceLabel = computed(() => {
  if (!source.value?.connected) {
    if (!overview.value) return '连接中…'
    if (source.value?.last_heartbeat_at) return '心跳超时'
    return '模拟器未推送'
  }
  const speed = source.value.speed_multiplier || 1
  return `${source.value.scenario_id || 'normal'} · ${speed}×`
})

const dataModeLabel = computed(() => {
  const mode = overview.value?.data_source?.kpi_mode
  if (mode === 'telemetry') return '实时遥测'
  if (currentLine.value?.simulation_enabled) return '仿真配置'
  return '登记估算'
})

const freshnessLabel = computed(() => {
  if (!computedAt.value) return null
  return computedAt.value.slice(11, 19)
})

let timer = null

function onWorkshopChange(ws) {
  workshopFilter.value = ws
  if (ws === 'all' || !lineId.value) return
  const visible = filterLines(lines.value, { workshop: ws, search: '' })
  if (!visible.some(l => l.product_line_id === lineId.value)) {
    const first = visible[0]
    if (first) pickLine(first.product_line_id)
  }
}

function pickLine(id) {
  if (!id) return
  setLineId(id)
  if (route.path.startsWith('/lines/')) {
    const query = { ...route.query }
    delete query.step
    router.push({ path: `/lines/${id}`, query })
  } else if (route.path.startsWith('/batches/')) {
    router.push({ path: '/batches' })
  } else if (route.path === '/trends' || route.path === '/alarms') {
    router.replace({ path: route.path, query: { ...route.query, line_id: id } })
  }
}

function onBatchChange(e) {
  setBatchId(e.target.value)
}

function onTimeChange(e) {
  setTimeRange(e.target.value)
}

function goBatch() {
  if (activeBatchId.value) router.push(`/batches/${activeBatchId.value}`)
}

function goOrder() {
  if (activeOrderId.value) router.push(`/orders/${activeOrderId.value}`)
}

onMounted(() => {
  timer = setInterval(refreshMeta, 5000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <header v-if="showContext" class="global-ctx">
    <div class="ctx-header">
      <div class="ctx-header-left">
        <div class="ctx-module-label">产线工作台</div>
        <label class="ctx-field compact">
          <span>批次</span>
          <select :value="batchId" @change="onBatchChange">
            <option value="">跟随当前</option>
            <option v-for="b in batches" :key="b.batch_id" :value="b.batch_id">
              {{ b.batch_id }} · {{ b.status }}
            </option>
          </select>
        </label>
        <label class="ctx-field compact">
          <span>时间</span>
          <select :value="timeRange" @change="onTimeChange">
            <option value="live">实时</option>
            <option value="shift">当班</option>
            <option value="batch">本批次</option>
          </select>
        </label>
        <div class="ctx-links" v-if="activeBatchId || activeOrderId">
          <button v-if="activeBatchId" type="button" class="ctx-chip" @click="goBatch">
            批次 {{ activeBatchId }}
          </button>
          <button v-if="activeOrderId" type="button" class="ctx-chip order" @click="goOrder">
            订单 {{ activeOrderId }}
          </button>
        </div>
      </div>

      <div class="ctx-status">
        <span v-if="currentLine" class="ctx-line-meta">
          <span class="ctx-line-name">{{ currentLine.name }}</span>
          <span class="ctx-cat">{{ categoryLabel(currentLine.product_category) }}</span>
          <span class="ctx-mode" :class="source?.connected ? 'online' : 'offline'">{{ dataModeLabel }}</span>
        </span>
        <span v-if="freshnessLabel" class="ctx-refresh">刷新 {{ freshnessLabel }}</span>
        <span class="source-pill" :class="source?.connected ? 'online' : 'offline'">
          <span class="source-dot"></span>
          {{ source?.connected ? sourceLabel : (overview ? sourceLabel : 'DATA OFFLINE') }}
        </span>
        <span v-if="!source?.connected && !overview" class="ctx-hint">
          请启动模拟器 POST /sim/start-all
        </span>
      </div>
    </div>

    <div class="ctx-line-picker-wrap">
      <LinePickerBar
        :lines="lines"
        :workshop="workshopFilter"
        :active-line-id="lineId"
        @update:workshop="onWorkshopChange"
        @pick-line="pickLine"
      />
    </div>
  </header>
</template>

<style scoped>
.global-ctx {
  display: flex;
  flex-direction: column;
  background: var(--bg-panel-elevated);
  border-bottom: 1px solid var(--border-dim);
  flex-shrink: 0;
}

.ctx-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 16px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-dim);
}

.ctx-header-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.ctx-module-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-cyan);
  white-space: nowrap;
  padding-right: 10px;
  border-right: 1px solid var(--border-dim);
}

.ctx-field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-dim);
}

.ctx-field.compact select {
  background: var(--bg-root);
  border: 1px solid var(--border-dim);
  color: var(--text-primary);
  border-radius: 3px;
  padding: 4px 8px;
  font-size: 11px;
  font-family: var(--font-mono);
  min-width: 120px;
}

.ctx-links { display: flex; gap: 6px; }

.ctx-chip {
  background: rgba(34, 211, 238, 0.1);
  border: 1px solid rgba(34, 211, 238, 0.25);
  color: var(--accent-cyan);
  border-radius: 3px;
  padding: 3px 8px;
  font-size: 11px;
  font-family: var(--font-mono);
  cursor: pointer;
}

.ctx-chip.order {
  background: rgba(251, 191, 36, 0.08);
  border-color: rgba(251, 191, 36, 0.25);
  color: var(--accent-amber);
}

.ctx-chip:hover { filter: brightness(1.1); }

.ctx-status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-left: auto;
}

.ctx-line-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ctx-line-name { font-size: 11px; color: var(--text-secondary); }

.ctx-cat {
  font-size: 10px;
  color: var(--accent-cyan);
  background: rgba(34, 211, 238, 0.08);
  padding: 1px 5px;
  border-radius: 2px;
}

.ctx-mode {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 2px;
}

.ctx-mode.online {
  color: var(--status-ok);
  background: rgba(52, 211, 153, 0.1);
}

.ctx-mode.offline {
  color: var(--status-warning);
  background: rgba(251, 191, 36, 0.08);
}

.ctx-refresh {
  font-size: 11px;
  color: var(--text-dim);
  font-family: var(--font-mono);
}

.ctx-hint {
  font-size: 10px;
  color: var(--status-warning);
}

.ctx-line-picker-wrap {
  padding: 8px 16px 10px;
}
</style>
