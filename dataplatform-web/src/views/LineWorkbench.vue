<script setup>
import { ref, computed, watch, onMounted, toRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import AlarmHandleDialog from '../components/AlarmHandleDialog.vue'
import ProcessPipelineTrack from '../components/ProcessPipelineTrack.vue'
import LineVisualPanel from '../components/twin/LineVisualPanel.vue'
import LineTwinCanvas from '../components/twin/LineTwinCanvas.vue'
import TrendMini from '../components/TrendMini.vue'
import {
  getOverview, getTrends, getAlarms, getAllEquipmentLatest,
  getDataPoints, getStepDetail, getBatches, CRITICAL_FIELDS
} from '../api'
import { usePolling } from '../composables/usePolling'
import { useLineStream } from '../composables/useLineStream'
import { useAppContext } from '../composables/useAppContext'
import { fmtTime, fmtPct, statusClass, inSpec } from '../utils/format'
import { buildDataPointLookup, supportsTwin3D } from '../utils/dataPoints'
import { getTwinLayout } from '../config/twinLayout'
import { categoryLabel, lineStatusBadge } from '../utils/lineCatalog'
import { useAlarmNavigation } from '../composables/useAlarmNavigation'

const props = defineProps({ lineId: { type: String, required: true } })
const route = useRoute()
const router = useRouter()
const { navigateToAlarmTwin } = useAlarmNavigation()
const { setLineId, batchId, setBatchId, setOverview, overview: ctxOverview, lines: ctxLines } = useAppContext()

const lineMeta = computed(() =>
  ctxLines.value.find(l => l.product_line_id === props.lineId) || { product_line_id: props.lineId }
)

const TABS = [
  { id: 'overview', label: '总览' },
  { id: 'twin', label: '孪生' },
  { id: 'params', label: '参数表' }
]

const overview = ref(null)
const lineBatches = ref([])
const trendData = ref(null)
const alarms = ref([])
const equipment = ref([])
const dataPoints = ref([])
const sparkTrend = ref(null)
const stepDetail = ref(null)
const alarmHandleOpen = ref(false)
const alarmHandleTarget = ref(null)
const selectedStep = ref(null)
const selectedEquip = ref(null)
const viewMode = ref('split')

const hasTwin3D = computed(() => supportsTwin3D(props.lineId))
const dataModeLabel = computed(() => {
  const mode = overview.value?.data_source?.kpi_mode
  if (mode === 'telemetry') return '实时遥测'
  if (lineMeta.value?.simulation_enabled) return '仿真配置'
  return '登记估算'
})
const lineBadge = computed(() => lineStatusBadge(lineMeta.value))
const activeTab = computed(() => route.query.tab || 'overview')
const kpi = computed(() => overview.value?.kpi_bar || {})
const pipeline = computed(() => overview.value?.process_pipeline || [])
const displayBatch = computed(() => {
  const fromOverview = overview.value?.current_batch
  if (fromOverview?.batch_id) return fromOverview
  const inProgress = lineBatches.value.find(b => b.status === 'in_progress')
  if (inProgress) return inProgress
  const sorted = [...lineBatches.value].sort((a, b) =>
    String(b.started_at || '').localeCompare(String(a.started_at || '')))
  return sorted[0] || null
})

const batchEmptyHint = computed(() => {
  if (!lineMeta.value?.simulation_enabled) {
    return '登记产线，无仿真在制批次'
  }
  if (!overview.value?.data_source?.connected) {
    return '模拟器未推送实时批次，请确认该产线已启动仿真'
  }
  return '当前无在制批次'
})
const trendSeries = computed(() => (trendData.value?.series || []).slice(0, 2))

const selectedParams = computed(() => {
  if (!selectedEquip.value?.latest?.values) return []
  const dpMap = buildDataPointLookup(dataPoints.value)
  return Object.entries(selectedEquip.value.latest.values)
    .filter(([k]) => k !== 'status' && k !== 'run_mode')
    .map(([fieldId, value]) => {
      const dp = dpMap[`${selectedEquip.value.equipment_id}:${fieldId}`]
      return {
        fieldId,
        name: dp?.display_name || fieldId,
        value,
        unit: dp?.unit || '',
        spec: inSpec(value, dp?.spec_limits),
        limits: dp?.spec_limits
      }
    })
})

const liveRows = computed(() => {
  const dpMap = buildDataPointLookup(dataPoints.value)
  const rows = []
  for (const eq of equipment.value) {
    for (const [fieldId, value] of Object.entries(eq.latest?.values || {})) {
      if (fieldId === 'status' || fieldId === 'run_mode') continue
      const dp = dpMap[`${eq.equipment_id}:${fieldId}`]
      rows.push({
        equipment_id: eq.equipment_id,
        process_step_id: eq.process_step_id,
        field_id: fieldId,
        display_name: dp?.display_name || fieldId,
        unit: dp?.unit || '',
        value,
        spec: inSpec(value, dp?.spec_limits),
        limits: dp?.spec_limits,
        ts: eq.latest?.timestamp
      })
    }
  }
  return rows.sort((a, b) => a.process_step_id.localeCompare(b.process_step_id))
})

function switchTab(tabId) {
  router.replace({ path: `/lines/${props.lineId}`, query: { ...route.query, tab: tabId } })
}

function goTwinFromOverview(stepId) {
  router.replace({
    path: `/lines/${props.lineId}`,
    query: { tab: 'twin', step: stepId }
  })
}

function goAlarmTwin(alarm) {
  navigateToAlarmTwin(alarm, equipment.value, props.lineId)
}

function openAlarmHandle(alarm) {
  alarmHandleTarget.value = alarm
  alarmHandleOpen.value = true
}

function onAlarmHandled(updated) {
  const idx = alarms.value.findIndex(a => a.alarm_id === updated.alarm_id)
  if (idx >= 0) alarms.value[idx] = { ...alarms.value[idx], ...updated }
}

async function onTwinSelectStep(stepId) {
  selectedStep.value = stepId
  const eq = equipment.value.find(e => e.process_step_id === stepId)
  if (eq) selectedEquip.value = eq
  const [trendResp, detailResp] = await Promise.all([
    (async () => {
      const field = (dataPoints.value || []).find(d => d.process_step_id === stepId)?.field_id
        || CRITICAL_FIELDS[0]
      return getTrends(props.lineId, [field], 'buffer', displayBatch.value?.batch_id)
    })(),
    getStepDetail(props.lineId, stepId).catch(() => null)
  ])
  sparkTrend.value = trendResp
  stepDetail.value = detailResp
}

function stepEquipStatusClass(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'RUNNING') return 'running'
  if (s === 'ALARM') return 'alarm'
  if (s === 'MANUAL') return 'manual'
  return 'stopped'
}

