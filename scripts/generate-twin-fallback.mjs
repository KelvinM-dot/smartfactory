#!/usr/bin/env node
/**
 * 从 jqhc-factory-master-data.json 生成前端离线兜底 twin 布局。
 * 包含 twin_3d_ready === true 的全部仿真产线（当前 42 线）。
 *
 * 用法: node scripts/generate-twin-fallback.mjs
 */

import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const MASTER_PATH = join(ROOT, 'schemas/智造数据台/presets/jqhc-factory-master-data.json')
const OUT_PATH = join(ROOT, 'dataplatform-web/src/config/twinFallback.generated.js')

function normalizePosition3d(pos) {
  const out = { ...pos }
  if (out.x == null && pos.position && typeof pos.position === 'object') {
    out.x = pos.position.x
    out.y = pos.position.y
    out.z = pos.position.z
  }
  if (out.variant == null && pos.model_variant != null) {
    out.variant = pos.model_variant
  }
  return out
}

/** 与 TwinLayoutService.toFrontendLayout 对齐 */
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
      if (pos3d) positions3d[stepId] = normalizePosition3d(pos3d)
    }
  }

  if (!Object.keys(nodes).length && lineRaw.nodes) Object.assign(nodes, lineRaw.nodes)
  if (!Object.keys(keyFields).length) {
    if (lineRaw.key_fields) Object.assign(keyFields, lineRaw.key_fields)
    else if (lineRaw.keyFields) Object.assign(keyFields, lineRaw.keyFields)
  }
  if (!Object.keys(positions3d).length) {
    if (lineRaw.positions_3d) Object.assign(positions3d, lineRaw.positions_3d)
    else if (lineRaw.positions3d) Object.assign(positions3d, lineRaw.positions3d)
  }

  out.nodes = nodes
  out.keyFields = keyFields
  out.positions3d = positions3d

  const twin3dReady = lineRaw.twin_3d_ready ?? lineRaw.twin3dReady
  if (typeof twin3dReady === 'boolean') out.twin3dReady = twin3dReady

  out.fieldLabels = lineRaw.field_labels ?? fieldLabels
  return out
}

const master = JSON.parse(readFileSync(MASTER_PATH, 'utf8'))
const fieldLabels = master.twin_field_labels ?? {}
const twinLayouts = master.twin_layouts ?? {}

const fallback = {}
const twin3dReady = {}

for (const [lineId, raw] of Object.entries(twinLayouts)) {
  if (!raw || raw.twin_3d_ready !== true) continue
  fallback[lineId] = toFrontendLayout(raw, fieldLabels)
  twin3dReady[lineId] = true
}

const content = `/** AUTO-GENERATED — 请勿手改。运行: node scripts/generate-twin-fallback.mjs */
export const FALLBACK_TWIN_LAYOUTS = ${JSON.stringify(fallback, null, 2)}

export const FALLBACK_TWIN_3D_READY = ${JSON.stringify(twin3dReady, null, 2)}
`

writeFileSync(OUT_PATH, content, 'utf8')
console.log(`Wrote ${Object.keys(fallback).length} layouts -> ${OUT_PATH}`)
