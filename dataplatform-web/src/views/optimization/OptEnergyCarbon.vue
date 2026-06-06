<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import {
  getOptEnergyCarbon,
  projectOptGreenShift,
  getOptEnergyCarbonBatches,
  applyEnergyCarbonScenario
} from '../../api/optimization'
import { usePolling } from '../../composables/usePolling'
import { fmtDelta } from '../../utils/format'
import {
  EC_TABS,
  EC_PRINCIPLE_SECTIONS,
  REC_CATEGORIES,
  ANOMALY_LABELS,
  ASSET_TYPE_COLORS,
  BATCH_FOOTPRINT_GUIDE,
  ENERGY_SCENARIO_IDS
} from '../../data/energyCarbonCatalog'

const router = useRouter()
const route = useRoute()
const activeTab = ref('overview')
const data = ref(null)
const loading = ref(true)
const greenShift = ref(50)
const greenProj = ref(null)
const greenLoading = ref(false)
const lineFilter = ref('')
const workshopFilter = ref('')
const batchData = ref(null)
const applyingScenario = ref(false)

let greenDebounce = null

onMounted(() => {
  if (route.query.tab && EC_TABS.some(t => t.key === route.query.tab)) {
    activeTab.value = route.query.tab
  }
  if (route.query.green_shift != null) {
    greenShift.value = Number(route.query.green_shift) || 50
  }
})

watch(activeTab, (tab) => {
  if (tab === 'batches' && !batchData.value) loadBatches()
})

