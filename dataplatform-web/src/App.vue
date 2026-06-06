<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { provideAppContext } from './composables/useAppContext'
import { useUserRole, USER_ROLES } from './composables/useUserRole'
import { useRoleNavigation } from './composables/useRoleNavigation'
import GlobalContextBar from './components/GlobalContextBar.vue'
import { lineShortId } from './utils/lineCatalog'

const route = useRoute()
const ctx = provideAppContext()
const { role, setRole } = useUserRole()
const {
  isSupervisor,
  goOrdersNav,
  onRoleSwitch,
  ordersNavHint,
  workbenchNavLabel
} = useRoleNavigation()

const currentLine = computed(() =>
  ctx.lines.value.find(l => l.product_line_id === ctx.lineId.value)
)
const workbenchLink = computed(() => `/lines/${ctx.lineId.value || 'FCW-LINE-07'}`)

function onRoleChange(e) {
  const next = e.target.value
  setRole(next)
  onRoleSwitch(next, ctx.lineId.value)
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="title">智造数据台</div>
        <div class="sub">JQHC · POC</div>
      </div>

      <div class="sidebar-nav-scroll">
        <div class="nav-group">
          <div class="nav-group-label">工厂</div>
          <nav>
            <router-link to="/factory"><span class="icon">⌂</span> 工厂驾驶舱</router-link>
          </nav>
        </div>

        <div class="nav-group">
          <div class="nav-group-label">生产</div>
          <nav>
            <a
              href="/orders"
              class="orders-nav-link"
              :class="{ 'router-link-active': route.path === '/orders' || route.path.startsWith('/orders/') }"
              @click.prevent="goOrdersNav"
            >
              <span class="icon">▣</span>
              <span class="orders-nav-text">
                交付与订单
                <span class="orders-nav-hint">{{ ordersNavHint }}</span>
              </span>
            </a>
            <router-link :to="workbenchLink" class="workbench-nav">
              <span class="icon">◈</span>
              <span class="workbench-nav-text">
                {{ workbenchNavLabel }}
                <span class="workbench-nav-hint">页内切换产线</span>
              </span>
            </router-link>
            <div v-if="isSupervisor && currentLine" class="line-scope-panel">
              <div class="line-scope-label">线长 · 默认产线</div>
              <div class="line-scope-value mono">{{ lineShortId(currentLine.product_line_id) }}</div>
              <div class="line-scope-name">{{ currentLine.name }}</div>
              <div class="line-scope-hint">可切换查看全厂任意产线</div>
            </div>
          </nav>
        </div>

        <div class="nav-group">
          <div class="nav-group-label">批次</div>
          <nav>
            <router-link to="/batches"><span class="icon">◎</span> 批次与物料</router-link>
          </nav>
        </div>

        <div class="nav-group">
          <div class="nav-group-label">分析</div>
          <nav>
            <router-link to="/trends"><span class="icon">∿</span> 趋势分析</router-link>
            <router-link to="/alarms"><span class="icon">⚠</span> 报警中心</router-link>
          </nav>
        </div>

        <div class="nav-group">
          <div class="nav-group-label">智优</div>
          <nav>
            <router-link to="/optimization"><span class="icon">◆</span> 决策总览</router-link>
            <router-link to="/optimization/efficiency"><span class="icon">▲</span> 极致效率</router-link>
            <router-link to="/optimization/cost"><span class="icon">▼</span> 极致成本</router-link>
            <router-link to="/optimization/energy-carbon"><span class="icon">◎</span> 能碳决策</router-link>
            <router-link to="/optimization/quality"><span class="icon">●</span> 极致质量</router-link>
            <router-link to="/optimization/balance"><span class="icon">△</span> 平衡点分析</router-link>
            <router-link to="/optimization/scenarios"><span class="icon">⬡</span> 场景实验室</router-link>
          </nav>
        </div>
      </div>

      <div class="sidebar-footer">
        <label class="role-switch">
          <span>视角</span>
          <select :value="role" @change="onRoleChange">
            <option :value="USER_ROLES.director.id">{{ USER_ROLES.director.label }}</option>
            <option :value="USER_ROLES.supervisor.id">{{ USER_ROLES.supervisor.label }}</option>
          </select>
        </label>
        <div class="footer-hint">启动清库 · 窗口 4h</div>
      </div>
    </aside>

    <div class="main">
      <GlobalContextBar />
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.orders-nav-link { align-items: flex-start; cursor: pointer; }
.orders-nav-text,
.workbench-nav-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.orders-nav-hint,
.workbench-nav-hint {
  font-size: 10px;
  color: var(--text-dim);
  font-weight: 400;
}
.orders-nav-link.router-link-active .orders-nav-hint {
  color: rgba(34, 211, 238, 0.65);
}
.workbench-nav { align-items: flex-start; }
.line-scope-panel {
  margin: 4px 12px 8px 28px;
  padding: 8px 10px;
  border-radius: 4px;
  border: 1px solid var(--border-dim);
  background: rgba(34, 211, 238, 0.04);
}
.line-scope-label {
  font-size: 9px;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 4px;
}
.line-scope-value {
  font-size: 12px;
  color: var(--accent-cyan);
}
.line-scope-name {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.line-scope-hint {
  font-size: 10px;
  color: var(--text-dim);
  margin-top: 6px;
  line-height: 1.35;
}
.role-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: var(--text-dim);
  margin-bottom: 6px;
}
.role-switch select {
  flex: 1;
  background: var(--bg-root);
  border: 1px solid var(--border-dim);
  color: var(--text-primary);
  border-radius: 3px;
  padding: 3px 6px;
  font-size: 11px;
}
.footer-hint { font-size: 10px; color: var(--text-dim); opacity: 0.8; }
</style>
