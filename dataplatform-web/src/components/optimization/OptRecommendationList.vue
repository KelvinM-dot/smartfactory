<script setup>
defineProps({
  items: { type: Array, default: () => [] }
})

const DIM_LABELS = { efficiency: '效率', cost: '成本', quality: '质量' }
const PRIORITY_CLASS = { high: 'pri-high', medium: 'pri-med', low: 'pri-low' }
</script>

<template>
  <div class="rec-list">
    <div v-for="(item, i) in items" :key="i" class="rec-item" :class="PRIORITY_CLASS[item.priority] || ''">
      <div class="rec-meta">
        <span class="rec-dim">{{ DIM_LABELS[item.dimension] || item.dimension }}</span>
        <span class="rec-pri">{{ item.priority }}</span>
        <span v-if="item.model_id" class="rec-model">{{ item.model_id }}</span>
      </div>
      <div class="rec-title">{{ item.title }}</div>
      <div class="rec-detail">{{ item.detail }}</div>
    </div>
    <div v-if="!items.length" class="rec-empty">暂无优化建议</div>
  </div>
</template>

<style scoped>
.rec-list { display: flex; flex-direction: column; gap: 8px; }
.rec-item {
  padding: 10px 12px;
  background: var(--bg-panel-elevated);
  border-left: 3px solid var(--border-accent);
  border-radius: 0 var(--radius) var(--radius) 0;
}
.rec-item.pri-high { border-left-color: var(--status-alarm); }
.rec-item.pri-med { border-left-color: var(--status-warning); }
.rec-meta { display: flex; gap: 8px; font-size: 10px; margin-bottom: 4px; }
.rec-dim { color: var(--accent-cyan); font-family: var(--font-mono); }
.rec-pri { color: var(--text-dim); text-transform: uppercase; }
.rec-model { color: var(--text-dim); }
.rec-title { font-size: 13px; font-weight: 600; }
.rec-detail { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.rec-empty { color: var(--text-dim); font-size: 12px; padding: 12px; }
</style>
