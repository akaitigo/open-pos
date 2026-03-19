import { z } from 'zod'
import { api } from '@/lib/api'
import {
  getPendingTransactions,
  updateTransactionSyncStatus,
  cleanupSyncedTransactions,
  getCachedProducts,
} from '@/lib/offline-db'
import type { OfflineTransaction, OfflineTransactionItem } from '@/lib/offline-db'
import { createLogger } from '@/lib/logger'

const log = createLogger('SyncManager')

/**
 * 同期結果のスキーマ。
 */
const SyncResultSchema = z.object({
  clientId: z.string(),
  success: z.boolean(),
  transactionId: z.string().nullable().optional(),
  error: z.string().nullable().optional(),
})

const SyncResponseSchema = z.object({
  results: z.array(SyncResultSchema),
})

type SyncResponse = z.infer<typeof SyncResponseSchema>

/** 同期状態の種類 */
export type SyncState = 'idle' | 'syncing' | 'retrying' | 'failed' | 'completed'

/**
 * 同期マネージャの状態。
 */
export interface SyncStatus {
  state: SyncState
  /** 後方互換のため isSyncing も保持 */
  isSyncing: boolean
  pendingCount: number
  lastSyncAt: string | null
  lastError: string | null
  retryCount: number
  /** 価格変更が検出された取引の clientId リスト */
  priceConflicts: PriceConflict[]
}

/** 価格競合の情報 */
export interface PriceConflict {
  clientId: string
  items: PriceConflictItem[]
}

export interface PriceConflictItem {
  productId: string
  productName: string
  /** オフライン取引時の価格（銭単位） */
  offlinePrice: number
  /** 現在のサーバー/キャッシュ価格（銭単位） */
  currentPrice: number
}

/** Maximum retry attempts for sync */
const MAX_SYNC_RETRIES = 5

/** Base delay for exponential backoff (ms) */
const BASE_BACKOFF_MS = 1000

/** Maximum backoff delay (ms) */
const MAX_BACKOFF_MS = 30_000

let syncState: SyncState = 'idle'
let retryCount = 0
let lastSyncAt: string | null = null
let lastError: string | null = null
let priceConflicts: PriceConflict[] = []
let retryTimeoutId: ReturnType<typeof setTimeout> | null = null
const listeners: Set<() => void> = new Set()

function notifyListeners(): void {
  for (const listener of listeners) {
    listener()
  }
}

/**
 * 同期ステータスの変更を購読する。
 */
export function subscribeSyncStatus(callback: () => void): () => void {
  listeners.add(callback)
  return () => {
    listeners.delete(callback)
  }
}

/**
 * 現在の同期ステータスを取得する。
 */
export function getSyncStatus(): SyncStatus {
  return {
    state: syncState,
    isSyncing: syncState === 'syncing' || syncState === 'retrying',
    pendingCount: 0,
    lastSyncAt,
    lastError,
    retryCount,
    priceConflicts,
  }
}

/**
 * Calculate exponential backoff delay with jitter.
 */
function calculateBackoffMs(attempt: number): number {
  const exponentialDelay = BASE_BACKOFF_MS * Math.pow(2, attempt)
  const clampedDelay = Math.min(exponentialDelay, MAX_BACKOFF_MS)
  // Add random jitter (0-25% of delay) to prevent thundering herd
  const jitter = clampedDelay * Math.random() * 0.25
  return clampedDelay + jitter
}

/**
 * Check for price changes between offline transactions and current product cache.
 * Returns price conflicts for items whose prices have changed since going offline.
 */
async function detectPriceConflicts(transactions: OfflineTransaction[]): Promise<PriceConflict[]> {
  const cachedProducts = await getCachedProducts()
  const productPriceMap = new Map<string, number>()
  for (const product of cachedProducts) {
    productPriceMap.set(product.id, product.price)
  }

  const conflicts: PriceConflict[] = []

  for (const tx of transactions) {
    const conflictItems: PriceConflictItem[] = []
    for (const item of tx.items) {
      const currentPrice = productPriceMap.get(item.productId)
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
      conflicts.push({
        clientId: tx.clientId,
        items: conflictItems,
      })
    }
  }

  return conflicts
}

/**
 * Recalculate transaction items with updated prices.
 * Returns updated items with current prices applied.
 */
export function applyCurrentPrices(
  items: OfflineTransactionItem[],
  priceMap: Map<string, number>,
): OfflineTransactionItem[] {
  return items.map((item) => {
    const currentPrice = priceMap.get(item.productId)
    if (currentPrice !== undefined && currentPrice !== item.unitPrice) {
      return { ...item, unitPrice: currentPrice }
    }
    return item
  })
}