async function loadOverview() {
  loading.value = true
  try {
    data.value = await getOptEnergyCarbon()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

usePolling(loadOverview, 10000)
loadOverview()

async function loadGreenProjection() {
  greenLoading.value = true
  try {
    greenProj.value = await projectOptGreenShift(greenShift.value)
  } catch (e) {
    console.error(e)
  } finally {
    greenLoading.value = false
  }
}

function scheduleGreenProj() {
  clearTimeout(greenDebounce)
  greenDebounce = setTimeout(loadGreenProjection, 350)
}

watch(greenShift, scheduleGreenProj)
loadGreenProjection()

const headline = computed(() => data.value?.headline || {})
const supply = computed(() => data.value?.supply || {})
const demand = computed(() => data.value?.demand || {})
const carbon = computed(() => data.value?.carbon || {})
const recommendations = computed(() => data.value?.recommendations || [])
const energyScenarios = computed(() => data.value?.energy_scenarios || [])
const batchSummary = computed(() => batchData.value?.summary || {})
const batchRecords = computed(() => batchData.value?.records || [])
const hourlyCurve = computed(() => supply.value?.hourly_green_curve || [])
const maxCurvePct = computed(() => Math.max(...hourlyCurve.value.map(p => p.green_power_ratio_pct || 0), 60))

const filteredLines = computed(() => {
  let rows = demand.value?.line_breakdown || []
  if (workshopFilter.value) {
    rows = rows.filter(r => r.workshop_id === workshopFilter.value)
  }
  const q = lineFilter.value.trim().toLowerCase()
  if (q) {
    rows = rows.filter(r =>
      String(r.product_line_id || '').toLowerCase().includes(q) ||
      String(r.line_name || '').toLowerCase().includes(q)
    )
  }
  return rows
})

function curveHeight(pct) {
  return `${Math.max(4, (pct / maxCurvePct.value) * 100)}%`
}

function recCat(key) {
  return REC_CATEGORIES[key] || REC_CATEGORIES.energy
}

async function loadBatches() {
  try {
    batchData.value = await getOptEnergyCarbonBatches()
  } catch (e) {
    console.error(e)
  }
}

function goBalanceWithShift() {
  router.push({
    path: '/optimization/balance',
    query: { green_shift_pct: greenShift.value, from: 'energy-carbon' }
  })
}

function goScenariosWith(id, autorun = false) {
  const query = { scenario: id, from: 'energy-carbon' }
  if (autorun) query.autorun = '1'
  router.push({ path: '/optimization/scenarios', query })
}

async function applyEnergyScenario(scenarioId) {
  const meta = ENERGY_SCENARIO_IDS[scenarioId]
  applyingScenario.value = true
  try {
    await applyEnergyCarbonScenario({
      scenario_id: scenarioId,
      green_shift_pct: meta?.greenShift ?? greenShift.value
    })
    goScenariosWith(scenarioId, true)
  } catch (e) {
    console.error(e)
  } finally {
    applyingScenario.value = false
  }
}

function syncGreenShiftFromSlider(pct) {
  greenShift.value = pct
}

function mixPct(kwh) {
  const total = supply.value?.generation_mix?.total_kwh || 1
  return `${Math.min(100, (Number(kwh) / total) * 100)}%`
}
</script>

<template>
  <div class="page">
    <PageHeader section="智优决策中心" title="能碳决策" />

    <div class="content">
      <div class="panel status-bar">
        <div class="status-main">
          <span class="dot" :class="{ on: headline.green_window_active }" />
          {{ headline.green_window_active ? '绿电窗口进行中' : '非绿电峰值时段' }}
          <span class="mono dim">· 绿电 {{ headline.green_power_ratio_pct ?? '—' }}%</span>
          <span class="mono dim">· 碳强 {{ headline.carbon_intensity_kg_per_t ?? '—' }} kg/t</span>
        </div>
        <div class="tab-nav">
          <button
            v-for="tab in EC_TABS"
            :key="tab.key"
            type="button"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div class="panel guide-panel">
        <div class="panel-title">使用说明</div>
        <div class="guide-grid">
          <section v-for="sec in EC_PRINCIPLE_SECTIONS" :key="sec.title" class="guide-block">
            <h4>{{ sec.title }}</h4>
            <p>{{ sec.body }}</p>
          </section>
        </div>
      </div>

      <!-- Tab 1: 能碳总览 -->
      <template v-if="activeTab === 'overview'">
        <div class="kpi-grid">
          <div class="kpi-card energy">
            <div class="kpi-label">实时总能耗</div>
            <div class="kpi-value">{{ headline.total_consumption_kwh ?? '—' }}</div>
            <div class="kpi-unit">kWh</div>
          </div>
          <div class="kpi-card energy">
            <div class="kpi-label">实时功率</div>
            <div class="kpi-value">{{ headline.total_active_power_kw ?? '—' }}</div>
            <div class="kpi-unit">kW</div>
          </div>
          <div class="kpi-card green">
            <div class="kpi-label">绿电占比</div>
            <div class="kpi-value">{{ headline.green_power_ratio_pct ?? '—' }}</div>
            <div class="kpi-unit">%</div>
          </div>
          <div class="kpi-card grid">
            <div class="kpi-label">电网购电</div>
            <div class="kpi-value">{{ headline.grid_power_kwh ?? '—' }}</div>
            <div class="kpi-unit">kWh</div>
          </div>
          <div class="kpi-card carbon">
            <div class="kpi-label">净碳排</div>
            <div class="kpi-value">{{ carbon.net_kg_co2e ?? '—' }}</div>
            <div class="kpi-unit">kg CO₂e</div>
          </div>
          <div class="kpi-card carbon">
            <div class="kpi-label">碳排强度</div>
            <div class="kpi-value">{{ headline.carbon_intensity_kg_per_t ?? '—' }}</div>
            <div class="kpi-unit">kg/t</div>
          </div>
        </div>

        <div class="grid-2">
          <div class="panel">
            <div class="panel-title">年减碳进度</div>
            <div class="progress-block">
              <div class="progress-head">
                <span>目标 {{ headline.annual_carbon_reduction_target_t ?? 35000 }} t/年</span>
                <span class="mono">已完成 {{ headline.annual_carbon_reduction_progress_pct ?? 0 }}%</span>
              </div>
              <div class="progress-bar">
                <div
                  class="progress-fill"
                  :style="{ width: (headline.annual_carbon_reduction_progress_pct || 0) + '%' }"
                />
              </div>
              <p class="panel-hint">
                折算年内进度约 {{ headline.annual_carbon_reduction_ytd_t ?? '—' }} t（含余热回收等设施贡献叙事）
              </p>
            </div>
            <div class="workshop-split">
              <div class="ws-item">
                <span class="ws-label">焊丝车间</span>
                <span class="ws-val">{{ headline.wire_shop_kwh ?? '—' }} kWh</span>
              </div>
              <div class="ws-item">
                <span class="ws-label">焊条车间</span>
                <span class="ws-val">{{ headline.rod_shop_kwh ?? '—' }} kWh</span>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-title">24h 绿电可覆盖曲线</div>
            <p class="panel-hint">柱状高度 = 该小时绿电占比估算；琥珀色区域为推荐排产窗口 11:00–14:00</p>
            <div class="curve-chart">
              <div
                v-for="p in hourlyCurve"
                :key="p.hour"
                class="curve-col"
                :class="{ window: p.green_window }"
                :title="`${p.label} 绿电约 ${p.green_power_ratio_pct}%`"
              >
                <div class="curve-bar" :style="{ height: curveHeight(p.green_power_ratio_pct) }" />
                <span v-if="p.hour % 3 === 0" class="curve-label">{{ p.hour }}</span>
              </div>
            </div>
            <p v-if="headline.green_window_hint" class="window-hint" :class="{ active: headline.green_window_active }">
              {{ headline.green_window_hint }}
            </p>
          </div>
        </div>

        <div class="panel carbon-panel">
          <div class="panel-title">碳排核算摘要</div>
          <div class="carbon-grid">
            <div class="carbon-item">
              <span class="ci-label">Scope2（外购电力）</span>
              <span class="ci-val">{{ carbon.scope2_kg_co2e ?? '—' }} kg</span>
            </div>
            <div class="carbon-item offset">
              <span class="ci-label">绿电抵扣</span>
              <span class="ci-val">−{{ carbon.green_power_offset_kg_co2e ?? '—' }} kg</span>
            </div>
            <div class="carbon-item net">
              <span class="ci-label">净碳排</span>
              <span class="ci-val">{{ carbon.net_kg_co2e ?? '—' }} kg</span>
            </div>
            <div class="carbon-item">
              <span class="ci-label">估算产量</span>
              <span class="ci-val">{{ carbon.estimated_output_t ?? '—' }} t</span>
            </div>
          </div>
          <p class="panel-hint">{{ data?.methodology?.note }}</p>
        </div>
      </template>

      <!-- Tab 2: 能源结构 -->
      <template v-if="activeTab === 'supply'">
        <div class="panel">
          <div class="panel-title">能源资产</div>
          <p class="panel-hint">供给侧装机与年发电能力（来自主数据 energy_assets）</p>
          <div class="asset-grid">
            <div
              v-for="a in supply.assets || []"
              :key="a.asset_id"
              class="asset-card"
              :class="`asset-${ASSET_TYPE_COLORS[a.asset_type] || 'cyan'}`"
            >
              <div class="asset-type">{{ a.type_label || a.asset_type }}</div>
              <div class="asset-name">{{ a.name }}</div>
              <div class="asset-kw">{{ a.rated_power_kw }} kW 装机</div>
              <div v-if="a.annual_generation_kwh" class="asset-gen">
                年发电 {{ (a.annual_generation_kwh / 10000).toFixed(0) }} 万 kWh
              </div>
              <div v-if="a.energy_saving_pct" class="asset-save">节能 {{ a.energy_saving_pct }}%</div>
              <div v-if="a.annual_carbon_reduction_t" class="asset-carbon">
                年减碳 {{ (a.annual_carbon_reduction_t / 10000).toFixed(1) }} 万吨
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">当前用能结构（需求侧拆分）</div>
          <div class="mix-bars">
            <div class="mix-row">
              <span class="mix-label">光伏消纳</span>
              <div class="mix-track"><div class="mix-fill pv" :style="{ width: mixPct(supply.generation_mix?.pv_kwh) }" /></div>
              <span class="mix-val">{{ supply.generation_mix?.pv_kwh ?? 0 }} kWh</span>
            </div>
            <div class="mix-row">
              <span class="mix-label">风电消纳</span>
              <div class="mix-track"><div class="mix-fill wind" :style="{ width: mixPct(supply.generation_mix?.wind_kwh) }" /></div>
              <span class="mix-val">{{ supply.generation_mix?.wind_kwh ?? 0 }} kWh</span>
            </div>
            <div class="mix-row">
              <span class="mix-label">余热节约</span>
              <div class="mix-track"><div class="mix-fill whr" :style="{ width: mixPct(supply.generation_mix?.waste_heat_saving_kwh) }" /></div>
              <span class="mix-val">{{ supply.generation_mix?.waste_heat_saving_kwh ?? 0 }} kWh</span>
            </div>
            <div class="mix-row">
              <span class="mix-label">电网购电</span>
              <div class="mix-track"><div class="mix-fill grid" :style="{ width: mixPct(supply.generation_mix?.grid_kwh) }" /></div>
              <span class="mix-val">{{ supply.generation_mix?.grid_kwh ?? 0 }} kWh</span>
            </div>
          </div>
          <p class="panel-hint">
            年清洁发电能力 {{ supply.annual_clean_generation_kwh ? (supply.annual_clean_generation_kwh / 10000).toFixed(0) + ' 万 kWh' : '—' }}
          </p>
        </div>
      </template>

      <!-- Tab 3: 用能诊断 -->
      <template v-if="activeTab === 'demand'">
        <div class="panel">
          <div class="panel-title">车间能耗分布</div>
          <div class="ws-bars">
            <div v-for="ws in demand.workshop_breakdown || []" :key="ws.workshop_id" class="ws-bar-row">
              <span class="ws-name">{{ ws.workshop_name }}</span>
              <div class="ws-track">
                <div class="ws-fill" :style="{ width: (ws.share_pct || 0) + '%' }" />
              </div>
              <span class="ws-pct">{{ ws.share_pct }}%</span>
              <span class="ws-kwh">{{ ws.consumption_kwh }} kWh</span>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">高耗能工序榜</div>
          <p class="panel-hint">按全厂能耗份额估算（启发式）；标注「绿电窗口」的工序建议排入 11:00–14:00</p>
          <div class="process-list">
            <div v-for="p in demand.process_ranking || []" :key="p.step_id" class="process-row">
              <span class="proc-name">{{ p.label }}</span>
              <span class="proc-kwh">{{ p.consumption_kwh }} kWh</span>
              <span class="proc-share">{{ p.share_pct }}%</span>
              <span v-if="p.green_window_recommended" class="proc-tag">绿电窗口</span>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">42 线用能诊断</div>
          <div class="table-filters">
            <select v-model="workshopFilter" class="filter-select">
              <option value="">全部车间</option>
              <option value="WS-WIRE-01">焊丝车间</option>
              <option value="WS-ROD-01">焊条车间</option>
            </select>
            <input v-model="lineFilter" type="search" placeholder="搜索产线…" class="filter-input" />
            <span class="filter-count">{{ filteredLines.length }} 线</span>
          </div>
          <div class="line-table">
            <div class="line-head">
              <span>产线</span><span>车间</span><span>能耗</span><span>功率</span><span>单吨</span><span>状态</span>
            </div>
            <div v-for="row in filteredLines" :key="row.product_line_id" class="line-row">
              <span class="mono line-id">{{ row.product_line_id }}</span>
              <span class="ws-tag">{{ row.workshop_name }}</span>
              <span class="mono">{{ row.consumption_kwh }} kWh</span>
              <span class="mono">{{ row.active_power_kw }} kW</span>
              <span class="mono">{{ row.specific_energy_kwh_per_t }}</span>
              <span class="flags">
                <span
                  v-for="f in row.anomalies || []"
                  :key="f"
                  class="flag"
                  :class="`flag-${ANOMALY_LABELS[f]?.color || 'amber'}`"
                >
                  {{ ANOMALY_LABELS[f]?.label || f }}
                </span>
                <span v-if="!(row.anomalies || []).length" class="flag-ok">正常</span>
              </span>
            </div>
          </div>
        </div>
      </template>

      <!-- Tab 4: 批次碳足迹 -->
      <template v-if="activeTab === 'batches'">
        <div class="panel">
          <div class="panel-title">批次碳足迹</div>
          <p class="panel-hint">{{ BATCH_FOOTPRINT_GUIDE }}</p>
          <div v-if="batchSummary.batch_count" class="batch-summary">
            <span>样本 <b>{{ batchSummary.batch_count }}</b> 批</span>
            <span>合计产量 <b>{{ batchSummary.total_quantity_t }}</b> t</span>
            <span>净碳排 <b>{{ batchSummary.total_net_kg_co2e }}</b> kg</span>
            <span>均强度 <b>{{ batchSummary.avg_intensity_kg_per_t }}</b> kg/t</span>
          </div>
          <div class="batch-table">
            <div class="batch-head">
              <span>批次</span><span>产线</span><span>产量</span><span>能耗</span><span>净碳排</span><span>强度</span><span>状态</span>
            </div>
            <div v-for="b in batchRecords" :key="b.batch_id" class="batch-row">
              <span class="mono">{{ b.batch_id }}</span>
              <span class="mono dim">{{ b.product_line_id }}</span>
              <span class="mono">{{ b.quantity_t }} t</span>
              <span class="mono">{{ b.energy_kwh }} kWh</span>
              <span class="mono carbon-val">{{ b.total_kg_co2e }} kg</span>
              <span class="mono">{{ b.carbon_intensity_kg_per_t }} kg/t</span>
              <span class="batch-status">{{ b.status }}</span>
            </div>
          </div>
          <div v-if="!batchRecords.length" class="empty-rec">加载批次数据…</div>
        </div>
      </template>

      <!-- Tab 5: 能碳优化 -->
      <template v-if="activeTab === 'optimize'">
        <div class="panel linkage-panel">
          <div class="panel-title">深度联动</div>
          <p class="panel-hint">纸面推演 → 平衡点全杠杆验证 → 能碳场景仿真对比，三步闭环</p>
          <div class="linkage-steps">
            <div class="link-step">
              <span class="step-num">1</span>
              <span>本页快调绿电偏移 {{ greenShift }}%</span>
            </div>
            <div class="link-step">
              <span class="step-num">2</span>
              <button type="button" class="link-btn sm" @click="goBalanceWithShift">平衡点验证四杠杆 →</button>
            </div>
            <div class="link-step">
              <span class="step-num">3</span>
              <span>应用能碳场景并自动对比</span>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">能碳专用场景</div>
          <p class="panel-hint">切换模拟器运行态并跳转场景实验室自动对比（含 green_shift_pct 参数）</p>
          <div class="ec-scenario-grid">
            <div v-for="s in energyScenarios" :key="s.id" class="ec-scenario-card">
              <div class="es-id">{{ s.id }}</div>
              <div class="es-label">{{ s.label }}</div>
              <div class="es-desc">{{ s.description }}</div>
              <button
                type="button"
                class="link-btn scenario full"
                :disabled="applyingScenario"
                @click="applyEnergyScenario(s.id)"
              >
                {{ applyingScenario ? '应用中…' : '应用并对比' }}
              </button>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">绿电排产偏移快调</div>
          <p class="panel-hint">单杠杆纸面推演（不下发模拟器）。全量四杠杆请用「平衡点分析」。</p>
          <div class="green-shift-block">
            <div class="shift-head">
              <span>绿电排产偏移</span>
              <span class="shift-val">{{ greenShift }}%</span>
            </div>
            <input v-model.number="greenShift" type="range" min="0" max="100" step="5" class="shift-slider" />
            <div v-if="greenProj" class="shift-result" :class="{ loading: greenLoading }">
              <div class="sr-row">
                <span>绿电占比</span>
                <span class="mono">
                  {{ greenProj.baseline?.green_power_ratio_pct }}% → {{ greenProj.projected?.green_power_ratio_pct }}%
                  ({{ fmtDelta(greenProj.delta?.green_power_ratio_pct) }})
                </span>
              </div>
              <div class="sr-row">
                <span>净碳排</span>
                <span class="mono down">
                  {{ greenProj.baseline?.net_kg_co2e }} → {{ greenProj.projected?.net_kg_co2e }} kg
                  ({{ fmtDelta(greenProj.delta?.net_kg_co2e) }})
                </span>
              </div>
              <div class="sr-row">
                <span>成本指数</span>
                <span class="mono">
                  {{ greenProj.baseline?.cost_index }} → {{ greenProj.projected?.cost_index }}
                  ({{ fmtDelta(greenProj.delta?.cost_index) }})
                </span>
              </div>
              <p class="shift-hint">{{ greenProj.hint }}</p>
            </div>
          </div>
          <div class="quick-pct">
            <button type="button" class="pct-btn" @click="syncGreenShiftFromSlider(60)">60%</button>
            <button type="button" class="pct-btn" @click="syncGreenShiftFromSlider(80)">80%</button>
          </div>
          <div class="link-row">
            <button type="button" class="link-btn" @click="goBalanceWithShift">平衡点验证（带当前偏移）→</button>
            <button type="button" class="link-btn scenario" @click="goScenariosWith('clean_energy_noon')">午间绿电场景 →</button>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">能碳优化建议</div>
          <div class="rec-list">
            <div
              v-for="(r, i) in recommendations"
              :key="i"
              class="rec-card"
              :class="`rec-${recCat(r.category).color}`"
            >
              <div class="rec-top">
                <span class="rec-cat">{{ recCat(r.category).label }}</span>
                <span class="rec-pri" :class="r.priority">{{ r.priority === 'high' ? '高' : '中' }}</span>
              </div>
              <div class="rec-title">{{ r.title }}</div>
              <div class="rec-detail">{{ r.detail || r.message }}</div>
            </div>
          </div>
          <div v-if="!recommendations.length" class="empty-rec">暂无建议，请确认模拟器与 API 已启动</div>
        </div>
      </template>

      <div v-if="loading && !data" class="loading-overlay">加载能碳数据…</div>
    </div>
  </div>
</template>

<style scoped>
.panel {
  background: var(--bg-panel);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  padding: 12px 14px;
  margin-bottom: 12px;
}
.panel-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.panel-hint { font-size: 10px; color: var(--text-dim); margin: 0 0 10px; line-height: 1.45; }

.status-bar {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 10px;
}
.status-main { font-size: 12px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-dim); flex-shrink: 0; }
.dot.on { background: var(--accent-amber); box-shadow: 0 0 8px rgba(251, 191, 36, 0.4); }
.mono { font-family: var(--font-mono); }
.dim { color: var(--text-dim); }

.tab-nav { display: flex; gap: 4px; flex-wrap: wrap; }
.tab-btn {
  font-size: 11px; padding: 5px 12px; border-radius: 3px;
  border: 1px solid var(--border-dim); background: transparent;
  color: var(--text-secondary); cursor: pointer;
}
.tab-btn.active {
  border-color: var(--accent-amber); color: var(--accent-amber);
  background: rgba(251, 191, 36, 0.1);
}

.guide-panel { border-color: rgba(251, 191, 36, 0.25); }
.guide-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 12px;
}
.guide-block h4 { font-size: 12px; font-weight: 600; margin: 0 0 6px; }
.guide-block p { font-size: 11px; line-height: 1.55; margin: 0; color: var(--text-secondary); }

