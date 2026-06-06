<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import OptTradeoffTriangle from '../../components/optimization/OptTradeoffTriangle.vue'
import OptIndexGauge from '../../components/optimization/OptIndexGauge.vue'
import { analyzeOptBalance } from '../../api/optimization'
import { fmtDelta as fmtDeltaValue } from '../../utils/format'
import {
  BALANCE_PRINCIPLE_SECTIONS,
  LEVER_CATEGORIES,
  LEVER_CATALOG,
  WEIGHT_CATALOG,
  TRIANGLE_GUIDE,
  PARETO_GUIDE,
  SENSITIVITY_LABELS,
  fmtSensValue,
  sensClass
} from '../../data/balanceCatalog'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const data = ref(null)
const expandedLever = ref(null)
const fromEnergy = computed(() => route.query.from === 'energy-carbon')

const levers = ref({
  batch_size_kg: 1200,
  line_speed_pct: 100,
  green_shift_pct: 50,
  grade_concentration_pct: 60
})

const weights = ref({
  efficiency: 0.34,
  cost: 0.33,
  quality: 0.33
})

let debounceTimer = null

async function runAnalysis() {
  loading.value = true
  errorMsg.value = ''
  try {
    data.value = await analyzeOptBalance({
      ...levers.value,
      weight_efficiency: weights.value.efficiency,
      weight_cost: weights.value.cost,
      weight_quality: weights.value.quality
    })
  } catch (e) {
    errorMsg.value = e.message || '分析请求失败'
    console.error(e)
  } finally {
    loading.value = false
  }
}

function scheduleAnalysis() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(runAnalysis, 400)
}

watch([levers, weights], scheduleAnalysis, { deep: true })

onMounted(() => {
  const pct = route.query.green_shift_pct
  if (pct != null && pct !== '') {
    levers.value.green_shift_pct = Number(pct) || levers.value.green_shift_pct
  }
})
runAnalysis()

function backToEnergyCarbon() {
  router.push({
    path: '/optimization/energy-carbon',
    query: { tab: 'optimize', green_shift: levers.value.green_shift_pct }
  })
}

const current = computed(() => data.value?.current)
const optimal = computed(() => data.value?.optimal)
const currentIdx = computed(() => current.value?.indices || {})
const optimalIdx = computed(() => optimal.value?.indices || {})
const baselineSnap = computed(() => data.value?.baseline_snapshot || {})
const baselineExtremes = computed(() => baselineSnap.value?.three_extremes || {})
const lineSummary = computed(() => baselineSnap.value?.line_summary || {})

const weightSum = computed(() => {
  const w = data.value?.weights || weights.value
  return ((w.efficiency || 0) + (w.cost || 0) + (w.quality || 0)).toFixed(2)
})

function fmtDelta(key) {
  const c = currentIdx.value[key] ?? 0
  const o = optimalIdx.value[key] ?? 0
  return fmtDeltaValue(o - c)
}

function toggleLever(key) {
  expandedLever.value = expandedLever.value === key ? null : key
}

function applyOptimal() {
  const ol = optimal.value?.levers
  if (!ol) return
  levers.value = {
    batch_size_kg: ol.batch_size_kg ?? 1200,
    line_speed_pct: ol.line_speed_pct ?? 100,
    green_shift_pct: ol.green_shift_pct ?? 50,
    grade_concentration_pct: ol.grade_concentration_pct ?? 60
  }
}

function resetLevers() {
  for (const m of LEVER_CATALOG) {
    levers.value[m.key] = m.default
  }
}

function leverCategoryColor(cat) {
  return LEVER_CATEGORIES[cat]?.color || 'cyan'
}

function formatParetoLevers(p) {
  const l = p.levers || {}
  return [
    `批次 ${l.batch_size_kg}kg`,
    `线速 ${l.line_speed_pct}%`,
    `绿电 ${l.green_shift_pct}%`,
    `规格 ${l.grade_concentration_pct}%`
  ].join(' · ')
}
</script>

