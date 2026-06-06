<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import {
  getInventory, getRecentMaterialEvents, getBatches, getLines
} from '../api'
import { usePolling } from '../composables/usePolling'
import { useAppContext } from '../composables/useAppContext'
import { fmtDateTime } from '../utils/format'
import { materialEventLabel } from '../utils/eventLabels'

const route = useRoute()
const router = useRouter()
useAppContext()

const LIST_TABS = [
  { id: 'active', label: '进行中' },
  { id: 'completed', label: '已完成' },
  { id: 'inventory', label: '库存快照' }
]

const STATUS_FILTERS = [
  { id: 'all', label: '全部' },
  { id: 'in_progress', label: '进行中' },
  { id: 'hold', label: 'Hold' },
  { id: 'ready_to_ship', label: '待发运' },
  { id: 'completed', label: '已完成' }
]

const inventory = ref(null)
const events = ref([])
const batches = ref([])
const lines = ref([])

const activeListTab = computed(() => route.query.tab || 'active')
const activeStatus = computed(() => route.query.status || 'all')

const filteredBatches = computed(() => {
  let list = batches.value
  if (activeListTab.value === 'active') {
    list = list.filter(b => ['in_progress', 'hold', 'ready_to_ship'].includes(b.status))
  } else if (activeListTab.value === 'completed') {
    list = list.filter(b => b.status === 'completed')
  }
  if (activeStatus.value !== 'all') {
    list = list.filter(b => b.status === activeStatus.value)
  }
  return list
})
const batchStats = computed(() => ({
  in_progress: batches.value.filter(b => b.status === 'in_progress').length,
  hold: batches.value.filter(b => b.status === 'hold').length,
  ready_to_ship: batches.value.filter(b => b.status === 'ready_to_ship').length,
  completed: batches.value.filter(b => b.status === 'completed').length
}))
const rawItems = computed(() => inventory.value?.raw || [])
const wipItems = computed(() => inventory.value?.wip || [])
const finishedItems = computed(() => inventory.value?.finished || [])
const materialBatches = computed(() => inventory.value?.material_batches || [])

async function load() {
  const [inv, ev, bt, ln] = await Promise.all([
    getInventory(),
    getRecentMaterialEvents(30),
    getBatches(),
    getLines()
  ])
  inventory.value = inv
  events.value = ev.slice(0, 20)
  batches.value = bt
  lines.value = ln
}

function switchTab(tabId) {
  router.replace({ path: '/batches', query: { ...route.query, tab: tabId } })
}

function switchStatus(status) {
  router.replace({ path: '/batches', query: { ...route.query, status } })
}

function goDetail(batchId) {
  router.push(`/batches/${batchId}`)
}

function lineName(id) {
  return lines.value.find(l => l.product_line_id === id)?.name || id
}

function statusTone(status) {
  if (status === 'completed') return 'running'
  if (status === 'ready_to_ship') return 'manual'
  if (status === 'hold') return 'stopped'
  return 'manual'
}

usePolling(load, 5000)
</script>

