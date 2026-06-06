<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import OrderProgressFunnel from '../components/orders/OrderProgressFunnel.vue'
import OrderBlockReason from '../components/orders/OrderBlockReason.vue'
import OrderTimelineAxis from '../components/orders/OrderTimelineAxis.vue'
import OrderReleasePanel from '../components/orders/OrderReleasePanel.vue'
import { getOrderDetail, getOrderTimeline } from '../api'
import { usePolling } from '../composables/usePolling'
import { useUserRole } from '../composables/useUserRole'
import { fmtDateTime } from '../utils/format'
import { lineShortId } from '../utils/lineCatalog'
import {
  fmtDaysToDue,
  fmtQtyT,
  logisticsStatusLabel,
  orderBadgeTone,
  orderRiskLabel,
  orderStatusLabel,
  orderTypeLabel,
  priorityLabel,
  productCategoryLabel,
  qualityDecisionLabel
} from '../utils/orderLabels'
import { formatProgressPct as fmtProg } from '../utils/orderTriage'

const props = defineProps({ orderId: { type: String, required: true } })
const router = useRouter()
const { role } = useUserRole()

const detail = ref(null)
const timeline = ref(null)

const order = computed(() => detail.value?.order)
const risk = computed(() => detail.value?.risk || {})
const batches = computed(() => detail.value?.batches || [])
const qualityGates = computed(() => detail.value?.quality_gates || [])
const logisticsTasks = computed(() => detail.value?.logistics_tasks || [])
const timelineEvents = computed(() => timeline.value?.timeline || [])
const batchSummary = computed(() => risk.value?.batch_status_summary || {})
const mergedOrder = computed(() => ({ ...order.value, ...risk.value }))

function goBatch(batchId) {
  if (batchId) router.push(`/batches/${batchId}`)
}

function goLine(lineId) {
  if (lineId) router.push(`/lines/${lineId}`)
}

function badgeTone(status) {
  return orderBadgeTone(status)
}

async function load() {
  const [detailResp, timelineResp] = await Promise.all([
    getOrderDetail(props.orderId),
    getOrderTimeline(props.orderId)
  ])
  detail.value = detailResp
  timeline.value = timelineResp
}

watch(() => props.orderId, load)
usePolling(load, 6000)
</script>