<template>
  <div class="page">
    <PageHeader section="智优决策中心" title="三极致平衡点">
      <div class="header-actions">
        <button type="button" class="btn-reset" @click="resetLevers">恢复默认杠杆</button>
        <button type="button" class="btn-apply" :disabled="!optimal?.levers" @click="applyOptimal">
          采纳最佳杠杆
        </button>
      </div>
    </PageHeader>

    <div class="content">
      <div v-if="fromEnergy" class="panel linkage-banner">
        <span>来自 <strong>能碳决策</strong> · 已同步绿电排产偏移 <code>{{ levers.green_shift_pct }}%</code></span>
        <button type="button" class="banner-btn" @click="backToEnergyCarbon">← 返回能碳模块</button>
      </div>

      <div class="panel status-bar">
        <div class="baseline-info">
          <span class="status-label">分析基线</span>
          <span v-if="baselineExtremes.efficiency_index != null" class="mono">
            E {{ baselineExtremes.efficiency_index }} ·
            C {{ baselineExtremes.cost_index }} ·
            Q {{ baselineExtremes.quality_index }}
          </span>
          <span v-if="lineSummary.total_lines" class="mono dim">
            · 全厂 {{ lineSummary.total_lines }} 线
            <template v-if="lineSummary.telemetry_lines">· 仿真 {{ lineSummary.telemetry_lines }} 线</template>
          </span>
          <span v-if="data?.computed_at" class="mono dim">· {{ new Date(data.computed_at).toLocaleTimeString() }}</span>
        </div>
        <div class="legend">
          <span
            v-for="cat in LEVER_CATEGORIES"
            :key="cat.key"
            class="legend-item"
            :class="`legend-${cat.color}`"
          >
            {{ cat.label }}
          </span>
        </div>
      </div>

      <div v-if="errorMsg" class="panel alert-banner">
        <strong>分析失败</strong>
        <span>{{ errorMsg }}</span>
      </div>

      <div class="panel guide-panel">
        <div class="panel-title">使用说明</div>
        <div class="guide-grid">
          <section v-for="sec in BALANCE_PRINCIPLE_SECTIONS" :key="sec.title" class="guide-block">
            <h4>{{ sec.title }}</h4>
            <p>{{ sec.body }}</p>
          </section>
        </div>
        <div class="category-legend">
          <div
            v-for="cat in LEVER_CATEGORIES"
            :key="cat.key"
            class="cat-card"
            :class="`cat-${cat.color}`"
          >
            <div class="cat-card-title">{{ cat.label }}</div>
            <div class="cat-card-desc">{{ cat.desc }}</div>
          </div>
        </div>
        <p class="apply-hint">
          「采纳最佳杠杆」将左侧滑杆同步至网格搜索最优解，便于继续微调；<strong>不会</strong>改写 line-simulator 运行态。
          若需验证异常工况影响，请前往<strong>场景实验室</strong>。
        </p>
      </div>

      <div class="grid-main" :class="{ loading: loading }">
        <div class="panel levers-panel">
          <div class="panel-title">生产杠杆</div>
          <p class="panel-hint">拖动滑杆实时重算三指数投影与 Pareto 最优（约 400ms 防抖）</p>

          <div
            v-for="m in LEVER_CATALOG"
            :key="m.key"
            class="lever-block"
            :class="[`lever-${leverCategoryColor(m.category)}`, { expanded: expandedLever === m.key }]"
          >
            <div class="lever-head">
              <div class="lever-title-row">
                <span class="cat-tag" :class="`cat-${leverCategoryColor(m.category)}`">
                  {{ LEVER_CATEGORIES[m.category]?.label }}
                </span>
                <span class="lever-name">{{ m.label }}</span>
              </div>
              <span class="lever-val">{{ levers[m.key] }} {{ m.unit }}</span>
            </div>
            <input
              v-model.number="levers[m.key]"
              type="range"
              :min="m.min"
              :max="m.max"
              :step="m.step"
              class="lever-slider"
              :class="`slider-${leverCategoryColor(m.category)}`"
            />
            <div class="lever-range-hint">{{ m.min }} – {{ m.max }} {{ m.unit }} · 默认 {{ m.default }}</div>
            <p class="lever-desc">{{ m.desc }}</p>
            <div class="tradeoff-chip" :class="`chip-${leverCategoryColor(m.category)}`">{{ m.tradeoff }}</div>
            <button type="button" class="detail-toggle" @click="toggleLever(m.key)">
              {{ expandedLever === m.key ? '收起业务说明 ▲' : '展开业务说明 ▼' }}
            </button>
            <p v-if="expandedLever === m.key" class="lever-business">{{ m.businessMeaning }}</p>
          </div>

          <div class="panel-title sub">目标权重</div>
          <p class="panel-hint">三权重自动归一化，综合分 = Σ(指数 × 权重)。当前权重和 ≈ {{ weightSum }}</p>
          <div v-for="w in WEIGHT_CATALOG" :key="w.key" class="lever-row weight-row">
            <div class="lever-head">
              <span class="weight-label" :class="`label-${w.color}`">{{ w.label }}</span>
              <span class="lever-val">{{ (weights[w.key] * 100).toFixed(0) }}%</span>
            </div>
            <input
              v-model.number="weights[w.key]"
              type="range"
              min="0.1"
              max="0.7"
              step="0.01"
              class="lever-slider"
              :class="`slider-${w.color}`"
            />
            <p class="weight-desc">{{ w.desc }}</p>
          </div>
        </div>

        <div class="panel tri-panel">
          <div class="panel-title">权衡三角</div>
          <OptTradeoffTriangle :current="current" :optimal="optimal" :width="360" :height="300" />
          <div v-if="data?.score_gap != null" class="score-gap">
            距最佳综合分
            <b>{{ data.score_gap > 0 ? '+' : '' }}{{ data.score_gap?.toFixed(1) }}</b>
            <span class="score-hint">（越大说明调整空间越大）</span>
          </div>
          <ul class="tri-guide">
            <li v-for="(tip, i) in TRIANGLE_GUIDE" :key="i">{{ tip }}</li>
          </ul>
        </div>

        <div class="panel indices-panel">
          <div class="panel-title">指数对比</div>
          <p class="panel-hint">「当前」= 滑杆位置投影 · 「最佳平衡」= 840 组合网格中的加权最高分</p>
          <div class="idx-compare">
            <div class="idx-col">
              <div class="idx-head">当前策略</div>
              <OptIndexGauge label="效率" :value="currentIdx.efficiency_index" color="cyan" />
              <OptIndexGauge label="成本" :value="currentIdx.cost_index" color="amber" />
              <OptIndexGauge label="质量" :value="currentIdx.quality_index" color="teal" />
              <div class="wscore">综合 {{ current?.weighted_score?.toFixed(1) }}</div>
            </div>
            <div class="idx-col optimal-col">
              <div class="idx-head">最佳平衡</div>
              <OptIndexGauge label="效率" :value="optimalIdx.efficiency_index" color="cyan" />
              <OptIndexGauge label="成本" :value="optimalIdx.cost_index" color="amber" />
              <OptIndexGauge label="质量" :value="optimalIdx.quality_index" color="teal" />
              <div class="wscore optimal">综合 {{ optimal?.weighted_score?.toFixed(1) }}</div>
            </div>
          </div>
          <table class="delta-table">
            <thead>
              <tr><th>指标</th><th>Δ 最优−当前</th></tr>
            </thead>
            <tbody>
              <tr>
                <td>效率指数</td>
                <td :class="fmtDelta('efficiency_index').startsWith('+') ? 'up' : 'down'">{{ fmtDelta('efficiency_index') }}</td>
              </tr>
              <tr>
                <td>成本指数</td>
                <td :class="fmtDelta('cost_index').startsWith('+') ? 'up' : 'down'">{{ fmtDelta('cost_index') }}</td>
              </tr>
              <tr>
                <td>质量指数</td>
                <td :class="fmtDelta('quality_index').startsWith('+') ? 'up' : 'down'">{{ fmtDelta('quality_index') }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="optimal?.rationale" class="rationale">
            <div class="rationale-label">推荐调整路径</div>
            {{ optimal.rationale }}
          </div>
          <div v-if="optimal?.levers" class="optimal-levers">
            <span>最优杠杆：</span>
            批次 {{ optimal.levers.batch_size_kg }}kg ·
            线速 {{ optimal.levers.line_speed_pct }}% ·
            绿电 {{ optimal.levers.green_shift_pct }}% ·
            规格 {{ optimal.levers.grade_concentration_pct }}%
          </div>
        </div>

        <div v-if="loading" class="grid-loading">重算中…</div>
      </div>

      <div v-if="data?.sensitivity?.length" class="panel sensitivity-panel">
        <div class="panel-title">杠杆灵敏度</div>
        <p class="panel-hint">在当前基线附近，各杠杆单位变化对三指数的理论边际影响（启发式系数，非实测斜率）</p>
        <div class="sens-grid">
          <div
            v-for="row in data.sensitivity"
            :key="row.lever"
            class="sens-card"
          >
            <div class="sens-title">
              {{ SENSITIVITY_LABELS[row.lever]?.label || row.lever }}
              <span class="sens-unit">{{ SENSITIVITY_LABELS[row.lever]?.unit }}</span>
            </div>
            <div class="sens-values">
              <span
                v-for="f in (SENSITIVITY_LABELS[row.lever]?.fields || [])"
                :key="f.key"
                class="sens-item"
                :class="sensClass(row[f.key])"
              >
                {{ f.label }} {{ fmtSensValue(row[f.key]) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">典型权衡说明</div>
        <p class="panel-hint">基于当前基线，单因子极端调整的投影效果（帮助理解三极致此消彼长）</p>
        <ul class="tradeoff-list">
          <li v-for="(note, i) in data?.tradeoff_notes || []" :key="i">{{ note }}</li>
        </ul>
      </div>

      <div v-if="data?.pareto_frontier?.length" class="panel pareto-panel">
        <div class="panel-title">Pareto 前沿样本（{{ data.pareto_frontier.length }} 点）</div>
        <p class="panel-hint">{{ PARETO_GUIDE }}</p>
        <div class="pareto-grid">
          <div v-for="(p, i) in data.pareto_frontier" :key="i" class="pareto-card">
            <div class="p-rank">#{{ i + 1 }}</div>
            <div class="p-score">{{ p.weighted_score }}</div>
            <div class="p-score-label">综合分</div>
            <div class="p-levers">{{ formatParetoLevers(p) }}</div>
            <div class="p-idx">
              <span class="idx-e">E {{ p.indices?.efficiency_index }}</span>
              <span class="idx-c">C {{ p.indices?.cost_index }}</span>
              <span class="idx-q">Q {{ p.indices?.quality_index }}</span>
            </div>
          </div>
        </div>
      </div>
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
.panel-title.sub { margin-top: 16px; font-size: 12px; color: var(--text-secondary); }
.panel-hint { font-size: 10px; color: var(--text-dim); margin: 0 0 10px; line-height: 1.45; }

.header-actions { display: flex; gap: 8px; }
.btn-reset {
  font-size: 12px; padding: 6px 12px; border-radius: 3px;
  border: 1px solid var(--border-dim); background: transparent;
  color: var(--text-secondary); cursor: pointer;
}
.btn-reset:hover { border-color: var(--accent-cyan); color: var(--accent-cyan); }
.btn-apply {
  font-size: 12px; padding: 6px 12px; border-radius: 3px;
  border: 1px solid var(--status-running); background: rgba(52, 211, 153, 0.12);
  color: var(--status-running); cursor: pointer;
}
.btn-apply:disabled { opacity: 0.4; cursor: not-allowed; }

.status-bar {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 10px;
}
.baseline-info { font-size: 12px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.status-label { font-weight: 600; color: var(--text-primary); }
.mono { font-family: var(--font-mono); }
.dim { color: var(--text-dim); }

.legend { display: flex; gap: 8px; flex-wrap: wrap; }
.legend-item {
  font-size: 10px; padding: 3px 8px; border-radius: 3px;
  border: 1px solid var(--border-dim);
}
.legend-cyan { color: var(--accent-cyan); border-color: rgba(34, 211, 238, 0.35); background: rgba(34, 211, 238, 0.08); }
.legend-amber { color: var(--accent-amber); border-color: rgba(251, 191, 36, 0.35); background: rgba(251, 191, 36, 0.08); }
.legend-teal { color: var(--accent-teal); border-color: rgba(45, 212, 191, 0.35); background: rgba(45, 212, 191, 0.08); }

.alert-banner {
  display: flex; flex-direction: column; gap: 4px;
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.06);
  font-size: 12px; color: var(--text-secondary);
}
.alert-banner strong { color: var(--status-alarm); }

.guide-panel { border-color: rgba(34, 211, 238, 0.2); }
.guide-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px; margin-bottom: 14px;
}
.guide-block h4 { font-size: 12px; font-weight: 600; margin: 0 0 6px; color: var(--text-primary); }
.guide-block p { font-size: 11px; line-height: 1.55; margin: 0; color: var(--text-secondary); }

.category-legend { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 10px; }
@media (max-width: 700px) { .category-legend { grid-template-columns: 1fr; } }
.cat-card { padding: 8px 10px; border-radius: var(--radius); border: 1px solid var(--border-dim); }
.cat-card-title { font-size: 11px; font-weight: 600; margin-bottom: 3px; }
.cat-card-desc { font-size: 10px; color: var(--text-secondary); line-height: 1.4; }
.cat-cyan { border-left: 3px solid var(--accent-cyan); background: rgba(34, 211, 238, 0.05); }
.cat-cyan .cat-card-title { color: var(--accent-cyan); }
.cat-amber { border-left: 3px solid var(--accent-amber); background: rgba(251, 191, 36, 0.05); }
.cat-amber .cat-card-title { color: var(--accent-amber); }
.cat-teal { border-left: 3px solid var(--accent-teal); background: rgba(45, 212, 191, 0.05); }
.cat-teal .cat-card-title { color: var(--accent-teal); }

.apply-hint {
  font-size: 10px; color: var(--text-dim); margin: 0;
  padding: 8px 10px; background: rgba(0,0,0,0.12); border-radius: 3px; line-height: 1.5;
}
.apply-hint strong { color: var(--accent-amber); }

.grid-main {
  display: grid; grid-template-columns: 1fr 380px 1fr; gap: 12px;
  position: relative; margin-bottom: 12px;
}
@media (max-width: 1200px) { .grid-main { grid-template-columns: 1fr; } }
.grid-main.loading { opacity: 0.72; pointer-events: none; }
.grid-loading {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.25); border-radius: var(--radius);
  font-size: 13px; color: var(--accent-cyan); z-index: 2;
}

.lever-block {
  margin-bottom: 14px; padding: 10px; border-radius: var(--radius);
  border: 1px solid var(--border-dim); border-left-width: 3px;
  background: var(--bg-panel-elevated);
}
.lever-cyan { border-left-color: var(--accent-cyan); }
.lever-amber { border-left-color: var(--accent-amber); }
.lever-teal { border-left-color: var(--accent-teal); }

.lever-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; gap: 8px; }
.lever-title-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.cat-tag { font-size: 9px; font-weight: 600; padding: 2px 6px; border-radius: 2px; }
.cat-tag.cat-cyan { color: var(--accent-cyan); background: rgba(34, 211, 238, 0.12); }
.cat-tag.cat-amber { color: var(--accent-amber); background: rgba(251, 191, 36, 0.12); }
.cat-tag.cat-teal { color: var(--accent-teal); background: rgba(45, 212, 191, 0.12); }
.lever-name { font-size: 12px; font-weight: 600; }
.lever-val { font-family: var(--font-mono); font-size: 12px; color: var(--accent-cyan); flex-shrink: 0; }

