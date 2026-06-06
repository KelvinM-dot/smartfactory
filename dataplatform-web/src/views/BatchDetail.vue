<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import { getBatchTimeline } from '../api'
import { usePolling } from '../composables/usePolling'
import { useAppContext } from '../composables/useAppContext'
import { fmtDateTime, statusLabel } from '../utils/format'
import { materialEventLabel } from '../utils/eventLabels'

const props = defineProps({ batchId: { type: String, required: true } })
const router = useRouter()
const { lineId, setLineId, setBatchId } = useAppContext()

const timeline = ref(null)

const batch = computed(() => timeline.value?.batch)
const order = computed(() => timeline.value?.order)
const nodes = computed(() => timeline.value?.nodes || [])
const gaps = computed(() => timeline.value?.trace_gaps || [])
const materialEvents = computed(() => timeline.value?.material_events || [])
const qualityGates = computed(() => timeline.value?.quality_gates || [])
const logisticsTasks = computed(() => timeline.value?.logistics_tasks || [])
const blockers = computed(() => timeline.value?.blockers || {})
const orderId = computed(() => order.value?.production_order_id || batch.value?.production_order_id || null)

const highlightStep = computed(() => {
  const ip = nodes.value.find(n => n.status === 'in_progress')
  if (ip) return ip.process_step_id
  const done = [...nodes.value].reverse().find(n => n.status === 'completed')
  return done?.process_step_id
})

async function load() {
  timeline.value = await getBatchTimeline(props.batchId)
  setBatchId(props.batchId)
  if (batch.value?.product_line_id) {
    setLineId(batch.value.product_line_id)
  }
}

function badgeTone(status) {
  if (status === 'completed') return 'running'
  if (status === 'ready_to_ship') return 'manual'
  if (status === 'blocked' || status === 'hold') return 'stopped'
  return 'manual'
}

function goTwin(stepId) {
  const lid = batch.value?.product_line_id || lineId.value
  router.push({ path: `/lines/${lid}`, query: { tab: 'twin', step: stepId || highlightStep.value } })
}

function goTrends() {
  router.push('/trends')
}

function goOrder() {
  if (orderId.value) router.push(`/orders/${orderId.value}`)
}

watch(() => props.batchId, load)
usePolling(load, 8000)
</script>

