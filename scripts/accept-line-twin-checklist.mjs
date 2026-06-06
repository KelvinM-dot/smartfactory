#!/usr/bin/env node
/**
 * 1.txt 对齐：42 条产线 2D/3D 孪生与数据点逐条验收
 *
 * 用法:
 *   node scripts/accept-line-twin-checklist.mjs
 *   node scripts/accept-line-twin-checklist.mjs --json-only
 *   node scripts/accept-line-twin-checklist.mjs --line FCW-LINE-01
 *
 * 输出:
 *   docs/line-twin-acceptance-checklist.md  — 验收清单（含逐条结果）
 *   reports/line-twin-acceptance-report.json — 机器可读报告
 */

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const MASTER_PATH = join(ROOT, 'schemas/智造数据台/presets/jqhc-factory-master-data.json')
const FALLBACK_PATH = join(ROOT, 'dataplatform-web/src/config/twinFallback.generated.js')
const REPORT_JSON = join(ROOT, 'reports/line-twin-acceptance-report.json')
const REPORT_MD = join(ROOT, 'docs/line-twin-acceptance-checklist.md')

const SKIP_DP_FIELDS = new Set(['status', 'location', 'recipe_id', 'run_mode', 'alarm_code', 'finished_batch_id'])

const CATEGORY_LABEL = {
  flux_core_wire: '药芯焊丝',
  solid_wire: '实心焊丝',
  submerged_arc_wire: '埋弧焊丝',
  welding_rod: '焊条'
}

/** 验收项定义（对照 1.txt 全链路） */
const CHECK_DEFS = [
  { id: 'A01', cat: '拓扑', name: 'process_steps ↔ flow_path 双向一致', dim: '2D' },
  { id: 'A02', cat: '拓扑', name: 'twin_layouts.steps 覆盖全部工序节点', dim: '2D' },
  { id: 'A03', cat: '2D模型', name: '每工序 node 含 x/y/label', dim: '2D' },
  { id: 'A04', cat: '设备', name: 'twin.equipment_id ↔ master equipment 一致', dim: '2D' },
  { id: 'A05', cat: '设备', name: '每工序有且仅有 1 台 enabled 设备', dim: '数据' },
  { id: 'B01', cat: '3D模型', name: '每工序 position_3d 含 x/y/z/scale/type', dim: '3D' },
  { id: 'B02', cat: '3D模型', name: 'twin_3d_ready 标记一致（产线/布局/前端）', dim: '3D' },
  { id: 'B03', cat: '3D模型', name: 'flowPath 每步在 positions3d 有点位', dim: '3D' },
  { id: 'C01', cat: '数据点', name: '每设备至少有 1 个 data_point', dim: '数据' },
  { id: 'C02', cat: '数据点', name: 'key_field_id 在 data_points 可解析', dim: '数据' },
  { id: 'C03', cat: '数据点', name: 'key_field 在 telemetry profile 可模拟', dim: '数据' },
  { id: 'C04', cat: '数据点', name: 'data_point.product_line_id 与设备产线一致', dim: '数据' },
  { id: 'C05', cat: '数据点', name: 'profile 工序与设备 process_step_id 一致', dim: '数据' },
  { id: 'D01', cat: '生产链', name: '产线有归属 recipe', dim: '生产' },
  { id: 'D02', cat: '生产链', name: '产线有 assigned production_order', dim: '生产' },
  { id: 'D03', cat: '生产链', name: '运行产线有 in_progress 批次', dim: '生产' },
  { id: 'D04', cat: '生产链', name: '批次 recipe_id / order product_id 引用有效', dim: '生产' },
  { id: 'E01', cat: '前端', name: 'twinFallback.generated 含该产线布局', dim: '前端' },
  { id: 'E02', cat: '前端', name: '前端 flowPath 与 master 一致', dim: '前端' },
  { id: 'E03', cat: '前端', name: '前端 keyFields 与 master 一致', dim: '前端' },
  { id: 'E04', cat: '前端', name: '前端 positions3d 步骤覆盖 flowPath', dim: '前端' }
]