.lever-slider { width: 100%; margin-bottom: 4px; }
.slider-cyan { accent-color: var(--accent-cyan); }
.slider-amber { accent-color: var(--accent-amber); }
.slider-teal { accent-color: var(--accent-teal); }

.lever-range-hint { font-size: 9px; color: var(--text-dim); font-family: var(--font-mono); margin-bottom: 6px; }
.lever-desc { font-size: 10px; color: var(--text-secondary); line-height: 1.45; margin: 0 0 6px; }

.tradeoff-chip {
  display: inline-block; font-size: 9px; font-family: var(--font-mono);
  padding: 3px 8px; border-radius: 2px; margin-bottom: 6px;
}
.chip-cyan { color: var(--accent-cyan); background: rgba(34, 211, 238, 0.1); }
.chip-amber { color: var(--accent-amber); background: rgba(251, 191, 36, 0.1); }
.chip-teal { color: var(--accent-teal); background: rgba(45, 212, 191, 0.1); }

.detail-toggle {
  font-size: 10px; padding: 0; background: none; border: none;
  color: var(--accent-cyan); cursor: pointer;
}
.detail-toggle:hover { text-decoration: underline; }
.lever-business {
  font-size: 10px; color: var(--text-secondary); line-height: 1.5;
  margin: 6px 0 0; padding: 8px; background: rgba(0,0,0,0.12); border-radius: 3px;
}

