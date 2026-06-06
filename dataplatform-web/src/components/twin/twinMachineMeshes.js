/**
 * 产线 3D 工位精细化模型（按 master-data position_3d.type / variant 构建）
 */

function bodyMat(THREE) {
  return new THREE.MeshStandardMaterial({
    color: 0x1a2744,
    metalness: 0.62,
    roughness: 0.34,
    emissive: 0x000000,
    emissiveIntensity: 0.5
  })
}

function accentMat(THREE, color, opts = {}) {
  return new THREE.MeshStandardMaterial({
    color,
    metalness: opts.metalness ?? 0.75,
    roughness: opts.roughness ?? 0.22,
    transparent: opts.transparent ?? false,
    opacity: opts.opacity ?? 1,
    emissive: opts.emissive ?? 0x000000,
    emissiveIntensity: opts.emissiveIntensity ?? 0
  })
}

function addBase(group, THREE, sx, sz) {
  const base = new THREE.Mesh(
    new THREE.BoxGeometry(sx + 0.35, 0.15, sz + 0.35),
    accentMat(THREE, 0x0f172a, { metalness: 0.4, roughness: 0.82 })
  )
  base.position.y = 0.075
  group.add(base)
}

function addLamp(group, THREE, sx, sy) {
  const lamp = new THREE.Mesh(
    new THREE.SphereGeometry(0.18, 12, 12),
    new THREE.MeshBasicMaterial({ color: 0x64748b, transparent: true })
  )
  lamp.position.set(sx / 2 + 0.22, sy + 0.32, 0)
  lamp.userData.isLamp = true
  group.add(lamp)
}

function buildCutter(group, THREE, sx, sy, sz, mat) {
  const frame = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * 0.55, sz * 0.85), mat)
  frame.position.y = sy * 0.3
  group.add(frame)
  const rollerMat = accentMat(THREE, 0x475569)
  for (const xOff of [-sx * 0.22, sx * 0.22]) {
    const roller = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.28, sz * 0.7, 16), rollerMat)
    roller.rotation.z = Math.PI / 2
    roller.position.set(xOff, sy * 0.42, 0)
    roller.userData.spin = true
    group.add(roller)
  }
  const blade = new THREE.Mesh(
    new THREE.BoxGeometry(0.08, sy * 0.35, sz * 0.5),
    accentMat(THREE, 0x94a3b8, { metalness: 0.95, roughness: 0.1 })
  )
  blade.position.set(0, sy * 0.58, 0)
  group.add(blade)
  return frame
}

function buildDrawer(group, THREE, sx, sy, sz, mat, variant) {
  const isFine = variant === 'fine'
  const body = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.85, sy * (isFine ? 0.45 : 0.55), sz * 0.75),
    mat
  )
  body.position.y = sy * (isFine ? 0.22 : 0.28)
  group.add(body)
  const coneH = sy * (isFine ? 0.55 : 0.65)
  const die = new THREE.Mesh(
    new THREE.ConeGeometry(isFine ? 0.35 : 0.5, coneH, 12),
    accentMat(THREE, 0x334155, { metalness: 0.88, roughness: 0.18 })
  )
  die.position.set(0, sy * 0.55 + coneH * 0.15, sz * 0.15)
  group.add(die)
  const capstan = new THREE.Mesh(
    new THREE.CylinderGeometry(0.42, 0.42, 0.55, 18),
    accentMat(THREE, 0x64748b, { metalness: 0.9, roughness: 0.12 })
  )
  capstan.rotation.z = Math.PI / 2
  capstan.position.set(-sx * 0.28, sy * 0.5, 0)
  capstan.userData.spin = true
  group.add(capstan)
  const guide = new THREE.Mesh(
    new THREE.TorusGeometry(0.25, 0.05, 8, 20),
    accentMat(THREE, 0x22d3ee, { metalness: 0.6, roughness: 0.3 })
  )
  guide.rotation.y = Math.PI / 2
  guide.position.set(sx * 0.3, sy * 0.45, 0)
  group.add(guide)
  return body
}

