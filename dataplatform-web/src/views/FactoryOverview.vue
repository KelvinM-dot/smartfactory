<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import LinePickerBar from '../components/LinePickerBar.vue'
import ProcessPipelineTrack from '../components/ProcessPipelineTrack.vue'
import { getFactoryDashboard, getAllAlarms } from '../api'
import { usePolling } from '../composables/usePolling'
import { useAlarmNavigation } from '../composables/useAlarmNavigation'
import { fmtTime, fmtPct } from '../utils/format'
import { orderBadgeTone, orderRiskLabel, orderStatusLabel } from '../utils/orderLabels'
import OrderMustHandleList from '../components/orders/OrderMustHandleList.vue'
import { computeTriageCounts, lineBelongsToWorkshop } from '../utils/orderTriage'
import {
  filterLines,
  kpiModeBadge,
  lineStatusBadge,
  countSimulationLines
} from '../utils/lineCatalog'

const router = useRouter()
const { navigateToAlarmTwin } = useAlarmNavigation()

const factory = ref(null)
const lineCards = ref([])
const energy = ref(null)
const kpis = ref(null)
const logisticsTasks = ref([])
const qualityGates = ref([])
const orderRisk = ref([])
const pendingAlarms = ref([])
const workshopFilter = ref('all')
const lineSearch = ref('')
const lineCardFilter = ref('')
const showInactivePipelines = ref(false)

const factoryLines = computed(() => lineCards.value.map(c => c.line))

const BLOCKING_GATE_DECISIONS = new Set(['hold', 'fail', 'rework'])
const BLOCKING_LOGISTICS_STATUSES = new Set(['pending', 'failed', 'blocked', 'in_progress'])

const factoryName = computed(() => factory.value?.factory_name || '金桥焊材科技公司')

const factoryProfile = computed(() => factory.value?.factory_profile || {})

const totalLineCount = computed(() => {
  const p = factoryProfile.value
  if (p.total_production_line_count) return p.total_production_line_count
  const wire = p.wire_line_count || 0
  const rod = p.rod_line_count || 0
  if (wire || rod) return wire + rod
  return kpis.value?.registry_line_count || lineCards.value.length
})

const DWELL_LABELS = {
  maintenance: '维保停留',
  rework: '返工停留',
  hold: '质控 Hold',
  material_shortage: '缺料等待'
}

const capacitySummary = computed(() => kpis.value?.capacity_summary || null)

const situation = computed(() => {
  const cards = lineCards.value
  const dwellLines = cards.filter(c => c.overview?.data_source?.dwell_mode)
  const cap = capacitySummary.value
  return {
    running: cards.filter(c => c.line.status === 'active').length,
    maintenance: cards.filter(c => c.line.status === 'maintenance').length,
    inactive: cards.filter(c => c.line.status === 'inactive').length,
    pendingAlarms: cards.reduce((s, c) => s + (c.overview?.kpi_bar?.pending_alarms || 0), 0),
    highRisk: kpis.value?.high_risk_orders ?? 0,
    blocked: kpis.value?.blocked_orders ?? 0,
    blockingGates: qualityGates.value.filter(g => BLOCKING_GATE_DECISIONS.has(g.decision)).length,
    pendingLogistics: logisticsTasks.value.filter(t => BLOCKING_LOGISTICS_STATUSES.has(t.status)).length,
    dwellLines: dwellLines.length,
    totalActivePowerKw: energy.value?.total_active_power_kw ?? 0,
    totalRunningEquipment: energy.value?.total_running_equipment ?? 0,
    telemetryRunningEquipment: energy.value?.telemetry_running_equipment ?? 0,
    plantEquipmentCount: energy.value?.plant_equipment_count ?? factoryProfile.value.equipment_count ?? 0,
    totalLineCount: totalLineCount.value,
    telemetryLines: kpis.value?.telemetry_line_count
      ?? countSimulationLines(cards.map(c => c.line)),
    registryEstimated: kpis.value?.registry_line_count_estimated ?? 0,
    designWireCap: cap?.design_wire_t_per_year,
    designRodCap: cap?.design_rod_t_per_year,
    capacityAligned: cap?.total_capacity_aligned
  }
})

function kpiModeLabel(card) {
  return kpiModeBadge(card).label
}

function kpiModeClass(card) {
  return kpiModeBadge(card).cls
}

const filteredLineCards = computed(() => {
  const ids = new Set(
    filterLines(
      factoryLines.value,
      { workshop: workshopFilter.value, search: lineSearch.value }
    ).map(l => l.product_line_id)
  )
  return lineCards.value.filter(c => {
    const id = c.line.product_line_id
    if (lineCardFilter.value && id !== lineCardFilter.value) return false
    return ids.has(id)
  })
})

function pickFactoryLine(lineId) {
  lineCardFilter.value = lineCardFilter.value === lineId ? '' : lineId
}

function onFactoryWorkshopChange(ws) {
  workshopFilter.value = ws
  if (lineCardFilter.value && !lineBelongsToWorkshop(lineCardFilter.value, factoryLines.value, ws)) {
    lineCardFilter.value = ''
  }
}

const lineDwellItems = computed(() =>
  lineCards.value
    .filter(c => c.overview?.data_source?.dwell_mode)
    .map(c => {
      const ds = c.overview.data_source
      return {
        line_id: c.line.product_line_id,
        line_name: c.line.name,
        dwell_mode: ds.dwell_mode,
        dwell_label: DWELL_LABELS[ds.dwell_mode] || ds.dwell_mode,
        dwell_reason: ds.dwell_reason || '—',
        current_step: ds.current_step || '—',
        active_power_kw: ds.active_power_kw ?? 0,
        running_equipment_count: ds.running_equipment_count ?? 0,
        raw_material_low: ds.raw_material_low
      }
    })
    .slice(0, 6)
)

