import { createRouter, createWebHistory } from 'vue-router'
import FactoryOverview from '../views/FactoryOverview.vue'
import LineWorkbench from '../views/LineWorkbench.vue'
import BatchCenter from '../views/BatchCenter.vue'
import BatchDetail from '../views/BatchDetail.vue'
import OrderCenter from '../views/OrderCenter.vue'
import OrderDetail from '../views/OrderDetail.vue'
import Trends from '../views/Trends.vue'
import Alarms from '../views/Alarms.vue'
import OptOverview from '../views/optimization/OptOverview.vue'
import OptEfficiency from '../views/optimization/OptEfficiency.vue'
import OptCost from '../views/optimization/OptCost.vue'
import OptQuality from '../views/optimization/OptQuality.vue'
import OptScenarios from '../views/optimization/OptScenarios.vue'
import OptBalance from '../views/optimization/OptBalance.vue'
import OptEnergyCarbon from '../views/optimization/OptEnergyCarbon.vue'
import { getDefaultHomePath } from '../composables/useUserRole'
import { LINE_ID } from '../api'

function lineRedirect(tab) {
  return (to) => ({
    path: `/lines/${to.params.lineId || LINE_ID}`,
    query: { tab, ...to.query }
  })
}

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: () => getDefaultHomePath() },
    { path: '/factory', component: FactoryOverview },
    { path: '/orders', component: OrderCenter },
    { path: '/orders/:orderId', component: OrderDetail, props: true },
    { path: '/lines/:lineId', component: LineWorkbench, props: true },
    { path: '/batches', component: BatchCenter },
    { path: '/batches/:batchId', component: BatchDetail, props: true },
    { path: '/trends', component: Trends },
    { path: '/alarms', component: Alarms },

    { path: '/optimization', component: OptOverview },
    { path: '/optimization/efficiency', component: OptEfficiency },
    { path: '/optimization/cost', component: OptCost },
    { path: '/optimization/quality', component: OptQuality },
    { path: '/optimization/scenarios', component: OptScenarios },
    { path: '/optimization/balance', component: OptBalance },
    { path: '/optimization/energy-carbon', component: OptEnergyCarbon },

    // 旧路由重定向
    { path: '/material', redirect: { path: '/batches', query: { tab: 'inventory' } } },
    { path: '/monitor/trends', redirect: '/trends' },
    { path: '/monitor/alarms', redirect: '/alarms' },
    { path: '/monitor/lines/:lineId', redirect: lineRedirect('overview') },
    { path: '/monitor/twin/:lineId', redirect: lineRedirect('twin') },
    { path: '/monitor/process/:lineId', redirect: (to) => ({
      path: `/lines/${to.params.lineId}`,
      query: { tab: 'twin', step: to.query.step }
    }) },
    { path: '/monitor/lines', redirect: `/lines/${LINE_ID}` },
    { path: '/monitor/twin', redirect: { path: `/lines/${LINE_ID}`, query: { tab: 'twin' } } },
    { path: '/monitor/process/:lineId/legacy', redirect: lineRedirect('twin') }
  ]
})