function buildMixer(group, THREE, sx, sy, sz, mat) {
  const body = new THREE.Mesh(new THREE.CylinderGeometry(sx * 0.42, sx * 0.48, sy * 0.75, 18), mat)
  body.position.y = sy * 0.38
  group.add(body)
  const hopper = new THREE.Mesh(
    new THREE.CylinderGeometry(sx * 0.28, sx * 0.38, sy * 0.35, 4),
    accentMat(THREE, 0x334155, { metalness: 0.5, roughness: 0.5 })
  )
  hopper.position.y = sy * 0.88
  group.add(hopper)
  const drum = new THREE.Mesh(
    new THREE.TorusGeometry(sx * 0.32, 0.07, 8, 24),
    accentMat(THREE, 0x475569, { metalness: 0.82, roughness: 0.2 })
  )
  drum.rotation.x = Math.PI / 2
  drum.position.y = sy * 0.55
  drum.userData.spin = true
  group.add(drum)
  return body
}

function buildFiller(group, THREE, sx, sy, sz, mat) {
  const body = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * 0.55, sz), mat)
  body.position.y = sy * 0.28
  group.add(body)
  const hopper = new THREE.Mesh(
    new THREE.CylinderGeometry(sx * 0.22, sx * 0.35, sy * 0.45, 4),
    accentMat(THREE, 0x1e293b, { metalness: 0.45, roughness: 0.55 })
  )
  hopper.position.set(-sx * 0.15, sy * 0.78, 0)
  group.add(hopper)
  for (const zOff of [-0.35, 0.35]) {
    const roll = new THREE.Mesh(
      new THREE.CylinderGeometry(0.32, 0.32, sz * 0.55, 14),
      accentMat(THREE, 0x64748b, { metalness: 0.85, roughness: 0.15 })
    )
    roll.rotation.z = Math.PI / 2
    roll.position.set(sx * 0.15, sy * 0.42, zOff)
    roll.userData.spin = true
    group.add(roll)
  }
  const die = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.35, sy * 0.2, sz * 0.25),
    accentMat(THREE, 0x0891b2, { metalness: 0.7, roughness: 0.25 })
  )
  die.position.set(sx * 0.32, sy * 0.35, 0)
  group.add(die)
  return body
}

function buildCoater(group, THREE, sx, sy, sz, mat) {
  const body = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * 0.5, sz * 0.8), mat)
  body.position.y = sy * 0.25
  group.add(body)
  const pasteHopper = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.35, sy * 0.4, sz * 0.45),
    accentMat(THREE, 0x78350f, { metalness: 0.3, roughness: 0.6 })
  )
  pasteHopper.position.set(-sx * 0.25, sy * 0.65, 0)
  group.add(pasteHopper)
  for (const xOff of [-0.2, 0.2]) {
    const coatRoll = new THREE.Mesh(
      new THREE.CylinderGeometry(0.38, 0.38, sz * 0.65, 16),
      accentMat(THREE, 0x57534e, { metalness: 0.8, roughness: 0.2 })
    )
    coatRoll.rotation.z = Math.PI / 2
    coatRoll.position.set(xOff, sy * 0.48, 0)
    coatRoll.userData.spin = true
    group.add(coatRoll)
  }
  const doctor = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.5, 0.06, sz * 0.08),
    accentMat(THREE, 0xcbd5e1, { metalness: 0.95, roughness: 0.08 })
  )
  doctor.position.set(0, sy * 0.62, sz * 0.2)
  group.add(doctor)
  return body
}

