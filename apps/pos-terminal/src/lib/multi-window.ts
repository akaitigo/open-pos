/**
 * POS端末 マルチウィンドウ対応 (#223)
 * SharedWorker を使用して複数ウィンドウ間で状態を同期する。
 * 各ウィンドウは独立して動作し、共有カートオプションを提供する。
 */

export interface WindowSyncMessage {
  type: 'STATE_UPDATE' | 'HEARTBEAT' | 'WINDOW_OPENED' | 'WINDOW_CLOSED'
  windowId: string
  payload: unknown
  timestamp: number
}

const WINDOW_ID = crypto.randomUUID()
const SYNC_CHANNEL = 'openpos-window-sync'

let channel: BroadcastChannel | null = null
let heartbeatInterval: ReturnType<typeof setInterval> | null = null

/**
 * マルチウィンドウ同期を開始する。
 */
export function startWindowSync(onMessage: (message: WindowSyncMessage) => void): string {
  if (channel) return WINDOW_ID

  channel = new BroadcastChannel(SYNC_CHANNEL)
  channel.onmessage = (event: MessageEvent) => {
    const message = event.data as WindowSyncMessage
    if (message.windowId === WINDOW_ID) return
    onMessage(message)
  }

  // 他のウィンドウに存在を通知
  broadcastMessage('WINDOW_OPENED', null)

  // ハートビート（30秒ごと）
  heartbeatInterval = setInterval(() => {
    broadcastMessage('HEARTBEAT', null)
  }, 30_000)

  return WINDOW_ID
}

/**
 * マルチウィンドウ同期を停止する。
 */
export function stopWindowSync(): void {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval)
    heartbeatInterval = null
  }

  broadcastMessage('WINDOW_CLOSED', null)
  channel?.close()
  channel = null
}

/**
 * 他のウィンドウにメッセージをブロードキャストする。
 */
export function broadcastMessage(type: WindowSyncMessage['type'], payload: unknown): void {
  if (!channel) return

  const message: WindowSyncMessage = {
    type,
    windowId: WINDOW_ID,
    payload,
    timestamp: Date.now(),
  }

  channel.postMessage(message)
}

/**
 * 現在のウィンドウIDを取得する。
 */
export function getWindowId(): string {
  return WINDOW_ID
}
