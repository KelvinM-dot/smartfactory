export const MATERIAL_EVENT_LABELS = {
  LINE_ON: '工序上线',
  LINE_OFF: '工序下线',
  STOCK_IN: '成品入库',
  STOCK_OUT: '成品出库',
  TRANSFER: '工序转移',
  AGV_DISPATCH: 'AGV 发出',
  AGV_ARRIVE: 'AGV 到达',
  BATCH_CREATE: '批次创建',
  BATCH_CLOSE: '批次完工'
}

export function materialEventLabel(type) {
  return MATERIAL_EVENT_LABELS[type] || type
}
