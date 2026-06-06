<script setup>
import { ref, computed, watch } from 'vue'
import PageHeader from '../components/PageHeader.vue'
import AlarmHandleDialog from '../components/AlarmHandleDialog.vue'
import { getAlarms, getAllAlarms, getPlantAlarms, getEquipment, getLines } from '../api'
import { usePolling } from '../composables/usePolling'
import { useAppContext } from '../composables/useAppContext'
import { fmtDateTime, statusLabel } from '../utils/format'
import { useAlarmNavigation } from '../composables/useAlarmNavigation'

const { navigateToAlarmTwin } = useAlarmNavigation()
const { lineId, lines } = useAppContext()

const alarms = ref([])
const equipmentByLine = ref({})
const filter = ref('')
const severityFilter = ref('')
const scope = ref('plant')
const handleOpen = ref(false)
const handleTarget = ref(null)

const lineNameMap = computed(() => {
  const m = {}
  for (const l of lines.value) m[l.product_line_id] = l.name
  return m
})

const filtered = computed(() => {
  let list = alarms.value
  if (filter.value) list = list.filter(a => a.handle_status === filter.value)
  if (severityFilter.value) list = list.filter(a => a.severity === severityFilter.value)
  return list.sort((a, b) => String(b.triggered_at).localeCompare(String(a.triggered_at)))
})

const stats = computed(() => ({
  total: alarms.value.length,
  pending: alarms.value.filter(a => a.handle_status === 'pending').length,
  acknowledged: alarms.value.filter(a => a.handle_status === 'acknowledged').length,
  critical: alarms.value.filter(a => a.severity === 'critical').length
}))

async function loadEquipmentForLine(lid) {
  if (equipmentByLine.value[lid]) return equipmentByLine.value[lid]
  const eq = await getEquipment(lid).catch(() => [])
  equipmentByLine.value = { ...equipmentByLine.value, [lid]: eq }
  return eq
}

async function load() {
  if (scope.value === 'plant') {
    let ids = lines.value.map(l => l.product_line_id)
    if (!ids.length) {
      const ln = await getLines().catch(() => [])
      ids = ln.map(l => l.product_line_id)
    }
    alarms.value = await getPlantAlarms().catch(() => (ids.length ? getAllAlarms(ids) : []))
    if (!ids.length && alarms.value.length) {
      ids = [...new Set(alarms.value.map(a => a.product_line_id).filter(Boolean))]
    }
    await Promise.all(ids.map(loadEquipmentForLine))
  } else {
    const lid = lineId.value
    const [alarmList] = await Promise.all([
      getAlarms(lid),
      loadEquipmentForLine(lid)
    ])
    alarms.value = alarmList.map(a => ({ ...a, product_line_id: a.product_line_id || lid }))
  }
}

function goAlarmTwin(alarm) {
  const lid = alarm.product_line_id || lineId.value
  const eqList = equipmentByLine.value[lid] || []
  navigateToAlarmTwin(alarm, eqList, lid)
}

function openHandle(alarm, e) {
  e?.stopPropagation()
  handleTarget.value = alarm
  handleOpen.value = true
}

function onHandled(updated) {
  const idx = alarms.value.findIndex(a => a.alarm_id === updated.alarm_id)
  if (idx >= 0) alarms.value[idx] = { ...alarms.value[idx], ...updated }
}

function lineLabel(alarm) {
  const id = alarm.product_line_id
  return lineNameMap.value[id] ? `${lineNameMap.value[id]}` : (id || '—')
}

watch([lineId, scope], load)
watch(lines, load, { deep: true })
usePolling(load, 5000)
</script>

