<script setup>
import { ref, computed, watch } from 'vue'
import { createOrder } from '../../api'
import { fmtDateTime } from '../../utils/format'
import {
  LINE_ACTUAL_UTILIZATION,
  fmtQtyT,
  productCategoryLabel,
  priorityLabel
} from '../../utils/orderLabels'

const props = defineProps({
  open: { type: Boolean, default: false },
  factoryId: { type: String, required: true },
  orders: { type: Array, default: () => [] },
  lines: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  recipes: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'created'])

const createForm = ref(emptyForm())
const createBusy = ref(false)

function emptyForm() {
  return {
    customer_order_id: '',
    product_id: '',
    product_category: '',
    grade: '',
    recipe_id: '',
    assigned_line_id: '',
    priority: 'normal',
    planned_quantity_t: 10,
    due_date: '',
    remark: 'created_from_order_center'
  }
}

const productOptions = computed(() => props.products)
const recipeOptions = computed(() => props.recipes.filter(item => {
  if (createForm.value.product_category && item.product_category !== createForm.value.product_category) return false
  if (createForm.value.grade && item.grade !== createForm.value.grade) return false
  if (createForm.value.assigned_line_id && item.product_line_id !== createForm.value.assigned_line_id) return false
  if (createForm.value.product_id) {
    const product = props.products.find(p => p.product_id === createForm.value.product_id)
    if (product?.default_recipe_ids?.length && !product.default_recipe_ids.includes(item.recipe_id)) return false
  }
  return true
}))
const lineOptions = computed(() => props.lines.filter(item => {
  if (createForm.value.product_category && item.product_category !== createForm.value.product_category) return false
  if (createForm.value.product_id) {
    const product = props.products.find(p => p.product_id === createForm.value.product_id)
    if (product?.allowed_line_ids?.length && !product.allowed_line_ids.includes(item.product_line_id)) return false
  }
  if (createForm.value.recipe_id) {
    const recipe = props.recipes.find(r => r.recipe_id === createForm.value.recipe_id)
    if (recipe && item.product_line_id !== recipe.product_line_id) return false
  }
  return true
}))
const selectedProduct = computed(() => props.products.find(p => p.product_id === createForm.value.product_id))
const selectedRecipe = computed(() => props.recipes.find(r => r.recipe_id === createForm.value.recipe_id))
const selectedLine = computed(() => props.lines.find(l => l.product_line_id === createForm.value.assigned_line_id))

const lineDesignCapacityPerDay = computed(() => Number(selectedLine.value?.design_capacity_t_per_day || 0))
const lineCapacityPerDay = computed(() => lineDesignCapacityPerDay.value * LINE_ACTUAL_UTILIZATION)
const lineBacklogOrders = computed(() => props.orders.filter(item => {
  const assignedLines = item.assigned_line_ids || []
  return createForm.value.assigned_line_id
    && assignedLines.includes(createForm.value.assigned_line_id)
    && item.order_status !== 'completed'
}))
const lineBacklogQuantityT = computed(() => lineBacklogOrders.value.reduce((sum, item) => {
  const planned = Number(item.planned_quantity_t || 0)
  const completed = Number(item.completed_quantity_t || 0)
  return sum + Math.max(planned - completed, 0)
}, 0))
const lineLoadDays = computed(() => lineCapacityPerDay.value ? lineBacklogQuantityT.value / lineCapacityPerDay.value : null)
const estimatedProductionDays = computed(() => {
  if (!lineCapacityPerDay.value || !createForm.value.planned_quantity_t) return null
  return (lineBacklogQuantityT.value + Number(createForm.value.planned_quantity_t || 0)) / lineCapacityPerDay.value
})
const estimatedFinishAt = computed(() => {
  if (!estimatedProductionDays.value) return null
  return new Date(Date.now() + estimatedProductionDays.value * 24 * 60 * 60 * 1000)
})
const deliveryAssessment = computed(() => {
  if (!createForm.value.due_date || !estimatedFinishAt.value) return '待选择交期后生成预计交付说明。'
  const dueTs = new Date(createForm.value.due_date).getTime()
  const finishTs = estimatedFinishAt.value.getTime()
  if (Number.isNaN(dueTs) || Number.isNaN(finishTs)) return '交期信息不足，无法评估。'
  const diffHours = (dueTs - finishTs) / (1000 * 60 * 60)
  if (diffHours >= 24) return '按实际可达产能推算，可在交期前完成并留有缓冲。'
  if (diffHours >= 0) return '按实际可达产能推算，可按期完成，但交付缓冲较小。'
  return '按实际可达产能推算，存在延期风险，建议调整产线或交期。'
})

function hydrateDefaults() {
  if (!createForm.value.product_id && productOptions.value.length) {
    createForm.value.product_id = productOptions.value[0].product_id
  }
  const product = props.products.find(p => p.product_id === createForm.value.product_id)
  if (product) {
    createForm.value.product_category = product.product_category || ''
    createForm.value.grade = product.grade || ''
  }
  if (!createForm.value.recipe_id && recipeOptions.value.length) {
    createForm.value.recipe_id = recipeOptions.value[0].recipe_id
  }
  if (!createForm.value.assigned_line_id && lineOptions.value.length) {
    createForm.value.assigned_line_id = lineOptions.value[0].product_line_id
  }
}

watch(() => props.open, (v) => { if (v) hydrateDefaults() })
watch(() => props.products.length, hydrateDefaults)

