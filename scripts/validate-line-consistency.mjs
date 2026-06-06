#!/usr/bin/env node
/**
 * 产线数据契约校验：主数据 process_steps / equipment / twin_layouts / datapoints / telemetry 一致性
 * 用法: node scripts/validate-line-consistency.mjs
 */
import { readFileSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const MASTER_PATH = join(__dirname, '../schemas/智造数据台/presets/jqhc-factory-master-data.json')

const md = JSON.parse(readFileSync(MASTER_PATH, 'utf8'))
const errors = []
const warnings = []

function err(lineId, msg) {
  errors.push({ lineId: lineId || '—', msg })
}
function warn(lineId, msg) {
  warnings.push({ lineId: lineId || '—', msg })
}

const lines = md.product_lines || []
const equipment = md.equipment || []
const dataPoints = md.data_points || []
const twinLayouts = md.twin_layouts || {}
const telemetry = md.equipment_telemetry_profiles || {}

for (const line of lines) {
  const lineId = line.product_line_id
  const processSteps = line.process_steps || []
  const layout = twinLayouts[lineId]
  const lineEquip = equipment.filter(e => e.product_line_id === lineId)
  const isFull = line.detail_level === 'full' || line.simulation_enabled === true

  if (!layout) {
    err(lineId, '缺少 twin_layouts 配置')
    continue
  }

  const flowPath = layout.flow_path || layout.flowPath || []
  const steps = layout.steps || {}

  // process_steps ↔ flow_path
  const stepsSet = new Set(processSteps)
  const flowSet = new Set(flowPath)
  for (const s of processSteps) {
    if (!flowSet.has(s)) err(lineId, `process_steps 含 ${s} 但 flow_path 缺失`)
  }
  for (const s of flowPath) {
    if (!stepsSet.has(s)) err(lineId, `flow_path 含 ${s} 但 process_steps 缺失`)
    if (!steps[s]) err(lineId, `twin_layouts.steps 缺少节点 ${s}`)
    if (!steps[s]?.node?.label) err(lineId, `工序 ${s} 缺少 node.label`)
  }

  // twin_3d_ready 一致性
  const line3d = line.twin_3d_ready
  const layout3d = layout.twin_3d_ready
  if (line3d !== undefined && layout3d !== undefined && line3d !== layout3d) {
    err(lineId, `twin_3d_ready 不一致: product_lines=${line3d} twin_layouts=${layout3d}`)
  }
  if (!line3d || !layout3d) {
    if (isFull) warn(lineId, '精细产线 twin_3d_ready 未启用')
  }

  if (!isFull) {
    // 登记产线：仅校验孪生拓扑与工序链一致
    continue
  }

  // 精细产线：设备 / 数据点 / 遥测 / 3D 全量校验
  for (const stepId of flowPath) {
    const stepCfg = steps[stepId]
    if (!stepCfg) continue
    const twinEquipId = stepCfg.equipment_id
    const eq = lineEquip.find(e => e.process_step_id === stepId)
    if (!eq) {
      err(lineId, `工序 ${stepId} 无 equipment 记录`)
    } else if (twinEquipId && eq.equipment_id !== twinEquipId) {
      err(lineId, `工序 ${stepId} equipment 不一致: twin=${twinEquipId} meta=${eq.equipment_id}`)
    }

    const keyField = stepCfg.key_field_id || stepCfg.keyFieldId
    if (keyField && eq) {
      const dp = dataPoints.find(d =>
        d.equipment_id === eq.equipment_id
        && (d.field_id === keyField || d.data_point_id === keyField)
      )
      if (!dp) {
        warn(lineId, `工序 ${stepId} key_field ${keyField} 在 data_points 中未找到`)
      }
    }

    const tele = telemetry[eq?.equipment_id]
    if (eq && !tele) warn(lineId, `设备 ${eq.equipment_id} 缺少 telemetry_profile`)
    if (tele && tele.process_step_id !== stepId) {
      err(lineId, `telemetry ${eq.equipment_id} process_step_id=${tele.process_step_id} ≠ ${stepId}`)
    }

    const pos = stepCfg?.position_3d || stepCfg?.position3d
    if (!pos?.type) err(lineId, `工序 ${stepId} 缺少 position_3d.type`)
    if (!pos?.scale?.length) err(lineId, `工序 ${stepId} 缺少 position_3d.scale`)
  }

  for (const eq of lineEquip) {
    if (!processSteps.includes(eq.process_step_id)) {
      err(lineId, `设备 ${eq.equipment_id} 的 process_step_id=${eq.process_step_id} 不在 process_steps`)
    }
  }
}

// 汇总
console.log('\n=== 产线数据契约校验 ===')
console.log(`产线数: ${lines.length}`)
console.log(`错误: ${errors.length}  警告: ${warnings.length}\n`)

if (errors.length) {
  console.log('--- 错误 ---')
  for (const e of errors) console.log(`  [${e.lineId}] ${e.msg}`)
}
if (warnings.length) {
  console.log('--- 警告 ---')
  for (const w of warnings) console.log(`  [${w.lineId}] ${w.msg}`)
}
if (!errors.length && !warnings.length) {
  console.log('✓ 全部校验通过')
}

process.exit(errors.length ? 1 : 0)
