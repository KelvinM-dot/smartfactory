import { watch, onUnmounted, unref } from 'vue'

/**
 * 订阅产线 WebSocket；产线切换时自动重连；失败时静默降级为纯轮询
 */
export function useLineStream(lineIdSource, onMessage) {
  let ws = null
  let closed = false

  function disconnect() {
    if (ws) {
      ws.onclose = null
      ws.onerror = null
      ws.onmessage = null
      ws.close()
      ws = null
    }
  }

  function connect(lineId) {
    disconnect()
    if (!lineId || closed) return
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${proto}://${window.location.host}/v1/stream/lines/${lineId}`
    try {
      ws = new WebSocket(url)
      ws.onmessage = (ev) => {
        try {
          onMessage(JSON.parse(ev.data))
        } catch { /* ignore */ }
      }
      ws.onclose = () => {
        if (!closed) setTimeout(() => connect(unref(lineIdSource)), 5000)
      }
      ws.onerror = () => ws?.close()
    } catch {
      setTimeout(() => connect(unref(lineIdSource)), 5000)
    }
  }

  watch(
    () => unref(lineIdSource),
    (lineId) => connect(lineId),
    { immediate: true }
  )

  onUnmounted(() => {
    closed = true
    disconnect()
  })
}