function parseArgs() {
  const args = process.argv.slice(2)
  let lineFilter = null
  let jsonOnly = false
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--line') lineFilter = args[++i]
    if (args[i] === '--json-only') jsonOnly = true
  }
  return { lineFilter, jsonOnly }
}

function loadFallbackLayouts() {
  const raw = readFileSync(FALLBACK_PATH, 'utf8')
  const m = raw.match(/export const FALLBACK_TWIN_LAYOUTS = (\{[\s\S]*?\})\s*\n\nexport const FALLBACK_TWIN_3D_READY/)
  if (!m) throw new Error('无法解析 twinFallback.generated.js')
  return JSON.parse(m[1])
}

function toFrontendLayout(lineRaw, fieldLabels) {
  const out = {}
  out.viewBox = lineRaw.view_box ?? lineRaw.viewBox
  out.flowPath = lineRaw.flow_path ?? lineRaw.flowPath
  const nodes = {}
  const keyFields = {}
  const positions3d = {}
  if (lineRaw.steps && typeof lineRaw.steps === 'object') {
    for (const [stepId, step] of Object.entries(lineRaw.steps)) {
      if (!step || typeof step !== 'object') continue
      if (step.node) nodes[stepId] = step.node
      const keyField = step.key_field_id ?? step.keyFieldId
      if (keyField != null) keyFields[stepId] = keyField
      const pos3d = step.position_3d ?? step.position3d
      if (pos3d) {
        const p = { ...pos3d }
        if (p.x == null && pos3d.position) {
          p.x = pos3d.position.x
          p.y = pos3d.position.y
          p.z = pos3d.position.z
        }
        positions3d[stepId] = p
      }
    }
  }
  out.nodes = nodes
  out.keyFields = keyFields
  out.positions3d = positions3d
  out.twin3dReady = lineRaw.twin_3d_ready ?? lineRaw.twin3dReady
  out.fieldLabels = lineRaw.field_labels ?? fieldLabels
  return out
}

function resolveFieldId(dp) {
  return dp.field_id || dp.data_point_id
}

function record(checks, id, status, detail = '') {
  checks[id] = { status, detail }
}

