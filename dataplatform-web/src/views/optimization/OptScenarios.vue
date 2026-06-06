<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import OptIndexGauge from '../../components/optimization/OptIndexGauge.vue'
import {
  getOptScenarios,
  getOptSimStatus,
  applyOptScenario,
  applyOptScenarioWithParams,
  applySimScenarioDirect,
  getOptKpiSnapshot,
  compareOptSnapshots
} from '../../api/optimization'
import { usePolling } from '../../composables/usePolling'
import { fmtDelta } from '../../utils/format'
import {
  SCENARIO_CATEGORIES,
  SCENARIO_GROUPS,
  enrichScenarios,
  kpiHintClass
} from '../../data/scenarioCatalog'

const scenarios = ref([])
const simStatus = ref(null)
const applying = ref(false)
const activeScenarioId = ref(null)
const pendingScenarioId = ref(null)
const expandedId = ref(null)
const compareResult = ref(null)
const workflowStep = ref('idle')
const failedAtStep = ref('')
const workflowError = ref('')
const waitRemaining = ref(0)
const comparePanelRef = ref(null)
const route = useRoute()
const router = useRouter()
const fromEnergy = computed(() => route.query.from === 'energy-carbon')
const autorunDone = ref(false)

const WORKFLOW_STEPS = [
  { key: 'baseline', label: '捕获基线' },
  { key: 'switch', label: '切换场景' },
  { key: 'wait', label: '等待稳定' },
  { key: 'compare', label: '对比完成' }
]

const PRINCIPLE_SECTIONS = [
  {
    title: '场景实验室做什么',
    body: '在 line-simulator 全厂 42 线并行仿真环境中，一键切换预置工况，自动对比切换前后的效率 · 成本 · 质量三极致指数，量化「若工厂进入该态势，指标会变化多少」。'
  },
  {
    title: '与能碳/平衡点的联动',
    body: '能碳模块可带 scenario 与 autorun=1 跳转本页自动对比；plant_green_shift_* 场景会附带 green_shift_pct 下发模拟器。对比完成后可回跳能碳模块查看绿电 Δ。'
  },
  {
    title: '对比流程',
    body: '① 捕获当前场景 KPI 基线 → ② POST 切换场景至模拟器 → ③ 等待约 4s 态势收敛 → ④ 再采样并计算 Δ（含 OEE、合格率、绿电占比等 headline 指标）。'
  }
]

function scenarioApplyParams(scenarioId) {
  if (!scenarioId?.startsWith('plant_green_shift_')) return {}
  const pct = parseInt(String(scenarioId).split('_').pop(), 10)
  return Number.isNaN(pct) ? {} : { green_shift_pct: pct }
}

async function callApplyScenario(scenarioId) {
  const extra = scenarioApplyParams(scenarioId)
  if (Object.keys(extra).length) {
    return applyOptScenarioWithParams(scenarioId, extra)
  }
  return applyOptScenario(scenarioId)
}

async function loadMeta() {
  const [list, status] = await Promise.all([
    getOptScenarios().catch(() => []),
    getOptSimStatus().catch(() => null)
  ])
  scenarios.value = enrichScenarios(list)
  simStatus.value = status
  if (status?.scenario_id) activeScenarioId.value = status.scenario_id
}

usePolling(loadMeta, 10000)

onMounted(() => {
  tryAutorunFromQuery()
})

watch(scenarios, () => {
  tryAutorunFromQuery()
})

function tryAutorunFromQuery() {
  const targetId = route.query.scenario
  if (!targetId || autorunDone.value || applying.value) return
  const target = scenarios.value.find(s => s.id === targetId)
  if (!target) return
  if (route.query.autorun === '1') {
    autorunDone.value = true
    expandedId.value = targetId
    applyAndCompare(target)
  } else {
    activeScenarioId.value = targetId
    expandedId.value = targetId
  }
}

