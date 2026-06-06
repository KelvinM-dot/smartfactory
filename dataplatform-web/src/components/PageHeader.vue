<script setup>
defineProps({
  section: { type: String, default: '' },
  title: { type: String, default: '' }
})
</script>

<template>
  <div class="page-header">
    <div class="breadcrumb">{{ section }}<span v-if="title"> / {{ title }}</span></div>
    <slot />
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-dim);
  background: var(--bg-panel);
  flex-shrink: 0;
}

.breadcrumb {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