.kpi-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px; margin-bottom: 12px;
}
.kpi-card {
  padding: 12px; border-radius: var(--radius); border: 1px solid var(--border-dim);
  background: var(--bg-panel-elevated); text-align: center;
  border-top-width: 3px;
}
.kpi-card.energy { border-top-color: var(--accent-amber); }
.kpi-card.green { border-top-color: var(--status-running); }
.kpi-card.grid { border-top-color: var(--status-alarm); }
.kpi-card.carbon { border-top-color: var(--accent-teal); }
.kpi-label { font-size: 10px; color: var(--text-dim); }
.kpi-value { font-size: 22px; font-weight: 700; font-family: var(--font-mono); margin: 4px 0; }
.kpi-unit { font-size: 10px; color: var(--text-secondary); }

.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 900px) { .grid-2 { grid-template-columns: 1fr; } }

.progress-head { display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 6px; }
.progress-bar { height: 8px; background: var(--border-dim); border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--accent-teal); border-radius: 4px; transition: width 0.3s; }
.workshop-split { display: flex; gap: 16px; margin-top: 12px; font-size: 11px; }
.ws-label { color: var(--text-dim); margin-right: 6px; }
.ws-val { font-family: var(--font-mono); color: var(--accent-amber); }

.curve-chart {
  display: flex; align-items: flex-end; gap: 2px; height: 120px;
  padding: 8px 4px 20px; position: relative;
}
.curve-col {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  height: 100%; justify-content: flex-end; position: relative;
}
.curve-col.window { background: rgba(251, 191, 36, 0.06); border-radius: 2px; }
.curve-bar {
  width: 100%; max-width: 14px; background: var(--status-running);
  border-radius: 2px 2px 0 0; min-height: 4px;
}
.curve-col.window .curve-bar { background: var(--accent-amber); }
.curve-label { font-size: 8px; color: var(--text-dim); margin-top: 4px; font-family: var(--font-mono); }
.window-hint { font-size: 11px; color: var(--text-dim); margin: 8px 0 0; }
.window-hint.active { color: var(--accent-amber); font-weight: 500; }

