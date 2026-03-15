/**
 * マルチ端末カート共有 (#192)
 * WebSocket/BroadcastChannel を使用してカートを端末間で同期する。
 */

import type { CartItem } from '@/stores/cart-store'

export type CartSyncAction = 'ADD' | 'REMOVE' | 'UPDATE' | 'CLEAR'

export interface CartSyncMessage {
  orgId: string
  storeId: string
  cartId: string
  items: CartItem[]
  action: CartSyncAction
  timestamp: number
  senderId: string
}

const CHANNEL_NAME = 'openpos-cart-sync'
const SENDER_ID = crypto.randomUUID()

let channel: BroadcastChannel | null = null
let messageHandler: ((message: CartSyncMessage) => void) | null = null

/**
 * カート同期を開始する。
 * BroadcastChannel を使用して同一オリジンの他タブ・ウィンドウと同期する。
 */
export function startCartSync(onMessage: (message: CartSyncMessage) => void): void {
  if (channel) return

  channel = new BroadcastChannel(CHANNEL_NAME)
  messageHandler = onMessage

  channel.onmessage = (event: MessageEvent) => {
    const message = event.data as CartSyncMessage
    // 自分自身からのメッセージは無視
    if (message.senderId === SENDER_ID) return
    messageHandler?.(message)
  }
}

/**
 * カート同期を停止する。
 */
export function stopCartSync(): void {
  channel?.close()
  channel = null
  messageHandler = null
}

/**
 * カート変更を他の端末にブロードキャストする。
 */
export function broadcastCartChange(
  orgId: string,
  storeId: string,
  cartId: string,
  items: CartItem[],
  action: CartSyncAction,
): void {
  if (!channel) return

  const message: CartSyncMessage = {
    orgId,
    storeId,
    cartId,
    items,
    action,
    timestamp: Date.now(),
    senderId: SENDER_ID,
  }

  channel.postMessage(message)
}