const lineEnergyById = computed(() => {
  const map = {}
  for (const row of energy.value?.line_breakdown || []) {
    if (row.product_line_id) map[row.product_line_id] = row
  }
  return map
})

const lineEnergyItems = computed(() => {
  const rows = (energy.value?.line_breakdown || []).map(item => ({
    ...item,
    dwell_label: item.dwell_mode ? (DWELL_LABELS[item.dwell_mode] || item.dwell_mode) : null
  }))
  const simRows = rows.filter(r => r.simulation_enabled)
  const registryRows = rows.filter(r => !r.simulation_enabled)
  if (!registryRows.length) return simRows
  const aggRunning = registryRows.reduce((s, r) => s + (r.running_equipment_count || 0), 0)
  const aggPower = registryRows.reduce((s, r) => s + (r.active_power_kw || 0), 0)
  const aggKwh = registryRows.reduce((s, r) => s + (r.consumption_kwh || 0), 0)
  return [
    ...simRows,
    {
      product_line_id: 'REGISTRY-AGG',
      line_name: `登记产线 ×${registryRows.length}`,
      simulation_enabled: false,
      running_equipment_count: aggRunning,
      active_power_kw: Math.round(aggPower * 10) / 10,
      consumption_kwh: Math.round(aggKwh * 10) / 10,
      dwell_label: null
    }
  ]
})

const mustHandleGates = computed(() =>
  qualityGates.value
    .filter(g => BLOCKING_GATE_DECISIONS.has(g.decision))
    .slice(0, 5)
)

const mustHandleLogistics = computed(() =>
  logisticsTasks.value
    .filter(t => BLOCKING_LOGISTICS_STATUSES.has(t.status))
    .slice(0, 5)
)

const mustHandleOrders = computed(() => orderRisk.value)

const orderKpi = computed(() => {
  const k = kpis.value || {}
  const list = orderRisk.value || []
  const triage = computeTriageCounts(list)
  const inProgress = list.filter(o => o.order_status === 'in_progress').length
  const released = list.filter(o => o.order_status === 'released').length
  return {
    total: k.total_orders ?? list.length,
    active: triage.active,
    mustHandle: triage.mustHandle,
    inProgress,
    released,
    blocked: k.blocked_orders ?? triage.blocked,
    highRisk: k.high_risk_orders ?? triage.highRisk,
    readyShip: k.ready_to_ship_orders ?? triage.readyShip,
    constrained: k.constrained_orders ?? 0,
    releasePct: k.release_progress_pct ?? 0,
    dueSoon: triage.dueSoon
  }
})

const topCriticalAlarms = computed(() =>
  [...pendingAlarms.value]
    .sort((a, b) => {
      if (a.severity === 'critical' && b.severity !== 'critical') return -1
      if (b.severity === 'critical' && a.severity !== 'critical') return 1
      return String(b.triggered_at).localeCompare(String(a.triggered_at))
    })
    .slice(0, 5)
)

function buildStaticPipeline(card, aggregateStatus) {
  const steps = card.line?.process_steps
  if (!Array.isArray(steps) || !steps.length) return null
  return {
    ...card,
    pipelineFrozen: card.line.status === 'inactive',
    overview: {
      ...(card.overview || {}),
      process_pipeline: steps.map(stepId => ({
        process_step_id: stepId,
        display_name: stepId,
        aggregate_status: aggregateStatus
      }))
    }
  }
}

/** 含停用线：停用线展示静态工序轨（全部 stopped） */
const pipelineLines = computed(() =>
  lineCards.value
    .map(card => {
      if (card.overview?.process_pipeline?.length) {
        return { ...card, pipelineFrozen: card.line.status === 'inactive' }
      }
      if (card.line?.status === 'inactive') {
        return buildStaticPipeline(card, 'stopped')
      }
      if (Array.isArray(card.line?.process_steps) && card.line.process_steps.length) {
        const agg = card.line.status === 'maintenance' ? 'manual' : 'running'
        return buildStaticPipeline(card, agg)
      }
      return null
    })
    .filter(Boolean)
)

const activePipelineLines = computed(() => {
  const ids = new Set(filteredLineCards.value.map(c => c.line.product_line_id))
  return pipelineLines.value.filter(c => {
    if (!ids.has(c.line.product_line_id)) return false
    if (showInactivePipelines.value) return true
    return c.line.status === 'active' || c.line.status === 'maintenance'
  })
})

const inactivePipelineLines = computed(() => {
  const ids = new Set(filteredLineCards.value.map(c => c.line.product_line_id))
  return pipelineLines.value.filter(c =>
    ids.has(c.line.product_line_id) && c.line.status === 'inactive'
  )
})

function pipelineMaxParams(card) {
  return card.line?.simulation_enabled ? 3 : 2
}

const energyCards = computed(() => {
  if (!energy.value) return []
  return [
    { label: '总能耗', value: `${energy.value.total_consumption_kwh ?? 0} kWh` },
    { label: '实时功率', value: `${energy.value.total_active_power_kw ?? 0} kW` },
    {
      label: '运行设备(全厂估算)',
      value: `${energy.value.total_running_equipment ?? 0} 台`,
      hint: `遥测 ${energy.value.telemetry_running_equipment ?? 0} · 装机 ${energy.value.plant_equipment_count ?? 0}`
    },
    { label: '绿电', value: `${energy.value.green_power_kwh ?? 0} kWh` },
    { label: '电网', value: `${energy.value.grid_power_kwh ?? 0} kWh` },
    { label: '绿电占比', value: `${energy.value.green_power_ratio_pct ?? 0}%` }
  ]
})