.carbon-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px; }
.carbon-item { padding: 10px; background: var(--bg-panel-elevated); border-radius: var(--radius); }
.carbon-item.offset .ci-val { color: var(--status-running); }
.carbon-item.net .ci-val { color: var(--accent-teal); font-weight: 700; }
.ci-label { display: block; font-size: 10px; color: var(--text-dim); margin-bottom: 4px; }
.ci-val { font-size: 14px; font-family: var(--font-mono); }

.asset-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }
.asset-card {
  padding: 12px; border-radius: var(--radius); border: 1px solid var(--border-dim);
  border-left-width: 3px; background: var(--bg-panel-elevated);
}
.asset-amber { border-left-color: var(--accent-amber); }
.asset-cyan { border-left-color: var(--accent-cyan); }
.asset-red { border-left-color: var(--status-alarm); }
.asset-teal { border-left-color: var(--accent-teal); }
.asset-type { font-size: 10px; font-weight: 600; color: var(--text-dim); }
.asset-name { font-size: 13px; font-weight: 600; margin: 4px 0; }
.asset-kw, .asset-gen, .asset-save, .asset-carbon { font-size: 10px; color: var(--text-secondary); margin-top: 3px; }
.asset-carbon { color: var(--accent-teal); }

.mix-bars { display: flex; flex-direction: column; gap: 8px; }
.mix-row { display: grid; grid-template-columns: 72px 1fr 72px; gap: 8px; align-items: center; font-size: 11px; }
.mix-track { height: 8px; background: var(--border-dim); border-radius: 4px; overflow: hidden; }
.mix-fill { height: 100%; border-radius: 4px; }
.mix-fill.pv { background: var(--accent-amber); }
.mix-fill.wind { background: var(--accent-cyan); }
.mix-fill.whr { background: var(--accent-teal); }
.mix-fill.grid { background: var(--status-alarm); }
.mix-val { font-family: var(--font-mono); text-align: right; color: var(--text-dim); }