<template>
  <div class="analysis-root">
    <PageHeader section="分析" title="报警中心" />

    <div class="page-body full-bleed">
      <div class="alarm-toolbar">
        <div class="scope-switch">
          <button class="tab-btn" :class="{ active: scope === 'plant' }" @click="scope = 'plant'">全厂</button>
          <button class="tab-btn" :class="{ active: scope === 'line' }" @click="scope = 'line'">当前产线</button>
        </div>
        <span v-if="scope === 'line'" class="scope-hint">{{ lineId }}</span>
      </div>

      <div class="kpi-strip" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 12px">
        <div class="kpi-tile"><div class="label">报警总数</div><div class="value">{{ stats.total }}</div></div>
        <div class="kpi-tile alarm"><div class="label">待处理</div><div class="value">{{ stats.pending }}</div></div>
        <div class="kpi-tile"><div class="label">已确认</div><div class="value">{{ stats.acknowledged }}</div></div>
        <div class="kpi-tile alarm"><div class="label">Critical</div><div class="value">{{ stats.critical }}</div></div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <h3>报警事件流</h3>
          <div class="filter-group">
            <button class="tab-btn" :class="{ active: !filter }" @click="filter = ''">全部</button>
            <button class="tab-btn" :class="{ active: filter === 'pending' }" @click="filter = 'pending'">待处理</button>
            <button class="tab-btn" :class="{ active: filter === 'acknowledged' }" @click="filter = 'acknowledged'">已确认</button>
            <button class="tab-btn" :class="{ active: filter === 'resolved' }" @click="filter = 'resolved'">已解决</button>
            <span class="filter-sep">|</span>
            <button class="tab-btn" :class="{ active: !severityFilter }" @click="severityFilter = ''">全部级别</button>
            <button class="tab-btn" :class="{ active: severityFilter === 'critical' }" @click="severityFilter = 'critical'">Critical</button>
          </div>
        </div>
        <div class="panel-body flush">
          <div class="data-table-wrap" style="max-height:none">
            <table class="data-table alarm-table">
              <thead>
                <tr>
                  <th>触发时间</th>
                  <th v-if="scope === 'plant'">产线</th>
                  <th>设备</th><th>批次</th><th>代码</th>
                  <th>描述</th><th>严重程度</th><th>状态</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="a in filtered"
                  :key="a.alarm_id"
                  class="clickable-row"
                  :class="{ 'alarm-row-critical': a.severity === 'critical' }"
                  @click="goAlarmTwin(a)"
                  title="点击行定位孪生；操作列可处理"
                >
                  <td>{{ fmtDateTime(a.triggered_at) }}</td>
                  <td v-if="scope === 'plant'">{{ lineLabel(a) }}</td>
                  <td>{{ a.equipment_id }}</td>
                  <td>{{ a.product_batch || '—' }}</td>
                  <td>{{ a.alarm_code }}</td>
                  <td>
                    {{ a.alarm_message }}
                    <div v-if="a.handle_note" class="note-hint">{{ a.handle_note }}</div>
                  </td>
                  <td><span class="severity-badge" :class="a.severity">{{ a.severity }}</span></td>
                  <td>
                    {{ statusLabel(a.handle_status) }}
                    <span v-if="a.handler" class="handler-hint">· {{ a.handler }}</span>
                  </td>
                  <td class="action-cell" @click.stop>
                    <button
                      v-if="a.handle_status !== 'resolved'"
                      type="button"
                      class="tab-btn mini"
                      @click="openHandle(a, $event)"
                    >处理</button>
                    <span v-else class="resolved-at">{{ a.resolved_at ? fmtDateTime(a.resolved_at).slice(11) : '—' }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!filtered.length" class="empty-state">
              {{ scope === 'plant' ? '全厂暂无报警' : '当前产线暂无报警' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <AlarmHandleDialog
      :open="handleOpen"
      :alarm="handleTarget"
      @close="handleOpen = false"
      @done="onHandled"
    />
  </div>
</template>

<style scoped>
/* 布局见 main.css .analysis-root */
.alarm-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.scope-switch { display: flex; gap: 6px; }
.scope-hint { font-size: 11px; color: var(--text-dim); font-family: var(--font-mono); }
.filter-group { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.filter-sep { color: var(--text-dim); font-size: 11px; }
.clickable-row { cursor: pointer; }
.clickable-row:hover { background: rgba(34, 211, 238, 0.06); }
.handler-hint, .note-hint { font-size: 10px; color: var(--text-dim); }
.note-hint { margin-top: 2px; }
.action-cell { white-space: nowrap; }
.tab-btn.mini { font-size: 10px; padding: 2px 8px; }
.resolved-at { font-size: 10px; color: var(--text-dim); font-family: var(--font-mono); }
</style>