<template>
  <div class="order-detail-root">
    <PageHeader section="订单中心" :title="orderId">
      <div class="detail-actions">
        <span v-if="role === 'supervisor'" class="meta">线长视角 · 全厂可阅</span>
        <button class="tab-btn" @click="router.push('/orders')">← 返回订单中心</button>
      </div>
    </PageHeader>

    <div class="page-body full-bleed" v-if="detail?.order">
      <div class="kpi-strip hero-strip">
        <div class="kpi-tile hero">
          <div class="label">交付进度</div>
          <OrderProgressFunnel :order="mergedOrder" />
        </div>
        <div class="kpi-tile">
          <div class="label">状态 / 风险</div>
          <div class="value" style="font-size:14px">
            <span class="line-badge" :class="badgeTone(order?.status)">{{ orderStatusLabel(order?.status) }}</span>
            <span class="line-badge" :class="badgeTone(risk?.delivery_risk_level)">{{ orderRiskLabel(risk?.delivery_risk_level) }}</span>
          </div>
        </div>
        <div class="kpi-tile">
          <div class="label">类型 / 优先级</div>
          <div class="value" style="font-size:14px">{{ orderTypeLabel(risk?.order_type) }}{{ risk?.is_export ? ' · 出口' : '' }} / {{ priorityLabel(order?.priority) }}</div>
        </div>
        <div class="kpi-tile" :class="{ alarm: risk?.delivery_blocked }">
          <div class="label">完工进度</div>
          <div class="value">{{ fmtProg(risk?.progress_pct) }}</div>
        </div>
      </div>

      <div class="kpi-strip" style="grid-template-columns:repeat(6,1fr);margin-bottom:12px">
        <div class="kpi-tile"><div class="label">计划量</div><div class="value">{{ fmtQtyT(order?.planned_quantity_t) }}</div></div>
        <div class="kpi-tile"><div class="label">释放量</div><div class="value">{{ fmtQtyT(order?.released_quantity_t) }}</div></div>
        <div class="kpi-tile"><div class="label">已完成</div><div class="value">{{ fmtQtyT(risk?.completed_quantity_t) }}</div></div>
        <div class="kpi-tile"><div class="label">执行中</div><div class="value">{{ fmtQtyT(risk?.in_progress_quantity_t) }}</div></div>
        <div class="kpi-tile"><div class="label">待发运</div><div class="value">{{ fmtQtyT(risk?.ready_to_ship_quantity_t) }}</div></div>
        <div class="kpi-tile"><div class="label">阻塞量</div><div class="value">{{ fmtQtyT(risk?.blocked_quantity_t) }}</div></div>
      </div>

      <div class="panel" style="margin-bottom:var(--grid-gap)">
        <div class="panel-head"><h3>班次下达 / Release 窗口</h3></div>
        <div class="panel-body compact">
          <OrderReleasePanel :order="order" :risk="risk" />
        </div>
      </div>

      <div class="grid-dashboard" style="margin-bottom:var(--grid-gap)">
        <div class="col-6">
          <div class="panel">
            <div class="panel-head"><h3>交付信息</h3></div>
            <div class="panel-body compact delivery-grid">
              <div class="summary-line">客户单号：{{ order?.customer_order_id || '—' }}</div>
              <div class="summary-line">客户段：{{ risk?.customer_segment || order?.customer_segment || '—' }}</div>
              <div class="summary-line">品类 / 牌号：{{ productCategoryLabel(order?.product_category) }} / {{ order?.grade }}</div>
              <div class="summary-line">配方：{{ order?.recipe_id || '—' }}</div>
              <div class="summary-line">
                下发产线：
                <template v-for="(lid, idx) in (risk?.assigned_line_ids || [])" :key="lid">
                  <button class="link-btn" @click="goLine(lid)">{{ lineShortId(lid) }}</button><span v-if="idx < (risk.assigned_line_ids.length - 1)">、</span>
                </template>
              </div>
              <div class="summary-line">交期：{{ fmtDateTime(order?.due_date) }}（{{ fmtDaysToDue(risk?.days_to_due) }}）</div>
              <div class="summary-line">交付 SLA：{{ risk?.delivery_sla_days ?? order?.delivery_sla_days ?? '—' }} 天</div>
              <div class="summary-line">预计发运：{{ fmtDateTime(risk?.estimated_ship_date) }}</div>
              <div class="summary-line">备注：{{ order?.remark || '—' }}</div>
            </div>
          </div>
        </div>
        <div class="col-6">
          <div class="panel">
            <div class="panel-head"><h3>阻塞归因链</h3></div>
            <div class="panel-body compact">
              <OrderBlockReason :order="order" :detail="detail" />
              <div class="summary-line" style="margin-top:10px">交付阻塞：{{ risk?.delivery_blocked ? '是' : '否' }}</div>
              <div class="summary-line">质门阻塞：{{ risk?.blocking_quality_gates ?? 0 }} · 物流阻塞：{{ risk?.blocking_logistics_tasks ?? 0 }}</div>
              <div class="summary-line">待放行量：{{ fmtQtyT(risk?.blocked_quantity_t) }}</div>
              <div class="summary-line">批次：Hold {{ batchSummary.hold ?? 0 }} / 执行中 {{ batchSummary.in_progress ?? 0 }} / 待发运 {{ batchSummary.ready_to_ship ?? 0 }} / 完成 {{ batchSummary.completed ?? 0 }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="panel" style="margin-bottom:var(--grid-gap)">
        <div class="panel-head"><h3>订单时间线</h3></div>
        <div class="panel-body">
          <OrderTimelineAxis :events="timelineEvents" @batch="goBatch" />
        </div>
      </div>

      <div class="grid-dashboard">
        <div class="col-6">
          <div class="panel">
            <div class="panel-head"><h3>批次构成</h3></div>
            <div class="panel-body flush">
              <table class="data-table" v-if="batches.length">
                <thead><tr><th>批次</th><th>产线</th><th>状态</th><th>数量</th></tr></thead>
                <tbody>
                  <tr v-for="batch in batches" :key="batch.batch_id" class="clickable-row" @click="goBatch(batch.batch_id)">
                    <td class="link-cell">{{ batch.batch_id }}</td>
                    <td>{{ lineShortId(batch.product_line_id) }}</td>
                    <td><span class="line-badge" :class="badgeTone(batch.status)">{{ orderStatusLabel(batch.status) }}</span></td>
                    <td>{{ batch.quantity_kg ?? 0 }} kg</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-state">暂无批次</div>
            </div>
          </div>
        </div>
        <div class="col-6">
          <div class="panel">
            <div class="panel-head"><h3>质量门</h3></div>
            <div class="panel-body flush">
              <table class="data-table" v-if="qualityGates.length">
                <thead><tr><th>批次</th><th>工序</th><th>判定</th><th>原因</th></tr></thead>
                <tbody>
                  <tr v-for="gate in qualityGates" :key="gate.gate_event_id">
                    <td>{{ gate.batch_id }}</td>
                    <td>{{ gate.process_step_id || '—' }}</td>
                    <td><span class="line-badge" :class="badgeTone(gate.decision)">{{ qualityDecisionLabel(gate.decision) }}</span></td>
                    <td>{{ gate.reason_text || '—' }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-state">暂无质量门记录</div>
            </div>
          </div>
        </div>
        <div class="col-6">
          <div class="panel">
            <div class="panel-head"><h3>物流任务</h3></div>
            <div class="panel-body flush">
              <table class="data-table" v-if="logisticsTasks.length">
                <thead><tr><th>批次</th><th>类型</th><th>状态</th><th>路径</th></tr></thead>
                <tbody>
                  <tr v-for="task in logisticsTasks" :key="task.task_id">
                    <td>{{ task.product_batch || '—' }}</td>
                    <td>{{ task.task_type || '—' }}</td>
                    <td><span class="line-badge" :class="badgeTone(task.status)">{{ logisticsStatusLabel(task.status) }}</span></td>
                    <td>{{ task.source_location_id || '—' }} → {{ task.target_location_id || '—' }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-state">暂无物流任务</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="page-body" v-else>
      <div class="order-skeleton-grid"><div v-for="i in 4" :key="i" class="order-skeleton-card" /></div>
      <div class="loading-state">LOADING…</div>
    </div>
  </div>
</template>

<style scoped>
.detail-actions { display: flex; gap: 8px; align-items: center; }
.hero-strip { grid-template-columns: 2fr 1fr 1fr 0.8fr !important; margin-bottom: 12px; }
.kpi-tile.hero { align-items: stretch; }
.delivery-grid { display: grid; grid-template-columns: 1fr; gap: 6px; }
.summary-line { font-size: 12px; margin-bottom: 6px; }
.meta { font-size: 12px; color: var(--text-secondary); }
.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; margin-right: 4px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(248,113,113,0.15); color: var(--status-alarm); }
.clickable-row { cursor: pointer; }
.clickable-row:hover { background: rgba(34, 211, 238, 0.06); }
.link-cell { color: var(--accent-cyan); }
.link-btn { background: none; border: none; color: var(--accent-cyan); cursor: pointer; padding: 0; font: inherit; }
@media (max-width: 900px) {
  .hero-strip { grid-template-columns: 1fr !important; }
}
</style>
