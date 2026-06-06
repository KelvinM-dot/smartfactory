import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserRole } from './useUserRole'

/**
 * 厂长 / 线长侧栏导航（不含产线选择；产线由各业务页独立控件负责）。
 */
export function useRoleNavigation() {
  const router = useRouter()
  const { role } = useUserRole()

  const isSupervisor = computed(() => role.value === 'supervisor')

  function goOrdersNav() {
    router.push({ path: '/orders', query: { view: 'workbench' } })
  }

  function onRoleSwitch(nextRole, lineId) {
    if (nextRole === 'supervisor') {
      router.push(`/lines/${lineId || 'FCW-LINE-07'}`)
      return
    }
    router.push('/factory')
  }

  const ordersNavHint = computed(() =>
    isSupervisor.value ? '线长 · 全厂订单' : '全厂交付'
  )

  const workbenchNavLabel = '产线工作台'

  return {
    isSupervisor,
    goOrdersNav,
    onRoleSwitch,
    ordersNavHint,
    workbenchNavLabel
  }
}
