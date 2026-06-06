<script setup>
import LineTwin3D from './LineTwin3D.vue'
import LineTwinCanvas from './LineTwinCanvas.vue'
import { supportsTwinVisual, supportsTwin3D } from '../../utils/dataPoints'

defineProps({
  lineId: { type: String, default: 'FCW-LINE-07' },
  pipeline: { type: Array, default: () => [] },
  equipment: { type: Array, default: () => [] },
  batch: { type: Object, default: null },
  selectedStep: { type: String, default: null },
  /** compact | medium | large */
  size: { type: String, default: 'medium' },
  show2d: { type: Boolean, default: false },
  title: { type: String, default: '产线镜像' },
  subtitle: { type: String, default: '' }
})

defineEmits(['select-step', 'select-equipment'])
</script>

<template>
  <div class="visual-panel" :class="[`size-${size}`, { 'with-2d': show2d }]">
    <div class="visual-3d panel">
      <div v-if="title" class="visual-head">
        <h3>{{ title }}</h3>
        <span v-if="subtitle" class="meta">{{ subtitle }}</span>
      </div>
      <div class="visual-body flush">
        <template v-if="supportsTwin3D(lineId)">
          <LineTwin3D
            :line-id="lineId"
            :pipeline="pipeline"
            :equipment="equipment"
            :batch="batch"
            :selected-step="selectedStep"
            @select-step="$emit('select-step', $event)"
          />
        </template>
        <div v-else class="visual-placeholder">
          <div class="ph-title">{{ lineId }}</div>
          <div class="ph-sub">3D 布局未就绪 · 请检查主数据 twin_layouts / twin_3d_ready</div>
        </div>
      </div>
    </div>

    <div v-if="show2d && supportsTwinVisual(lineId)" class="visual-2d panel">
      <div class="visual-head">
        <h3>2D 拓扑</h3>
        <span class="meta">物料流向</span>
      </div>
      <div class="visual-body flush">
        <LineTwinCanvas
          :line-id="lineId"
          :pipeline="pipeline"
          :equipment="equipment"
          :batch="batch"
          :selected-step="selectedStep"
          @select-step="$emit('select-step', $event)"
          @select-equipment="$emit('select-equipment', $event)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.visual-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.visual-panel.with-2d {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.visual-3d, .visual-2d {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--bg-panel);
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
}

.visual-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-dim);
}

.visual-head h3 {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
}

.visual-body {
  flex: 1;
  min-height: 0;
}

.size-compact .visual-3d { height: 200px; }
.size-medium .visual-3d { height: min(32vh, 320px); min-height: 220px; }
.size-large .visual-3d { height: min(42vh, 420px); min-height: 280px; }

.size-compact.with-2d .visual-3d,
.size-compact.with-2d .visual-2d { height: 200px; }

.visual-placeholder {
  height: 100%;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #070b14;
  color: var(--text-dim);
  text-align: center;
  padding: 20px;
}

.ph-title {
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--accent-cyan);
  margin-bottom: 6px;
}

.ph-sub { font-size: 12px; }

@media (max-width: 1100px) {
  .visual-panel.with-2d { grid-template-columns: 1fr; }
}
</style>
