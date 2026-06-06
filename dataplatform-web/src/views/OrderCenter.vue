<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import OrderTriageStrip from '../components/orders/OrderTriageStrip.vue'
import OrderRiskRow from '../components/orders/OrderRiskRow.vue'
import OrderCreateDrawer from '../components/orders/OrderCreateDrawer.vue'
import OrderBlockReason from '../components/orders/OrderBlockReason.vue'
import OrderProgressFunnel from '../components/orders/OrderProgressFunnel.vue'
import OrderReleasePanel from '../components/orders/OrderReleasePanel.vue'
import LinePickerBar from '../components/LinePickerBar.vue'
import { getFactory, getOrderSummary, getOrders, getOrderDetail, getLines, getProducts, getRecipes } from '../api'
import { usePolling } from '../composables/usePolling'
import { useAppContext } from '../composables/useAppContext'
import { useUserRole } from '../composables/useUserRole'
import { fmtDateTime } from '../utils/format'
import { workshopLabel, lineShortId } from '../utils/lineCatalog'
import { lineBelongsToWorkshop } from '../utils/orderTriage'
import {
  fmtDaysToDue,
  fmtQtyT,
  orderBadgeTone,
  orderRiskLabel,
  orderStatusLabel,
  orderTypeLabel,
  priorityLabel,
  productCategoryLabel
} from '../utils/orderLabels'
import {
  VIEW_MODES,
  SCENARIO_FILTERS,
  filterOrdersByContext,
  formatProgressPct
} from '../utils/orderTriage'

const router = useRouter()
const route = useRoute()
const ctx = useAppContext()
const { role } = useUserRole()

const STATUS_FILTERS = [
  { id: '', label: '全部状态' },
  { id: 'released', label: '已下达' },
  { id: 'in_progress', label: '执行中' },
  { id: 'blocked', label: '受阻' },
  { id: 'ready_to_ship', label: '待发运' },
  { id: 'completed', label: '已完成' }
]

const RISK_FILTERS = [
  { id: '', label: '全部风险' },
  { id: 'low', label: '低' },
  { id: 'medium', label: '中' },
  { id: 'high', label: '高' },
  { id: 'critical', label: '严重' }
]

const factory = ref(null)
const summary = ref(null)
const orders = ref([])
const detail = ref(null)
const selectedOrderId = ref('')
const loading = ref(true)
const loadError = ref('')
const statusFilter = ref('')
const riskFilter = ref('')
const scenarioFilter = ref('')
const viewMode = ref(VIEW_MODES.workbench)
const createDrawerOpen = ref(false)
const lines = ref([])
const products = ref([])
const recipes = ref([])

const isSupervisor = computed(() => role.value === 'supervisor')
const canCreate = computed(() => !isSupervisor.value)
const roleLabel = computed(() => (isSupervisor.value ? '线长' : '厂长'))
const orderScopeMode = computed(() => {
  if (orderLineFilter.value) return 'line'
  if (orderWorkshopFilter.value !== 'all') return 'workshop'
  return 'factory'
})

/** 订单页独立范围筛选（与产线工作台 ctx.lineId 解耦） */
const orderWorkshopFilter = computed({
  get() {
    const w = route.query.workshop
    return w === 'wire' || w === 'rod' ? w : 'all'
  },
  set(value) {
    const query = { ...route.query }
    if (value && value !== 'all') query.workshop = value
    else delete query.workshop
    if (query.line && !lineBelongsToWorkshop(query.line, lines.value, value)) {
      delete query.line
    }
    router.replace({ path: '/orders', query })
  }
})

const orderLineFilter = computed({
  get() {
    const q = route.query.line
    return typeof q === 'string' ? q : ''
  },
  set(value) {
    const query = { ...route.query }
    if (value) query.line = value
    else delete query.line
    router.replace({ path: '/orders', query })
  }
})

function pickOrderLine(lineId) {
  const query = { ...route.query }
  if (!lineId || orderLineFilter.value === lineId) {
    delete query.line
  } else {
    query.line = lineId
  }
  router.replace({ path: '/orders', query })
}