function valClass(spec) {
  if (spec === true) return 'val-good'
  if (spec === false) return 'val-bad'
  return ''
}

function syncBatchContext(batchDoc) {
  if (batchDoc?.batch_id) setBatchId(batchDoc.batch_id)
}

async function load() {
  const [ov, tr, al, eq, dp, batches] = await Promise.all([
    getOverview(props.lineId),
    getTrends(props.lineId, CRITICAL_FIELDS.slice(0, 2)),
    getAlarms(props.lineId, 'pending'),
    getAllEquipmentLatest(props.lineId),
    getDataPoints(props.lineId),
    getBatches(props.lineId).catch(() => [])
  ])
  overview.value = ov
  setOverview(ov)
  lineBatches.value = batches
  trendData.value = tr
  alarms.value = al
  equipment.value = eq
  dataPoints.value = dp
  syncBatchContext(ov.current_batch)
  if (!ov.current_batch?.batch_id) {
    const fallback = batches.find(b => b.status === 'in_progress') || batches[0]
    syncBatchContext(fallback)
  }
}

function resetTwinSelection() {
  selectedStep.value = null
  selectedEquip.value = null
  sparkTrend.value = null
  stepDetail.value = null
}

function resolveTwinStep() {
  const steps = pipeline.value.map(s => s.process_step_id)
  const fromQuery = route.query.step
  if (fromQuery && steps.includes(fromQuery)) return fromQuery
  const equipId = route.query.equip
  if (equipId) {
    const step = equipment.value.find(e => e.equipment_id === equipId)?.process_step_id
    if (step && steps.includes(step)) return step
  }
  const layoutStep = getTwinLayout(props.lineId)?.flowPath?.[0]
  if (layoutStep && steps.includes(layoutStep)) return layoutStep
  return steps[0] || layoutStep || null
}