const kpiCards = computed(() => {
  if (!kpis.value) return []
  return [
    { label: '平均 OEE', value: `${fmtPct(kpis.value.avg_oee_pct, 0)}%` },
    { label: '质量通过率', value: `${fmtPct(kpis.value.quality_pass_rate_pct)}%` },
    { label: '物流完成率', value: `${fmtPct(kpis.value.logistics_completion_rate_pct)}%` },
    { label: '投放进度', value: `${fmtPct(kpis.value.release_progress_pct)}%` },
    { label: '受约束订单', value: `${kpis.value.constrained_orders ?? 0} 单` }
  ]
})

const stateSummaryCards = computed(() => {
  if (!kpis.value) return []
  return [
    { label: '受阻订单', value: `${kpis.value.blocked_orders ?? 0} 单`, tone: 'stopped' },
    { label: '待发运订单', value: `${kpis.value.ready_to_ship_orders ?? 0} 单`, tone: 'manual' },
    { label: 'Hold 批次', value: `${kpis.value.hold_batches ?? 0} 批`, tone: 'stopped' },
    { label: '待发运批次', value: `${kpis.value.ready_to_ship_batches ?? 0} 批`, tone: 'manual' }
  ]
})

const topLogisticsTasks = computed(() => logisticsTasks.value.slice(0, 6))
const topQualityGates = computed(() => qualityGates.value.slice(0, 6))

async function load() {
  const dash = await getFactoryDashboard()
  factory.value = dash.factory
  lineCards.value = dash.lines
  energy.value = dash.energy
  kpis.value = dash.kpis
  logisticsTasks.value = dash.logisticsTasks || []
  qualityGates.value = dash.qualityGates || []
  orderRisk.value = dash.orderRisk || []

  const lineIds = (dash.lines || []).map(card => card.line.product_line_id)
  const alarms = await getAllAlarms(lineIds, 'pending').catch(() => [])
  const cardByLine = Object.fromEntries(
    (dash.lines || []).map(card => [card.line.product_line_id, card])
  )
  pendingAlarms.value = alarms.map(a => {
    const card = cardByLine[a.product_line_id]
    return {
      ...a,
      line_name: card?.line?.name,
      equipment_list: card?.equipment || []
    }
  })
}

function goWorkbench(lineId) {
  router.push({ path: `/lines/${lineId}`, query: { tab: 'overview' } })
}

function workbenchLabel(line) {
  if (line.status === 'active') return '进入工作台'
  if (line.status === 'maintenance') return '查看维护状态'
  return '查看产线'
}

function goBatches() {
  router.push('/batches')
}

function goOrders() {
  router.push({ path: '/orders', query: { view: 'workbench' } })
}

function goOrder(orderId) {
  router.push(`/orders/${orderId}`)
}

function resolveLineOrderId(card, lineId) {
  const batchOrder = card.overview?.current_batch?.production_order_id
  if (batchOrder) return batchOrder
  const active = orderRisk.value.find(o => {
    const lines = o.assigned_line_ids || []
    return lines.includes(lineId) && o.order_status === 'in_progress'
  })
  if (active?.production_order_id) return active.production_order_id
  const queued = orderRisk.value.find(o => {
    const lines = o.assigned_line_ids || []
    return lines.includes(lineId) && ['released', 'in_progress'].includes(o.order_status)
  })
  return queued?.production_order_id || null
}

const lineExtrasById = computed(() => {
  const result = {}
  for (const card of lineCards.value) {
    const lineId = card.line?.product_line_id
    if (!lineId) continue
    const energyRow = lineEnergyById.value[lineId]
    result[lineId] = {
      orderId: resolveLineOrderId(card, lineId),
      energy: energyRow
        ? {
            consumptionKwh: energyRow.consumption_kwh ?? 0,
            activePowerKw: energyRow.active_power_kw ?? 0
          }
        : null
    }
  }
  return result
})

function goAlarms() {
  router.push('/alarms')
}

function goBatch(batchId) {
  if (batchId) router.push(`/batches/${batchId}`)
}

function goAlarmTwin(alarm) {
  const lineId = alarm.product_line_id
  const card = lineCards.value.find(c => c.line.product_line_id === lineId)
  navigateToAlarmTwin(alarm, card?.equipment || [], lineId)
}

function lineStatus(line) {
  return lineStatusBadge(line)
}

function gateBadge(decision) {
  if (decision === 'pass') return 'running'
  if (decision === 'hold' || decision === 'rework') return 'manual'
  return 'stopped'
}

function logisticsBadge(status) {
  if (status === 'completed') return 'running'
  if (status === 'in_progress' || status === 'pending') return 'manual'
  return 'stopped'
}

function riskBadge(level) {
  return orderBadgeTone(level)
}

function dwellBadge(mode) {
  if (mode === 'material_shortage') return 'stopped'
  if (mode === 'maintenance' || mode === 'rework' || mode === 'hold') return 'manual'
  return 'unknown'
}

usePolling(load, 5000)
</script>