watch(() => createForm.value.product_id, (productId) => {
  const product = props.products.find(p => p.product_id === productId)
  if (!product) return
  createForm.value.product_category = product.product_category || ''
  createForm.value.grade = product.grade || ''
  if (!recipeOptions.value.some(r => r.recipe_id === createForm.value.recipe_id)) {
    createForm.value.recipe_id = recipeOptions.value[0]?.recipe_id || ''
  }
  if (!lineOptions.value.some(l => l.product_line_id === createForm.value.assigned_line_id)) {
    createForm.value.assigned_line_id = lineOptions.value[0]?.product_line_id || ''
  }
})

async function submitOrder() {
  if (createBusy.value) return
  createBusy.value = true
  try {
    const payload = {
      factory_id: props.factoryId,
      customer_order_id: createForm.value.customer_order_id,
      product_id: createForm.value.product_id,
      product_category: createForm.value.product_category,
      grade: createForm.value.grade,
      recipe_id: createForm.value.recipe_id,
      priority: createForm.value.priority,
      planned_quantity_t: createForm.value.planned_quantity_t,
      due_date: createForm.value.due_date ? new Date(createForm.value.due_date).toISOString() : undefined,
      assigned_line_ids: [createForm.value.assigned_line_id],
      remark: createForm.value.remark
    }
    const created = await createOrder(payload)
    emit('created', created.productionOrderId || created.production_order_id)
    createForm.value = emptyForm()
    hydrateDefaults()
    emit('close')
  } finally {
    createBusy.value = false
  }
}
</script>

<template>
  <div v-if="open" class="order-drawer-backdrop" @click.self="$emit('close')">
    <div class="order-drawer">
      <div class="order-drawer-head">
        <h3>新增订单 / 下发</h3>
        <button class="tab-btn" @click="$emit('close')">关闭</button>
      </div>
      <div class="create-order-grid">
        <label class="form-field">
          <span class="field-label">客户单号</span>
          <input v-model="createForm.customer_order_id" class="filter-select" placeholder="例如 SO-EXPORT-202606-A12">
        </label>
        <label class="form-field">
          <span class="field-label">产品</span>
          <select v-model="createForm.product_id" class="filter-select">
            <option v-for="item in productOptions" :key="item.product_id" :value="item.product_id">
              {{ item.display_name }} / {{ item.product_id }}
            </option>
          </select>
        </label>
        <label class="form-field">
          <span class="field-label">执行配方</span>
          <select v-model="createForm.recipe_id" class="filter-select">
            <option v-for="item in recipeOptions" :key="item.recipe_id" :value="item.recipe_id">
              {{ item.display_name }} / {{ item.recipe_id }}
            </option>
          </select>
        </label>
        <label class="form-field">
          <span class="field-label">下发产线</span>
          <select v-model="createForm.assigned_line_id" class="filter-select">
            <option v-for="item in lineOptions" :key="item.product_line_id" :value="item.product_line_id">
              {{ item.name }} / {{ item.product_line_id }}
            </option>
          </select>
        </label>
        <label class="form-field">
          <span class="field-label">订单优先级</span>
          <select v-model="createForm.priority" class="filter-select">
            <option value="low">{{ priorityLabel('low') }}</option>
            <option value="normal">{{ priorityLabel('normal') }}</option>
            <option value="high">{{ priorityLabel('high') }}</option>
          </select>
        </label>
        <label class="form-field">
          <span class="field-label">计划量（吨）</span>
          <input v-model.number="createForm.planned_quantity_t" class="filter-select" type="number" min="1" step="0.1">
        </label>
        <label class="form-field">
          <span class="field-label">交期</span>
          <input v-model="createForm.due_date" class="filter-select" type="datetime-local">
        </label>
        <div class="explain-card capability-card">
          <div class="field-label">产线能力提示</div>
          <div class="summary-line">品类：{{ productCategoryLabel(createForm.product_category) }} / {{ createForm.grade }}</div>
          <div class="summary-line">实际可达产能：{{ lineCapacityPerDay ? `${lineCapacityPerDay.toFixed(1)} t/天` : '—' }}</div>
          <div class="summary-line">当前积压：{{ lineBacklogOrders.length }} 单 / {{ fmtQtyT(lineBacklogQuantityT) }}</div>
          <div class="summary-line">合并后理论周期：{{ estimatedProductionDays != null ? `${estimatedProductionDays.toFixed(1)} 天` : '—' }}</div>
        </div>
        <div class="explain-card delivery-card">
          <div class="field-label">预计交付解释</div>
          <div class="summary-line">理论完工：{{ fmtDateTime(estimatedFinishAt) }}</div>
          <div class="summary-line">评估：{{ deliveryAssessment }}</div>
        </div>
        <button class="tab-btn primary" :disabled="createBusy" @click="submitOrder">
          {{ createBusy ? '提交中…' : '创建并下发' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-order-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 11px; color: var(--text-secondary); }
.filter-select {
  background: var(--bg-panel-elevated);
  color: var(--text-primary);
  border: 1px solid var(--border-dim);
  padding: 6px 10px;
  border-radius: var(--radius);
}
.explain-card {
  padding: 10px 12px;
  border: 1px solid var(--border-dim);
  border-radius: var(--radius);
  grid-column: span 2;
}
.capability-card { background: rgba(59,130,246,0.06); }
.delivery-card { background: rgba(251,191,36,0.08); }
.summary-line { font-size: 12px; margin-bottom: 4px; }
.tab-btn.primary { border-color: var(--accent-cyan); color: var(--accent-cyan); }
</style>