.ws-bars { display: flex; flex-direction: column; gap: 10px; }
.ws-bar-row { display: grid; grid-template-columns: 120px 1fr 48px 80px; gap: 8px; align-items: center; font-size: 11px; }
.ws-track { height: 10px; background: var(--border-dim); border-radius: 4px; overflow: hidden; }
.ws-fill { height: 100%; background: var(--accent-amber); border-radius: 4px; }
.ws-pct { font-family: var(--font-mono); color: var(--text-dim); }
.ws-kwh { font-family: var(--font-mono); text-align: right; }

.process-list { display: flex; flex-direction: column; gap: 6px; }
.process-row {
  display: flex; align-items: center; gap: 10px; font-size: 11px;
  padding: 8px 10px; background: var(--bg-panel-elevated); border-radius: var(--radius);
}
.proc-name { font-weight: 600; flex: 1; }
.proc-kwh, .proc-share { font-family: var(--font-mono); color: var(--text-dim); }
.proc-tag {
  font-size: 9px; padding: 2px 6px; border-radius: 2px;
  color: var(--accent-amber); background: rgba(251, 191, 36, 0.12);
}

.table-filters { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; flex-wrap: wrap; }
.filter-select, .filter-input {
  font-size: 11px; padding: 5px 8px; border-radius: 3px;
  border: 1px solid var(--border-dim); background: var(--bg-panel-elevated); color: var(--text-primary);
}
.filter-count { font-size: 10px; color: var(--text-dim); }