function acceptLine(line, ctx) {
  const {
    lineId, layout, lineEquip, dpByEq, profiles, recipes, orders, batches, products,
    fallbackLayout, expectedFrontend
  } = ctx
  const checks = {}
  const issues = []

  const processSteps = line.process_steps || []
  const flowPath = layout?.flow_path || layout?.flowPath || []
  const steps = layout?.steps || {}
  const isFull = line.detail_level === 'full' || line.simulation_enabled === true
  const isActive = line.status === 'active'

  if (!layout) {
    for (const def of CHECK_DEFS) record(checks, def.id, 'FAIL', '缺少 twin_layouts')
    return { checks, issues, score: 0, pass: 0, fail: CHECK_DEFS.length, warn: 0, skip: 0 }
  }

  // A01
  {
    const stepsSet = new Set(processSteps)
    const flowSet = new Set(flowPath)
    const missingInFlow = processSteps.filter(s => !flowSet.has(s))
    const missingInSteps = flowPath.filter(s => !stepsSet.has(s))
    if (!missingInFlow.length && !missingInSteps.length) {
      record(checks, 'A01', 'PASS')
    } else {
      const detail = [...missingInFlow.map(s => `steps缺flow:${s}`), ...missingInSteps.map(s => `flow缺steps:${s}`)].join('; ')
      record(checks, 'A01', 'FAIL', detail)
      issues.push(detail)
    }
  }

  // A02
  {
    const missing = flowPath.filter(s => !steps[s])
    record(checks, 'A02', missing.length ? 'FAIL' : 'PASS', missing.length ? `缺节点: ${missing.join(',')}` : '')
    if (missing.length) issues.push(`A02: ${missing.join(',')}`)
  }

  // A03
  if (!isFull) {
    record(checks, 'A03', 'SKIP', '非 full 产线')
  } else {
    const bad = flowPath.filter(s => {
      const n = steps[s]?.node
      return !n || n.x == null || n.y == null || !n.label
    })
    record(checks, 'A03', bad.length ? 'FAIL' : 'PASS', bad.length ? bad.join(',') : '')
    if (bad.length) issues.push(`A03: ${bad.join(',')}`)
  }

  // A04 + A05
  if (!isFull) {
    record(checks, 'A04', 'SKIP', '非 full 产线')
    record(checks, 'A05', 'SKIP', '非 full 产线')
  } else {
    const a4bad = []
    const a5bad = []
    for (const stepId of flowPath) {
      const twinEquipId = steps[stepId]?.equipment_id
      const eqList = lineEquip.filter(e => e.process_step_id === stepId && e.enabled !== false)
      const eq = eqList[0]
      if (!eq) a5bad.push(stepId)
      else if (twinEquipId && eq.equipment_id !== twinEquipId) {
        a4bad.push(`${stepId}: twin=${twinEquipId} meta=${eq.equipment_id}`)
      }
      if (eqList.length !== 1) a5bad.push(`${stepId}×${eqList.length}`)
    }
    record(checks, 'A04', a4bad.length ? 'FAIL' : 'PASS', a4bad.join('; '))
    record(checks, 'A05', a5bad.length ? 'FAIL' : 'PASS', a5bad.join('; '))
    if (a4bad.length) issues.push(...a4bad)
    if (a5bad.length) issues.push(`A05: ${a5bad.join('; ')}`)
  }

  // B01
  if (!isFull) {
    record(checks, 'B01', 'SKIP', '非 full 产线')
  } else {
    const bad = flowPath.filter(s => {
      const p = steps[s]?.position_3d || steps[s]?.position3d
      return !p || p.x == null || p.y == null || p.z == null || !p.scale?.length || !p.type
    })
    record(checks, 'B01', bad.length ? 'FAIL' : 'PASS', bad.length ? bad.join(',') : '')
    if (bad.length) issues.push(`B01: ${bad.join(',')}`)
  }

  // B02
  {
    const line3d = line.twin_3d_ready === true
    const layout3d = layout.twin_3d_ready === true
    const fb3d = ctx.fallback3dReady[lineId] === true
    const ok = line3d && layout3d && fb3d
    record(checks, 'B02', ok ? 'PASS' : 'FAIL',
      ok ? '' : `line=${line3d} layout=${layout3d} fallback=${fb3d}`)
    if (!ok) issues.push('B02: 3D ready 不一致')
  }

  // B03 (master positions3d via expected frontend transform)
  if (!isFull) {
    record(checks, 'B03', 'SKIP', '非 full 产线')
  } else {
    const pos = expectedFrontend.positions3d || {}
    const missing = flowPath.filter(s => !pos[s])
    record(checks, 'B03', missing.length ? 'FAIL' : 'PASS', missing.length ? missing.join(',') : '')
    if (missing.length) issues.push(`B03: ${missing.join(',')}`)
  }

  // C01-C05
  if (!isFull) {
    for (const id of ['C01', 'C02', 'C03', 'C04', 'C05']) record(checks, id, 'SKIP', '非 full 产线')
  } else {
    const c1 = []
    const c2 = []
    const c3 = []
    const c4 = []
    const c5 = []
    for (const stepId of flowPath) {
      const eq = lineEquip.find(e => e.process_step_id === stepId && e.enabled !== false)
      if (!eq) continue
      const eid = eq.equipment_id
      const dps = dpByEq[eid] || []
      if (!dps.length) c1.push(eid)

      const keyField = steps[stepId]?.key_field_id || steps[stepId]?.keyFieldId
      if (keyField) {
        const dpHit = dps.some(d => resolveFieldId(d) === keyField || d.data_point_id === `${eid}_${keyField}`)
        if (!dpHit) c2.push(`${stepId}:${keyField}`)
        const prof = profiles[eid]
        const profFields = prof?.fields || {}
        if (!(keyField in profFields) && !SKIP_DP_FIELDS.has(keyField)) c3.push(`${eid}:${keyField}`)
      }

      for (const dp of dps) {
        if (dp.product_line_id && dp.product_line_id !== lineId) c4.push(dp.data_point_id)
      }

      const prof = profiles[eid]
      if (prof && prof.process_step_id && prof.process_step_id !== stepId) {
        c5.push(`${eid}:${prof.process_step_id}≠${stepId}`)
      }
      if (!prof) c5.push(`${eid}:无profile`)
    }
    record(checks, 'C01', c1.length ? 'FAIL' : 'PASS', c1.join(', '))
    record(checks, 'C02', c2.length ? 'FAIL' : 'PASS', c2.join(', '))
    record(checks, 'C03', c3.length ? 'WARN' : 'PASS', c3.join(', '))
    record(checks, 'C04', c4.length ? 'FAIL' : 'PASS', c4.join(', '))
    record(checks, 'C05', c5.length ? 'FAIL' : 'PASS', c5.join(', '))
    if (c1.length) issues.push(`C01: ${c1.join(',')}`)
    if (c2.length) issues.push(`C02: ${c2.join(',')}`)
    if (c4.length) issues.push(`C04: ${c4.join(',')}`)
    if (c5.length) issues.push(`C05: ${c5.join(',')}`)
  }

  // D01-D04
  const lineRecipes = recipes.filter(r => r.product_line_id === lineId)
  const lineOrders = orders.filter(o => (o.assigned_line_ids || []).includes(lineId))
  const lineBatches = batches.filter(b => b.product_line_id === lineId)
  const inProgress = lineBatches.filter(b => b.status === 'in_progress')

  record(checks, 'D01', lineRecipes.length ? 'PASS' : 'FAIL', lineRecipes.length ? `${lineRecipes.length} recipe` : '无 recipe')
  record(checks, 'D02', lineOrders.length ? 'PASS' : 'FAIL', lineOrders.length ? lineOrders[0].production_order_id : '无 order')

  if (!isActive) {
    record(checks, 'D03', 'SKIP', `产线 status=${line.status}`)
  } else {
    record(checks, 'D03', inProgress.length ? 'PASS' : 'WARN', inProgress.length ? inProgress[0].batch_id : '无 in_progress 批次')
    if (!inProgress.length) issues.push('D03: 无进行中批次')
  }

  {
    const bad = []
    for (const b of lineBatches) {
      if (b.recipe_id && !recipes.some(r => r.recipe_id === b.recipe_id)) bad.push(`batch ${b.batch_id} recipe`)
      const ord = orders.find(o => o.production_order_id === b.production_order_id)
      if (ord?.product_id && !products.has(ord.product_id)) bad.push(`order product ${ord.product_id}`)
    }
    for (const o of lineOrders) {
      if (o.product_id && !products.has(o.product_id)) bad.push(`product ${o.product_id}`)
      if (o.recipe_id && !recipes.some(r => r.recipe_id === o.recipe_id)) bad.push(`order recipe ${o.recipe_id}`)
    }
    record(checks, 'D04', bad.length ? 'FAIL' : 'PASS', bad.join('; '))
    if (bad.length) issues.push(`D04: ${bad.join('; ')}`)
  }

  // E01-E04
  if (!fallbackLayout) {
    for (const id of ['E01', 'E02', 'E03', 'E04']) record(checks, id, 'FAIL', 'fallback 缺失')
    issues.push('E01: fallback 缺失')
  } else {
    record(checks, 'E01', 'PASS')
    const expFp = expectedFrontend.flowPath || []
    const fbFp = fallbackLayout.flowPath || []
    const e2 = expFp.length === fbFp.length && expFp.every((s, i) => s === fbFp[i])
    record(checks, 'E02', e2 ? 'PASS' : 'FAIL', e2 ? '' : `master=${expFp.join('>')} fb=${fbFp.join('>')}`)
    if (!e2) issues.push('E02: flowPath 不一致')

    const expKf = expectedFrontend.keyFields || {}
    const fbKf = fallbackLayout.keyFields || {}
    const kfKeys = Object.keys(expKf)
    const e3 = kfKeys.every(k => expKf[k] === fbKf[k]) && Object.keys(fbKf).length === kfKeys.length
    record(checks, 'E03', e3 ? 'PASS' : 'FAIL', e3 ? '' : 'keyFields 不一致')
    if (!e3) issues.push('E03: keyFields 不一致')

    const fbPos = fallbackLayout.positions3d || {}
    const missingPos = expFp.filter(s => !fbPos[s])
    record(checks, 'E04', missingPos.length ? 'FAIL' : 'PASS', missingPos.join(', '))
    if (missingPos.length) issues.push(`E04: ${missingPos.join(',')}`)
  }

  let pass = 0; let fail = 0; let warn = 0; let skip = 0
  for (const def of CHECK_DEFS) {
    const st = checks[def.id]?.status || 'FAIL'
    if (st === 'PASS') pass++
    else if (st === 'FAIL') fail++
    else if (st === 'WARN') warn++
    else skip++
  }
  const scored = CHECK_DEFS.filter(d => checks[d.id]?.status !== 'SKIP').length
  const score = scored ? Math.round((pass / scored) * 100) : 0
  const overall = fail ? 'FAIL' : (warn ? 'WARN' : 'PASS')

  return { checks, issues, score, pass, fail, warn, skip, overall }
}

