<script setup>
import { ref, shallowRef, computed, watch, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import {
  twinLayoutsRef, getTwinLayout, statusColor
} from '../../config/twinLayout'
import { useTwinPipeline } from '../../composables/useTwinPipeline'
import { createMachineMesh } from './twinMachineMeshes'

const props = defineProps({
  lineId: { type: String, default: 'FCW-LINE-07' },
  pipeline: { type: Array, default: () => [] },
  equipment: { type: Array, default: () => [] },
  batch: { type: Object, default: null },
  selectedStep: { type: String, default: null }
})

const emit = defineEmits(['select-step'])

const containerRef = ref(null)
const layoutKeyFields = shallowRef({})
const layoutFieldLabels = shallowRef({})
let flowPath = []
let positions3d = {}
let nodes = {}

const twinPipeline = useTwinPipeline(
  computed(() => props.pipeline),
  computed(() => props.equipment),
  layoutKeyFields,
  layoutFieldLabels
)

function applyLayout(lineId) {
  const layout = getTwinLayout(lineId)
  if (!layout) {
    flowPath = []
    positions3d = {}
    nodes = {}
    layoutKeyFields.value = {}
    layoutFieldLabels.value = {}
    return
  }
  flowPath = layout.flowPath
  positions3d = layout.positions3d
  nodes = layout.nodes
  layoutKeyFields.value = layout.keyFields || {}
  layoutFieldLabels.value = layout.fieldLabels || {}
}

let scene, camera, renderer, labelRenderer, controls, animId
let resizeObserver = null
let stationMeshes = {}
let flowParticles = []
let conveyorLines = []

function equipByStep() {
  return twinPipeline.equipByStepFirst.value
}

function stepStatus(stepId) {
  return twinPipeline.stepStatus(stepId)
}

function liveMetricText(stepId) {
  return twinPipeline.liveMetricText(stepId)
}

function buildStationMesh(stepId, cfg) {
  return createMachineMesh(THREE, stepId, cfg)
}

function createLabel(stepId, cfg) {
  const div = document.createElement('div')
  div.className = 'twin3d-label'
  div.innerHTML = `
    <div class="lbl-title">${nodes[stepId].label}</div>
    <div class="lbl-eq">${equipByStep()[stepId]?.equipment_id || '—'}</div>
    <div class="lbl-metric" data-metric="${stepId}"></div>
  `
  div.addEventListener('click', (e) => {
    e.stopPropagation()
    emit('select-step', stepId)
  })
  const obj = new CSS2DObject(div)
  obj.position.set(0, cfg.scale[1] + 0.8, 0)
  return obj
}

function buildConveyors() {
  const pts = flowPath.map(id => positions3d[id]).filter(Boolean)
  const mat = new THREE.LineBasicMaterial({ color: 0x1e3a5f, transparent: true, opacity: 0.8 })
  const activeMat = new THREE.LineBasicMaterial({ color: 0x22d3ee, transparent: true, opacity: 0.9 })

  for (let i = 0; i < pts.length - 1; i++) {
    const a = pts[i]
    const b = pts[i + 1]
    const y = 0.35
    const points = []
    points.push(new THREE.Vector3(a.x, y, a.z))
    if (Math.abs(b.z - a.z) > 1) {
      points.push(new THREE.Vector3(a.x, y, (a.z + b.z) / 2))
      points.push(new THREE.Vector3(b.x, y, (a.z + b.z) / 2))
    }
    points.push(new THREE.Vector3(b.x, y, b.z))
    const geo = new THREE.BufferGeometry().setFromPoints(points)
    const line = new THREE.Line(geo, mat.clone())
    const activeLine = new THREE.Line(geo, activeMat.clone())
    activeLine.visible = false
    activeLine.userData.isActive = true
    scene.add(line)
    scene.add(activeLine)
    conveyorLines.push(line, activeLine)
  }
}

function fitCameraToLine() {
  const pts = flowPath.map(id => positions3d[id]).filter(Boolean)
  if (!pts.length || !camera || !controls) return
  let minX = Infinity
  let maxX = -Infinity
  let minZ = Infinity
  let maxZ = -Infinity
  for (const p of pts) {
    minX = Math.min(minX, p.x)
    maxX = Math.max(maxX, p.x)
    minZ = Math.min(minZ, p.z)
    maxZ = Math.max(maxZ, p.z)
  }
  const cx = (minX + maxX) / 2
  const cz = (minZ + maxZ) / 2
  const span = Math.max(maxX - minX, maxZ - minZ, 10)
  controls.target.set(cx, 0, cz)
  camera.position.set(cx, span * 0.95, cz + span * 0.9)
  camera.lookAt(cx, 0, cz)
  controls.update()
}

function buildFlowParticles() {
  const pts = flowPath.map(id => {
    const p = positions3d[id]
    return new THREE.Vector3(p.x, 0.5, p.z)
  })
  if (pts.length < 2) return

  const curvePoints = []
  for (let i = 0; i < pts.length - 1; i++) {
    const a = pts[i]
    const b = pts[i + 1]
    curvePoints.push(a.clone())
    if (Math.abs(b.z - a.z) > 1) {
      curvePoints.push(new THREE.Vector3(a.x, 0.5, (a.z + b.z) / 2))
      curvePoints.push(new THREE.Vector3(b.x, 0.5, (a.z + b.z) / 2))
    }
  }
  curvePoints.push(pts[pts.length - 1].clone())
  const curve = new THREE.CatmullRomCurve3(curvePoints)

  for (let i = 0; i < 8; i++) {
    const m = new THREE.Mesh(
      new THREE.SphereGeometry(0.12, 8, 8),
      new THREE.MeshBasicMaterial({ color: 0x22d3ee })
    )
    m.userData.t = i / 8
    m.userData.curve = curve
    scene.add(m)
    flowParticles.push(m)
  }
}

function initScene() {
  const el = containerRef.value
  if (!el) return

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x070b14)
  scene.fog = new THREE.FogExp2(0x070b14, 0.035)

  const w = el.clientWidth
  const h = el.clientHeight
  camera = new THREE.PerspectiveCamera(42, w / h, 0.1, 200)
  camera.position.set(0, 14, 16)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  el.appendChild(renderer.domElement)

  labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(w, h)
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0'
  labelRenderer.domElement.style.pointerEvents = 'none'
  el.appendChild(labelRenderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.maxPolarAngle = Math.PI / 2.1
  controls.minDistance = 8
  controls.maxDistance = 35
  controls.target.set(0, 0, 0)

  // 灯光
  scene.add(new THREE.AmbientLight(0x404060, 0.8))
  const dir = new THREE.DirectionalLight(0x88ccff, 1.2)
  dir.position.set(10, 20, 10)
  dir.castShadow = true
  scene.add(dir)
  const rim = new THREE.PointLight(0x22d3ee, 0.6, 40)
  rim.position.set(-8, 8, -5)
  scene.add(rim)

  // 地面网格
  const grid = new THREE.GridHelper(40, 40, 0x1e3a5f, 0x111827)
  grid.position.y = 0
  scene.add(grid)
  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(40, 40),
    new THREE.MeshStandardMaterial({ color: 0x0a101c, metalness: 0.2, roughness: 0.9 })
  )
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  scene.add(floor)

  // 工位
  for (const stepId of flowPath) {
    const cfg = positions3d[stepId]
    if (!cfg) continue
    const group = buildStationMesh(stepId, cfg)
    const label = createLabel(stepId, cfg)
    group.add(label)
    scene.add(group)
    stationMeshes[stepId] = group
  }

  buildConveyors()
  buildFlowParticles()
  fitCameraToLine()

  // 点击拾取
  renderer.domElement.addEventListener('click', onCanvasClick)

  animate()
  window.addEventListener('resize', onResize)
  resizeObserver = new ResizeObserver(() => onResize())
  resizeObserver.observe(el)
  updateStations()
}

