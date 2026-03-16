import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const mockGetPendingTransactions = vi.fn()
const mockUpdateTransactionSyncStatus = vi.fn()
const mockCleanupSyncedTransactions = vi.fn()
const mockApiPost = vi.fn()

vi.mock('@/lib/offline-db', () => ({
  getPendingTransactions: (...args: unknown[]) => mockGetPendingTransactions(...args),
  updateTransactionSyncStatus: (...args: unknown[]) => mockUpdateTransactionSyncStatus(...args),
  cleanupSyncedTransactions: (...args: unknown[]) => mockCleanupSyncedTransactions(...args),
}))

vi.mock('@/lib/api', () => ({
  api: {
    post: (...args: unknown[]) => mockApiPost(...args),
    get: vi.fn(),
    setOrganizationId: vi.fn(),
  },
}))

const { getSyncStatus, subscribeSyncStatus, syncPendingTransactions, setupAutoSync } =
  await import('./sync-manager')

describe('sync-manager', () => {
  const originalOnLine = navigator.onLine

  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(navigator, 'onLine', {
      value: true,
      writable: true,
      configurable: true,
    })
  })

  afterEach(() => {
    Object.defineProperty(navigator, 'onLine', {
      value: originalOnLine,
      writable: true,
      configurable: true,
    })
  })

  describe('getSyncStatus', () => {
    it('初期状態の同期ステータスを返す', () => {
      const status = getSyncStatus()
      expect(status.isSyncing).toBe(false)
      expect(status.pendingCount).toBe(0)
    })
  })

  describe('subscribeSyncStatus', () => {
    it('リスナーを登録して解除関数を返す', () => {
      const listener = vi.fn()
      const unsubscribe = subscribeSyncStatus(listener)
      expect(typeof unsubscribe).toBe('function')
      unsubscribe()
    })
  })

  describe('syncPendingTransactions', () => {
    it('オフライン時は null を返す', async () => {
      Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
      const result = await syncPendingTransactions()
      expect(result).toBeNull()
    })

    it('未同期の取引がない場合は null を返す', async () => {
      mockGetPendingTransactions.mockResolvedValue([])
      const result = await syncPendingTransactions()
      expect(result).toBeNull()
    })

    it('未同期の取引を同期する', async () => {
      const pendingTx = {
        localId: 1,
        clientId: 'client-001',
        storeId: 'store-001',
        terminalId: 'terminal-001',
        staffId: 'staff-001',
        items: [
          {
            productId: 'prod-001',
            productName: 'テスト商品',
            unitPrice: 15000,
            quantity: 1,
            taxRateName: '標準税率',
            taxRate: '0.10',
            isReducedTax: false,
          },
        ],
        payments: [{ method: 'CASH', amount: 16500 }],
        createdAt: '2026-01-01T12:00:00Z',
        syncStatus: 'pending' as const,
      }

      mockGetPendingTransactions.mockResolvedValue([pendingTx])
      mockApiPost.mockResolvedValue({
        results: [{ clientId: 'client-001', success: true, transactionId: 'server-tx-001' }],
      })
      mockUpdateTransactionSyncStatus.mockResolvedValue(undefined)
      mockCleanupSyncedTransactions.mockResolvedValue(0)

      const result = await syncPendingTransactions()
      expect(result).not.toBeNull()
      expect(result!.results).toHaveLength(1)
      expect(result!.results[0]!.success).toBe(true)

      expect(mockUpdateTransactionSyncStatus).toHaveBeenCalledWith(1, 'synced', 'server-tx-001')
      expect(mockCleanupSyncedTransactions).toHaveBeenCalled()
    })

    it('同期失敗した取引のステータスを failed に更新する', async () => {
      const pendingTx = {
        localId: 2,
        clientId: 'client-002',
        storeId: 'store-001',
        terminalId: 'terminal-001',
        staffId: 'staff-001',
        items: [],
        payments: [],
        createdAt: '2026-01-01T12:00:00Z',
        syncStatus: 'pending' as const,
      }

      mockGetPendingTransactions.mockResolvedValue([pendingTx])
      mockApiPost.mockResolvedValue({
        results: [{ clientId: 'client-002', success: false, error: 'Duplicate clientId' }],
      })
      mockUpdateTransactionSyncStatus.mockResolvedValue(undefined)
      mockCleanupSyncedTransactions.mockResolvedValue(0)

      await syncPendingTransactions()

      expect(mockUpdateTransactionSyncStatus).toHaveBeenCalledWith(
        2,
        'failed',
        undefined,
        'Duplicate clientId',
      )
    })

    it('API エラー時は null を返しエラーを記録する', async () => {
      mockGetPendingTransactions.mockResolvedValue([
        {
          localId: 3,
          clientId: 'client-003',
          storeId: 'store-001',
          terminalId: 'terminal-001',
          staffId: 'staff-001',
          items: [],
          payments: [],
          createdAt: '2026-01-01T12:00:00Z',
          syncStatus: 'pending',
        },
      ])
      mockApiPost.mockRejectedValue(new Error('Network error'))

      const result = await syncPendingTransactions()
      expect(result).toBeNull()

      const status = getSyncStatus()
      expect(status.lastError).toBe('Network error')
    })
  })

  describe('setupAutoSync', () => {
    it('解除関数を返す', () => {
      mockGetPendingTransactions.mockResolvedValue([])
      const cleanup = setupAutoSync()
      expect(typeof cleanup).toBe('function')
      cleanup()
    })

    it('online イベントで同期を開始する', () => {
      Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
      mockGetPendingTransactions.mockResolvedValue([])
      const cleanup = setupAutoSync()

      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
      window.dispatchEvent(new Event('online'))

      // syncPendingTransactions が内部で呼ばれる
      expect(mockGetPendingTransactions).toHaveBeenCalled()
      cleanup()
    })
  })
})
