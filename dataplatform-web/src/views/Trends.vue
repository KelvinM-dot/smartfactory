<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import PageHeader from '../components/PageHeader.vue'
import TrendMini from '../components/TrendMini.vue'
import { getTrends, getLines, CRITICAL_FIELDS } from '../api'
import { usePolling } from '../composables/usePolling'
import { useAppContext } from '../composables/useAppContext'

const { lineId, batchId, timeRange, lines, setLineId } = useAppContext()

const trendData = ref(null)
const range = ref('buffer')
const loadError = ref(false)

const series = computed(() => trendData.value?.series || [])
const hasPoints = computed(() => series.value.some(s => (s.points || []).length > 0))
const activeLineName = computed(() =>
  lines.value.find(l => l.product_line_id === lineId.value)?.name || lineId.value
)

async function ensureLines() {
  if (!lines.value.length) {
    lines.value = await getLines().catch(() => [])
  }
}

async function load() {
  loadError.value = false
  await ensureLines()
  const batch = batchId.value || undefined
  const r = timeRange.value === 'batch' ? 'batch' : range.value
  try {
    trendData.value = await getTrends(lineId.value, CRITICAL_FIELDS, r, batch)
  } catch {
    loadError.value = true
    trendData.value = null
  }
}

function onLineChange(e) {
  setLineId(e.target.value)
}

onMounted(ensureLines)
watch([lineId, batchId, timeRange, range], load)
usePolling(load, 5000)
</script>

<template>
  <div class="analysis-root">
    <PageHeader section="分析" title="趋势分析" />

    <div class="page-body full-bleed">
      <div class="trend-toolbar">
        <label class="line-picker">
          <span>产线</span>
          <select :value="lineId" @change="onLineChange">
            <option v-for="l in lines" :key="l.product_line_id" :value="l.product_line_id">
              {{ l.name }} ({{ l.product_line_id }})
            </option>
          </select>
        </label>
        <button class="tab-btn" :class="{ active: range === 'buffer' }" @click="range = 'buffer'">窗口内</button>
        <button class="tab-btn" :class="{ active: range === 'batch' }" @click="range = 'batch'">当前批次</button>
        <span class="scope-hint">{{ activeLineName }}</span>
      </div>

      <div class="grid-dashboard">
        <div v-for="s in series" :key="s.field_id" class="col-6">
          <TrendMini :series="s" :title="s.display_name || s.field_id" />
        </div>
      </div>
      <div v-if="loadError" class="empty-state">无法连接数据台 API，请确认 dataplatform-api 已启动（端口 3001）</div>
      <div v-else-if="!series.length || !hasPoints" class="empty-state">
        暂无 {{ lineId }} 时序数据。请启动 line-simulator 并执行 POST /sim/start-all，且模拟器 DATAPLATFORM_URL 需指向本地 127.0.0.1:3001
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 布局见 main.css .analysis-root */
.trend-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 12px; }
.line-picker { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--text-dim); }
.line-picker select {
  background: var(--bg-panel);
  border: 1px solid var(--border-dim);
  color: var(--text-primary);
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 4px;
}
.scope-hint { font-size: 11px; color: var(--text-dim); font-family: var(--font-mono); margin-left: auto; }
</style>