/**
 * 未同期のオフライン取引をサーバーに同期する。
 * オンライン復帰時やアプリ起動時に呼び出す。
 */
export async function syncPendingTransactions(): Promise<SyncResponse | null> {
  if (syncState === 'syncing') return null
  if (!navigator.onLine) return null

  syncState = 'syncing'
  lastError = null
  priceConflicts = []
  notifyListeners()

  try {
    const pending = await getPendingTransactions()
    if (pending.length === 0) {
      syncState = 'completed'
      notifyListeners()
      return null
    }

    // Detect price conflicts before syncing
    const conflicts = await detectPriceConflicts(pending)
    if (conflicts.length > 0) {
      priceConflicts = conflicts
      log.warn('Price conflicts detected during sync', { conflictCount: conflicts.length })
      // Continue syncing with original prices - server should validate
    }

    const body = {
      transactions: pending.map((tx) => ({
        clientId: tx.clientId,
        storeId: tx.storeId,
        terminalId: tx.terminalId,
        staffId: tx.staffId,
        items: tx.items.map((item) => ({
          productId: item.productId,
          productName: item.productName,
          unitPrice: item.unitPrice,
          quantity: item.quantity,
          taxRateName: item.taxRateName,
          taxRate: item.taxRate,
          isReducedTax: item.isReducedTax,
        })),
        payments: tx.payments.map((p) => ({
          method: p.method,
          amount: p.amount,
          received: p.received,
          reference: p.reference,
        })),
        createdAt: tx.createdAt,
        hasPriceConflict: conflicts.some((c) => c.clientId === tx.clientId),
      })),
    }

    const response = await api.post('/api/sync/transactions', body, SyncResponseSchema)

    // 結果を元にローカルDBを更新
    for (const result of response.results) {
      const localTx = pending.find((tx) => tx.clientId === result.clientId)
      if (localTx?.localId !== undefined) {
        if (result.success) {
          await updateTransactionSyncStatus(
            localTx.localId,
            'synced',
            result.transactionId ?? undefined,
          )
        } else {
          await updateTransactionSyncStatus(
            localTx.localId,
            'failed',
            undefined,
            result.error ?? 'Unknown error',
          )
        }
      }
    }

    // 古い同期済み取引をクリーンアップ
    await cleanupSyncedTransactions()

    lastSyncAt = new Date().toISOString()
    syncState = 'completed'
    retryCount = 0
    notifyListeners()
    return response
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : 'Sync failed'
    lastError = message
    log.error('Sync failed', { error: message, retryCount })

    if (retryCount < MAX_SYNC_RETRIES) {
      syncState = 'retrying'
      notifyListeners()
      scheduleRetry()
    } else {
      syncState = 'failed'
      log.error('Max sync retries exceeded, giving up')
      notifyListeners()
    }

    return null
  }
}

/**
 * Schedule a retry with exponential backoff.
 */
function scheduleRetry(): void {
  if (retryTimeoutId !== null) {
    clearTimeout(retryTimeoutId)
  }

  const delay = calculateBackoffMs(retryCount)
  retryCount++
  log.info(`Scheduling sync retry #${retryCount} in ${Math.round(delay)}ms`)

  retryTimeoutId = setTimeout(() => {
    retryTimeoutId = null
    if (navigator.onLine) {
      // Reset state to allow re-entry
      syncState = 'idle'
      void syncPendingTransactions()
    } else {
      // Wait for online event
      syncState = 'retrying'
      notifyListeners()
    }
  }, delay)
}

/**
 * Reset retry counter and cancel pending retries.
 */
export function resetSyncRetries(): void {
  retryCount = 0
  syncState = 'idle'
  lastError = null
  priceConflicts = []
  if (retryTimeoutId !== null) {
    clearTimeout(retryTimeoutId)
    retryTimeoutId = null
  }
  notifyListeners()
}

/**
 * Acknowledge and clear price conflicts (user has reviewed them).
 */
export function acknowledgePriceConflicts(): void {
  priceConflicts = []
  notifyListeners()
}

/**
 * オンライン復帰時に自動同期を開始するリスナーを設定する。
 */
export function setupAutoSync(): () => void {
  const handleOnline = () => {
    retryCount = 0
    syncState = 'idle'
    void syncPendingTransactions()
  }

  window.addEventListener('online', handleOnline)

  // 初回チェック
  if (navigator.onLine) {
    void syncPendingTransactions()
  }

  return () => {
    window.removeEventListener('online', handleOnline)
    if (retryTimeoutId !== null) {
      clearTimeout(retryTimeoutId)
      retryTimeoutId = null
    }
  }
}