function backToEnergyCarbon() {
  router.push({
    path: '/optimization/energy-carbon',
    query: { tab: 'optimize' }
  })
}

const isEnergyScenario = computed(() => {
  const id = compareResult.value?.scenario_to || activeScenarioId.value
  return id && (id.startsWith('plant_green_shift_') || id === 'clean_energy_noon')
})

function groupScenarios(group) {
  return scenarios.value.filter(s => group.ids.includes(s.id))
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms))
}

function stepIndex(key) {
  return WORKFLOW_STEPS.findIndex(s => s.key === key)
}

function stepState(key) {
  const idx = stepIndex(key)
  if (workflowStep.value === 'error') {
    const failedIdx = stepIndex(failedAtStep.value)
    if (idx < failedIdx) return 'done'
    if (idx === failedIdx) return 'error'
    return 'pending'
  }
  if (workflowStep.value === 'done') return 'done'
  const current = stepIndex(workflowStep.value)
  if (idx < current) return 'done'
  if (idx === current) return 'active'
  return 'pending'
}

const progressMsg = computed(() => {
  if (workflowError.value) return workflowError.value
  const pending = scenarios.value.find(s => s.id === pendingScenarioId.value)
  switch (workflowStep.value) {
    case 'baseline': return '正在捕获切换前的全厂三极致 KPI…'
    case 'switch': return pending ? `正在将模拟器切换为「${pending.label}」…` : '正在切换模拟器场景…'
    case 'wait': return `仿真态势收敛中，约 ${waitRemaining.value}s 后采样对比 KPI…`
    case 'compare': return '正在捕获切换后 KPI 并计算 Δ…'
    case 'done': return '对比完成，结果如下'
    case 'error': return workflowError.value
    default: return ''
  }
})

async function scrollToComparePanel() {
  await nextTick()
  comparePanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function failAt(step, message) {
  failedAtStep.value = step
  workflowStep.value = 'error'
  workflowError.value = message
}

function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id
}

async function applyAndCompare(scenario) {
  if (applying.value) return

  if (!simOnline.value) {
    failedAtStep.value = 'switch'
    workflowStep.value = 'error'
    workflowError.value = '模拟器未连接，请先启动 line-simulator（:3002）后再试'
    pendingScenarioId.value = scenario.id
    await scrollToComparePanel()
    return
  }

  applying.value = true
  pendingScenarioId.value = scenario.id
  compareResult.value = null
  workflowError.value = ''
  failedAtStep.value = ''
  workflowStep.value = 'baseline'
  waitRemaining.value = 0
  await scrollToComparePanel()

  try {
    const baseline = await getOptKpiSnapshot()
    baseline.scenario_id = simStatus.value?.scenario_id || baseline.scenario_id

    workflowStep.value = 'switch'
    let applyRes = await callApplyScenario(scenario.id)
    if (applyRes.simulator?.ok === false) {
      try {
        const direct = await applySimScenarioDirect(scenario.id, scenarioApplyParams(scenario.id))
        if (direct?.ok) {
          applyRes = { ...applyRes, simulator: direct }
        } else {
          failAt('switch', '模拟器不可用：' + (applyRes.simulator?.error || '请确认 line-simulator :3002 已启动'))
          return
        }
      } catch {
        failAt('switch', '模拟器不可用：' + (applyRes.simulator?.error || '请确认 line-simulator :3002 已启动'))
        return
      }
    }
    activeScenarioId.value = scenario.id

    const waitSec = applyRes.poll_hint_sec || 4
    workflowStep.value = 'wait'
    for (let i = waitSec; i > 0; i--) {
      waitRemaining.value = i
      await sleep(1000)
    }
    waitRemaining.value = 0

    workflowStep.value = 'compare'
    const after = await getOptKpiSnapshot()
    after.scenario_id = scenario.id

    compareResult.value = await compareOptSnapshots(baseline, after)
    workflowStep.value = 'done'
    await loadMeta()
    await scrollToComparePanel()
  } catch (e) {
    failAt(workflowStep.value, '操作失败：' + (e.message || '未知错误'))
  } finally {
    applying.value = false
    pendingScenarioId.value = null
  }
}