const contextLabel = computed(() => {
  if (orderLineFilter.value) {
    const line = lines.value.find(l => l.product_line_id === orderLineFilter.value)
    return line?.name || lineShortId(orderLineFilter.value)
  }
  if (orderWorkshopFilter.value === 'wire') return workshopLabel('WS-WIRE-01')
  if (orderWorkshopFilter.value === 'rod') return workshopLabel('WS-ROD-01')
  return '全厂'
})

const filterOpts = computed(() => ({
  workshop: orderWorkshopFilter.value,
  lineId: orderLineFilter.value,
  scenarioType: scenarioFilter.value,
  statusFilter: statusFilter.value,
  riskFilter: riskFilter.value,
  lines: lines.value
}))

const pageReady = computed(() => !!factory.value && !loading.value)
const hasCoreData = computed(() => !!summary.value || orders.value.length > 0)

const contextOrders = computed(() =>
  filterOrdersByContext(orders.value, {
    ...filterOpts.value,
    viewMode: VIEW_MODES.all
  })
)

const displayOrders = computed(() =>
  filterOrdersByContext(orders.value, {
    ...filterOpts.value,
    viewMode: viewMode.value
  })
)

const selectedRisk = computed(() => detail.value?.risk || null)
const selectedBatches = computed(() => detail.value?.batches || [])

function badgeTone(status) {
  return orderBadgeTone(status)
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const meta = factory.value || await getFactory()
    factory.value = meta
    const factoryId = meta.factory_id
    const [summaryRes, ordersRes, linesRes, productsRes, recipesRes] = await Promise.allSettled([
      getOrderSummary(factoryId),
      getOrders(factoryId, statusFilter.value || undefined, riskFilter.value || undefined),
      getLines(),
      getProducts(),
      getRecipes()
    ])

    const failures = []
    if (summaryRes.status === 'fulfilled') summary.value = summaryRes.value
    else failures.push('订单汇总')

    const ordersResp = ordersRes.status === 'fulfilled' ? ordersRes.value : []
    if (ordersRes.status === 'fulfilled') orders.value = ordersResp
    else failures.push('订单列表')

    const linesResp = linesRes.status === 'fulfilled' ? linesRes.value : lines.value
    if (linesRes.status === 'fulfilled') {
      lines.value = linesResp
      if (!ctx.lines.value.length) ctx.lines.value = linesResp
    } else failures.push('产线主数据')

    if (productsRes.status === 'fulfilled') products.value = productsRes.value
    else failures.push('产品主数据')

    if (recipesRes.status === 'fulfilled') recipes.value = recipesRes.value
    else failures.push('配方主数据')

    if (failures.length === 5) {
      loadError.value = '订单中心数据加载失败，请检查 API 服务'
      return
    }
    if (failures.length) {
      loadError.value = `部分数据不可用：${failures.join('、')}`
    }

    const visible = filterOrdersByContext(ordersResp, {
      ...filterOpts.value,
      viewMode: viewMode.value,
      lines: linesResp
    })
    if (!visible.length) {
      selectedOrderId.value = ''
      detail.value = null
    } else if (!selectedOrderId.value) {
      selectedOrderId.value = visible[0].production_order_id
    } else if (!visible.some(o => o.production_order_id === selectedOrderId.value)) {
      selectedOrderId.value = visible[0]?.production_order_id || ''
    }
    if (selectedOrderId.value) {
      detail.value = await getOrderDetail(selectedOrderId.value)
    }
  } catch (e) {
    loadError.value = e?.message || '订单中心数据加载失败'
  } finally {
    loading.value = false
  }
}

function onOrderCreated(orderId) {
  selectedOrderId.value = orderId
  load()
}

function openOrder(orderId) {
  router.push(`/orders/${orderId}`)
}

watch([statusFilter, riskFilter, scenarioFilter], async () => {
  selectedOrderId.value = ''
  detail.value = null
  await load()
})

function clearOrderScope() {
  const query = { ...route.query }
  delete query.line
  delete query.workshop
  router.replace({ path: '/orders', query })
}

