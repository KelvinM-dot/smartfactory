<script setup>
import { ref, watch, computed } from 'vue'
import { acknowledgeAlarm, resolveAlarm } from '../api'
import { useUserRole } from '../composables/useUserRole'

const props = defineProps({
  alarm: { type: Object, default: null },
  open: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'done'])

const { roleMeta } = useUserRole()
const handler = ref(localStorage.getItem('jqhc-handler-name') || '')
const note = ref('')
const busy = ref(false)
const error = ref('')

const title = computed(() => props.alarm?.alarm_message || '报警处理')
const canAck = computed(() => props.alarm?.handle_status === 'pending')
const canResolve = computed(() =>
  props.alarm && ['pending', 'acknowledged'].includes(props.alarm.handle_status)
)

watch(() => props.open, (v) => {
  if (v) {
    note.value = props.alarm?.handle_note || ''
    if (!handler.value) handler.value = roleMeta.value.label
    error.value = ''
  }
})

function close() {
  emit('close')
}

async function submit(action) {
  if (!props.alarm?.alarm_id) return
  busy.value = true
  error.value = ''
  const payload = {
    handler: handler.value || roleMeta.value.label,
    handle_note: note.value
  }
  if (handler.value) localStorage.setItem('jqhc-handler-name', handler.value)
  try {
    const fn = action === 'resolve' ? resolveAlarm : acknowledgeAlarm
    const updated = await fn(props.alarm.alarm_id, payload)
    emit('done', updated)
    emit('close')
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '操作失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div v-if="open && alarm" class="alarm-dialog-backdrop" @click.self="close">
    <div class="alarm-dialog panel">
      <div class="panel-head">
        <h3>报警处理</h3>
        <button type="button" class="tab-btn" @click="close">✕</button>
      </div>
      <div class="panel-body compact">
        <div class="alarm-summary">
          <div><span class="k">设备</span> {{ alarm.equipment_id }}</div>
          <div><span class="k">代码</span> {{ alarm.alarm_code }}</div>
          <div><span class="k">描述</span> {{ alarm.alarm_message }}</div>
          <div><span class="k">状态</span> {{ alarm.handle_status }}</div>
        </div>

        <label class="field">
          <span>处理人</span>
          <input v-model="handler" type="text" placeholder="线长姓名" />
        </label>
        <label class="field">
          <span>处理备注</span>
          <textarea v-model="note" rows="3" placeholder="原因、措施、复检结果…" />
        </label>

        <div v-if="error" class="error-msg">{{ error }}</div>

        <div class="actions">
          <button
            v-if="canAck"
            type="button"
            class="tab-btn"
            :disabled="busy"
            @click="submit('acknowledge')"
          >确认知晓</button>
          <button
            v-if="canResolve"
            type="button"
            class="tab-btn primary"
            :disabled="busy"
            @click="submit('resolve')"
          >标记已解决</button>
          <button type="button" class="tab-btn" :disabled="busy" @click="close">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.alarm-dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(7, 11, 20, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}
.alarm-dialog { width: min(440px, 100%); }
.alarm-summary { font-size: 12px; margin-bottom: 12px; display: grid; gap: 4px; }
.alarm-summary .k { color: var(--text-dim); margin-right: 6px; }
.field { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; font-size: 11px; color: var(--text-dim); }
.field input, .field textarea {
  background: var(--bg-root);
  border: 1px solid var(--border-dim);
  color: var(--text-primary);
  border-radius: 3px;
  padding: 6px 8px;
  font-size: 12px;
  font-family: inherit;
}
.actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.tab-btn.primary { border-color: var(--accent-cyan); color: var(--accent-cyan); }
.error-msg { color: var(--status-alarm); font-size: 12px; margin-bottom: 8px; }
</style>
