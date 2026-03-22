/**
 * Sync Web Worker
 *
 * Offloads heavy offline sync operations (data transformation, conflict detection,
 * batch preparation) from the main thread to keep the POS UI responsive.
 *
 * Communication protocol:
 *   Main thread → Worker: SyncWorkerRequest (via postMessage)
 *   Worker → Main thread: SyncWorkerResponse (via postMessage)
 */

/** Request types from main thread to worker */
export type SyncWorkerRequest =
  | { type: 'PREPARE_SYNC_BATCH'; payload: SyncBatchPayload }
  | { type: 'DETECT_PRICE_CONFLICTS'; payload: PriceConflictPayload }
  | { type: 'PING' }

/** Response types from worker to main thread */
export type SyncWorkerResponse =
  | { type: 'SYNC_BATCH_READY'; payload: PreparedSyncBatch }
  | { type: 'PRICE_CONFLICTS_RESULT'; payload: PriceConflictResult[] }
  | { type: 'PONG' }
  | { type: 'ERROR'; error: string }

export interface SyncBatchPayload {
  transactions: TransactionData[]
}

export interface TransactionData {
  clientId: string
  storeId: string
  terminalId: string
  staffId: string
  items: TransactionItemData[]
  payments: PaymentData[]
  createdAt: string
}

export interface TransactionItemData {
  productId: string
  productName: string
  unitPrice: number
  quantity: number
  taxRateName: string
  taxRate: number
  isReducedTax: boolean
}

export interface PaymentData {
  method: string
  amount: number
  received: number
  reference?: string
}

export interface PreparedSyncBatch {
  transactions: TransactionData[]
  totalCount: number
  totalAmount: number
}

export interface PriceConflictPayload {
  transactions: TransactionData[]
  currentPrices: Record<string, number>
}

export interface PriceConflictResult {
  clientId: string
  items: Array<{
    productId: string
    productName: string
    offlinePrice: number
    currentPrice: number
  }>
}

/**
 * Prepare a batch of transactions for sync by transforming and summarizing.
 */
function prepareSyncBatch(payload: SyncBatchPayload): PreparedSyncBatch {
  let totalAmount = 0
  for (const tx of payload.transactions) {
    for (const item of tx.items) {
      totalAmount += item.unitPrice * item.quantity
    }
  }

  return {
    transactions: payload.transactions,
    totalCount: payload.transactions.length,
    totalAmount,
  }
}

/**
 * Detect price differences between offline transaction prices and current prices.
 */
function detectPriceConflicts(payload: PriceConflictPayload): PriceConflictResult[] {
  const conflicts: PriceConflictResult[] = []

  for (const tx of payload.transactions) {
    const conflictItems: PriceConflictResult['items'] = []
    for (const item of tx.items) {
      const currentPrice = payload.currentPrices[item.productId]
      if (currentPrice !== undefined && currentPrice !== item.unitPrice) {
        conflictItems.push({
          productId: item.productId,
          productName: item.productName,
          offlinePrice: item.unitPrice,
          currentPrice,
        })
      }
    }
    if (conflictItems.length > 0) {
      conflicts.push({ clientId: tx.clientId, items: conflictItems })
    }
  }

  return conflicts
}

// Worker message handler
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const ctx = self as any as {
  onmessage: ((event: MessageEvent<SyncWorkerRequest>) => void) | null
  postMessage: (message: SyncWorkerResponse) => void
}

ctx.onmessage = (event: MessageEvent<SyncWorkerRequest>) => {
  const request = event.data

  try {
    switch (request.type) {
      case 'PREPARE_SYNC_BATCH': {
        const result = prepareSyncBatch(request.payload)
        ctx.postMessage({ type: 'SYNC_BATCH_READY', payload: result })
        break
      }
      case 'DETECT_PRICE_CONFLICTS': {
        const result = detectPriceConflicts(request.payload)
        ctx.postMessage({ type: 'PRICE_CONFLICTS_RESULT', payload: result })
        break
      }
      case 'PING': {
        ctx.postMessage({ type: 'PONG' })
        break
      }
    }
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : 'Unknown worker error'
    ctx.postMessage({ type: 'ERROR', error: message })
  }
}
