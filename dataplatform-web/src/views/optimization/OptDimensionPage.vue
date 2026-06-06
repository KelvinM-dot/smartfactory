<script setup>
import { ref, computed } from 'vue'
import PageHeader from '../../components/PageHeader.vue'
import OptWorkshopPanel from '../../components/optimization/OptWorkshopPanel.vue'
import OptRecommendationList from '../../components/optimization/OptRecommendationList.vue'
import { getOptEfficiency, getOptCost, getOptQuality } from '../../api/optimization'
import { usePolling } from '../../composables/usePolling'

const props = defineProps({
  dimension: { type: String, required: true },
  title: { type: String, required: true }
})

const data = ref(null)
const workshopFilter = ref('all')
const showAllRows = ref(false)
const pageByModel = ref({})

const LOADERS = {
  efficiency: getOptEfficiency,
  cost: getOptCost,
  quality: getOptQuality
}

const PAGE_SIZE = 15

async function load() {
  const fn = LOADERS[props.dimension]
  if (fn) data.value = await fn().catch(() => null)
}

usePolling(load, 8000)

const models = computed(() => {
  const m = data.value?.models
  if (!m) return []
  return Object.values(m)
})

const workshops = computed(() => data.value?.workshops || [])
const recommendations = computed(() => data.value?.recommendations || [])
const lineSummary = computed(() => data.value?.line_summary || data.value?.plant_layout || {})

function modelKpis(model) {
  const skip = new Set([
    'model_id', 'title', 'recommendations', 'line_breakdown', 'loss_ranking',
    'assignments', 'ctq_monitoring', 'recipe_breakdown', 'coq_breakdown_cny',
    'line_mix_analysis', 'grade_distribution', 'line_utilization',
    'loss_ranking_count', 'ctq_monitoring_count'
  ])
  return Object.entries(model).filter(([k]) => !skip.has(k) && typeof model[k] !== 'object')
}

function tableData(model) {
  if (model.line_breakdown) return { key: 'line_breakdown', rows: model.line_breakdown }
  if (model.loss_ranking) return { key: 'loss_ranking', rows: model.loss_ranking }
  if (model.assignments) return { key: 'assignments', rows: model.assignments }
  if (model.ctq_monitoring) return { key: 'ctq_monitoring', rows: model.ctq_monitoring }
  if (model.recipe_breakdown) return { key: 'recipe_breakdown', rows: model.recipe_breakdown }
  if (model.line_utilization) return { key: 'line_utilization', rows: model.line_utilization }
  if (model.line_mix_analysis) return { key: 'line_mix_analysis', rows: model.line_mix_analysis }
  return null
}

function filterRows(rows) {
  if (!rows?.length) return []
  if (workshopFilter.value === 'wire') {
    return rows.filter(r => r.workshop_id === 'WS-WIRE-01' || String(r.product_line_id || '').startsWith('FCW')
      || String(r.product_line_id || '').startsWith('SW') || String(r.product_line_id || '').startsWith('SAW'))
  }
  if (workshopFilter.value === 'rod') {
    return rows.filter(r => r.workshop_id === 'WS-ROD-01' || String(r.product_line_id || '').startsWith('WR'))
  }
  return rows
}

function visibleRows(model) {
  const td = tableData(model)
  if (!td) return []
  const filtered = filterRows(td.rows)
  if (showAllRows.value) return filtered
  const page = pageByModel.value[model.model_id] || 0
  return filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
}

function tableColumns(model) {
  const rows = visibleRows(model)
  if (!rows.length) return []
  return Object.keys(rows[0]).slice(0, 8)
}

function totalPages(model) {
  const td = tableData(model)
  if (!td) return 1
  return Math.max(1, Math.ceil(filterRows(td.rows).length / PAGE_SIZE))
}

function currentPage(model) {
  return pageByModel.value[model.model_id] || 0
}

function setPage(modelId, page) {
  pageByModel.value = { ...pageByModel.value, [modelId]: page }
}

function filteredCount(model) {
  const td = tableData(model)
  return td ? filterRows(td.rows).length : 0
}
</script>

