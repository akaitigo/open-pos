import { z } from 'zod'
import { api } from '@/lib/api'
import {
  getPendingTransactions,
  updateTransactionSyncStatus,
  cleanupSyncedTransactions,
} from '@/lib/offline-db'

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

/**
 * 同期マネージャの状態。
 */
export interface SyncStatus {
  isSyncing: boolean
  pendingCount: number
  lastSyncAt: string | null
  lastError: string | null
}

let syncInProgress = false
let lastSyncAt: string | null = null
let lastError: string | null = null
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
 * pendingCount は同期開始時にカウントするため、リアルタイムではない。
 */
export function getSyncStatus(): SyncStatus {
  return {
    isSyncing: syncInProgress,
    pendingCount: 0,
    lastSyncAt,
    lastError,
  }
}

/**
 * 未同期のオフライン取引をサーバーに同期する。
 * オンライン復帰時やアプリ起動時に呼び出す。
 */
export async function syncPendingTransactions(): Promise<SyncResponse | null> {
  if (syncInProgress) return null
  if (!navigator.onLine) return null

  syncInProgress = true
  lastError = null
  notifyListeners()

  try {
    const pending = await getPendingTransactions()
    if (pending.length === 0) {
      return null
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
    return response
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : 'Sync failed'
    lastError = message
    return null
  } finally {
    syncInProgress = false
    notifyListeners()
  }
}

/**
 * オンライン復帰時に自動同期を開始するリスナーを設定する。
 */
export function setupAutoSync(): () => void {
  const handleOnline = () => {
    void syncPendingTransactions()
  }

  window.addEventListener('online', handleOnline)

  // 初回チェック
  if (navigator.onLine) {
    void syncPendingTransactions()
  }

  return () => {
    window.removeEventListener('online', handleOnline)
  }
}