<template>
  <div class="batch-center-root">
    <PageHeader section="批次中心" title="批次与物料">
      <div class="workbench-tabs">
        <button
          v-for="t in LIST_TABS"
          :key="t.id"
          class="tab-btn"
          :class="{ active: activeListTab === t.id }"
          @click="switchTab(t.id)"
        >{{ t.label }}</button>
      </div>
    </PageHeader>

    <div class="page-body full-bleed" v-if="inventory">
      <!-- 进行中 / 已完成 -->
      <template v-if="activeListTab === 'active' || activeListTab === 'completed'">
        <div class="kpi-strip" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 12px">
          <div class="kpi-tile"><div class="label">进行中</div><div class="value">{{ batchStats.in_progress }}</div></div>
          <div class="kpi-tile alarm"><div class="label">Hold</div><div class="value">{{ batchStats.hold }}</div></div>
          <div class="kpi-tile"><div class="label">待发运</div><div class="value">{{ batchStats.ready_to_ship }}</div></div>
          <div class="kpi-tile ok"><div class="label">已完成</div><div class="value">{{ batchStats.completed }}</div></div>
        </div>

        <div class="panel">
          <div class="panel-head">
            <h3>{{ activeListTab === 'active' ? '进行中批次' : '已完成批次' }}</h3>
            <div class="panel-actions-inline">
              <button
                v-for="s in STATUS_FILTERS"
                :key="s.id"
                class="tab-btn"
                :class="{ active: activeStatus === s.id }"
                @click="switchStatus(s.id)"
              >{{ s.label }}</button>
            </div>
          </div>
          <div class="panel-body flush">
            <table class="data-table">
              <thead>
                <tr><th>批次 ID</th><th>产线</th><th>订单</th><th>牌号</th><th>配方</th><th>状态</th><th>开始</th></tr>
              </thead>
              <tbody>
                <tr
                  v-for="b in filteredBatches"
                  :key="b.batch_id"
                  class="click-row"
                  @click="goDetail(b.batch_id)"
                >
                  <td>{{ b.batch_id }}</td>
                  <td>{{ lineName(b.product_line_id) }}</td>
                  <td>{{ b.production_order_id || '—' }}</td>
                  <td>{{ b.grade }}</td>
                  <td>{{ b.recipe_id }}</td>
                  <td><span class="line-badge" :class="statusTone(b.status)">{{ b.status }}</span></td>
                  <td>{{ fmtDateTime(b.started_at) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="!filteredBatches.length" class="empty-state">暂无批次</div>
          </div>
        </div>
      </template>

      <!-- 库存 -->
      <template v-else-if="activeListTab === 'inventory'">
        <div class="material-grid">
          <div class="panel">
            <div class="panel-head"><h3>原材料</h3></div>
            <div class="panel-body flush">
              <table class="data-table">
                <tbody>
                  <tr v-for="item in rawItems" :key="item.inventory_id">
                    <td>{{ item.display_name || item.sku }}</td>
                    <td>{{ item.quantity_kg }} kg</td>
                    <td>{{ item.location }}</td>
                  </tr>
                  <tr v-for="mb in materialBatches" :key="mb.batch_id">
                    <td>{{ mb.material_type }} · {{ mb.batch_id }}</td>
                    <td>{{ mb.quantity_kg }} kg</td>
                    <td>{{ mb.location }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="panel">
            <div class="panel-head"><h3>在制品 WIP</h3></div>
            <div class="panel-body flush">
              <div v-for="wip in wipItems" :key="wip.inventory_id" class="wip-card" @click="goDetail(wip.product_batch)">
                <div class="wip-batch">{{ wip.product_batch }}</div>
                <div class="wip-step">{{ wip.process_step_id }}</div>
                <div class="wip-qty">{{ wip.quantity_kg }} kg</div>
              </div>
            </div>
          </div>
          <div class="panel">
            <div class="panel-head"><h3>成品</h3></div>
            <div class="panel-body flush">
              <div v-for="fg in finishedItems" :key="fg.inventory_id" class="wip-card" @click="goDetail(fg.product_batch)">
                <div class="wip-batch">{{ fg.product_batch || fg.batch_id }}</div>
                <div class="wip-qty">{{ fg.quantity_kg }} kg · {{ fg.location }}</div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 最近物料事件 -->
      <div class="panel" style="margin-top:var(--grid-gap)">
        <div class="panel-head"><h3>最近物料事件</h3></div>
        <div class="panel-body flush">
          <table class="data-table" v-if="events.length">
            <thead><tr><th>时间</th><th>类型</th><th>批次</th><th>工序</th><th>备注</th></tr></thead>
            <tbody>
              <tr v-for="ev in events" :key="ev.event_id" class="click-row" @click="goDetail(ev.material_batch)">
                <td>{{ fmtDateTime(ev.timestamp) }}</td>
                <td>{{ materialEventLabel(ev.event_type) }}</td>
                <td>{{ ev.material_batch }}</td>
                <td>{{ ev.process_step_id || '—' }}</td>
                <td>{{ ev.remark || '—' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-state">启动模拟器后产生 LINE_ON/OFF、STOCK_IN 等事件</div>
        </div>
      </div>
    </div>

    <div class="page-body" v-else><div class="loading-state">LOADING…</div></div>
  </div>
</template>

<style scoped>
/* 布局见 main.css .batch-center-root */
.workbench-tabs { display: flex; gap: 4px; }
.panel-actions-inline { display: flex; gap: 6px; flex-wrap: wrap; }
.material-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: var(--grid-gap); }
.wip-card { padding: 10px 12px; border-bottom: 1px solid var(--border-dim); cursor: pointer; }
.wip-card:hover { background: rgba(34,211,238,0.06); }
.wip-batch { font-family: var(--font-mono); font-size: 12px; color: var(--accent-cyan); }
.wip-step { font-size: 11px; color: var(--status-running); }
.click-row { cursor: pointer; }
.click-row:hover { background: rgba(34,211,238,0.04); }
.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(100,116,139,0.2); color: var(--status-stopped); }
@media (max-width: 1100px) { .material-grid { grid-template-columns: 1fr; } }
</style>