watch(() => props.lineId, async () => {
  setLineId(props.lineId)
  resetTwinSelection()
  await load()
  if (activeTab.value === 'twin') {
    const step = resolveTwinStep()
    if (step) await onTwinSelectStep(step)
  }
})

watch(activeTab, (tab) => {
  if (tab === 'twin') {
    const step = resolveTwinStep()
    if (step) onTwinSelectStep(step)
  }
})

watch(() => route.query.step, (s) => {
  if (s && activeTab.value === 'twin') {
    onTwinSelectStep(s)
  }
})

watch(() => route.query.equip, (equipId) => {
  if (!equipId || activeTab.value !== 'twin') return
  const step = resolveTwinStep()
  if (step) onTwinSelectStep(step)
  const eq = equipment.value.find(e => e.equipment_id === equipId)
  if (eq) selectedEquip.value = eq
})

watch(() => route.query.tab, (tab) => {
  if (tab === 'process') {
    router.replace({
      path: `/lines/${props.lineId}`,
      query: { tab: 'twin', step: route.query.step }
    })
  }
})

usePolling(load, 3000)
useLineStream(toRef(props, 'lineId'), (msg) => {
  if (msg.type === 'overview_patch' && msg.payload) {
    const prev = overview.value || {}
    const next = { ...prev, ...msg.payload }
    if (!msg.payload.current_batch && prev.current_batch) {
      next.current_batch = prev.current_batch
    }
    overview.value = next
    setOverview(next)
    syncBatchContext(next.current_batch || displayBatch.value)
  }
})

onMounted(() => {
  setLineId(props.lineId, { force: !ctxOverview.value })
  load().then(() => {
    if (activeTab.value === 'twin') {
      const step = resolveTwinStep()
      if (step) onTwinSelectStep(step)
    }
  })
})
</script>