function loadFallback3dReady(rawJs) {
  const m = rawJs.match(/export const FALLBACK_TWIN_3D_READY = (\{[\s\S]*?\})\s*$/)
  return m ? JSON.parse(m[1]) : {}
}

function buildMarkdown(report) {
  const lines = []
  lines.push('# 产线 2D/3D 与数据点一致性验收清单')
  lines.push('')
  lines.push('> 依据 `1.txt`：以产线为单位，模型数据（2D/3D）、生产数据、产品数据全链路完备且严格一致。')
  lines.push('')
  lines.push(`**生成时间**：${report.generated_at}`)
  lines.push(`**数据源**：\`jqhc-factory-master-data.json\` + \`twinFallback.generated.js\``)
  lines.push('')
  lines.push('## 验收摘要')
  lines.push('')
  lines.push(`| 指标 | 值 |`)
  lines.push(`|------|-----|`)
  lines.push(`| 产线总数 | ${report.summary.total_lines} |`)
  lines.push(`| 全部通过 (PASS) | ${report.summary.lines_pass} |`)
  lines.push(`| 有警告 (WARN) | ${report.summary.lines_warn} |`)
  lines.push(`| 未通过 (FAIL) | ${report.summary.lines_fail} |`)
  lines.push(`| 验收项 | ${CHECK_DEFS.length} 项 × ${report.summary.total_lines} 线 |`)
  lines.push('')
  lines.push('## 验收项定义（21 项）')
  lines.push('')
  lines.push('| ID | 类别 | 维度 | 验收标准 |')
  lines.push('|----|------|------|----------|')
  for (const d of CHECK_DEFS) {
    lines.push(`| ${d.id} | ${d.cat} | ${d.dim} | ${d.name} |`)
  }
  lines.push('')
  lines.push('## 逐条产线验收结果')
  lines.push('')
  lines.push('| 产线 | 品类 | 状态 | 得分 | 结论 | 问题摘要 |')
  lines.push('|------|------|------|------|------|----------|')
  for (const row of report.lines) {
    const issueBrief = row.issues.length ? row.issues.slice(0, 2).join('；') : '—'
    lines.push(`| ${row.line_id} | ${row.category_label} | ${row.line_status} | ${row.score}% | **${row.overall}** | ${issueBrief} |`)
  }
  lines.push('')
  lines.push('## 逐条明细矩阵')
  lines.push('')
  const header = ['产线', ...CHECK_DEFS.map(d => d.id)].join(' | ')
  lines.push(`| ${header} |`)
  lines.push(`|${['------', ...CHECK_DEFS.map(() => '----')].join('|')}|`)
  for (const row of report.lines) {
    const cells = [row.line_id, ...CHECK_DEFS.map(d => {
      const st = row.checks[d.id]?.status || '—'
      return st === 'PASS' ? '✓' : st === 'WARN' ? '△' : st === 'SKIP' ? '—' : '✗'
    })]
    lines.push(`| ${cells.join(' | ')} |`)
  }
  lines.push('')
  lines.push('## 未通过 / 警告明细')
  lines.push('')
  const problemLines = report.lines.filter(l => l.overall !== 'PASS')
  if (!problemLines.length) {
    lines.push('无。全部 42 条产线验收通过。')
  } else {
    for (const row of problemLines) {
      lines.push(`### ${row.line_id} — ${row.name} (${row.overall})`)
      lines.push('')
      for (const def of CHECK_DEFS) {
        const c = row.checks[def.id]
        if (!c || c.status === 'PASS' || c.status === 'SKIP') continue
        lines.push(`- **${def.id}** ${def.name}：${c.status}${c.detail ? ` — ${c.detail}` : ''}`)
      }
      lines.push('')
    }
  }
  lines.push('## 前端手工抽检建议（每条产线）')
  lines.push('')
  lines.push('自动化通过后，建议在 UI 中逐条确认：')
  lines.push('')
  lines.push('1. 打开 `/lines/{lineId}?tab=twin`，2D 流程图工序数与 `flowPath` 一致')
  lines.push('2. 切换 3D 视图，设备占位与 `position_3d.type` 一致（无缺失/重叠异常）')
  lines.push('3. 点击各工序，右侧参数面板 key_field 有实时数值跳动')
  lines.push('4. 总览页 `process_pipeline` 设备名与孪生节点 label 一致')
  lines.push('5. 批次/订单/配方与驾驶舱卡片信息一致')
  lines.push('')
  lines.push('```bash')
  lines.push('# 重新生成报告')
  lines.push('node scripts/accept-line-twin-checklist.mjs')
  lines.push('node scripts/validate-line-consistency.mjs')
  lines.push('```')
  lines.push('')
  return lines.join('\n')
}