.lever-row { margin-bottom: 12px; }
.weight-row { padding: 8px; background: var(--bg-panel-elevated); border-radius: var(--radius); }
.weight-label { font-size: 12px; font-weight: 600; }
.label-cyan { color: var(--accent-cyan); }
.label-amber { color: var(--accent-amber); }
.label-teal { color: var(--accent-teal); }
.weight-desc { font-size: 10px; color: var(--text-dim); margin: 4px 0 0; line-height: 1.4; }

.tri-panel { display: flex; flex-direction: column; align-items: center; }
.score-gap { font-size: 12px; color: var(--text-secondary); margin-top: 8px; text-align: center; }
.score-gap b { color: var(--status-running); font-family: var(--font-mono); }
.score-hint { font-size: 10px; color: var(--text-dim); }
.tri-guide {
  margin: 10px 0 0; padding-left: 16px; font-size: 10px;
  color: var(--text-dim); line-height: 1.5; width: 100%;
}
.tri-guide li { margin-bottom: 3px; }

.idx-compare { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.idx-head { font-size: 11px; font-weight: 600; margin-bottom: 8px; text-align: center; }
.optimal-col .idx-head { color: var(--status-running); }
.idx-col .gauge { margin-bottom: 8px; }
.wscore { text-align: center; font-size: 12px; font-family: var(--font-mono); color: var(--text-dim); margin-top: 6px; }
.wscore.optimal { color: var(--status-running); }

.delta-table { width: 100%; margin-top: 12px; font-size: 11px; border-collapse: collapse; }
.delta-table th, .delta-table td { padding: 4px 8px; border-bottom: 1px solid var(--border-dim); }
.delta-table .up { color: var(--status-running); }
.delta-table .down { color: var(--status-alarm); }

.rationale {
  margin-top: 10px; padding: 8px 10px; font-size: 11px;
  background: rgba(52, 211, 153, 0.08); border-radius: 3px; color: var(--accent-teal); line-height: 1.5;
}
.rationale-label { font-weight: 600; margin-bottom: 4px; color: var(--status-running); }
.optimal-levers {
  margin-top: 8px; font-size: 10px; font-family: var(--font-mono);
  color: var(--text-dim); line-height: 1.5;
}

.sens-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 8px; }
.sens-card {
  padding: 10px; background: var(--bg-panel-elevated);
  border: 1px solid var(--border-dim); border-radius: var(--radius);
}
.sens-title { font-size: 11px; font-weight: 600; margin-bottom: 6px; }
.sens-unit { font-size: 9px; font-weight: 400; color: var(--text-dim); margin-left: 4px; }
.sens-values { display: flex; flex-direction: column; gap: 3px; }
.sens-item { font-size: 10px; font-family: var(--font-mono); color: var(--text-dim); }
.sens-item.up { color: var(--status-running); }
.sens-item.down { color: var(--status-alarm); }