<template>
  <div class="factory-root">
    <PageHeader section="工厂" title="工厂驾驶舱" />

    <div class="page-body full-bleed" v-if="factory">
      <!-- ① 今日态势 -->
      <div class="situation-strip panel">
        <div class="panel-body compact situation-body">
          <div class="situation-title">{{ factoryName }}</div>
          <div class="situation-metrics">
            <span class="sit-item running">
              全厂产线 <strong>{{ situation.totalLineCount }}</strong> 条
              <template v-if="factoryProfile.wire_line_count">
                （丝 {{ factoryProfile.wire_line_count }} + 条 {{ factoryProfile.rod_line_count }}）
              </template>
            </span>
            <span class="sit-item running">运行 <strong>{{ situation.running }}</strong> 线</span>
            <span class="sit-item manual">维护 <strong>{{ situation.maintenance }}</strong> 线</span>
            <span class="sit-item stopped">停用 <strong>{{ situation.inactive }}</strong> 线</span>
            <span class="sit-item" :class="{ alarm: situation.pendingAlarms > 0 }">
              待处理报警 <strong>{{ situation.pendingAlarms }}</strong>
            </span>
            <span class="sit-item" :class="{ alarm: situation.blockingGates > 0 }">
              质门阻塞 <strong>{{ situation.blockingGates }}</strong>
            </span>
            <span class="sit-item" :class="{ manual: situation.pendingLogistics > 0 }">
              物流待办 <strong>{{ situation.pendingLogistics }}</strong>
            </span>
            <span class="sit-item" :class="{ manual: situation.dwellLines > 0 }">
              产线停留 <strong>{{ situation.dwellLines }}</strong>
            </span>
            <span class="sit-item running">
              实时功率 <strong>{{ situation.totalActivePowerKw }}</strong> kW
            </span>
            <span class="sit-item">
              运行设备 <strong>{{ situation.totalRunningEquipment }}</strong> 台
              <span class="dim">（遥测 {{ situation.telemetryRunningEquipment }} / 装机 {{ situation.plantEquipmentCount }}）</span>
            </span>
            <span class="sit-item running" v-if="situation.designWireCap">
              设计产能 <strong>{{ situation.designWireCap }}</strong> + <strong>{{ situation.designRodCap }}</strong> t/年
              <span class="dim">（丝+条，{{ situation.capacityAligned ? '已对齐40万吨' : '待核对' }}）</span>
            </span>
            <span class="sit-item running">
              仿真产线 <strong>{{ situation.telemetryLines }}</strong> 条全链路建模
              <template v-if="situation.registryEstimated">
                · 估算 <strong>{{ situation.registryEstimated }}</strong> 条
              </template>
            </span>
          </div>
        </div>
      </div>

      <div
        class="panel order-kpi-band"
        :class="{ alarm: orderKpi.blocked > 0 || orderKpi.highRisk > 0 }"
        @click="goOrders"
      >
        <div class="panel-body compact order-kpi-band-body">
          <div class="order-kpi-band-inner">
            <div class="order-kpi-hero">
              <div class="order-kpi-head">
                <span class="order-kpi-title">交付与订单</span>
                <button type="button" class="tab-btn mini" @click.stop="goOrders">订单中心 →</button>
              </div>
              <div class="order-kpi-primary">
                <span class="order-kpi-value">{{ orderKpi.active }}</span>
                <span class="order-kpi-label">在制订单</span>
              </div>
            </div>

            <div class="order-kpi-chips">
              <div class="order-kpi-chip">
                <span class="chip-label">订单总量</span>
                <span class="chip-value">{{ orderKpi.total }}<small>单</small></span>
              </div>
              <div class="order-kpi-chip running">
                <span class="chip-label">执行中</span>
                <span class="chip-value">{{ orderKpi.inProgress }}<small>单</small></span>
              </div>
              <div class="order-kpi-chip manual">
                <span class="chip-label">已下达</span>
                <span class="chip-value">{{ orderKpi.released }}<small>单</small></span>
              </div>
              <div class="order-kpi-chip" :class="{ stopped: orderKpi.blocked > 0 }">
                <span class="chip-label">受阻</span>
                <span class="chip-value">{{ orderKpi.blocked }}<small>单</small></span>
              </div>
              <div class="order-kpi-chip" :class="{ alarm: orderKpi.highRisk > 0 }">
                <span class="chip-label">高风险</span>
                <span class="chip-value">{{ orderKpi.highRisk }}<small>单</small></span>
              </div>
              <div class="order-kpi-chip manual">
                <span class="chip-label">待发运</span>
                <span class="chip-value">{{ orderKpi.readyShip }}<small>单</small></span>
              </div>
            </div>

            <div class="order-kpi-side">
              <div class="order-kpi-progress-wrap">
                <div class="order-kpi-progress-head">
                  <span>APS 投放进度</span>
                  <strong>{{ fmtPct(orderKpi.releasePct) }}%</strong>
                </div>
                <div class="order-kpi-progress-track">
                  <div
                    class="order-kpi-progress-fill"
                    :style="{ width: `${Math.min(100, Math.max(0, orderKpi.releasePct))}%` }"
                  />
                </div>
              </div>
              <div class="order-kpi-alerts">
                <span v-if="orderKpi.mustHandle > 0" class="order-kpi-alert">
                  需关注 <strong>{{ orderKpi.mustHandle }}</strong> 单
                </span>
                <span v-else-if="orderKpi.dueSoon > 0" class="order-kpi-hint">
                  7 日内交期 <strong>{{ orderKpi.dueSoon }}</strong> 单
                </span>
                <span v-if="orderKpi.constrained > 0" class="order-kpi-hint">
                  受约束 <strong>{{ orderKpi.constrained }}</strong> 单
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ② 必须处理 -->
      <div class="must-handle-grid">
        <div class="panel must-panel">
          <div class="panel-head">
            <h3>精益营运 · 产线停留</h3>
          </div>
          <div class="panel-body compact">
            <div v-if="lineDwellItems.length" class="compact-table">
              <div class="compact-row head dwell-row">
                <span>产线</span><span>停留类型</span><span>工序</span><span>原因 / 功率</span>
              </div>
              <div
                v-for="item in lineDwellItems"
                :key="item.line_id"
                class="compact-row dwell-row clickable"
                @click="goWorkbench(item.line_id)"
              >
                <span>{{ item.line_name }}</span>
                <span><span class="line-badge" :class="dwellBadge(item.dwell_mode)">{{ item.dwell_label }}</span></span>
                <span class="mono">{{ item.current_step }}</span>
                <span class="dim">
                  {{ item.dwell_reason }}
                  <template v-if="item.raw_material_low"> · 缺料场景</template>
                  · {{ item.active_power_kw }} kW / {{ item.running_equipment_count }} 台运行
                </span>
              </div>
            </div>
            <div v-else class="empty-state">全厂产线无停留，WIP 正常推进</div>
          </div>
        </div>
        <div class="panel must-panel">
          <div class="panel-head">
            <h3>必须处理 · 订单风险</h3>
            <button class="tab-btn" @click="goOrders">交付与订单 →</button>
          </div>
          <div class="panel-body compact">
            <OrderMustHandleList
              :orders="mustHandleOrders"
              :limit="5"
              @select="goOrder"
            />
          </div>
        </div>

        <div class="panel must-panel">
          <div class="panel-head">
            <h3>必须处理 · 待处理报警</h3>
            <button class="tab-btn" @click="goAlarms">报警中心 →</button>
          </div>
          <div class="panel-body compact">
            <div v-if="topCriticalAlarms.length" class="compact-table">
              <div class="compact-row head alarm-row">
                <span>时间</span><span>产线</span><span>设备</span><span>描述</span>
              </div>
              <div
                v-for="a in topCriticalAlarms"
                :key="a.alarm_id"
                class="compact-row alarm-row clickable"
                :class="{ critical: a.severity === 'critical' }"
                @click="goAlarmTwin(a)"
              >
                <span>{{ fmtTime(a.triggered_at) }}</span>
                <span>{{ a.line_name || a.product_line_id }}</span>
                <span class="mono">{{ a.equipment_id }}</span>
                <span>{{ a.alarm_message }}</span>
              </div>
            </div>
            <div v-else class="empty-state">全厂无待处理报警</div>
          </div>
        </div>

        <div class="panel must-panel">
          <div class="panel-head">
            <h3>必须处理 · 质量门</h3>
            <button class="tab-btn" @click="goBatches">批次与物料 →</button>
          </div>
          <div class="panel-body compact">
            <div v-if="mustHandleGates.length" class="compact-table">
              <div class="compact-row head gate-row">
                <span>批次</span><span>工序</span><span>判定</span><span>原因</span>
              </div>
              <div
                v-for="g in mustHandleGates"
                :key="g.gate_event_id"
                class="compact-row gate-row clickable"
                @click="goBatch(g.batch_id)"
              >
                <span class="mono">{{ g.batch_id }}</span>
                <span>{{ g.process_step_id || '—' }}</span>
                <span><span class="line-badge" :class="gateBadge(g.decision)">{{ g.decision }}</span></span>
                <span class="dim">{{ g.reason_text || g.reason_code || '—' }}</span>
              </div>
            </div>
            <div v-else class="empty-state">暂无阻塞质门</div>
          </div>
        </div>

        <div class="panel must-panel">
          <div class="panel-head">
            <h3>必须处理 · 物流任务</h3>
            <button class="tab-btn" @click="goBatches">批次与物料 →</button>
          </div>
          <div class="panel-body compact">
            <div v-if="mustHandleLogistics.length" class="compact-table">
              <div class="compact-row head logistics-row">
                <span>批次</span><span>类型</span><span>状态</span><span>路径</span>
              </div>
              <div
                v-for="t in mustHandleLogistics"
                :key="t.task_id"
                class="compact-row logistics-row clickable"
                @click="goBatch(t.product_batch)"
              >
                <span class="mono">{{ t.product_batch || '—' }}</span>
                <span>{{ t.task_type || '—' }}</span>
                <span><span class="line-badge" :class="logisticsBadge(t.status)">{{ t.status }}</span></span>
                <span class="dim">{{ t.source_location_id || '—' }} → {{ t.target_location_id || '—' }}</span>
              </div>
            </div>
            <div v-else class="empty-state">暂无待办物流</div>
          </div>
        </div>
      </div>

      <!-- ③ 产线健康 -->
      <div class="line-filter-bar panel">
        <div class="panel-body compact filter-body">
          <LinePickerBar
            :lines="factoryLines"
            :workshop="workshopFilter"
            :search="lineSearch"
            :active-line-id="lineCardFilter"
            @update:workshop="onFactoryWorkshopChange"
            @update:search="lineSearch = $event"
            @pick-line="pickFactoryLine"
          />
        </div>
      </div>
      <div class="line-cards">
        <div
          v-for="card in filteredLineCards"
          :key="card.line.product_line_id"
          class="line-card panel"
          :class="lineStatus(card.line).cls"
        >
          <div class="panel-head">
            <h3>{{ card.line.name }}</h3>
            <span class="line-badge" :class="lineStatus(card.line).cls">{{ lineStatus(card.line).label }}</span>
            <span class="line-badge" :class="kpiModeClass(card)">{{ kpiModeLabel(card) }}</span>
          </div>
          <div class="panel-body compact">
            <div class="lc-id">{{ card.line.product_line_id }}</div>
            <div class="lc-meta" v-if="lineExtrasById[card.line.product_line_id]">
              <div class="lc-meta-row">
                <span class="lc-meta-label">当前订单</span>
                <span
                  v-if="lineExtrasById[card.line.product_line_id].orderId"
                  class="lc-meta-value mono lc-order-link"
                  @click.stop="goOrder(lineExtrasById[card.line.product_line_id].orderId)"
                >{{ lineExtrasById[card.line.product_line_id].orderId }}</span>
                <span v-else class="lc-meta-value dim">—</span>
              </div>
              <div
                class="lc-meta-row"
                v-if="lineExtrasById[card.line.product_line_id].energy"
              >
                <span class="lc-meta-label">能耗</span>
                <span class="lc-meta-value">
                  <strong>{{ lineExtrasById[card.line.product_line_id].energy.consumptionKwh }}</strong> kWh
                  <span class="dim">· {{ lineExtrasById[card.line.product_line_id].energy.activePowerKw }} kW</span>
                </span>
              </div>
            </div>
            <div v-if="card.line.status === 'inactive'" class="lc-inactive-hint">
              产线已停用 · 无在制批次 · 不接收模拟器实时推送
            </div>
            <div v-else-if="card.line.status === 'maintenance'" class="lc-maint-hint">
              计划/故障维保中 · 产能暂停
            </div>
            <template v-else-if="card.overview">
              <div class="lc-kpi">
                <span>OEE <strong>{{ fmtPct(card.overview.kpi_bar?.oee_pct) }}%</strong></span>
                <span>报警 <strong>{{ card.overview.kpi_bar?.pending_alarms ?? 0 }}</strong></span>
              </div>
              <div class="lc-batch" v-if="card.overview.current_batch">
                批次 {{ card.overview.current_batch.batch_id }}
              </div>
            </template>
            <div v-else-if="card.error" class="lc-inactive-hint dim">
              数据暂不可用（{{ card.error }}）
            </div>
            <div class="lc-actions">
              <button class="tab-btn primary" @click="goWorkbench(card.line.product_line_id)">
                {{ workbenchLabel(card.line) }}
              </button>
              <button class="tab-btn" @click="goBatches">批次</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ④ 工序监控 · 全厂仿真产线 -->
      <div class="pipe-section-head">
        <h3>工序监控 · 实时遥测</h3>
        <label v-if="inactivePipelineLines.length" class="pipe-toggle">
          <input v-model="showInactivePipelines" type="checkbox" />
          显示停用产线 ({{ inactivePipelineLines.length }})
        </label>
      </div>
      <div
        class="panel pipe-panel"
        v-for="card in activePipelineLines"
        :key="'pipe-' + card.line.product_line_id"
        :class="{ 'pipe-frozen': card.pipelineFrozen, 'pipe-sim': card.line.simulation_enabled }"
      >
        <div class="panel-head">
          <h3>
            {{ card.line.name }}
            <span class="mono dim">{{ card.line.product_line_id }}</span>
            <span v-if="card.line.simulation_enabled" class="line-badge running">全链路仿真</span>
            <span v-else-if="card.pipelineFrozen" class="line-badge stopped">停用</span>
            <span v-else-if="card.line.status === 'maintenance'" class="line-badge manual">维保</span>
            <span v-if="kpiModeLabel(card) === '实时遥测'" class="line-badge running">遥测</span>
          </h3>
          <button class="tab-btn" @click="goWorkbench(card.line.product_line_id)">工作台 →</button>
        </div>
        <div class="panel-body">
          <ProcessPipelineTrack
            :pipeline="card.overview.process_pipeline"
            :equipment="card.equipment"
            :data-points="card.dataPoints"
            :line="card.line"
            compact
            :max-params="pipelineMaxParams(card)"
          />
        </div>
      </div>

      <!-- 运营详情（折叠） -->
      <details class="ops-details panel">
        <summary class="ops-summary">运营详情 · KPI / 能耗 / 物流 / 质门 / 全量订单风险</summary>
        <div class="ops-inner">
          <div class="summary-grid" v-if="kpis">
            <div class="panel" v-for="card in kpiCards" :key="card.label">
              <div class="panel-body compact summary-card">
                <div class="summary-label">{{ card.label }}</div>
                <div class="summary-value">{{ card.value }}</div>
              </div>
            </div>
          </div>

          <div class="summary-grid state-grid" v-if="kpis">
            <div class="panel" v-for="card in stateSummaryCards" :key="card.label">
              <div class="panel-body compact summary-card" :class="card.tone">
                <div class="summary-label">{{ card.label }}</div>
                <div class="summary-value">{{ card.value }}</div>
              </div>
            </div>
          </div>

          <div class="summary-grid" v-if="energy">
            <div class="panel" v-for="card in energyCards" :key="card.label">
              <div class="panel-body compact summary-card">
                <div class="summary-label">{{ card.label }}</div>
                <div class="summary-value">{{ card.value }}</div>
                <div v-if="card.hint" class="summary-hint">{{ card.hint }}</div>
              </div>
            </div>
          </div>

          <div class="ops-grid">
            <div class="panel">
              <div class="panel-head"><h3>工厂物流任务</h3></div>
              <div class="panel-body compact">
                <div v-if="topLogisticsTasks.length" class="list-table">
                  <div class="list-row head"><span>任务</span><span>批次</span><span>路径</span><span>状态</span></div>
                  <div class="list-row" v-for="task in topLogisticsTasks" :key="task.task_id">
                    <span>{{ task.task_type || '—' }}</span>
                    <span>{{ task.product_batch || task.material_batch || '—' }}</span>
                    <span>{{ task.source_location_id || '—' }} → {{ task.target_location_id || '—' }}</span>
                    <span class="line-badge" :class="task.status === 'completed' ? 'running' : (task.status === 'failed' ? 'stopped' : 'manual')">{{ task.status }}</span>
                  </div>
                </div>
                <div v-else class="empty-state">暂无物流任务</div>
              </div>
            </div>
            <div class="panel">
              <div class="panel-head"><h3>质量门控</h3></div>
              <div class="panel-body compact">
                <div v-if="topQualityGates.length" class="list-table">
                  <div class="list-row head"><span>批次</span><span>工序</span><span>判定</span><span>时间</span></div>
                  <div class="list-row" v-for="gate in topQualityGates" :key="gate.gate_event_id">
                    <span>{{ gate.batch_id || '—' }}</span>
                    <span>{{ gate.process_step_id || '—' }}</span>
                    <span class="line-badge" :class="gateBadge(gate.decision)">{{ gate.decision }}</span>
                    <span>{{ gate.decided_at ? String(gate.decided_at).slice(5, 16).replace('T', ' ') : '—' }}</span>
                  </div>
                </div>
                <div v-else class="empty-state">暂无质量门记录</div>
              </div>
            </div>
          </div>

          <div class="panel" v-if="lineEnergyItems.length">
            <div class="panel-head"><h3>产线能耗分解 · 动态计量</h3></div>
            <div class="panel-body compact">
              <div class="list-table energy-table">
                <div class="list-row head">
                  <span>产线</span><span>实时功率</span><span>运行设备</span><span>能耗</span><span>停留</span>
                </div>
                <div class="list-row" v-for="item in lineEnergyItems" :key="item.product_line_id">
                  <span>{{ item.line_name || item.product_line_id }}</span>
                  <span>{{ item.active_power_kw ?? '—' }} kW</span>
                  <span>{{ item.running_equipment_count ?? '—' }} 台</span>
                  <span>{{ item.consumption_kwh }} kWh</span>
                  <span>
                    <span v-if="item.dwell_label" class="line-badge" :class="dwellBadge(item.dwell_mode)">{{ item.dwell_label }}</span>
                    <span v-else class="dim">—</span>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-head"><h3>全量订单交付风险</h3></div>
            <div class="panel-body compact">
              <div v-if="orderRisk.length" class="list-table">
                <div class="list-row head order-row">
                  <span>订单</span><span>进度</span><span>风险</span><span>阻塞</span>
                </div>
                <div
                  class="list-row order-row clickable"
                  v-for="item in orderRisk"
                  :key="item.production_order_id"
                  @click="goOrder(item.production_order_id)"
                >
                  <span class="mono">{{ item.production_order_id }}</span>
                  <span>{{ item.completed_quantity_t }} / {{ item.planned_quantity_t }} t</span>
                  <span class="line-badge" :class="riskBadge(item.delivery_risk_level)">{{ orderRiskLabel(item.delivery_risk_level) }}</span>
                  <span class="dim">{{ (item.blocking_reasons || []).join('、') || '—' }}</span>
                </div>
              </div>
              <div v-else class="empty-state">暂无订单风险数据</div>
            </div>
          </div>
        </div>
      </details>
    </div>

    <div class="page-body" v-else><div class="loading-state">LOADING…</div></div>
  </div>
