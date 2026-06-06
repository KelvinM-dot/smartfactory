<script setup>
import { computed } from 'vue'
import { resolveBlockReasons } from '../../utils/orderTriage'

const props = defineProps({
  order: { type: Object, default: null },
  detail: { type: Object, default: null },
  inline: { type: Boolean, default: false }
})

const reasons = computed(() => resolveBlockReasons(props.order, props.detail))
</script>

<template>
  <div class="order-block-reason" :class="{ inline }">
    <template v-if="reasons.length">
      <span v-if="inline">{{ reasons.join(' · ') }}</span>
      <div v-else class="order-block-tags">
        <span v-for="(r, i) in reasons" :key="i" class="order-block-tag">{{ r }}</span>
      </div>
    </template>
    <span v-else class="dim">暂无明显阻塞</span>
  </div>
</template>

<style scoped>
.dim { color: var(--text-secondary); font-size: 12px; }
</style>
