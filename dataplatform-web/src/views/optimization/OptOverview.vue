<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import OptIndexGauge from '../../components/optimization/OptIndexGauge.vue'
import OptWorkshopPanel from '../../components/optimization/OptWorkshopPanel.vue'
import OptRecommendationList from '../../components/optimization/OptRecommendationList.vue'
import OptLineGrid from '../../components/optimization/OptLineGrid.vue'
import { getOptOverview, getOptLines } from '../../api/optimization'
import { usePolling } from '../../composables/usePolling'
import { fmtPct } from '../../utils/format'

const router = useRouter()
const data = ref(null)
const lineData = ref(null)
const workshopFilter = ref('all')
const lineSearch = ref('')
const loading = ref(true)

async function load() {
  loading.value = true
  const [overview, lines] = await Promise.all([
    getOptOverview().catch(() => null),
    getOptLines().catch(() => null)
  ])
  data.value = overview
  lineData.value = lines
  loading.value = false
}

usePolling(load, 8000)

const extremes = computed(() => data.value?.three_extremes || {})
const layout = computed(() => data.value?.plant_layout || {})
const workshops = computed(() => data.value?.workshops || [])
const models = computed(() => data.value?.models || [])
const recommendations = computed(() => data.value?.recommendations || [])
const headline = computed(() => data.value?.headline_kpis || {})

const allLines = computed(() => lineData.value?.lines || [])

const filteredLines = computed(() => {
  let rows = allLines.value
  if (workshopFilter.value === 'wire') rows = rows.filter(l => l.workshop_id === 'WS-WIRE-01')
  if (workshopFilter.value === 'rod') rows = rows.filter(l => l.workshop_id === 'WS-ROD-01')
  const q = lineSearch.value.trim().toLowerCase()
  if (q) {
    rows = rows.filter(l =>
      String(l.product_line_id).toLowerCase().includes(q)
      || String(l.line_name || '').toLowerCase().includes(q)
    )
  }
  return rows
})

const lineStats = computed(() => {
  const all = allLines.value
  const fromLayout = layout.value
  const sim = fromLayout.telemetry_line_count ?? all.filter(l => l.simulation_enabled || l.data_confidence === 'telemetry').length
  const total = fromLayout.total_line_count ?? all.length
  return {
    total: total || null,
    wire: fromLayout.wire_line_count ?? all.filter(l => l.workshop_id === 'WS-WIRE-01').length,
    rod: fromLayout.rod_line_count ?? all.filter(l => l.workshop_id === 'WS-ROD-01').length,
    telemetry: sim,
    registry: fromLayout.registry_line_count ?? Math.max(0, (total || all.length) - sim),
    loaded: !!data.value && all.length > 0
  }
})

function goModel(route) {
  if (route) router.push(route)
}
</script>

<template>
  <div class="page">
    <PageHeader section="智优决策中心" title="全厂总览" />

    <div class="content">
      <div class="plant-banner panel">
        <div class="banner-title">{{ data?.factory_name || '金桥焊材科技公司' }}</div>
        <div class="banner-sub">
          单体厂房 · 双车间一体布局 ·
          <template v-if="lineStats.loaded">
            {{ lineStats.total }} 条产线（焊丝 {{ lineStats.wire }} + 焊条 {{ lineStats.rod }}）
            · 全链路仿真 {{ lineStats.telemetry }} 线
            <template v-if="lineStats.registry"> / 登记估算 {{ lineStats.registry }} 线</template>
          </template>
          <template v-else>加载产线快照…</template>
        </div>
        <div class="banner-hint">{{ data?.tradeoff_hint }}</div>
      </div>

      <div class="balance-cta panel">
        <div>
          <div class="cta-title">三极致最佳平衡点</div>
          <div class="cta-sub">拖动批次/线速/绿电/规格杠杆，网格搜索 Pareto 前沿与加权最优</div>
        </div>
        <router-link to="/optimization/balance" class="cta-btn">进入权衡分析 →</router-link>
      </div>

      <div class="gauge-row">
        <OptIndexGauge
          label="极致效率指数"
          :value="extremes.efficiency_index"
          :status-label="extremes.efficiency_label"
          color="cyan"
        />
        <OptIndexGauge
          label="极致成本指数"
          :value="extremes.cost_index"
          :status-label="extremes.cost_label"
          color="amber"
        />
        <OptIndexGauge
          label="极致质量指数"
          :value="extremes.quality_index"
          :status-label="extremes.quality_label"
          color="teal"
        />
      </div>

      <div class="kpi-strip">
        <span>运行产线 <b>{{ headline.active_lines ?? '—' }}</b></span>
        <span>仿真产线 <b>{{ headline.telemetry_lines ?? lineStats.telemetry ?? '—' }}</b></span>
        <span>均 OEE <b>{{ fmtPct(headline.avg_oee_pct) }}%</b></span>
        <span>一次合格率 <b>{{ headline.quality_pass_rate_pct }}%</b></span>
        <span>绿电占比 <b>{{ headline.green_power_ratio_pct }}%</b></span>
        <span>高风险订单 <b>{{ headline.high_risk_orders }}</b></span>
        <span>药芯 2025 目标 <b>{{ headline.target_fcw_sales_t_2025 ? (headline.target_fcw_sales_t_2025 / 10000).toFixed(1) + '万t' : '—' }}</b></span>
      </div>

      <div class="grid-2">
        <div class="panel">
          <div class="panel-title">双车间态势</div>
          <div class="ws-row">
            <OptWorkshopPanel v-for="ws in workshops" :key="ws.workshop_id" :workshop="ws" />
          </div>
        </div>
        <div class="panel">
          <div class="panel-title">优先行动建议</div>
          <OptRecommendationList :items="recommendations" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">11 个优化模型</div>
        <div class="model-grid">
          <button
            v-for="m in models"
            :key="m.model_id"
            type="button"
            class="model-card"
            @click="goModel(m.route)"
          >
            <span class="m-order">{{ m.order }}</span>
            <span class="m-id">{{ m.model_id }}</span>
            <span class="m-title">{{ m.title }}</span>
            <span class="m-dim">{{ m.dimension }}</span>
          </button>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">
            全厂产线快照
            <span class="count-hint" v-if="lineStats.loaded">（{{ filteredLines.length }}/{{ lineStats.total }}）</span>
          </div>
          <div class="filter-row">
            <input v-model="lineSearch" type="search" class="line-search" placeholder="搜索产线" />
            <div class="filter-tabs">
              <button :class="{ active: workshopFilter === 'all' }" @click="workshopFilter = 'all'">全部</button>
              <button :class="{ active: workshopFilter === 'wire' }" @click="workshopFilter = 'wire'">焊丝</button>
              <button :class="{ active: workshopFilter === 'rod' }" @click="workshopFilter = 'rod'">焊条</button>
            </div>
          </div>
        </div>
        <div v-if="loading && !filteredLines.length" class="loading-hint">加载 42 线优化快照…</div>
        <OptLineGrid v-else :lines="filteredLines" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 布局见 main.css .page / .page .content */