.line-table { font-size: 10px; }
.line-head, .line-row {
  display: grid; grid-template-columns: 1.2fr 1fr 0.8fr 0.7fr 0.6fr 1.2fr;
  gap: 6px; padding: 6px 4px; align-items: center;
}
.line-head { font-weight: 600; color: var(--text-dim); border-bottom: 1px solid var(--border-dim); }
.line-row { border-bottom: 1px solid rgba(255,255,255,0.04); }
.line-row:hover { background: rgba(255,255,255,0.02); }
.line-id { color: var(--accent-cyan); }
.ws-tag { color: var(--text-secondary); }
.flags { display: flex; flex-wrap: wrap; gap: 3px; }
.flag { font-size: 9px; padding: 1px 5px; border-radius: 2px; }
.flag-red { color: var(--status-alarm); background: rgba(248, 113, 113, 0.12); }
.flag-amber { color: var(--accent-amber); background: rgba(251, 191, 36, 0.12); }
.flag-cyan { color: var(--accent-cyan); background: rgba(34, 211, 238, 0.1); }
.flag-ok { color: var(--status-running); font-size: 9px; }

.green-shift-block { padding: 10px; background: var(--bg-panel-elevated); border-radius: var(--radius); }
.shift-head { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 8px; }
.shift-val { font-family: var(--font-mono); color: var(--accent-amber); font-weight: 600; }
.shift-slider { width: 100%; accent-color: var(--accent-amber); margin-bottom: 12px; }
.shift-result { font-size: 11px; }
.shift-result.loading { opacity: 0.6; }
.sr-row { display: flex; justify-content: space-between; margin-bottom: 6px; color: var(--text-secondary); }
.sr-row .down { color: var(--status-running); }
.shift-hint { font-size: 10px; color: var(--text-dim); margin: 8px 0 0; padding-top: 8px; border-top: 1px solid var(--border-dim); }