const deltaExtremes = computed(() => compareResult.value?.delta_extremes || {})
const deltaHeadline = computed(() => compareResult.value?.delta_headline || {})
const baselineExtremes = computed(() => compareResult.value?.baseline?.three_extremes || {})
const afterExtremes = computed(() => compareResult.value?.after?.three_extremes || {})
const lineSummary = computed(() => compareResult.value?.after?.line_summary || compareResult.value?.baseline?.line_summary || {})

const simOnline = computed(() => simStatus.value?.simulator_available !== false && simStatus.value?.running != null)
const simLineCount = computed(() => {
  const breakdown = simStatus.value?.factory_energy?.line_breakdown
  return Array.isArray(breakdown) ? breakdown.length : (lineSummary.value?.total_lines || '—')
})

const workspaceMode = computed(() => {
  if (workflowStep.value === 'error') return 'error'
  if (compareResult.value) return 'result'
  if (applying.value) return 'running'
  return 'idle'
})

const categoryStats = computed(() => {
  const stats = { normal: 0, abnormal: 0, business: 0 }
  for (const s of scenarios.value) {
    if (stats[s.category] != null) stats[s.category]++
  }
  return stats
})

function fmtExtremeDelta(key) {
  const v = deltaExtremes.value[key]
  return v == null ? '—' : fmtDelta(v)
}

function isCardBusy(s) {
  return applying.value && pendingScenarioId.value === s.id
}

function cardCategoryClass(s) {
  return `cat-${s.categoryColor || 'green'}`
}
</script>

