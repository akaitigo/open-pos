import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getSyncStatus,
  subscribeSyncStatus,
  resetSyncRetries,
  acknowledgePriceConflicts,
  applyCurrentPrices,
  syncPendingTransactions,
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
  })
})