.link-row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.link-btn {
  font-size: 11px; padding: 6px 12px; border-radius: 3px; cursor: pointer;
  border: 1px solid var(--accent-cyan); background: rgba(34, 211, 238, 0.08); color: var(--accent-cyan);
}
.link-btn.scenario { border-color: var(--status-running); color: var(--status-running); background: rgba(52, 211, 153, 0.08); }

.rec-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 8px; }
.rec-card { padding: 10px 12px; border-radius: var(--radius); border: 1px solid var(--border-dim); border-left-width: 3px; }
.rec-amber { border-left-color: var(--accent-amber); }
.rec-teal { border-left-color: var(--accent-teal); }
.rec-cyan { border-left-color: var(--accent-cyan); }
.rec-top { display: flex; justify-content: space-between; margin-bottom: 4px; }
.rec-cat { font-size: 9px; font-weight: 600; color: var(--text-dim); }
.rec-pri { font-size: 9px; padding: 1px 5px; border-radius: 2px; }
.rec-pri.high { color: var(--status-alarm); background: rgba(248, 113, 113, 0.12); }
.rec-pri.medium { color: var(--accent-amber); background: rgba(251, 191, 36, 0.12); }
.rec-title { font-size: 12px; font-weight: 600; margin-bottom: 4px; }
.rec-detail { font-size: 10px; color: var(--text-secondary); line-height: 1.45; }
.empty-rec { text-align: center; color: var(--text-dim); padding: 16px; font-size: 12px; }