.panel {
  background: var(--bg-panel);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  padding: 12px 14px;
  margin-bottom: 12px;
}
.panel-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.panel-head .panel-title { margin-bottom: 0; }
.plant-banner { border-left: 3px solid var(--accent-cyan); }
.banner-title { font-size: 16px; font-weight: 600; }
.banner-sub { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
.banner-hint { font-size: 11px; color: var(--text-dim); margin-top: 6px; }
.balance-cta {
  display: flex; justify-content: space-between; align-items: center;
  border-left: 3px solid var(--status-running); margin-bottom: 12px;
}
.cta-title { font-size: 14px; font-weight: 600; }
.cta-sub { font-size: 11px; color: var(--text-dim); margin-top: 2px; }
.cta-btn {
  font-size: 12px; padding: 8px 14px; border-radius: 3px; text-decoration: none;
  border: 1px solid var(--status-running); color: var(--status-running);
  background: rgba(52, 211, 153, 0.1); white-space: nowrap;
}
.cta-btn:hover { background: rgba(52, 211, 153, 0.18); }
.gauge-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 12px; }
.kpi-strip {
  display: flex; flex-wrap: wrap; gap: 16px;
  padding: 10px 14px; margin-bottom: 12px;
  background: var(--bg-panel); border: 1px solid var(--border-dim);
  border-radius: var(--radius); font-size: 12px; color: var(--text-secondary);
}
.kpi-strip b { color: var(--text-primary); font-family: var(--font-mono); }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 1100px) { .grid-2 { grid-template-columns: 1fr; } }
.ws-row { display: flex; flex-direction: column; gap: 10px; }
.model-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 8px; }
.model-card {
  text-align: left; padding: 10px 12px;
  background: var(--bg-panel-elevated); border: 1px solid var(--border-dim);
  border-radius: var(--radius); cursor: pointer; color: inherit;
}
.model-card:hover { border-color: var(--accent-cyan); }
.m-order { font-size: 10px; color: var(--text-dim); margin-right: 6px; }
.m-id { font-family: var(--font-mono); font-size: 10px; color: var(--accent-cyan); }
.m-title { display: block; font-size: 12px; font-weight: 600; margin-top: 4px; }
.m-dim { font-size: 10px; color: var(--text-dim); }
.filter-tabs { display: flex; gap: 4px; }
.filter-tabs button {
  font-size: 11px; padding: 4px 10px; border-radius: 3px;
  border: 1px solid var(--border-dim); background: transparent;
  color: var(--text-dim); cursor: pointer;
}
.filter-tabs button.active { border-color: var(--accent-cyan); color: var(--accent-cyan); }
.filter-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.line-search {
  font-size: 11px; padding: 4px 8px; border-radius: 3px;
  border: 1px solid var(--border-dim); background: var(--bg-root);
  color: var(--text-primary); min-width: 120px;
}
.count-hint { font-size: 11px; color: var(--text-dim); font-weight: 400; margin-left: 6px; }
.loading-hint { font-size: 12px; color: var(--text-dim); padding: 16px; text-align: center; }
</style>