</template>

<style scoped>
/* 布局见 main.css .factory-root */

.situation-strip { margin-bottom: var(--grid-gap); }
.situation-body { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
.situation-title { font-size: 18px; font-weight: 600; }
.situation-metrics { display: flex; flex-wrap: wrap; gap: 14px; font-size: 12px; color: var(--text-secondary); }
.sit-item strong { font-family: var(--font-mono); font-size: 15px; margin-left: 4px; color: var(--text-primary); }
.sit-item.running strong { color: var(--status-running); }
.sit-item.manual strong { color: var(--status-warning); }
.sit-item.stopped strong { color: var(--status-stopped); }
.sit-item.alarm strong { color: var(--status-alarm); }

.tab-btn.mini { font-size: 11px; padding: 2px 8px; }

.order-kpi-band {
  width: 100%;
  margin-bottom: var(--grid-gap);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  border-left: 3px solid var(--accent-cyan);
}
.order-kpi-band:hover {
  border-color: rgba(34, 211, 238, 0.35);
  background: rgba(34, 211, 238, 0.04);
}
.order-kpi-band.alarm { border-left-color: var(--status-alarm); }
.order-kpi-band-body { padding-top: 14px; padding-bottom: 14px; }
.order-kpi-band-inner {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(0, 2.4fr) minmax(200px, 0.9fr);
  gap: 20px 28px;
  align-items: stretch;
}
.order-kpi-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.order-kpi-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.order-kpi-primary {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.order-kpi-value {
  font-size: 40px;
  font-weight: 600;
  font-family: var(--font-mono);
  color: var(--accent-cyan);
  line-height: 1;
}
.order-kpi-band.alarm .order-kpi-value { color: var(--status-alarm); }
.order-kpi-label { font-size: 14px; color: var(--text-secondary); }
.order-kpi-chips {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  align-self: center;
}
.order-kpi-chip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 4px;
  border: 1px solid var(--border-dim);
  background: rgba(255, 255, 255, 0.02);
  min-width: 0;
}
.chip-label {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.chip-value {
  font-size: 20px;
  font-weight: 600;
  font-family: var(--font-mono);
  color: var(--text-primary);
  line-height: 1.1;
}
.chip-value small {
  font-size: 11px;
  font-weight: 400;
  color: var(--text-secondary);
  margin-left: 2px;
}
.order-kpi-chip.running .chip-value { color: var(--status-running); }
.order-kpi-chip.manual .chip-value { color: var(--status-warning); }
.order-kpi-chip.stopped .chip-value { color: var(--status-stopped); }
.order-kpi-chip.alarm .chip-value { color: var(--status-alarm); }
.order-kpi-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding-left: 20px;
  border-left: 1px solid var(--border-dim);
}
.order-kpi-progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.order-kpi-progress-head strong {
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--text-primary);
}
.order-kpi-progress-track {
  height: 6px;
  border-radius: 3px;
  background: rgba(100, 116, 139, 0.25);
  overflow: hidden;
}
.order-kpi-progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.55), var(--accent-cyan));
  transition: width 0.35s ease;
}
.order-kpi-band.alarm .order-kpi-progress-fill {
  background: linear-gradient(90deg, rgba(248, 113, 113, 0.55), var(--status-alarm));
}
.order-kpi-alerts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  font-size: 11px;
  color: var(--text-secondary);
}
.order-kpi-alert strong { color: var(--status-alarm); }
.order-kpi-hint strong {
  font-family: var(--font-mono);
  color: var(--text-primary);
}