.tradeoff-list { margin: 0; padding-left: 18px; font-size: 11px; color: var(--text-secondary); line-height: 1.55; }
.tradeoff-list li { margin-bottom: 6px; }

.pareto-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; }
.pareto-card {
  padding: 10px; background: var(--bg-panel-elevated);
  border: 1px solid var(--border-dim); border-radius: var(--radius);
  position: relative;
}
.p-rank {
  position: absolute; top: 8px; right: 8px;
  font-size: 9px; color: var(--text-dim); font-family: var(--font-mono);
}
.p-score { font-size: 20px; font-weight: 700; font-family: var(--font-mono); color: var(--accent-cyan); }
.p-score-label { font-size: 9px; color: var(--text-dim); margin-bottom: 6px; }
.p-levers { font-size: 9px; color: var(--text-secondary); line-height: 1.45; margin-bottom: 6px; }
.p-idx { display: flex; gap: 8px; font-size: 10px; font-family: var(--font-mono); }
.idx-e { color: var(--accent-cyan); }
.idx-c { color: var(--accent-amber); }
.idx-q { color: var(--accent-teal); }

.linkage-banner {
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;
  border-color: rgba(45, 212, 191, 0.35); background: rgba(45, 212, 191, 0.06);
  font-size: 12px; color: var(--text-secondary);
}
.linkage-banner strong { color: var(--accent-teal); }
.linkage-banner code { font-family: var(--font-mono); color: var(--accent-amber); }
.banner-btn {
  font-size: 11px; padding: 4px 10px; border-radius: 3px; cursor: pointer;
  border: 1px solid var(--accent-teal); background: transparent; color: var(--accent-teal);
}
</style>