<template>
  <div class="batch-detail-root">
    <PageHeader section="批次中心" :title="batchId">
      <div class="detail-actions">
        <button class="tab-btn" @click="router.push('/batches')">← 列表</button>
        <button v-if="orderId" class="tab-btn" @click="goOrder">查看订单 {{ orderId }}</button>
        <button class="tab-btn" @click="goTwin(highlightStep)">在孪生中定位</button>
        <button class="tab-btn" @click="goTrends">查看趋势</button>
      </div>
    </PageHeader>

    <div class="page-body full-bleed" v-if="timeline">
      <div class="kpi-strip" style="grid-template-columns:repeat(6,1fr);margin-bottom:12px">
        <div class="kpi-tile"><div class="label">批次</div><div class="value" style="font-size:15px">{{ batch?.batch_id }}</div></div>
        <div class="kpi-tile"><div class="label">牌号</div><div class="value">{{ batch?.grade }}</div></div>
        <div class="kpi-tile"><div class="label">配方</div><div class="value" style="font-size:13px">{{ batch?.recipe_id }}</div></div>
        <div class="kpi-tile"><div class="label">批次状态</div><div class="value" style="font-size:14px"><span class="line-badge" :class="badgeTone(batch?.status)">{{ batch?.status || '—' }}</span></div></div>
        <div class="kpi-tile"><div class="label">订单状态</div><div class="value" style="font-size:14px"><span class="line-badge" :class="badgeTone(order?.status)">{{ order?.status || '—' }}</span></div></div>
        <div class="kpi-tile" :class="{ alarm: gaps.length }"><div class="label">追溯断点</div><div class="value">{{ gaps.length }}</div></div>
      </div>

      <div class="kpi-strip" style="grid-template-columns:repeat(3,1fr);margin-bottom:12px">
        <div class="kpi-tile" :class="{ alarm: blockers.delivery_blocked }"><div class="label">交付阻塞</div><div class="value">{{ blockers.delivery_blocked ? '是' : '否' }}</div></div>
        <div class="kpi-tile"><div class="label">质量门阻塞</div><div class="value">{{ blockers.blocking_quality_gates ?? 0 }}</div></div>
        <div class="kpi-tile"><div class="label">物流阻塞</div><div class="value">{{ blockers.blocking_logistics_tasks ?? 0 }}</div></div>
      </div>

      <div class="panel" style="margin-bottom:var(--grid-gap)">
        <div class="panel-head"><h3>状态与阻塞摘要</h3></div>
        <div class="panel-body compact batch-summary-grid">
          <div>
            <div class="summary-title">订单</div>
            <div class="summary-line">
              订单号：
              <button v-if="orderId" class="link-btn" @click="goOrder">{{ orderId }}</button>
              <span v-else>—</span>
            </div>
            <div class="summary-line">计划量：{{ order?.planned_quantity_t ?? '—' }} t</div>
            <div class="summary-line">释放量：{{ order?.released_quantity_t ?? '—' }} t</div>
          </div>
          <div>
            <div class="summary-title">质量门</div>
            <div class="summary-line">当前阻塞：{{ blockers.quality_blocked ? '是' : '否' }}</div>
            <div class="summary-line">记录数：{{ qualityGates.length }}</div>
          </div>
          <div>
            <div class="summary-title">物流任务</div>
            <div class="summary-line">当前阻塞：{{ blockers.logistics_blocked ? '是' : '否' }}</div>
            <div class="summary-line">任务数：{{ logisticsTasks.length }}</div>
          </div>
        </div>
      </div>

      <div class="panel" style="margin-bottom:var(--grid-gap)">
        <div class="panel-head"><h3>工序穿透</h3></div>
        <div class="panel-body">
          <div class="timeline-horizontal">
            <div
              v-for="(node, i) in nodes"
              :key="node.process_step_id"
              class="tl-node clickable"
              :class="node.status"
              @click="goTwin(node.process_step_id)"
            >
              <div v-if="i > 0" class="tl-line"></div>
              <div class="dot"></div>
              <div class="label">{{ node.display_name }}</div>
              <div style="font-size:10px;color:var(--text-dim)">{{ statusLabel(node.status) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="panel" style="margin-bottom:var(--grid-gap)" v-if="materialEvents.length">
        <div class="panel-head"><h3>物料事件</h3></div>
        <div class="panel-body flush">
          <table class="data-table">
            <thead><tr><th>时间</th><th>类型</th><th>工序</th><th>位置</th><th>备注</th></tr></thead>
            <tbody>
              <tr v-for="ev in materialEvents" :key="ev.event_id">
                <td>{{ fmtDateTime(ev.timestamp) }}</td>
                <td>{{ materialEventLabel(ev.event_type) }}</td>
                <td>{{ ev.process_step_id || '—' }}</td>
                <td>{{ ev.location || ev.to_location || '—' }}</td>
                <td>{{ ev.remark || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="grid-dashboard" style="margin-bottom:var(--grid-gap)">
        <div class="col-6">
          <div class="panel">
            <div class="panel-head"><h3>质量门记录</h3></div>
            <div class="panel-body flush">
              <table class="data-table" v-if="qualityGates.length">
                <thead><tr><th>时间</th><th>工序</th><th>判定</th><th>原因</th></tr></thead>
                <tbody>
                  <tr v-for="gate in qualityGates" :key="gate.gate_event_id">
                    <td>{{ fmtDateTime(gate.decided_at) }}</td>
                    <td>{{ gate.process_step_id || '—' }}</td>
                    <td><span class="line-badge" :class="badgeTone(gate.decision === 'pass' ? 'completed' : 'hold')">{{ gate.decision }}</span></td>
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
                <thead><tr><th>时间</th><th>类型</th><th>路径</th><th>状态</th></tr></thead>
                <tbody>
                  <tr v-for="task in logisticsTasks" :key="task.task_id">
                    <td>{{ fmtDateTime(task.created_at) }}</td>
                    <td>{{ task.task_type || '—' }}</td>
                    <td>{{ task.source_location_id || '—' }} → {{ task.target_location_id || '—' }}</td>
                    <td><span class="line-badge" :class="badgeTone(task.status === 'completed' ? 'completed' : 'hold')">{{ task.status || '—' }}</span></td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-state">暂无物流任务</div>
            </div>
          </div>
        </div>
      </div>

      <div class="grid-dashboard">
        <div v-for="node in nodes" :key="node.process_step_id" class="col-6">
          <div class="panel">
            <div class="panel-head">
              <h3>{{ node.display_name }}</h3>
              <button class="tab-btn" @click="goTwin(node.process_step_id)">孪生</button>
            </div>
            <div class="panel-body compact">
              <table class="data-table" v-if="node.key_parameters?.length">
                <thead><tr><th>参数</th><th>均值</th><th>符合率</th></tr></thead>
                <tbody>
                  <tr v-for="p in node.key_parameters" :key="p.field_id">
                    <td>{{ p.field_id }}</td>
                    <td>{{ p.mean }}</td>
                    <td :class="p.spec_compliance_pct >= 95 ? 'val-good' : 'val-warn'">{{ p.spec_compliance_pct }}%</td>
                  </tr>
                </tbody>
              </table>
              <div v-else style="font-size:12px;color:var(--text-dim)">窗口内暂无采样</div>
            </div>
          </div>
        </div>
      </div>

      <div v-for="gap in gaps" :key="gap.gap_type" class="gap-banner">⚠ 追溯断点 · {{ gap.message }}</div>

      <div class="panel" v-if="batch?.parent_batches?.length" style="margin-top:var(--grid-gap)">
        <div class="panel-head"><h3>上游批次链</h3></div>
        <div class="panel-body">
          <span v-for="(pb, i) in batch.parent_batches" :key="pb" class="ctx-tag">{{ pb }}<span v-if="i < batch.parent_batches.length - 1"> → </span></span>
          <span class="ctx-tag">→ <strong>{{ batch.batch_id }}</strong></span>
        </div>
      </div>
    </div>

    <div class="page-body" v-else><div class="loading-state">LOADING…</div></div>
  </div>
</template>

<style scoped>
/* 布局见 main.css .batch-detail-root */
.detail-actions { display: flex; gap: 8px; }
.batch-summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.summary-title { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; }
.summary-line { font-size: 12px; margin-bottom: 4px; }
.line-badge { font-size: 10px; padding: 2px 8px; border-radius: 3px; }
.line-badge.running { background: rgba(52,211,153,0.15); color: var(--status-running); }
.line-badge.manual { background: rgba(251,191,36,0.15); color: var(--status-warning); }
.line-badge.stopped { background: rgba(100,116,139,0.2); color: var(--status-stopped); }
.tl-node.clickable { cursor: pointer; }
.tl-node.clickable:hover .label { color: var(--accent-cyan); }
.link-btn { background: none; border: none; color: var(--accent-cyan); cursor: pointer; padding: 0; font: inherit; }
.link-btn:hover { text-decoration: underline; }
@media (max-width: 1100px) { .batch-summary-grid { grid-template-columns: 1fr; } }
</style>
