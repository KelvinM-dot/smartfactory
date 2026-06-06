#!/usr/bin/env node
/**
 * 智优模块 42 线对齐验收
 * 用法: node scripts/accept-opt-checklist.mjs [--api http://127.0.0.1:3001]
 */

const API = (process.argv.find((a, i) => process.argv[i - 1] === '--api') || 'http://127.0.0.1:3001').replace(/\/$/, '')

const CHECKS = [
  { id: 'O01', name: 'GET /v1/opt/overview 返回 42 线布局', path: '/v1/opt/overview' },
  { id: 'O02', name: 'GET /v1/opt/lines 返回 42 条快照', path: '/v1/opt/lines' },
  { id: 'O03', name: 'GET /v1/opt/efficiency m01 全线 breakdown', path: '/v1/opt/efficiency' },
  { id: 'O04', name: 'GET /v1/opt/cost m08 全线 utilization', path: '/v1/opt/cost' },
  { id: 'O05', name: 'GET /v1/opt/quality m11 全线 mix', path: '/v1/opt/quality' },
  { id: 'O06', name: 'GET /v1/opt/workshops 双车间明细', path: '/v1/opt/workshops' }
]

async function fetchJson(path) {
  const res = await fetch(`${API}${path}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

function countLines(data, key) {
  const models = data?.models || {}
  for (const m of Object.values(models)) {
    if (m?.[key]?.length) return m[key].length
  }
  return 0
}

async function main() {
  console.log('\n=== 智优模块 42 线验收 ===\n')
  let pass = 0
  let fail = 0
  const details = []

  for (const chk of CHECKS) {
    try {
      const data = await fetchJson(chk.path)
      let ok = false
      let note = ''

      if (chk.id === 'O01') {
        const layout = data.plant_layout || {}
        ok = layout.total_line_count === 42 && layout.telemetry_line_count === 42
        note = `total=${layout.total_line_count} telemetry=${layout.telemetry_line_count}`
      } else if (chk.id === 'O02') {
        ok = (data.line_count || data.lines?.length) === 42
        note = `lines=${data.lines?.length}`
      } else if (chk.id === 'O03') {
        const n = countLines(data, 'line_breakdown')
        ok = n >= 42
        note = `m01 breakdown=${n}`
      } else if (chk.id === 'O04') {
        const n = countLines(data, 'line_utilization')
        ok = n >= 42
        note = `m08 util=${n}`
      } else if (chk.id === 'O05') {
        const n = countLines(data, 'line_mix_analysis')
        ok = n >= 42
        note = `m11 mix=${n}`
      } else if (chk.id === 'O06') {
        const ws = data.workshops || []
        const total = ws.reduce((s, w) => s + (w.lines?.length || w.line_count || 0), 0)
        ok = ws.length >= 2 && total === 42
        note = `workshops=${ws.length} lines=${total}`
      }

      if (ok) { pass++; console.log(`  ✓ ${chk.id} ${chk.name} — ${note}`) }
      else { fail++; console.log(`  ✗ ${chk.id} ${chk.name} — ${note}`) }
      details.push({ ...chk, ok, note })
    } catch (e) {
      fail++
      console.log(`  ✗ ${chk.id} ${chk.name} — ${e.message}`)
      details.push({ ...chk, ok: false, note: e.message })
    }
  }

  console.log(`\n汇总: PASS ${pass} / FAIL ${fail}`)
  if (fail) {
    console.log('提示: 需先启动 dataplatform-api 并完成 reseed')
    process.exit(1)
  }
  console.log('智优 42 线 API 验收通过\n')
}

main()