<template>
  <div class="page">
    <PageHeader section="智优决策中心" title="场景实验室" />

    <div class="content">
      <div class="panel status-bar">
        <div class="sim-info">
          <span class="dot" :class="{ on: simStatus?.running }" />
          模拟器 {{ simOnline ? '已连接' : '离线' }}
          <span v-if="simStatus?.scenario_id" class="mono">· 当前场景 {{ simStatus.scenario_id }}</span>
          <span v-if="simStatus?.speed_multiplier" class="mono">· {{ simStatus.speed_multiplier }}x</span>
          <span v-if="simOnline" class="mono">· {{ simLineCount }} 线 tick</span>
        </div>
        <div class="legend">
          <span
            v-for="cat in SCENARIO_CATEGORIES"
            :key="cat.key"
            class="legend-item"
            :class="`legend-${cat.color}`"
          >
            {{ cat.label }}
            <span class="legend-count">{{ categoryStats[cat.key] || 0 }}</span>
          </span>
        </div>
      </div>

      <div v-if="!simOnline" class="panel alert-banner">
        <strong>模拟器离线</strong>
        <span>场景切换与 KPI 对比依赖 line-simulator。请在 <code>line-simulator</code> 目录执行 <code>python3 main.py</code>（默认 :3002）后刷新本页。</span>
      </div>

      <div v-if="fromEnergy" class="panel linkage-banner">
        <span>来自 <strong>能碳决策</strong> · 场景 <code>{{ route.query.scenario || '—' }}</code></span>
        <button type="button" class="banner-btn" @click="backToEnergyCarbon">← 返回能碳模块</button>
      </div>

      <div class="panel guide-panel">
        <div class="panel-title">使用说明</div>
        <div class="guide-grid">
          <section v-for="sec in PRINCIPLE_SECTIONS" :key="sec.title" class="guide-block">
            <h4>{{ sec.title }}</h4>
            <p>{{ sec.body }}</p>
          </section>
        </div>
        <div class="category-legend">
          <div
            v-for="cat in SCENARIO_CATEGORIES"
            :key="cat.key"
            class="cat-card"
            :class="`cat-${cat.color}`"
          >
            <div class="cat-card-title">{{ cat.label }}</div>
            <div class="cat-card-desc">{{ cat.desc }}</div>
          </div>
        </div>
      </div>

      <div ref="comparePanelRef" class="panel compare-workspace" :class="workspaceMode">
        <div class="panel-title">
          What-if 对比工作区
          <span v-if="compareResult" class="scenario-flow">
            {{ compareResult.scenario_from || '—' }} → {{ compareResult.scenario_to }}
          </span>
        </div>

        <div v-if="workspaceMode === 'idle'" class="workspace-idle">
          <div class="idle-icon">◇</div>
          <p class="idle-title">尚未开始对比</p>
          <p class="idle-desc">
            在下方按分类选择场景，点击「应用并对比」。对比粒度为<strong>全厂三极致指数</strong>（非逐线），
            约 4–6 秒后可查看切换前后的效率 / 成本 / 质量 Δ 及 OEE、合格率、绿电占比变化。
          </p>
        </div>

        <div v-else-if="workspaceMode === 'running' || workspaceMode === 'error'" class="workspace-progress">
          <div class="step-bar">
            <div
              v-for="(step, i) in WORKFLOW_STEPS"
              :key="step.key"
              class="step-item"
              :class="stepState(step.key)"
            >
              <div class="step-dot">
                <span v-if="stepState(step.key) === 'done'">✓</span>
                <span v-else-if="stepState(step.key) === 'active'" class="spinner" />
                <span v-else-if="stepState(step.key) === 'error'">!</span>
                <span v-else>{{ i + 1 }}</span>
              </div>
              <div class="step-label">{{ step.label }}</div>
              <div v-if="i < WORKFLOW_STEPS.length - 1" class="step-line" :class="stepState(step.key)" />
            </div>
          </div>
          <p class="progress-msg" :class="{ error: workspaceMode === 'error' }">{{ progressMsg }}</p>
          <p v-if="workspaceMode === 'running'" class="progress-hint">请勿关闭页面，对比结果将自动显示在本区域</p>
        </div>

        <template v-if="workspaceMode === 'result'">
          <div class="compare-grid">
            <div class="compare-col">
              <div class="col-label">基线（切换前）</div>
              <OptIndexGauge label="效率" :value="baselineExtremes.efficiency_index" color="cyan" />
              <OptIndexGauge label="成本" :value="baselineExtremes.cost_index" color="amber" />
              <OptIndexGauge label="质量" :value="baselineExtremes.quality_index" color="teal" />
            </div>
            <div class="compare-col delta-col">
              <div class="col-label">Δ 变化</div>
              <div class="delta-item" :class="{ up: deltaExtremes.efficiency_index > 0, down: deltaExtremes.efficiency_index < 0 }">
                效率 {{ fmtExtremeDelta('efficiency_index') }}
              </div>
              <div class="delta-item" :class="{ up: deltaExtremes.cost_index > 0, down: deltaExtremes.cost_index < 0 }">
                成本 {{ fmtExtremeDelta('cost_index') }}
              </div>
              <div class="delta-item" :class="{ up: deltaExtremes.quality_index > 0, down: deltaExtremes.quality_index < 0 }">
                质量 {{ fmtExtremeDelta('quality_index') }}
              </div>
            </div>
            <div class="compare-col">
              <div class="col-label">切换后</div>
              <OptIndexGauge label="效率" :value="afterExtremes.efficiency_index" color="cyan" />
              <OptIndexGauge label="成本" :value="afterExtremes.cost_index" color="amber" />
              <OptIndexGauge label="质量" :value="afterExtremes.quality_index" color="teal" />
            </div>
          </div>
          <div v-if="Object.keys(deltaHeadline).length" class="headline-delta">
            <span v-if="deltaHeadline.avg_oee_pct != null">均 OEE Δ {{ fmtDelta(deltaHeadline.avg_oee_pct) }}</span>
            <span v-if="deltaHeadline.quality_pass_rate_pct != null">合格率 Δ {{ fmtDelta(deltaHeadline.quality_pass_rate_pct) }}</span>
            <span v-if="deltaHeadline.green_power_ratio_pct != null">绿电 Δ {{ fmtDelta(deltaHeadline.green_power_ratio_pct) }}%</span>
            <span v-if="lineSummary.total_lines">全厂 {{ lineSummary.total_lines }} 线 · 仿真 {{ lineSummary.telemetry_lines }} 线</span>
          </div>
          <div v-if="isEnergyScenario || fromEnergy" class="compare-linkage">
            <button type="button" class="banner-btn" @click="backToEnergyCarbon">
              对比完成 · 返回能碳决策查看绿电/碳排变化 →
            </button>
          </div>
        </template>
      </div>

      <div v-for="group in SCENARIO_GROUPS" :key="group.label" class="panel group-panel" :class="`group-${group.category}`">
        <div class="group-header">
          <div class="panel-title">
            <span class="group-badge" :class="`cat-${SCENARIO_CATEGORIES[group.category]?.color}`">
              {{ SCENARIO_CATEGORIES[group.category]?.label }}
            </span>
            {{ group.label }}
          </div>
          <p class="group-desc">{{ SCENARIO_CATEGORIES[group.category]?.desc }}</p>
        </div>
        <div class="scenario-grid">
          <div
            v-for="s in groupScenarios(group)"
            :key="s.id"
            class="scenario-card"
            :class="[
              cardCategoryClass(s),
              { active: activeScenarioId === s.id, busy: isCardBusy(s), expanded: expandedId === s.id }
            ]"
          >
            <div class="card-top">
              <span class="cat-tag" :class="`cat-${s.categoryColor}`">{{ s.categoryLabel }}</span>
              <span class="s-id">{{ s.id }}</span>
            </div>
            <div class="s-label">{{ s.label }}</div>
            <p class="s-summary">{{ s.summary }}</p>

            <div class="kpi-hints">
              <span class="kpi-hint" :class="kpiHintClass(s.kpiHint?.efficiency)">
                效率 {{ s.kpiHint?.efficiency || '—' }}
              </span>
              <span class="kpi-hint" :class="kpiHintClass(s.kpiHint?.cost)">
                成本 {{ s.kpiHint?.cost || '—' }}
              </span>
              <span class="kpi-hint" :class="kpiHintClass(s.kpiHint?.quality)">
                质量 {{ s.kpiHint?.quality || '—' }}
              </span>
            </div>

            <div class="card-meta">
              <span class="meta-item">影响范围：{{ s.affectedLines }}</span>
            </div>

            <button type="button" class="detail-toggle" @click="toggleExpand(s.id)">
              {{ expandedId === s.id ? '收起机理说明 ▲' : '展开机理说明 ▼' }}
            </button>

            <div v-if="expandedId === s.id" class="card-detail">
              <div v-if="s.effects?.length" class="detail-block">
                <div class="detail-label">仿真注入</div>
                <ul>
                  <li v-for="(fx, i) in s.effects" :key="i">{{ fx }}</li>
                </ul>
              </div>
              <div v-if="s.useCase" class="detail-block">
                <div class="detail-label">适用场景</div>
                <p>{{ s.useCase }}</p>
              </div>
              <div v-if="s.description && s.description !== s.summary" class="detail-block">
                <div class="detail-label">原始描述</div>
                <p>{{ s.description }}</p>
              </div>
            </div>

            <button
              type="button"
              class="apply-btn"
              :class="`btn-${s.categoryColor}`"
              :disabled="applying"
              @click="applyAndCompare(s)"
            >
              <span v-if="isCardBusy(s)" class="btn-spinner" />
              {{ isCardBusy(s) ? '对比进行中…' : (activeScenarioId === s.id ? '重新对比' : '应用并对比') }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="!scenarios.length" class="empty">加载场景列表中…</div>
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
.panel-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.scenario-flow { font-size: 11px; font-family: var(--font-mono); color: var(--accent-cyan); font-weight: 400; }

.status-bar {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 10px;
}
.sim-info { font-size: 12px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.sim-info .mono { font-family: var(--font-mono); color: var(--text-dim); }
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--status-stopped); flex-shrink: 0; }
.dot.on { background: var(--status-running); }