function buildPlater(group, THREE, sx, sy, sz, mat, variant) {
  const isSolid = variant === 'solid_wire'
  const body = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * (isSolid ? 0.65 : 0.75), sz * 0.7), mat)
  body.position.y = sy * (isSolid ? 0.32 : 0.38)
  group.add(body)
  const tank = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.88, sy * (isSolid ? 0.28 : 0.38), sz * 0.52),
    accentMat(THREE, isSolid ? 0x0e7490 : 0x0891b2, {
      transparent: true, opacity: isSolid ? 0.45 : 0.55, metalness: 0.35, roughness: 0.12
    })
  )
  tank.position.y = sy * (isSolid ? 0.5 : 0.58)
  group.add(tank)
  for (const xOff of [-sx * 0.35, sx * 0.35]) {
    const anode = new THREE.Mesh(
      new THREE.BoxGeometry(0.12, sy * 0.45, sz * 0.08),
      accentMat(THREE, 0xf59e0b, { metalness: 0.85, roughness: 0.2 })
    )
    anode.position.set(xOff, sy * 0.55, 0)
    group.add(anode)
  }
  const entryRoll = new THREE.Mesh(
    new THREE.CylinderGeometry(0.3, 0.3, sz * 0.6, 14),
    accentMat(THREE, 0x64748b, { metalness: 0.88, roughness: 0.15 })
  )
  entryRoll.rotation.z = Math.PI / 2
  entryRoll.position.set(-sx * 0.42, sy * 0.45, 0)
  entryRoll.userData.spin = true
  group.add(entryRoll)
  return body
}

function buildWinder(group, THREE, sx, sy, sz, mat) {
  const body = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * 0.42, sz), mat)
  body.position.y = sy * 0.21
  group.add(body)
  const spindle = new THREE.Mesh(
    new THREE.CylinderGeometry(0.55, 0.55, 0.85, 22),
    accentMat(THREE, 0x64748b, { metalness: 0.92, roughness: 0.12 })
  )
  spindle.rotation.z = Math.PI / 2
  spindle.position.set(0, sy * 0.52, 0)
  spindle.userData.spin = true
  group.add(spindle)
  const wireCoil = new THREE.Mesh(
    new THREE.TorusGeometry(0.42, 0.12, 10, 28),
    accentMat(THREE, 0xb45309, { metalness: 0.7, roughness: 0.35 })
  )
  wireCoil.rotation.y = Math.PI / 2
  wireCoil.position.set(0, sy * 0.52, 0)
  wireCoil.userData.spin = true
  group.add(wireCoil)
  const arm = new THREE.Mesh(
    new THREE.BoxGeometry(0.15, sy * 0.35, 0.15),
    accentMat(THREE, 0x334155, { metalness: 0.6, roughness: 0.4 })
  )
  arm.position.set(sx * 0.35, sy * 0.45, 0)
  group.add(arm)
  return body
}

function buildPacker(group, THREE, sx, sy, sz, mat) {
  const body = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * 0.45, sz), mat)
  body.position.y = sy * 0.22
  group.add(body)
  const belt = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 1.1, 0.08, sz * 0.55),
    accentMat(THREE, 0x1e293b, { metalness: 0.35, roughness: 0.7 })
  )
  belt.position.y = sy * 0.38
  group.add(belt)
  const strapL = new THREE.Mesh(
    new THREE.BoxGeometry(0.1, sy * 0.5, sz * 0.4),
    accentMat(THREE, 0x475569, { metalness: 0.7, roughness: 0.3 })
  )
  strapL.position.set(-sx * 0.2, sy * 0.5, 0)
  group.add(strapL)
  const strapR = strapL.clone()
  strapR.position.x = sx * 0.2
  group.add(strapR)
  const label = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.25, sy * 0.15, 0.04),
    accentMat(THREE, 0xf8fafc, { metalness: 0.2, roughness: 0.8 })
  )
  label.position.set(0, sy * 0.72, sz * 0.35)
  group.add(label)
  return body
}

