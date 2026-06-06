export function buildDataPointLookup(dataPoints) {
  const byEqField = {}
  for (const dp of dataPoints || []) {
    const fieldId = dp.field_id || dp.data_point_id
    byEqField[`${dp.equipment_id}:${fieldId}`] = dp
  }
  return byEqField
}

export { supportsTwinVisual, supportsTwin3D } from '../config/twinLayout'