.legend { display: flex; gap: 8px; flex-wrap: wrap; }
.legend-item {
  font-size: 10px; padding: 3px 8px; border-radius: 3px;
  border: 1px solid var(--border-dim); display: flex; align-items: center; gap: 4px;
}
.legend-count {
  font-family: var(--font-mono); opacity: 0.8;
  background: rgba(255,255,255,0.06); padding: 0 4px; border-radius: 2px;
}
.legend-green { color: var(--status-running); border-color: rgba(52, 211, 153, 0.35); background: rgba(52, 211, 153, 0.08); }
.legend-red { color: var(--status-alarm); border-color: rgba(248, 113, 113, 0.35); background: rgba(248, 113, 113, 0.08); }
.legend-amber { color: var(--accent-amber); border-color: rgba(251, 191, 36, 0.35); background: rgba(251, 191, 36, 0.08); }

.alert-banner {
  display: flex; flex-direction: column; gap: 4px;
  border-color: rgba(251, 191, 36, 0.45);
  background: rgba(251, 191, 36, 0.08);
  font-size: 12px; color: var(--text-secondary);
}
.alert-banner strong { color: var(--accent-amber); }
.alert-banner code { font-family: var(--font-mono); color: var(--accent-cyan); }

.guide-panel { border-color: rgba(34, 211, 238, 0.2); }
.guide-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px; margin-bottom: 14px;
}
.guide-block h4 { font-size: 12px; font-weight: 600; margin: 0 0 6px; color: var(--text-primary); }
.guide-block p { font-size: 11px; line-height: 1.55; margin: 0; color: var(--text-secondary); }
.guide-block strong { color: var(--text-primary); font-weight: 600; }

