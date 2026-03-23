import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getSyncStatus,
  subscribeSyncStatus,
  resetSyncRetries,
  acknowledgePriceConflicts,
  applyCurrentPrices,
  syncPendingTransactions,
  setupAutoSync,
} from './sync-manager'
import type { OfflineTransactionItem } from './offline-db'

vi.mock('@/lib/api', () => ({
  api: {
    post: vi.fn(),
    setBaseUrl: vi.fn(),
    setOrganizationId: vi.fn(),
  },
  configureApi: vi.fn(),
  getDefaultApiConfig: () => ({
    apiUrl: 'http://localhost:8080',
    organizationId: null,
  }),
}))

vi.mock('@/lib/offline-db', () => ({
  getPendingTransactions: vi.fn().mockResolvedValue([]),
  updateTransactionSyncStatus: vi.fn().mockResolvedValue(undefined),
  cleanupSyncedTransactions: vi.fn().mockResolvedValue(0),
  getCachedProducts: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/lib/logger', () => ({
  createLogger: () => ({
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  }),
}))

describe('sync-manager', () => {
  beforeEach(() => {
    resetSyncRetries()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('getSyncStatus', () => {
    it('初期状態は idle', () => {
      const status = getSyncStatus()
      expect(status.state).toBe('idle')
      expect(status.isSyncing).toBe(false)
      expect(status.pendingCount).toBe(0)
      expect(status.lastSyncAt).toBeNull()
      expect(status.lastError).toBeNull()
      expect(status.retryCount).toBe(0)
      expect(status.priceConflicts).toEqual([])
    })
  })

  describe('subscribeSyncStatus', () => {
    it('リスナーを登録・解除できる', () => {
      const listener = vi.fn()
      const unsubscribe = subscribeSyncStatus(listener)

      // resetSyncRetries triggers notifyListeners
      resetSyncRetries()
      expect(listener).toHaveBeenCalled()

      listener.mockClear()
      unsubscribe()
      resetSyncRetries()
      expect(listener).not.toHaveBeenCalled()
    })
  })

  describe('resetSyncRetries', () => {
    it('状態を idle にリセットする', () => {
      resetSyncRetries()
      const status = getSyncStatus()
      expect(status.state).toBe('idle')
      expect(status.retryCount).toBe(0)
      expect(status.lastError).toBeNull()
      expect(status.priceConflicts).toEqual([])
    })
  })

  describe('acknowledgePriceConflicts', () => {
    it('価格競合をクリアする', () => {
      const listener = vi.fn()
      subscribeSyncStatus(listener)
      acknowledgePriceConflicts()
      expect(getSyncStatus().priceConflicts).toEqual([])
      expect(listener).toHaveBeenCalled()
    })
  })

  describe('applyCurrentPrices', () => {
    it('価格が変更された場合に更新する', () => {
      const items: OfflineTransactionItem[] = [
        {
          productId: 'prod-1',
          productName: 'テスト商品',
          unitPrice: 10000,
          quantity: 1,
          taxRateName: '標準税率',
          taxRate: '0.10',
          isReducedTax: false,
        },
      ]
      const priceMap = new Map([['prod-1', 12000]])
      const updated = applyCurrentPrices(items, priceMap)

      expect(updated[0]!.unitPrice).toBe(12000)
    })

    it('価格に変更がない場合は元のオブジェクトを返す', () => {
      const items: OfflineTransactionItem[] = [
        {
          productId: 'prod-1',
          productName: 'テスト商品',
          unitPrice: 10000,
          quantity: 1,
          taxRateName: '標準税率',
          taxRate: '0.10',
          isReducedTax: false,
        },
      ]
      const priceMap = new Map([['prod-1', 10000]])
      const updated = applyCurrentPrices(items, priceMap)

      expect(updated[0]).toBe(items[0])
    })

    it('priceMap に存在しない商品は変更しない', () => {
      const items: OfflineTransactionItem[] = [
        {
          productId: 'prod-unknown',
          productName: 'テスト商品',
          unitPrice: 10000,
          quantity: 1,
          taxRateName: '標準税率',
          taxRate: '0.10',
          isReducedTax: false,
        },
      ]
      const priceMap = new Map<string, number>()
      const updated = applyCurrentPrices(items, priceMap)

      expect(updated[0]).toBe(items[0])
    })
  })

  describe('syncPendingTransactions', () => {
    it('オフラインの場合は null を返す', async () => {
      Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
      const result = await syncPendingTransactions()
      expect(result).toBeNull()
      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
    })

    it('pending 取引がない場合は completed になる', async () => {
      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
      const result = await syncPendingTransactions()
      expect(result).toBeNull()
      expect(getSyncStatus().state).toBe('completed')
    })

    it('pending 取引がある場合に API を呼んで結果を処理する', async () => {
      const {
        getPendingTransactions,
        updateTransactionSyncStatus,
        cleanupSyncedTransactions,
        getCachedProducts,
      } = await import('@/lib/offline-db')
      const { api } = await import('@/lib/api')

      const pendingTx = {
        localId: 1,
        clientId: 'tx-1',
        storeId: 'store-1',
        terminalId: 'terminal-1',
        staffId: 'staff-1',
        items: [
          {
            productId: 'prod-1',
            productName: 'テスト商品',
            unitPrice: 10000,
            quantity: 1,
            taxRateName: '標準税率',
            taxRate: '0.10',
            isReducedTax: false,
          },
        ],
        payments: [{ method: 'CASH', amount: 10000 }],
        createdAt: '2026-01-01T00:00:00Z',
        syncStatus: 'pending' as const,
      }

      vi.mocked(getPendingTransactions).mockResolvedValueOnce([pendingTx])
      vi.mocked(getCachedProducts).mockResolvedValueOnce([])
      vi.mocked(api.post).mockResolvedValueOnce({
        results: [{ clientId: 'tx-1', success: true, transactionId: 'server-tx-1' }],
      })

      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
      const result = await syncPendingTransactions()

      expect(result).toBeTruthy()
      expect(result?.results).toHaveLength(1)
      expect(updateTransactionSyncStatus).toHaveBeenCalledWith(1, 'synced', 'server-tx-1')
      expect(cleanupSyncedTransactions).toHaveBeenCalled()
      expect(getSyncStatus().state).toBe('completed')
    })

    it('同期失敗の取引を failed に更新する', async () => {
      const { getPendingTransactions, updateTransactionSyncStatus, getCachedProducts } =
        await import('@/lib/offline-db')
      const { api } = await import('@/lib/api')

      const pendingTx = {
        localId: 2,
        clientId: 'tx-2',
        storeId: 'store-1',
        terminalId: 'terminal-1',
        staffId: 'staff-1',
        items: [],
        payments: [],
        createdAt: '2026-01-01T00:00:00Z',
        syncStatus: 'pending' as const,
      }

      vi.mocked(getPendingTransactions).mockResolvedValueOnce([pendingTx])
      vi.mocked(getCachedProducts).mockResolvedValueOnce([])
      vi.mocked(api.post).mockResolvedValueOnce({
        results: [{ clientId: 'tx-2', success: false, error: 'Invalid data' }],
      })

      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
      await syncPendingTransactions()

      expect(updateTransactionSyncStatus).toHaveBeenCalledWith(
        2,
        'failed',
        undefined,
        'Invalid data',
      )
    })

    it('API エラー時にリトライ状態に遷移する', async () => {
      const { getPendingTransactions, getCachedProducts } = await import('@/lib/offline-db')
      const { api } = await import('@/lib/api')

      vi.mocked(getPendingTransactions).mockResolvedValueOnce([
        {
          localId: 3,
          clientId: 'tx-3',
          storeId: 'store-1',
          terminalId: 'terminal-1',
          staffId: 'staff-1',
          items: [],
          payments: [],
          createdAt: '2026-01-01T00:00:00Z',
          syncStatus: 'pending' as const,
        },
      ])
      vi.mocked(getCachedProducts).mockResolvedValueOnce([])
      vi.mocked(api.post).mockRejectedValueOnce(new Error('Network error'))

      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
      await syncPendingTransactions()

      const status = getSyncStatus()
      expect(status.state).toBe('retrying')
      expect(status.lastError).toBe('Network error')
    })

    it('価格競合を検出する', async () => {
      const { getPendingTransactions, getCachedProducts } = await import('@/lib/offline-db')
      const { api } = await import('@/lib/api')

      vi.mocked(getPendingTransactions).mockResolvedValueOnce([
        {
          localId: 4,
          clientId: 'tx-4',
          storeId: 'store-1',
          terminalId: 'terminal-1',
          staffId: 'staff-1',
          items: [
            {
              productId: 'prod-1',
              productName: 'テスト商品',
              unitPrice: 10000,
              quantity: 1,
              taxRateName: '標準税率',
              taxRate: '0.10',
              isReducedTax: false,
            },
          ],
          payments: [{ method: 'CASH', amount: 10000 }],
          createdAt: '2026-01-01T00:00:00Z',
          syncStatus: 'pending' as const,
        },
      ])
      vi.mocked(getCachedProducts).mockResolvedValueOnce([
        {
          id: 'prod-1',
          name: 'テスト商品',
          barcode: null,
          sku: null,
          price: 12000,
          categoryId: null,
          taxRateName: '標準税率',
          taxRate: '0.10',
          isReducedTax: false,
          imageUrl: null,
          displayOrder: 0,
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ])
      vi.mocked(api.post).mockResolvedValueOnce({
        results: [{ clientId: 'tx-4', success: true, transactionId: 'server-tx-4' }],
      })

      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
      await syncPendingTransactions()

      const status = getSyncStatus()
      expect(status.priceConflicts).toHaveLength(1)
      expect(status.priceConflicts[0]?.clientId).toBe('tx-4')
    })

    it('既に同期中の場合は null を返す', async () => {
      const { getPendingTransactions } = await import('@/lib/offline-db')
      await import('@/lib/api')

      // Create a deferred promise for the first call
      let resolveFirst: (() => void) | undefined
      const firstCallPromise = new Promise<void>((resolve) => {
        resolveFirst = resolve
      })

      vi.mocked(getPendingTransactions).mockImplementationOnce(async () => {
        await firstCallPromise
        return []
      })

      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })

      // Start first sync (will be pending)
      const firstPromise = syncPendingTransactions()

      // Try second sync while first is in progress
      const secondResult = await syncPendingTransactions()
      expect(secondResult).toBeNull()

      // Resolve first
      resolveFirst?.()
      await firstPromise
    })
  })

  describe('setupAutoSync', () => {
    it('cleanup 関数を返す', () => {
      Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
      const cleanup = setupAutoSync()
      expect(typeof cleanup).toBe('function')
      cleanup()
    })
  })

  describe('calculateBackoffMs (via retry behavior)', () => {
    it('リトライカウントが増加する', async () => {
      const { getPendingTransactions, getCachedProducts } = await import('@/lib/offline-db')
      const { api } = await import('@/lib/api')

      vi.mocked(getPendingTransactions).mockResolvedValue([
        {
          localId: 5,
          clientId: 'tx-5',
          storeId: 'store-1',
          terminalId: 'terminal-1',
          staffId: 'staff-1',
          items: [],
          payments: [],
          createdAt: '2026-01-01T00:00:00Z',
          syncStatus: 'pending' as const,
        },
      ])
      vi.mocked(getCachedProducts).mockResolvedValue([])
      vi.mocked(api.post).mockRejectedValue(new Error('fail'))

      Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
      await syncPendingTransactions()

      expect(getSyncStatus().retryCount).toBeGreaterThan(0)
    })
  })
})
