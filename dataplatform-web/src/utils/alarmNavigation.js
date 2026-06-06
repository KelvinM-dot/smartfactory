/** 根据报警设备 ID 解析工序，用于跳转孪生定位 */
export function stepIdForEquipment(equipmentId, equipmentList = []) {
  if (!equipmentId) return null
  const eq = equipmentList.find(item => item.equipment_id === equipmentId)
  return eq?.process_step_id || null
}

export function twinQueryForAlarm(alarm, equipmentList, fallbackLineId) {
  const lineId = alarm?.product_line_id || fallbackLineId
  const step = stepIdForEquipment(alarm?.equipment_id, equipmentList)
  return {
    path: `/lines/${lineId}`,
    query: {
      tab: 'twin',
      ...(step ? { step } : {}),
      ...(alarm?.equipment_id ? { equip: alarm.equipment_id } : {})
    }
  }
}