function setViewMode(mode) {
  viewMode.value = mode
  const query = { ...route.query }
  if (mode === VIEW_MODES.all) query.view = 'all'
  else query.view = 'workbench'
  router.replace({ path: '/orders', query })
}

watch([orderLineFilter, orderWorkshopFilter, viewMode, role], () => {
  if (!displayOrders.value.length) {
    selectedOrderId.value = ''
    detail.value = null
    return
  }
  if (!displayOrders.value.some(o => o.production_order_id === selectedOrderId.value)) {
    selectedOrderId.value = displayOrders.value[0]?.production_order_id || ''
  }
})

watch(selectedOrderId, async (orderId) => {
  if (!orderId) return
  detail.value = await getOrderDetail(orderId)
})

watch(() => route.query.view, (v) => {
  if (v === 'workbench') viewMode.value = VIEW_MODES.workbench
  if (v === 'all') viewMode.value = VIEW_MODES.all
}, { immediate: true })

usePolling(load, 6000)
</script>

<template>
  <div class="order-center-root">
    <PageHeader section="订单中心" :title="viewMode === VIEW_MODES.workbench ? '今日交付工作台' : '全部订单'">
      <div class="order-filters">
        <select v-model="scenarioFilter" class="filter-select" title="场景筛选">
          <option v-for="item in SCENARIO_FILTERS" :key="item.id" :value="item.id">{{ item.label }}</option>
        </select>
        <select v-model="statusFilter" class="filter-select">
          <option v-for="item in STATUS_FILTERS" :key="item.id" :value="item.id">{{ item.label }}</option>
        </select>
        <select v-model="riskFilter" class="filter-select">
          <option v-for="item in RISK_FILTERS" :key="item.id" :value="item.id">{{ item.label }}</option>
        </select>
      </div>
    </PageHeader>

    <div v-if="loadError && pageReady" class="order-load-error page-body full-bleed">
      <span>{{ loadError }}</span>
      <button type="button" class="tab-btn" @click="load">重试</button>
    </div>

    <div class="page-body full-bleed" v-if="pageReady && hasCoreData">
      <div
        class="order-scope-banner"
        :class="{
          'scope-factory': orderScopeMode === 'factory',
          'scope-filter': orderScopeMode === 'workshop',
          'scope-line': orderScopeMode === 'line'
        }"
      >
        <span class="scope-icon">{{ orderScopeMode === 'line' ? '▣' : (orderScopeMode === 'workshop' ? '◎' : '⌂') }}</span>
        <div class="scope-copy">
          <strong>
            {{ roleLabel }}视角 ·
            {{ orderScopeMode === 'line' ? '产线筛选' : (orderScopeMode === 'workshop' ? '车间筛选' : '全厂交付') }}
          </strong>
          <span class="scope-sub">
            使用下方「订单范围」筛选；与产线工作台顶栏产线选择相互独立
            <template v-if="isSupervisor">（线长可查看全厂，默认进入上次产线工作台）</template>
          </span>
        </div>
      </div>

      <div class="panel order-scope-panel">
        <div class="panel-body compact order-scope-body">
          <div class="order-scope-head">
            <span class="order-scope-title">订单范围</span>
            <button
              v-if="orderLineFilter || orderWorkshopFilter !== 'all'"
              type="button"
              class="tab-btn"
              @click="clearOrderScope"
            >重置为全厂</button>
          </div>
          <LinePickerBar
            :lines="lines"
            :workshop="orderWorkshopFilter"
            :active-line-id="orderLineFilter"
            allow-all
            all-label="全厂订单"
            @update:workshop="orderWorkshopFilter = $event"
            @pick-line="pickOrderLine"
            @pick-all="clearOrderScope"
          />
        </div>
      </div>

      <div class="order-workbench-toolbar">
        <div class="order-view-tabs">
          <button
            type="button"
            class="order-view-tab"
            :class="{ active: viewMode === VIEW_MODES.workbench }"
            @click="setViewMode(VIEW_MODES.workbench)"
          >今日交付</button>
          <button
            type="button"
            class="order-view-tab"
            :class="{ active: viewMode === VIEW_MODES.all }"
            @click="setViewMode(VIEW_MODES.all)"
          >全部订单</button>
        </div>
        <div class="order-toolbar-actions">
          <span class="meta">{{ contextLabel }} · {{ displayOrders.length }} 单 / 共 {{ orders.length }} 单</span>
          <button v-if="canCreate" class="tab-btn primary" @click="createDrawerOpen = true">+ 建单下发</button>
        </div>
      </div>

      <OrderTriageStrip :orders="contextOrders" :context-label="contextLabel" />

      <div class="order-layout">
        <div class="panel order-list-panel">
          <div class="panel-head">
            <h3>{{ viewMode === VIEW_MODES.workbench ? '在制订单队列' : '订单列表（含已完成）' }}</h3>
            <span class="meta">按交期与风险排序</span>
          </div>
          <div class="panel-body flush order-list-body">
            <table class="data-table" v-if="displayOrders.length">
              <thead>
                <tr>
                  <th>订单</th><th>类型</th><th>状态</th><th>风险</th><th>进度</th><th>交期</th>
                  <th v-if="viewMode === VIEW_MODES.workbench">阻塞</th>
                </tr>
              </thead>
              <tbody>
                <OrderRiskRow
                  v-for="item in displayOrders"
                  :key="item.production_order_id"
                  :order="item"
                  :selected="selectedOrderId === item.production_order_id"
                  :all-orders="orders"
                  :lines="lines"
                  :show-block-reason="viewMode === VIEW_MODES.workbench"
                  @select="selectedOrderId = $event"
                  @open="openOrder"
                />
              </tbody>
            </table>
            <div v-else class="empty-state">
              {{ viewMode === VIEW_MODES.workbench ? '当前上下文暂无需关注订单' : '暂无订单' }}
            </div>
          </div>
        </div>

        <div class="order-detail" v-if="detail?.order">
          <div class="panel" style="margin-bottom:var(--grid-gap)">
            <div class="panel-head"><h3>订单预览</h3></div>
            <div class="panel-body compact detail-grid">
              <div>
                <div class="summary-line">订单号：{{ detail.order.production_order_id }}</div>
                <div class="summary-line">客户单号：{{ detail.order.customer_order_id || '—' }}</div>
                <div class="summary-line">品类 / 牌号：{{ productCategoryLabel(detail.order.product_category) }} / {{ detail.order.grade }}</div>
                <div class="summary-line">下发产线：{{ (detail.order.assigned_line_ids || []).map(lineShortId).join('、') || '—' }}</div>
              </div>
              <div>
                <div class="summary-line">
                  状态：<span class="line-badge" :class="badgeTone(detail.order.status)">{{ orderStatusLabel(detail.order.status) }}</span>
                </div>
                <div class="summary-line">类型 / 优先级：{{ orderTypeLabel(selectedRisk?.order_type || detail.order.order_type) }} / {{ priorityLabel(detail.order.priority) }}</div>
                <div class="summary-line">交期：{{ fmtDateTime(detail.order.due_date) }}（{{ fmtDaysToDue(selectedRisk?.days_to_due) }}）</div>
                <OrderProgressFunnel :order="{ ...detail.order, ...selectedRisk }" />
              </div>
              <div>
                <div class="summary-line">
                  风险：<span class="line-badge" :class="badgeTone(selectedRisk?.delivery_risk_level)">{{ orderRiskLabel(selectedRisk?.delivery_risk_level) }}</span>
                  · {{ formatProgressPct(selectedRisk?.progress_pct) }}
                </div>
                <div class="summary-line">阻塞归因：</div>
                <OrderBlockReason :order="detail.order" :detail="detail" />
                <button class="tab-btn" style="margin-top:8px" @click="openOrder(detail.order.production_order_id)">打开完整详情 →</button>
              </div>
            </div>
          </div>

          <div class="panel" style="margin-bottom:var(--grid-gap)">
            <div class="panel-head"><h3>班次下达 / Release</h3></div>
            <div class="panel-body compact">
              <OrderReleasePanel :order="detail.order" :risk="selectedRisk" />
            </div>
          </div>

          <div class="grid-dashboard">
            <div class="col-6">
              <div class="panel">
                <div class="panel-head"><h3>批次构成</h3></div>
                <div class="panel-body flush">
                  <table class="data-table" v-if="selectedBatches.length">
                    <thead><tr><th>批次</th><th>产线</th><th>状态</th><th>数量</th></tr></thead>
                    <tbody>
                      <tr v-for="batch in selectedBatches" :key="batch.batch_id">
                        <td>{{ batch.batch_id }}</td>
                        <td>{{ lineShortId(batch.product_line_id) }}</td>
                        <td><span class="line-badge" :class="badgeTone(batch.status)">{{ orderStatusLabel(batch.status) }}</span></td>
                        <td>{{ batch.quantity_kg ?? 0 }} kg</td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-else class="empty-state">暂无批次构成</div>
                </div>
              </div>
            </div>
            <div class="col-6">
              <div class="panel">
                <div class="panel-head"><h3>质量门 / 物流</h3></div>
                <div class="panel-body compact">
                  <div class="summary-line">质门阻塞：{{ selectedRisk?.blocking_quality_gates ?? 0 }}</div>
                  <div class="summary-line">物流阻塞：{{ selectedRisk?.blocking_logistics_tasks ?? 0 }}</div>
                  <div class="summary-line">待放行量：{{ fmtQtyT(selectedRisk?.blocked_quantity_t) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="page-body full-bleed" v-else-if="loading || !hasCoreData">
      <div v-if="loading" class="order-skeleton-grid">
        <div v-for="i in 5" :key="i" class="order-skeleton-card" />
      </div>
      <div class="loading-state">{{ loading ? 'LOADING…' : loadError }}</div>
      <button v-if="!loading && loadError" type="button" class="tab-btn" style="margin-top:12px" @click="load">重试</button>
    </div>

    <OrderCreateDrawer
      :open="createDrawerOpen"
      :factory-id="factory?.factory_id"
      :orders="orders"
      :lines="lines"
      :products="products"
      :recipes="recipes"
      @close="createDrawerOpen = false"
      @created="onOrderCreated"
    />
  </div>
</template>

<style scoped>
.order-scope-banner {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 10px 14px;
  margin-bottom: var(--grid-gap);
  border-radius: var(--radius);
  border: 1px solid var(--border-dim);
  background: rgba(100, 116, 139, 0.06);
}
.order-scope-banner.scope-factory {
  border-left: 3px solid var(--accent-cyan);
}
.order-scope-banner.scope-filter {
  border-left: 3px solid var(--status-warning);
}
.order-scope-banner.scope-line {
  border-left: 3px solid var(--status-running);
}
.scope-icon {
  font-size: 16px;
  line-height: 1.2;
  color: var(--accent-cyan);
}
.scope-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
}
.scope-copy strong { color: var(--text-primary); }
.scope-sub { color: var(--text-secondary); font-size: 11px; line-height: 1.45; }
.order-scope-panel { margin-bottom: var(--grid-gap); }
.order-scope-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-scope-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.order-scope-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}
.order-load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: var(--grid-gap);
  border-radius: var(--radius);
  border: 1px solid rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.08);
  color: var(--status-warning);
  font-size: 12px;
}
.order-filters { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-select {
  background: var(--bg-panel-elevated);
  color: var(--text-primary);
  border: 1px solid var(--border-dim);
  padding: 6px 10px;
  border-radius: var(--radius);
}
.order-layout { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr); gap: var(--grid-gap); min-height: 0; }
.order-list-panel, .order-detail { min-height: 0; min-width: 0; display: flex; flex-direction: column; }
.detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.summary-line { font-size: 12px; margin-bottom: 6px; }
.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(248,113,113,0.15); color: var(--status-alarm); }
.tab-btn.primary { border-color: var(--accent-cyan); color: var(--accent-cyan); }
.meta { font-size: 12px; color: var(--text-secondary); }
@media (max-width: 1100px) {
  .detail-grid, .order-layout { grid-template-columns: 1fr; }
}
</style>