.category-legend { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
@media (max-width: 700px) { .category-legend { grid-template-columns: 1fr; } }
.cat-card {
  padding: 8px 10px; border-radius: var(--radius);
  border: 1px solid var(--border-dim);
}
.cat-card-title { font-size: 11px; font-weight: 600; margin-bottom: 3px; }
.cat-card-desc { font-size: 10px; color: var(--text-secondary); line-height: 1.4; }
.cat-green { border-left: 3px solid var(--status-running); background: rgba(52, 211, 153, 0.05); }
.cat-green .cat-card-title { color: var(--status-running); }
.cat-red { border-left: 3px solid var(--status-alarm); background: rgba(248, 113, 113, 0.05); }
.cat-red .cat-card-title { color: var(--status-alarm); }
.cat-amber { border-left: 3px solid var(--accent-amber); background: rgba(251, 191, 36, 0.05); }
.cat-amber .cat-card-title { color: var(--accent-amber); }

.compare-workspace { scroll-margin-top: 12px; }
.compare-workspace.running { border-color: rgba(34, 211, 238, 0.35); }
.compare-workspace.result { border-color: rgba(34, 211, 238, 0.5); box-shadow: 0 0 0 1px rgba(34, 211, 238, 0.08); }
.compare-workspace.error { border-color: rgba(248, 113, 113, 0.45); background: rgba(248, 113, 113, 0.04); }

.workspace-idle { text-align: center; padding: 20px 12px; }
.idle-icon { font-size: 28px; color: var(--text-dim); opacity: 0.5; }
.idle-title { font-size: 14px; font-weight: 600; margin: 8px 0 4px; color: var(--text-primary); }
.idle-desc { font-size: 12px; color: var(--text-secondary); max-width: 560px; margin: 0 auto; line-height: 1.55; }

.workspace-progress { padding: 8px 4px 4px; }
.step-bar {
  display: flex; align-items: flex-start; justify-content: center;
  gap: 0; margin-bottom: 16px; flex-wrap: wrap;
}
.step-item {
  display: flex; flex-direction: column; align-items: center;
  position: relative; min-width: 72px; flex: 1; max-width: 120px;
}
.step-dot {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600;
  border: 2px solid var(--border-dim);
  background: var(--bg-panel-elevated);
  color: var(--text-dim);
  z-index: 1;
}
.step-item.active .step-dot {
  border-color: var(--accent-cyan);
  color: var(--accent-cyan);
  box-shadow: 0 0 12px rgba(34, 211, 238, 0.25);
}
.step-item.done .step-dot {
  border-color: var(--status-running);
  background: rgba(34, 197, 94, 0.15);
  color: var(--status-running);
}
.step-item.error .step-dot {
  border-color: var(--status-alarm);
  background: rgba(248, 113, 113, 0.15);
  color: var(--status-alarm);
}
.step-label { font-size: 10px; color: var(--text-dim); margin-top: 6px; text-align: center; }
.step-item.active .step-label { color: var(--accent-cyan); font-weight: 600; }
.step-line {
  position: absolute; top: 14px; left: calc(50% + 16px);
  width: calc(100% - 8px); height: 2px;
  background: var(--border-dim);
  z-index: 0;
}
.step-line.done { background: var(--status-running); }

.progress-msg {
  text-align: center; font-size: 13px; font-weight: 500;
  color: var(--accent-amber); margin: 0;
}
.progress-msg.error { color: var(--status-alarm); }
.progress-hint { text-align: center; font-size: 11px; color: var(--text-dim); margin: 6px 0 0; }

.spinner, .btn-spinner {
  display: inline-block;
  width: 12px; height: 12px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  vertical-align: middle;
}
.spinner { width: 14px; height: 14px; border-top-color: var(--accent-cyan); }
.btn-spinner { margin-right: 6px; }
@keyframes spin { to { transform: rotate(360deg); } }

.group-panel { border-top-width: 2px; }
.group-normal { border-top-color: var(--status-running); }
.group-abnormal { border-top-color: var(--status-alarm); }
.group-business { border-top-color: var(--accent-amber); }
.group-header { margin-bottom: 10px; }
.group-desc { font-size: 11px; color: var(--text-dim); margin: 0; }
.group-badge {
  font-size: 10px; padding: 2px 6px; border-radius: 3px; font-weight: 600;
}

.scenario-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 10px;
}
.scenario-card {
  padding: 12px;
  background: var(--bg-panel-elevated);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  border-left-width: 3px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.scenario-card.cat-green { border-left-color: var(--status-running); }
.scenario-card.cat-red { border-left-color: var(--status-alarm); }
.scenario-card.cat-amber { border-left-color: var(--accent-amber); }
.scenario-card.active { box-shadow: 0 0 0 1px rgba(34, 211, 238, 0.35); }
.scenario-card.busy { box-shadow: 0 0 0 1px rgba(251, 191, 36, 0.3); }

.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.cat-tag {
  font-size: 9px; font-weight: 600; padding: 2px 6px; border-radius: 2px;
}
.cat-tag.cat-green { color: var(--status-running); background: rgba(52, 211, 153, 0.12); }
.cat-tag.cat-red { color: var(--status-alarm); background: rgba(248, 113, 113, 0.12); }
.cat-tag.cat-amber { color: var(--accent-amber); background: rgba(251, 191, 36, 0.12); }

.s-id { font-family: var(--font-mono); font-size: 10px; color: var(--text-dim); }
.s-label { font-size: 14px; font-weight: 600; margin-bottom: 6px; }
.s-summary { font-size: 11px; color: var(--text-secondary); line-height: 1.5; margin: 0 0 8px; min-height: 2.8em; }

.kpi-hints { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 6px; }
.kpi-hint {
  font-size: 9px; font-family: var(--font-mono); padding: 2px 6px;
  border-radius: 2px; background: rgba(255,255,255,0.04); color: var(--text-dim);
}
.kpi-hint.up, .kpi-hint.up-strong { color: var(--status-running); background: rgba(52, 211, 153, 0.1); }
.kpi-hint.down, .kpi-hint.down-strong { color: var(--status-alarm); background: rgba(248, 113, 113, 0.1); }
.kpi-hint.neutral { color: var(--text-dim); }

.card-meta { font-size: 10px; color: var(--text-dim); margin-bottom: 6px; }

.detail-toggle {
  width: 100%; font-size: 10px; padding: 4px 0; margin-bottom: 6px;
  background: none; border: none; color: var(--accent-cyan); cursor: pointer; text-align: left;
}
.detail-toggle:hover { text-decoration: underline; }

.card-detail {
  font-size: 10px; color: var(--text-secondary);
  padding: 8px; margin-bottom: 8px;
  background: rgba(0,0,0,0.15); border-radius: 3px;
  line-height: 1.5;
}
.detail-label { font-weight: 600; color: var(--text-primary); margin-bottom: 4px; font-size: 10px; }
.card-detail ul { margin: 0; padding-left: 16px; }
.card-detail li { margin-bottom: 3px; }
.card-detail p { margin: 0; }

.apply-btn {
  width: 100%; font-size: 11px; padding: 7px 10px;
  border-radius: 3px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid;
  background: transparent;
}
.btn-green { border-color: var(--status-running); color: var(--status-running); background: rgba(52, 211, 153, 0.08); }
.btn-green:hover:not(:disabled) { background: rgba(52, 211, 153, 0.16); }
.btn-red { border-color: var(--status-alarm); color: var(--status-alarm); background: rgba(248, 113, 113, 0.08); }
.btn-red:hover:not(:disabled) { background: rgba(248, 113, 113, 0.16); }
.btn-amber { border-color: var(--accent-amber); color: var(--accent-amber); background: rgba(251, 191, 36, 0.08); }
.btn-amber:hover:not(:disabled) { background: rgba(251, 191, 36, 0.16); }
.apply-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.compare-grid { display: grid; grid-template-columns: 1fr 120px 1fr; gap: 12px; align-items: start; }
@media (max-width: 800px) { .compare-grid { grid-template-columns: 1fr; } }
.col-label { font-size: 11px; font-weight: 600; text-align: center; margin-bottom: 8px; color: var(--text-dim); }
.compare-col .gauge { margin-bottom: 8px; }
.delta-col { display: flex; flex-direction: column; justify-content: center; gap: 12px; padding-top: 24px; }
.delta-item { font-family: var(--font-mono); font-size: 13px; text-align: center; font-weight: 600; }
.delta-item.up { color: var(--status-running); }
.delta-item.down { color: var(--status-alarm); }
.empty { color: var(--text-dim); padding: 24px; text-align: center; }
.headline-delta {
  display: flex; flex-wrap: wrap; gap: 12px; margin-top: 12px; padding-top: 10px;
  border-top: 1px solid var(--border-dim); font-size: 11px; color: var(--text-secondary);
  font-family: var(--font-mono);
}
.linkage-banner {
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;
  border-color: rgba(45, 212, 191, 0.35); background: rgba(45, 212, 191, 0.06);
  font-size: 12px; color: var(--text-secondary);
}
.linkage-banner strong { color: var(--accent-teal); }
.linkage-banner code { font-family: var(--font-mono); color: var(--accent-amber); }
.banner-btn {
  font-size: 11px; padding: 6px 12px; border-radius: 3px; cursor: pointer;
  border: 1px solid var(--accent-teal); background: rgba(45, 212, 191, 0.08); color: var(--accent-teal);
}
.compare-linkage { margin-top: 12px; text-align: center; }
</style>