<template>
  <div class="workbench-root">
    <PageHeader section="产线工作台" :title="lineMeta.name || lineId">
      <div class="workbench-header-right">
        <div class="workbench-meta">
          <span class="line-id mono">{{ lineId }}</span>
          <span class="line-cat">{{ categoryLabel(lineMeta.product_category) }}</span>
          <span class="line-badge" :class="lineBadge.cls">{{ lineBadge.label }}</span>
          <span class="line-badge" :class="overview?.data_source?.connected ? 'running' : 'manual'">{{ dataModeLabel }}</span>
        </div>
        <div class="workbench-tabs">
          <button
            v-for="t in TABS"
            :key="t.id"
            class="tab-btn"
            :class="{ active: activeTab === t.id }"
            @click="switchTab(t.id)"
          >{{ t.label }}</button>
        </div>
      </div>
    </PageHeader>

    <div
      class="workbench-body page-body full-bleed"
      :class="{ 'twin-active': activeTab === 'twin', 'params-active': activeTab === 'params' }"
      v-if="overview"
    >
      <!-- 总览 -->
      <template v-if="activeTab === 'overview'">
        <div class="kpi-strip">
          <div class="kpi-tile ok"><div class="label">OEE</div><div class="value">{{ fmtPct(kpi.oee_pct) }}<span class="unit">%</span></div></div>
          <div class="kpi-tile"><div class="label">当班产量</div><div class="value">{{ kpi.shift_output_t ?? '—' }}<span class="unit">t</span></div></div>
          <div class="kpi-tile ok"><div class="label">一次合格率</div><div class="value">{{ fmtPct(kpi.first_pass_yield_pct) }}<span class="unit">%</span></div></div>
          <div class="kpi-tile" :class="{ alarm: kpi.pending_alarms > 0 }"><div class="label">未处理报警</div><div class="value">{{ kpi.pending_alarms ?? 0 }}</div></div>
          <div class="kpi-tile"><div class="label">监控设备</div><div class="value">{{ equipment.length }}<span class="unit">台</span></div></div>
        </div>

        <div class="panel" style="margin-bottom:var(--grid-gap)">
          <div class="panel-head">
            <h3>工序流程 · 实时参数</h3>
            <button class="tab-btn" @click="switchTab('twin')">孪生视图 →</button>
          </div>
          <div class="panel-body">
            <ProcessPipelineTrack
              :pipeline="pipeline"
              :equipment="equipment"
              :data-points="dataPoints"
              :line="lineMeta"
              clickable
              :max-params="lineMeta.simulation_enabled ? 3 : 2"
              @select-step="goTwinFromOverview"
            />
          </div>
        </div>

        <div class="overview-secondary">
          <div class="panel batch-alarm-panel">
            <div class="panel-head">
              <h3>当前批次</h3>
              <button
                v-if="displayBatch?.batch_id"
                type="button"
                class="tab-btn mini"
                @click="router.push(`/batches/${displayBatch.batch_id}`)"
              >批次详情 →</button>
            </div>
            <div class="panel-body batch-card" v-if="displayBatch">
              <div class="row"><span class="k">批次</span><span class="v mono">{{ displayBatch.batch_id }}</span></div>
              <div class="row"><span class="k">订单</span><span class="v mono">{{ displayBatch.production_order_id || '—' }}</span></div>
              <div class="row"><span class="k">状态</span><span class="v">{{ displayBatch.status || '—' }}</span></div>
              <div class="row"><span class="k">牌号</span><span class="v">{{ displayBatch.grade || '—' }}</span></div>
              <div class="row"><span class="k">配方</span><span class="v">{{ displayBatch.recipe_id || '—' }}</span></div>
            </div>
            <div class="panel-body" v-else>
              <div class="empty-state">{{ batchEmptyHint }}</div>
            </div>
          </div>

          <div class="panel batch-alarm-panel">
            <div class="panel-head">
              <h3>待处理报警</h3>
              <span class="meta">{{ alarms.length }}</span>
            </div>
            <div class="panel-body flush">
              <table class="data-table alarm-click-table" v-if="alarms.length">
                <thead>
                  <tr><th>时间</th><th>设备</th><th>描述</th><th></th></tr>
                </thead>
                <tbody>
                  <tr
                    v-for="a in alarms.slice(0, 5)"
                    :key="a.alarm_id"
                    class="clickable-row"
                    @click="goAlarmTwin(a)"
                  >
                    <td>{{ fmtTime(a.triggered_at) }}</td>
                    <td>{{ a.equipment_id }}</td>
                    <td>
                      {{ a.alarm_message }}
                      <span class="alarm-status-tag">{{ a.handle_status }}</span>
                    </td>
                    <td @click.stop>
                      <button
                        v-if="a.handle_status !== 'resolved'"
                        type="button"
                        class="tab-btn mini"
                        @click="openAlarmHandle(a)"
                      >处理</button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-state">无待处理报警</div>
            </div>
          </div>

          <div class="panel trend-panel">
            <div class="panel-head"><h3>关键趋势</h3></div>
            <div class="panel-body">
              <div class="trend-grid compact-trends">
                <TrendMini v-for="s in trendSeries" :key="s.field_id" :series="s" :title="s.display_name || s.field_id" />
              </div>
              <div v-if="!trendSeries.length" class="empty-state">等待时序数据…</div>
            </div>
          </div>
        </div>

        <details class="equip-details panel">
          <summary class="equip-summary">设备矩阵（{{ equipment.length }} 台）· 点击展开</summary>
          <div class="panel-body">
            <div class="equip-grid">
              <div
                v-for="eq in equipment"
                :key="eq.equipment_id"
                class="equip-card clickable"
                :class="statusClass(eq.latest?.values?.status)"
                @click="goTwinFromOverview(eq.process_step_id)"
              >
                <div class="eq-id">{{ eq.equipment_id }}</div>
                <div class="eq-name">{{ eq.name }}</div>
                <div class="eq-status-label">{{ eq.latest?.values?.status || '—' }}</div>
              </div>
            </div>
          </div>
        </details>
      </template>

      <!-- 孪生 -->
      <template v-else-if="activeTab === 'twin'">
        <div class="twin-page" :class="viewMode">
          <div class="twin-3d-zone panel" v-if="hasTwin3D">
            <div class="panel-head">
              <h3>3D 产线镜像</h3>
              <div class="head-actions">
                <button class="tab-btn" :class="{ active: viewMode === 'split' }" @click="viewMode = 'split'">分屏</button>
                <button class="tab-btn" :class="{ active: viewMode === 'focus3d' }" @click="viewMode = 'focus3d'">3D 聚焦</button>
              </div>
            </div>
            <div class="panel-body flush twin-3d-body">
              <LineVisualPanel
                :key="`3d-${lineId}`"
                :line-id="lineId"
                :pipeline="pipeline"
                :equipment="equipment"
                :batch="displayBatch"
                :selected-step="selectedStep"
                size="large"
                :title="''"
                @select-step="onTwinSelectStep"
              />
            </div>
          </div>

          <div class="twin-fallback panel" v-else>
            <div class="panel-body compact empty-state">3D 布局未就绪，请检查主数据 twin_layouts</div>
          </div>

          <div class="twin-layout" v-show="viewMode === 'split'">
            <div class="twin-main panel">
              <div class="panel-head"><h3>2D 拓扑</h3></div>
              <div class="panel-body flush twin-main-body">
                <LineTwinCanvas
                  :key="`2d-${lineId}`"
                  :line-id="lineId"
                  :pipeline="pipeline"
                  :equipment="equipment"
                  :batch="displayBatch"
                  :selected-step="selectedStep"
                  @select-step="onTwinSelectStep"
                />
              </div>
            </div>
            <div class="twin-side" v-if="selectedStep">
              <div class="panel step-detail-panel" v-if="stepDetail">
                <div class="panel-head">
                  <h3>{{ stepDetail.display_name || selectedStep }}</h3>
                  <span class="meta">{{ stepDetail.equipment?.length || 0 }} 台</span>
                </div>
                <div class="panel-body compact">
                  <div
                    v-for="eq in stepDetail.equipment"
                    :key="eq.equipment_id"
                    class="step-eq-row"
                    :class="{ active: selectedEquip?.equipment_id === eq.equipment_id }"
                    @click="selectedEquip = equipment.find(e => e.equipment_id === eq.equipment_id) || { equipment_id: eq.equipment_id, latest: { values: { ...eq.latest, status: eq.status } } }"
                  >
                    <span class="eq-name">{{ eq.name || eq.equipment_id }}</span>
                    <span class="line-badge" :class="stepEquipStatusClass(eq.status)">{{ eq.status }}</span>
                  </div>
                </div>
              </div>
              <div class="panel" v-if="selectedEquip">
                <div class="panel-head"><h3>{{ selectedEquip.equipment_id }}</h3></div>
                <div class="panel-body compact">
                  <table class="data-table">
                    <thead><tr><th>参数</th><th>值</th><th>规格</th></tr></thead>
                    <tbody>
                      <tr v-for="p in selectedParams" :key="p.fieldId">
                        <td>{{ p.name }}</td>
                        <td :class="valClass(p.spec)">{{ p.value }} {{ p.unit }}</td>
                        <td>{{ p.limits ? `${p.limits.lsl}~${p.limits.usl}` : '—' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <div class="panel" v-if="sparkTrend?.series?.[0]">
                <div class="panel-body flush"><TrendMini :series="sparkTrend.series[0]" spark /></div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 参数表 -->
      <template v-else-if="activeTab === 'params'">
        <div class="panel params-panel">
          <div class="panel-head">
            <h3>实时工艺参数</h3>
            <span class="meta">{{ liveRows.length }} 点</span>
          </div>
          <div class="panel-body flush">
            <div class="data-table-wrap params-table-wrap">
              <table class="data-table">
                <thead>
                  <tr><th>工序</th><th>设备</th><th>参数</th><th>值</th><th>单位</th><th>规格</th><th>更新</th></tr>
                </thead>
                <tbody>
                  <tr v-for="row in liveRows" :key="`${row.equipment_id}-${row.field_id}`">
                    <td>{{ row.process_step_id }}</td>
                    <td>{{ row.equipment_id }}</td>
                    <td>{{ row.display_name }}</td>
                    <td :class="valClass(row.spec)">{{ row.value }}</td>
                    <td>{{ row.unit }}</td>
                    <td :class="valClass(row.spec)">{{ row.spec === true ? 'OK' : row.spec === false ? 'NG' : '—' }}</td>
                    <td>{{ fmtTime(row.ts) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
    </div>

    <div class="page-body" v-else><div class="loading-state">LOADING WORKBENCH…</div></div>

    <AlarmHandleDialog
      :open="alarmHandleOpen"
      :alarm="alarmHandleTarget"
      @close="alarmHandleOpen = false"
      @done="onAlarmHandled"
    />
  </div>
</template>

<style scoped>
/* 布局见 main.css .workbench-root / .workbench-body.page-body */
.workbench-body.twin-active {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
}
.workbench-body.params-active {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.workbench-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.workbench-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.workbench-meta .line-id {
  font-size: 11px;
  color: var(--text-dim);
}
.workbench-meta .line-cat {
  font-size: 10px;
  color: var(--accent-cyan);
  background: rgba(34, 211, 238, 0.08);
  padding: 1px 6px;
  border-radius: 2px;
}
.workbench-tabs { display: flex; gap: 4px; }

.twin-page { display: flex; flex-direction: column; gap: 10px; padding-bottom: 8px; }
.twin-page.split .twin-3d-zone { min-height: min(42vh, 420px); height: clamp(320px, 42vh, 560px); flex-shrink: 0; }
.twin-page.focus3d { flex: 1; min-height: 0; }
.twin-page.focus3d .twin-3d-zone { flex: 1; min-height: clamp(360px, 55vh, 720px); }
.twin-3d-zone { display: flex; flex-direction: column; min-height: 0; }
.twin-3d-body { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.twin-3d-body :deep(.visual-panel) { flex: 1; min-height: 0; }
.twin-3d-body :deep(.visual-3d) { flex: 1; height: auto !important; min-height: 0; }
.twin-3d-body :deep(.twin3d-container) { min-height: 200px; }
.head-actions { display: flex; gap: 8px; }
.twin-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 10px; min-height: clamp(420px, 48vh, 720px); flex: 1 0 auto; }
.twin-main { min-height: 0; display: flex; flex-direction: column; }
.twin-main-body { min-height: clamp(320px, 44vh, 680px); height: 100%; }
.twin-side { overflow-y: auto; min-height: 0; display: flex; flex-direction: column; gap: 10px; }
.params-panel { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.params-panel .panel-body { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.params-table-wrap { max-height: none; flex: 1; min-height: 360px; overflow: auto; }

.overview-secondary {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1.4fr;
  gap: var(--grid-gap);
  margin-bottom: var(--grid-gap);
}
.batch-alarm-panel { min-height: 140px; }
.trend-panel .compact-trends { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.alarm-click-table .clickable-row { cursor: pointer; }
.alarm-click-table .clickable-row:hover { background: rgba(34, 211, 238, 0.06); }
.alarm-status-tag { font-size: 9px; color: var(--text-dim); margin-left: 4px; }
.tab-btn.mini { font-size: 10px; padding: 2px 6px; }
.equip-details { margin-bottom: var(--grid-gap); }
.equip-summary { cursor: pointer; padding: 10px 14px; font-size: 12px; font-weight: 600; list-style: none; }
.equip-summary::-webkit-details-marker { display: none; }
.equip-card.clickable { cursor: pointer; }
.twin-layout { min-height: clamp(360px, 42vh, 640px); }
.step-detail-panel .step-eq-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 4px; font-size: 12px; cursor: pointer; border-radius: 3px;
}
.step-detail-panel .step-eq-row:hover,
.step-detail-panel .step-eq-row.active { background: rgba(34, 211, 238, 0.08); }
.step-detail-panel .eq-name { color: var(--text-secondary); }

@media (max-width: 1100px) {
  .overview-secondary { grid-template-columns: 1fr; }
  .twin-layout { grid-template-columns: 1fr; min-height: auto; }
  .twin-page.split .twin-3d-zone { height: clamp(280px, 38vh, 420px); }
  .twin-main-body { min-height: 300px; }
  .params-table-wrap { min-height: 280px; }
}
</style>