function onCanvasClick(ev) {
  const rect = renderer.domElement.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((ev.clientX - rect.left) / rect.width) * 2 - 1,
    -((ev.clientY - rect.top) / rect.height) * 2 + 1
  )
  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(mouse, camera)
  const groups = Object.values(stationMeshes)
  const hits = raycaster.intersectObjects(groups, true)
  if (hits.length) {
    let obj = hits[0].object
    while (obj && !obj.userData.stepId && obj.parent) obj = obj.parent
    if (obj?.userData?.stepId) emit('select-step', obj.userData.stepId)
  }
}

function updateStations() {
  const lineRunning = (props.pipeline || []).some(s => s.aggregate_status === 'running')

  for (const stepId of flowPath) {
    const group = stationMeshes[stepId]
    if (!group) continue
    const st = stepStatus(stepId)
    const col = new THREE.Color(statusColor(st))
    const mat = group.userData.baseMat
    if (mat) {
      mat.emissive.copy(col)
      mat.emissiveIntensity = st === 'running' ? 0.45 : st === 'alarm' ? 0.9 : 0.15
    }
    const lamp = group.children.find(c => c.userData?.isLamp)
    if (lamp) lamp.material.color.copy(col)

    if (props.selectedStep === stepId) {
      if (mat) mat.emissiveIntensity += 0.3
    }

  }

  // Update label metrics
  if (labelRenderer) {
    labelRenderer.domElement.querySelectorAll('[data-metric]').forEach(el => {
      const sid = el.getAttribute('data-metric')
      el.textContent = liveMetricText(sid)
      el.style.color = stepStatus(sid) === 'alarm' ? '#f87171' : '#94a3b8'
    })
    labelRenderer.domElement.querySelectorAll('.twin3d-label').forEach(el => {
      const sid = el.querySelector('[data-metric]')?.getAttribute('data-metric')
      if (sid === props.selectedStep) el.classList.add('selected')
      else el.classList.remove('selected')
    })
  }

  conveyorLines.forEach(line => {
    if (line.userData.isActive) line.visible = lineRunning
  })
}