.must-handle-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--grid-gap);
  margin-bottom: var(--grid-gap);
}
.compact-row.gate-row { grid-template-columns: 1.1fr 0.9fr 0.7fr 1.4fr; }
.compact-row.logistics-row { grid-template-columns: 1.1fr 0.8fr 0.7fr 1.4fr; }
.compact-row.dwell-row { grid-template-columns: 0.9fr 0.8fr 0.8fr 1.6fr; }
.must-panel { min-height: 180px; }

.compact-table { display: flex; flex-direction: column; gap: 6px; }
.compact-row {
  display: grid;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  padding: 6px 4px;
  border-radius: 3px;
}
.compact-row.risk-row { grid-template-columns: 1.1fr 1fr 0.7fr 1.4fr; }
.compact-row.alarm-row { grid-template-columns: 0.7fr 1fr 0.9fr 1.4fr; }
.compact-row.head { font-size: 11px; color: var(--text-secondary); text-transform: uppercase; }
.compact-row.clickable { cursor: pointer; }
.compact-row.clickable:hover { background: rgba(34, 211, 238, 0.06); }
.compact-row.critical { border-left: 2px solid var(--status-alarm); padding-left: 8px; }
.mono { font-family: var(--font-mono); font-size: 11px; }
.dim { color: var(--text-secondary); font-size: 11px; }

