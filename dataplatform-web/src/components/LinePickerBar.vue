<script setup>
import { ref, computed } from 'vue'
import {
  orderedWorkshopGroups,
  filterLines,
  lineShortId,
  workshopLabel,
  lineStatusBadge
} from '../utils/lineCatalog'

const props = defineProps({
  lines: { type: Array, default: () => [] },
  workshop: { type: String, default: 'all' },
  search: { type: String, default: undefined },
  activeLineId: { type: String, default: '' },
  allowAll: { type: Boolean, default: false },
  allLabel: { type: String, default: '全厂' }
})

const emit = defineEmits(['update:workshop', 'update:search', 'pick-line', 'pick-all'])

const WORKSHOP_TABS = [
  { id: 'all', label: '全部' },
  { id: 'wire', label: '焊丝' },
  { id: 'rod', label: '焊条' }
]

const internalSearch = ref('')

const lineSearch = computed({
  get() {
    return props.search !== undefined ? props.search : internalSearch.value
  },
  set(value) {
    if (props.search !== undefined) emit('update:search', value)
    else internalSearch.value = value
  }
})

const filteredLines = computed(() =>
  filterLines(props.lines, { workshop: props.workshop, search: lineSearch.value })
)

const groupedLines = computed(() => {
  const ids = new Set(filteredLines.value.map(l => l.product_line_id))
  return orderedWorkshopGroups(props.lines)
    .map(g => ({
      ...g,
      label: workshopLabel(g.workshopId),
      lines: g.lines.filter(l => ids.has(l.product_line_id))
    }))
    .filter(g => g.lines.length)
})

const showGroupLabels = computed(
  () => props.workshop === 'all' && !lineSearch.value.trim()
)

function setWorkshop(id) {
  emit('update:workshop', id)
}

function pickLine(lineId) {
  emit('pick-line', lineId)
}

function pickAllScope() {
  lineSearch.value = ''
  emit('pick-all')
}

function lineBtnClass(line) {
  return {
    active: line.product_line_id === props.activeLineId,
    stopped: line.status === 'inactive',
    manual: line.status === 'maintenance'
  }
}

function lineBtnTitle(line) {
  const badge = lineStatusBadge(line)
  return `${line.name} · ${line.product_line_id} · ${badge.label}`
}
</script>

<template>
  <div class="line-picker">
    <div class="line-picker-toolbar">
      <div class="line-picker-tabs">
        <button
          v-if="allowAll"
          type="button"
          class="line-picker-tab"
          :class="{ active: !activeLineId && workshop === 'all' }"
          @click="pickAllScope"
        >{{ allLabel }}</button>
        <button
          v-for="tab in WORKSHOP_TABS"
          :key="tab.id"
          type="button"
          class="line-picker-tab"
          :class="{ active: workshop === tab.id }"
          @click="setWorkshop(tab.id)"
        >
          {{ tab.label }}
          <span v-if="tab.id === 'all'" class="line-picker-tab-count">{{ lines.length }}</span>
        </button>
      </div>
      <label class="line-picker-search-wrap">
        <input
          v-model="lineSearch"
          type="search"
          placeholder="搜索产线 ID / 名称"
          class="line-picker-search"
        />
      </label>
      <span class="line-picker-hint">显示 {{ filteredLines.length }} / {{ lines.length }} 线</span>
    </div>

    <div class="line-picker-body">
      <template v-if="groupedLines.length">
        <div
          v-for="group in groupedLines"
          :key="group.workshopId"
          class="line-picker-group"
        >
          <span v-if="showGroupLabels" class="line-picker-group-label">{{ group.label }}</span>
          <div class="line-picker-btns">
            <button
              v-for="line in group.lines"
              :key="line.product_line_id"
              type="button"
              class="line-picker-btn"
              :class="lineBtnClass(line)"
              :title="lineBtnTitle(line)"
              @click="pickLine(line.product_line_id)"
            >
              {{ lineShortId(line.product_line_id) }}
            </button>
          </div>
        </div>
      </template>
      <div v-else class="line-picker-empty">无匹配产线，请调整车间或搜索条件</div>
    </div>
  </div>
</template>

<style scoped>
.line-picker {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.line-picker-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.line-picker-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.line-picker-tab {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 3px;
  border: 1px solid var(--border-dim);
  background: transparent;
  color: var(--text-dim);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.line-picker-tab:hover {
  border-color: rgba(34, 211, 238, 0.35);
  color: var(--text-primary);
}

.line-picker-tab.active {
  border-color: var(--accent-cyan);
  color: var(--accent-cyan);
  background: rgba(34, 211, 238, 0.08);
}

.line-picker-tab-count {
  font-size: 10px;
  font-family: var(--font-mono);
  opacity: 0.75;
}

.line-picker-search {
  background: var(--bg-root);
  border: 1px solid var(--border-dim);
  color: var(--text-primary);
  border-radius: 3px;
  padding: 4px 10px;
  font-size: 12px;
  font-family: var(--font-mono);
  min-width: 160px;
}

.line-picker-hint {
  margin-left: auto;
  font-size: 10px;
  color: var(--text-dim);
  white-space: nowrap;
}

.line-picker-body {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px 14px;
  max-height: 96px;
  overflow-y: auto;
  padding: 2px 0;
}

.line-picker-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.line-picker-group-label {
  font-size: 10px;
  color: var(--text-dim);
  white-space: nowrap;
  min-width: 28px;
}

.line-picker-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.line-picker-btn {
  font-size: 11px;
  font-family: var(--font-mono);
  padding: 4px 8px;
  border-radius: 3px;
  border: 1px solid var(--border-dim);
  background: rgba(100, 116, 139, 0.06);
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s, background 0.12s;
}

.line-picker-btn:hover {
  border-color: rgba(34, 211, 238, 0.4);
  color: var(--accent-cyan);
}

.line-picker-btn.active {
  border-color: var(--accent-cyan);
  color: var(--accent-cyan);
  background: rgba(34, 211, 238, 0.12);
  box-shadow: 0 0 0 1px rgba(34, 211, 238, 0.15);
}

.line-picker-btn.stopped { opacity: 0.5; }
.line-picker-btn.manual { border-color: rgba(251, 191, 36, 0.35); }

.line-picker-empty {
  font-size: 11px;
  color: var(--text-dim);
}
</style>