function animate() {
  animId = requestAnimationFrame(animate)
  const t = performance.now() * 0.001
  const lineRunning = (props.pipeline || []).some(s => s.aggregate_status === 'running')

  Object.entries(stationMeshes).forEach(([stepId, group]) => {
    const st = stepStatus(stepId)
    group.children.forEach(child => {
      if (child.userData?.spin && st === 'running') {
        child.rotation.y += 0.04
      }
      if (child.userData?.glow && st === 'running') {
        const intensity = 0.25 + 0.2 * Math.sin(t * 3)
        if (child.material?.emissiveIntensity != null) {
          child.material.emissiveIntensity = intensity
        }
      }
    })
    if (st === 'alarm') {
      const lamp = group.children.find(c => c.userData?.isLamp)
      if (lamp) lamp.material.opacity = 0.5 + 0.5 * Math.sin(t * 8)
    }
  })

  if (lineRunning) {
    flowParticles.forEach(p => {
      p.userData.t = (p.userData.t + 0.002) % 1
      const pos = p.userData.curve.getPoint(p.userData.t)
      p.position.copy(pos)
      p.visible = true
    })
  } else {
    flowParticles.forEach(p => { p.visible = false })
  }

  controls?.update()
  renderer?.render(scene, camera)
  labelRenderer?.render(scene, camera)
}

function onResize() {
  const el = containerRef.value
  if (!el || !camera || !renderer) return
  const w = el.clientWidth
  const h = el.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  labelRenderer.setSize(w, h)
}

function dispose() {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', onResize)
  resizeObserver?.disconnect()
  resizeObserver = null
  renderer?.domElement?.removeEventListener('click', onCanvasClick)
  controls?.dispose()
  renderer?.dispose()
  if (containerRef.value) {
    while (containerRef.value.firstChild) containerRef.value.removeChild(containerRef.value.firstChild)
  }
  stationMeshes = {}
  flowParticles = []
  conveyorLines = []
}

watch(() => [props.pipeline, props.equipment, props.selectedStep], updateStations, { deep: true })

function rebuildScene() {
  dispose()
  applyLayout(props.lineId)
  setTimeout(initScene, 50)
}

watch(() => props.lineId, rebuildScene)
watch(twinLayoutsRef, rebuildScene)

onMounted(() => {
  rebuildScene()
})
onUnmounted(dispose)
</script>

<template>
  <div ref="containerRef" class="twin3d-container"></div>
</template>

<style>
.twin3d-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: radial-gradient(ellipse at 50% 30%, #0f1a2e 0%, #070b14 70%);
}

.twin3d-label {
  pointer-events: auto;
  cursor: pointer;
  background: rgba(10, 16, 28, 0.88);
  border: 1px solid #1e3a5f;
  border-radius: 4px;
  padding: 4px 8px;
  text-align: center;
  min-width: 88px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.twin3d-label:hover,
.twin3d-label.selected {
  border-color: #22d3ee;
  box-shadow: 0 0 12px rgba(34, 211, 238, 0.35);
}

.twin3d-label .lbl-title {
  font-size: 11px;
  color: #e2e8f0;
  font-family: "PingFang SC", sans-serif;
}

.twin3d-label .lbl-eq {
  font-size: 9px;
  color: #22d3ee;
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
}

.twin3d-label .lbl-metric {
  font-size: 8px;
  color: #94a3b8;
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
  margin-top: 2px;
}
</style>