.line-filter-bar { margin-bottom: var(--grid-gap); }
.filter-body {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.filter-field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-dim);
}
.filter-field select,
.filter-field input {
  background: var(--bg-root);
  border: 1px solid var(--border-dim);
  color: var(--text-primary);
  border-radius: 3px;
  padding: 4px 8px;
  font-size: 12px;
}
.filter-hint { font-size: 11px; color: var(--text-secondary); margin-left: auto; }
.pipe-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--grid-gap);
  gap: 12px;
}
.pipe-section-head h3 {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.pipe-toggle {
  font-size: 11px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.line-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--grid-gap); margin-bottom: var(--grid-gap); }
.line-card.stopped { opacity: 0.92; border-color: rgba(100, 116, 139, 0.35); }
.lc-inactive-hint, .lc-maint-hint {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 10px;
  padding: 8px 10px;
  border-radius: 4px;
  background: rgba(100, 116, 139, 0.08);
  border-left: 2px solid var(--status-stopped);
}
.lc-maint-hint { border-left-color: var(--status-warning); background: rgba(251, 191, 36, 0.06); }
.pipe-panel { margin-bottom: var(--grid-gap); }
.pipe-panel.pipe-sim { border-color: rgba(34, 211, 238, 0.25); }
.pipe-panel.pipe-frozen { opacity: 0.88; }
.pipe-panel.pipe-frozen .panel-head h3 { display: flex; align-items: center; gap: 8px; }
.registry-pipes-details { margin-bottom: var(--grid-gap); }
.registry-pipes-summary {
  cursor: pointer;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  list-style: none;
}
.registry-pipes-summary::-webkit-details-marker { display: none; }
.registry-pipe { margin-top: var(--grid-gap); border-style: dashed; }
.lc-id { font-family: var(--font-mono); font-size: 11px; color: var(--accent-cyan); margin-bottom: 8px; }
.lc-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
  padding: 8px 10px;
  border-radius: 4px;
  background: rgba(100, 116, 139, 0.06);
  border: 1px solid var(--border-dim);
}
.lc-meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
}
.lc-meta-label { color: var(--text-secondary); flex-shrink: 0; }
.lc-meta-value { text-align: right; min-width: 0; word-break: break-all; }
.lc-meta-value strong {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--accent-cyan);
}
.lc-order-link {
  color: var(--accent-cyan);
  cursor: pointer;
}
.lc-order-link:hover { text-decoration: underline; }
.lc-kpi { display: flex; gap: 16px; font-size: 12px; margin-bottom: 8px; }
.lc-batch { font-size: 11px; color: var(--text-secondary); margin-bottom: 10px; }
.lc-actions { display: flex; gap: 8px; }
.tab-btn.primary { border-color: var(--accent-cyan); color: var(--accent-cyan); }

