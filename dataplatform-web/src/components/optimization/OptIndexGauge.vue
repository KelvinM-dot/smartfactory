<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: Number, default: 0 },
  statusLabel: { type: String, default: '' },
  color: { type: String, default: 'cyan' }
})
</script>

<template>
  <div class="gauge" :class="`gauge--${color}`">
    <div class="gauge-value">{{ value?.toFixed?.(1) ?? value }}</div>
    <div class="gauge-label">{{ label }}</div>
    <div v-if="statusLabel" class="gauge-status">{{ statusLabel }}</div>
  </div>
</template>

<style scoped>
.gauge {
  background: var(--bg-panel-elevated);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  padding: 14px 16px;
  text-align: center;
}
.gauge--cyan { border-top: 2px solid var(--accent-cyan); }
.gauge--amber { border-top: 2px solid var(--accent-amber); }
.gauge--teal { border-top: 2px solid var(--accent-teal); }
.gauge-value {
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--text-primary);
}
.gauge-label { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
.gauge-status { font-size: 11px; color: var(--text-dim); margin-top: 2px; }
</style>
