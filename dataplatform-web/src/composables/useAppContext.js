import { inject, provide, ref, computed } from 'vue'
import { getLines, getBatches, getOverview, getTwinLayouts } from '../api'
import { ensureTwinLayoutsLoaded } from '../config/twinLayout'
import { rememberLine } from './useUserRole'
import { pickDefaultLineId } from '../utils/lineCatalog'

const CONTEXT_KEY = Symbol('appContext')

export function provideAppContext() {
  const lineId = ref('')
  const batchId = ref('')
  const timeRange = ref('live')
  const lines = ref([])
  const batches = ref([])
  const overview = ref(null)

  const source = computed(() => overview.value?.data_source || {})
  const computedAt = computed(() => overview.value?.computed_at)
  const currentBatch = computed(() => overview.value?.current_batch)

  async function refreshMeta() {
    try {
      if (!lines.value.length) {
        lines.value = await getLines()
        if (!lineId.value) {
          lineId.value = pickDefaultLineId(lines.value)
          rememberLine(lineId.value)
        }
      }
      if (!lineId.value) return
      overview.value = await getOverview(lineId.value)
      batches.value = await getBatches(lineId.value)
      const current = overview.value?.current_batch?.batch_id
      if (current && (!batchId.value || !batches.value.some(b => b.batch_id === batchId.value))) {
        batchId.value = current
      }
    } catch {
      /* API 不可达时保留上次 overview，避免误闪 offline */
    }
  }

  /** 页面轮询/WS 更新后同步到顶栏上下文 */
  function setOverview(data) {
    if (!data) return
    overview.value = data
  }

  function setLineId(id, { force = false } = {}) {
    if (!id) return
    const sameLine = id === lineId.value
    if (sameLine && !force && overview.value) return
    lineId.value = id
    rememberLine(id)
    if (!sameLine) batchId.value = ''
    refreshMeta()
  }

  function setBatchId(id) {
    batchId.value = id || ''
  }

  function setTimeRange(range) {
    timeRange.value = range
  }

  const ctx = {
    lineId,
    batchId,
    timeRange,
    lines,
    batches,
    overview,
    source,
    computedAt,
    currentBatch,
    setLineId,
    setBatchId,
    setTimeRange,
    refreshMeta,
    setOverview
  }

  provide(CONTEXT_KEY, ctx)
  ensureTwinLayoutsLoaded(getTwinLayouts)
  refreshMeta()

  return ctx
}

export function useAppContext() {
  const ctx = inject(CONTEXT_KEY)
  if (!ctx) {
    throw new Error('useAppContext must be used within App')
  }
  return ctx
}