<template>
  <div class="page">
    <PageHeader :section="'智优决策中心'" :title="title">
      <div class="dim-meta" v-if="lineSummary.total_line_count || lineSummary.total_lines">
        全厂 {{ lineSummary.total_line_count || lineSummary.total_lines }} 线
        · 仿真 {{ lineSummary.telemetry_line_count || lineSummary.telemetry_lines || '—' }}
      </div>
    </PageHeader>

    <div class="content">
      <div class="toolbar panel">
        <div class="filter-tabs">
          <button :class="{ active: workshopFilter === 'all' }" @click="workshopFilter = 'all'">全部产线</button>
          <button :class="{ active: workshopFilter === 'wire' }" @click="workshopFilter = 'wire'">焊丝车间</button>
          <button :class="{ active: workshopFilter === 'rod' }" @click="workshopFilter = 'rod'">焊条车间</button>
        </div>
        <label class="show-all">
          <input v-model="showAllRows" type="checkbox" />
          展开全部行（42 线）
        </label>
      </div>

      <div class="ws-strip">
        <OptWorkshopPanel v-for="ws in workshops" :key="ws.workshop_id" :workshop="ws" />
      </div>

      <div class="grid-side">
        <div class="main-col">
          <div v-for="model in models" :key="model.model_id" class="panel model-panel">
            <div class="model-head">
              <span class="model-id">{{ model.model_id }}</span>
              <span class="model-title">{{ model.title }}</span>
            </div>
            <div class="kpi-chips">
              <span v-for="[k, v] in modelKpis(model)" :key="k" class="chip">
                {{ k }}: <b>{{ v }}</b>
              </span>
            </div>
            <div v-if="model.coq_breakdown_cny" class="coq-grid">
              <div v-for="(val, key) in model.coq_breakdown_cny" :key="key" class="coq-item">
                <span class="coq-key">{{ key }}</span>
                <span class="coq-val">¥{{ val?.toLocaleString?.() ?? val }}</span>
              </div>
            </div>
            <div v-if="model.green_window_hint" class="hint-box">{{ model.green_window_hint }}</div>
            <template v-if="tableData(model)">
              <div class="table-meta">
                产线明细 {{ filteredCount(model) }} 行
                <template v-if="!showAllRows && totalPages(model) > 1">
                  · 第 {{ currentPage(model) + 1 }}/{{ totalPages(model) }} 页
                </template>
              </div>
              <table class="data-table">
                <thead>
                  <tr>
                    <th v-for="col in tableColumns(model)" :key="col">{{ col }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, ri) in visibleRows(model)" :key="ri">
                    <td v-for="col in tableColumns(model)" :key="col">{{ row[col] }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!showAllRows && totalPages(model) > 1" class="pager">
                <button
                  type="button"
                  :disabled="currentPage(model) <= 0"
                  @click="setPage(model.model_id, currentPage(model) - 1)"
                >上一页</button>
                <button
                  type="button"
                  :disabled="currentPage(model) >= totalPages(model) - 1"
                  @click="setPage(model.model_id, currentPage(model) + 1)"
                >下一页</button>
              </div>
            </template>
          </div>
        </div>
        <div class="side-col panel">
          <div class="panel-title">维度建议</div>
          <OptRecommendationList :items="recommendations" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dim-meta { font-size: 11px; color: var(--text-dim); }
.toolbar {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 10px; margin-bottom: 12px;
}
.filter-tabs { display: flex; gap: 4px; }
.filter-tabs button {
  font-size: 11px; padding: 4px 10px; border-radius: 3px;
  border: 1px solid var(--border-dim); background: transparent;
  color: var(--text-dim); cursor: pointer;
}
.filter-tabs button.active { border-color: var(--accent-cyan); color: var(--accent-cyan); }
.show-all { font-size: 11px; color: var(--text-dim); display: flex; align-items: center; gap: 6px; cursor: pointer; }
.ws-strip { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px; }
@media (max-width: 900px) { .ws-strip { grid-template-columns: 1fr; } }
.grid-side { display: grid; grid-template-columns: 1fr 300px; gap: 12px; }
@media (max-width: 1100px) { .grid-side { grid-template-columns: 1fr; } }
.panel {
  background: var(--bg-panel);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  padding: 12px 14px;
  margin-bottom: 12px;
}
.panel-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.model-panel { border-left: 3px solid var(--border-accent); }
.model-head { margin-bottom: 8px; }
.model-id { font-family: var(--font-mono); font-size: 11px; color: var(--accent-cyan); margin-right: 8px; }
.model-title { font-size: 14px; font-weight: 600; }
.kpi-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
.chip {
  font-size: 10px; padding: 3px 8px; border-radius: 3px;
  background: var(--bg-panel-elevated); color: var(--text-secondary);
}
.chip b { color: var(--text-primary); font-family: var(--font-mono); }
.table-meta { font-size: 10px; color: var(--text-dim); margin-bottom: 6px; }
.hint-box {
  font-size: 11px; padding: 8px 10px; margin-bottom: 8px;
  background: rgba(34, 211, 238, 0.08); border-radius: 3px; color: var(--accent-teal);
}
.coq-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 10px; }
.coq-item { padding: 8px; background: var(--bg-panel-elevated); border-radius: 3px; text-align: center; }
.coq-key { display: block; font-size: 10px; color: var(--text-dim); }
.coq-val { font-family: var(--font-mono); font-size: 12px; font-weight: 600; }
.data-table { width: 100%; border-collapse: collapse; font-size: 10px; }
.data-table th, .data-table td {
  padding: 4px 6px; border-bottom: 1px solid var(--border-dim);
  text-align: left; font-family: var(--font-mono);
}
.data-table th { color: var(--text-dim); font-weight: 500; }
.pager { display: flex; gap: 8px; margin-top: 8px; }
.pager button {
  font-size: 11px; padding: 4px 10px; border-radius: 3px;
  border: 1px solid var(--border-dim); background: transparent;
  color: var(--text-secondary); cursor: pointer;
}
.pager button:disabled { opacity: 0.4; cursor: not-allowed; }
.side-col { position: sticky; top: 0; align-self: start; }
</style>