.ops-details { margin-bottom: var(--grid-gap); }
.ops-summary {
  cursor: pointer;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  list-style: none;
}
.ops-summary::-webkit-details-marker { display: none; }
.ops-inner { padding: 0 16px 16px; display: flex; flex-direction: column; gap: var(--grid-gap); }

.summary-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: var(--grid-gap); }
.list-table.energy-table .list-row { grid-template-columns: 1.1fr 0.8fr 0.7fr 0.8fr 0.9fr; }
.summary-card { min-height: 72px; justify-content: center; }
.summary-label { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; }
.summary-value { font-size: 20px; font-weight: 600; color: var(--accent-cyan); }
.summary-hint { margin-top: 4px; font-size: 11px; color: var(--text-secondary); line-height: 1.35; }
.summary-card.stopped .summary-value { color: var(--status-stopped); }
.summary-card.manual .summary-value { color: var(--status-warning); }
.ops-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--grid-gap); }

.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(100,116,139,0.2); color: var(--status-stopped); }

.list-table { display: flex; flex-direction: column; gap: 8px; }
.list-row { display: grid; grid-template-columns: 1fr 1fr 1.5fr 1fr; gap: 12px; align-items: center; font-size: 12px; }
.list-row.order-row { grid-template-columns: 1.2fr 1fr 0.8fr 1.5fr; }
.list-row.head { font-size: 11px; color: var(--text-secondary); text-transform: uppercase; }
.list-row.clickable { cursor: pointer; }
.list-row.clickable:hover { background: rgba(34, 211, 238, 0.05); }

@media (max-width: 1200px) {
  .order-kpi-band-inner {
    grid-template-columns: 1fr 1fr;
  }
  .order-kpi-side {
    grid-column: 1 / -1;
    padding-left: 0;
    border-left: none;
    padding-top: 4px;
    border-top: 1px solid var(--border-dim);
  }
  .order-kpi-chips {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 1100px) {
  .must-handle-grid, .summary-grid, .ops-grid, .line-cards { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .order-kpi-band-inner { grid-template-columns: 1fr; }
  .order-kpi-chips { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .order-kpi-value { font-size: 32px; }
}
</style>