.loading-overlay { text-align: center; padding: 24px; color: var(--text-dim); font-size: 12px; }

.batch-summary {
  display: flex; flex-wrap: wrap; gap: 14px; font-size: 11px;
  margin-bottom: 12px; padding: 8px 10px; background: var(--bg-panel-elevated); border-radius: var(--radius);
}
.batch-summary b { font-family: var(--font-mono); color: var(--accent-teal); }
.batch-table { font-size: 10px; }
.batch-head, .batch-row {
  display: grid; grid-template-columns: 1.1fr 0.9fr 0.6fr 0.7fr 0.7fr 0.7fr 0.6fr;
  gap: 6px; padding: 6px 4px; align-items: center;
}
.batch-head { font-weight: 600; color: var(--text-dim); border-bottom: 1px solid var(--border-dim); }
.batch-row { border-bottom: 1px solid rgba(255,255,255,0.04); }
.carbon-val { color: var(--accent-teal); }
.batch-status { font-size: 9px; color: var(--text-dim); }

.linkage-panel { border-color: rgba(45, 212, 191, 0.3); }
.linkage-steps { display: flex; flex-direction: column; gap: 8px; }
.link-step {
  display: flex; align-items: center; gap: 10px; font-size: 11px; color: var(--text-secondary);
}
.step-num {
  width: 22px; height: 22px; border-radius: 50%; background: rgba(45, 212, 191, 0.15);
  color: var(--accent-teal); font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.link-btn.sm { font-size: 10px; padding: 4px 10px; }

.ec-scenario-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 10px; }
.ec-scenario-card {
  padding: 12px; border-radius: var(--radius); border: 1px solid var(--border-dim);
  border-left: 3px solid var(--accent-teal); background: var(--bg-panel-elevated);
}
.es-id { font-family: var(--font-mono); font-size: 10px; color: var(--accent-teal); }
.es-label { font-size: 13px; font-weight: 600; margin: 4px 0; }
.es-desc { font-size: 10px; color: var(--text-secondary); min-height: 2.4em; margin-bottom: 8px; }
.link-btn.full { width: 100%; }

.quick-pct { display: flex; gap: 6px; margin-bottom: 10px; }
.pct-btn {
  font-size: 10px; padding: 4px 10px; border-radius: 3px; cursor: pointer;
  border: 1px solid var(--accent-amber); background: rgba(251, 191, 36, 0.08); color: var(--accent-amber);
}
</style>