function buildStock(group, THREE, sx, sy, sz, mat) {
  const platform = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * 0.25, sz), mat)
  platform.position.y = sy * 0.12
  group.add(platform)
  const agv = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.7, sy * 0.2, sz * 0.65),
    accentMat(THREE, 0x164e63, { metalness: 0.55, roughness: 0.45 })
  )
  agv.position.y = sy * 0.32
  group.add(agv)
  for (let i = 0; i < 3; i++) {
    const crate = new THREE.Mesh(
      new THREE.BoxGeometry(sx * 0.22, sy * 0.28, sz * 0.22),
      accentMat(THREE, 0xca8a04, { metalness: 0.25, roughness: 0.65 })
    )
    crate.position.set(-sx * 0.2 + i * sx * 0.2, sy * 0.52, 0)
    group.add(crate)
  }
  const mast = new THREE.Mesh(
    new THREE.CylinderGeometry(0.06, 0.08, sy * 0.35, 8),
    accentMat(THREE, 0x22d3ee, { emissive: 0x0891b2, emissiveIntensity: 0.4 })
  )
  mast.position.set(sx * 0.32, sy * 0.55, 0)
  mast.userData.glow = true
  group.add(mast)
  return platform
}

function buildDryer(group, THREE, sx, sy, sz, mat) {
  const body = new THREE.Mesh(new THREE.BoxGeometry(sx, sy * 0.82, sz * 0.72), mat)
  body.position.y = sy * 0.41
  group.add(body)
  const oven = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.72, sy * 0.42, sz * 0.52),
    accentMat(THREE, 0xf97316, { emissive: 0x7c2d12, emissiveIntensity: 0.35, metalness: 0.42, roughness: 0.48 })
  )
  oven.position.y = sy * 0.58
  oven.userData.glow = true
  group.add(oven)
  const chimney = new THREE.Mesh(
    new THREE.CylinderGeometry(0.12, 0.18, sy * 0.35, 8),
    accentMat(THREE, 0x57534e, { metalness: 0.5, roughness: 0.5 })
  )
  chimney.position.set(sx * 0.35, sy * 0.95, 0)
  group.add(chimney)
  const conveyor = new THREE.Mesh(
    new THREE.BoxGeometry(sx * 0.85, 0.07, sz * 0.35),
    accentMat(THREE, 0x292524, { metalness: 0.3, roughness: 0.75 })
  )
  conveyor.position.y = sy * 0.35
  group.add(conveyor)
  return body
}

/** @returns {THREE.Group} */
export function createMachineMesh(THREE, stepId, cfg) {
  const group = new THREE.Group()
  group.userData = { stepId }
  const [sx, sy, sz] = cfg.scale
  const mat = bodyMat(THREE)
  group.userData.baseMat = mat
  const variant = cfg.variant || ''

  let body
  switch (cfg.type) {
    case 'cutter':
      body = buildCutter(group, THREE, sx, sy, sz, mat)
      break
    case 'drawer':
      body = buildDrawer(group, THREE, sx, sy, sz, mat, variant)
      break
    case 'mixer':
      body = buildMixer(group, THREE, sx, sy, sz, mat)
      break
    case 'filler':
      body = buildFiller(group, THREE, sx, sy, sz, mat)
      break
    case 'coater':
      body = buildCoater(group, THREE, sx, sy, sz, mat)
      break
    case 'plater':
      body = buildPlater(group, THREE, sx, sy, sz, mat, variant)
      break
    case 'winder':
      body = buildWinder(group, THREE, sx, sy, sz, mat)
      break
    case 'packer':
      body = buildPacker(group, THREE, sx, sy, sz, mat)
      break
    case 'stock':
      body = buildStock(group, THREE, sx, sy, sz, mat)
      break
    case 'dryer':
      body = buildDryer(group, THREE, sx, sy, sz, mat)
      break
    default:
      body = new THREE.Mesh(new THREE.BoxGeometry(sx, sy, sz), mat)
      body.position.y = sy / 2
      group.add(body)
  }

  addBase(group, THREE, sx, sz)
  addLamp(group, THREE, sx, sy)
  group.position.set(cfg.x, 0, cfg.z)
  return group
}