function main() {
  const { lineFilter, jsonOnly } = parseArgs()
  const md = JSON.parse(readFileSync(MASTER_PATH, 'utf8'))
  const fallbackRaw = readFileSync(FALLBACK_PATH, 'utf8')
  const fallbackLayouts = loadFallbackLayouts()
  const fallback3dReady = loadFallback3dReady(fallbackRaw)
  const fieldLabels = md.twin_field_labels || {}

  const equipment = md.equipment || []
  const dataPoints = md.data_points || []
  const profiles = md.equipment_telemetry_profiles || {}
  const recipes = md.recipes || []
  const orders = md.production_orders || []
  const batches = md.product_batches || []
  const products = new Set((md.products || []).map(p => p.product_id))
  const twinLayouts = md.twin_layouts || {}

  const dpByEq = {}
  for (const dp of dataPoints) {
    const eid = dp.equipment_id
    if (!dpByEq[eid]) dpByEq[eid] = []
    dpByEq[eid].push(dp)
  }

  let lines = (md.product_lines || []).slice().sort((a, b) =>
    a.product_line_id.localeCompare(b.product_line_id)
  )
  if (lineFilter) lines = lines.filter(l => l.product_line_id === lineFilter)

  const lineReports = []
  for (const line of lines) {
    const lineId = line.product_line_id
    const layout = twinLayouts[lineId]
    const lineEquip = equipment.filter(e => e.product_line_id === lineId)
    const expectedFrontend = layout ? toFrontendLayout(layout, fieldLabels) : null
    const result = acceptLine(line, {
      lineId,
      layout,
      lineEquip,
      dpByEq,
      profiles,
      recipes,
      orders,
      batches,
      products,
      fallbackLayout: fallbackLayouts[lineId],
      fallback3dReady,
      expectedFrontend: expectedFrontend || {}
    })
    lineReports.push({
      line_id: lineId,
      name: line.name,
      product_category: line.product_category,
      category_label: CATEGORY_LABEL[line.product_category] || line.product_category,
      workshop_id: line.workshop_id,
      line_status: line.status,
      simulation_enabled: line.simulation_enabled,
      twin_3d_ready: line.twin_3d_ready,
      equipment_count: lineEquip.length,
      data_point_count: lineEquip.reduce((n, e) => n + (dpByEq[e.equipment_id]?.length || 0), 0),
      process_step_count: (line.process_steps || []).length,
      ...result
    })
  }

  const report = {
    generated_at: new Date().toISOString(),
    source: {
      master_data: MASTER_PATH.replace(ROOT + '/', ''),
      twin_fallback: FALLBACK_PATH.replace(ROOT + '/', '')
    },
    checklist_version: '1.0',
    requirement: '1.txt — 产线数据与 2D/3D 模型严格一致，全链路完备正确',
    check_definitions: CHECK_DEFS,
    summary: {
      total_lines: lineReports.length,
      lines_pass: lineReports.filter(l => l.overall === 'PASS').length,
      lines_warn: lineReports.filter(l => l.overall === 'WARN').length,
      lines_fail: lineReports.filter(l => l.overall === 'FAIL').length,
      avg_score: Math.round(lineReports.reduce((s, l) => s + l.score, 0) / lineReports.length)
    },
    lines: lineReports
  }

  mkdirSync(dirname(REPORT_JSON), { recursive: true })
  mkdirSync(dirname(REPORT_MD), { recursive: true })
  writeFileSync(REPORT_JSON, JSON.stringify(report, null, 2) + '\n', 'utf8')
  if (!jsonOnly) {
    writeFileSync(REPORT_MD, buildMarkdown(report) + '\n', 'utf8')
  }

  console.log('\n=== 1.txt 产线 2D/3D 数据点逐条验收 ===\n')
  console.log(`产线: ${report.summary.total_lines}  PASS: ${report.summary.lines_pass}  WARN: ${report.summary.lines_warn}  FAIL: ${report.summary.lines_fail}`)
  console.log(`平均得分: ${report.summary.avg_score}%\n`)
  console.log('产线 ID          | 得分 | 结论 | 设备 | 数据点 | 工序')
  console.log('-----------------|------|------|------|--------|------')
  for (const row of lineReports) {
    console.log(
      `${row.line_id.padEnd(16)} | ${String(row.score).padStart(3)}% | ${row.overall.padEnd(4)} | ${String(row.equipment_count).padStart(4)} | ${String(row.data_point_count).padStart(6)} | ${row.process_step_count}`
    )
  }
  const fails = lineReports.filter(l => l.overall === 'FAIL')
  if (fails.length) {
    console.log('\n--- 未通过明细 ---')
    for (const row of fails) {
      console.log(`  ${row.line_id}: ${row.issues.join('; ')}`)
    }
  }
  const warns = lineReports.filter(l => l.overall === 'WARN')
  if (warns.length) {
    console.log('\n--- 警告 ---')
    for (const row of warns) {
      console.log(`  ${row.line_id}: ${row.issues.join('; ')}`)
    }
  }
  console.log(`\n报告: ${REPORT_MD.replace(ROOT + '/', '')}`)
  console.log(`JSON: ${REPORT_JSON.replace(ROOT + '/', '')}\n`)

  process.exit(report.summary.lines_fail ? 1 : 0)
}

main()
