import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'

type MockFn = Mock

interface MockChain {
  bulkPut: MockFn
  toArray: MockFn
  first: MockFn
  add: MockFn
  update: MockFn
  bulkDelete: MockFn
  where: MockFn
  equals: MockFn
  filter: MockFn
}

const { mockProducts, mockOfflineTransactions } = vi.hoisted(() => {
  function makeChain(): MockChain {
    const chain = {} as MockChain
    chain.bulkPut = vi.fn().mockResolvedValue(undefined)
    chain.toArray = vi.fn().mockResolvedValue([])
    chain.first = vi.fn().mockResolvedValue(undefined)
    chain.add = vi.fn().mockResolvedValue(1)
    chain.update = vi.fn().mockResolvedValue(1)
    chain.bulkDelete = vi.fn().mockResolvedValue(undefined)
    chain.where = vi.fn().mockImplementation(() => chain)
    chain.equals = vi.fn().mockImplementation(() => chain)
    chain.filter = vi.fn().mockImplementation(() => chain)
    return chain
  }
  return { mockProducts: makeChain(), mockOfflineTransactions: makeChain() }
})

vi.mock('dexie', () => {
  return {
    default: class FakeDexie {
      constructor() {}

      version() {
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        const self = this
        return {
          stores() {
            // OpenPosOfflineDB declares fields with `!:` which under ES2022
            // useDefineForClassFields=true compiles to `this.x = undefined`,
            // overwriting class-field initialisers from the parent.  Re-assign
            // the mocks inside stores() which is called *after* the field init.
            ;(self as Record<string, unknown>).products = mockProducts
            ;(self as Record<string, unknown>).offlineTransactions = mockOfflineTransactions
          },
        }
      }
    },
  }
})

import {
  cacheProducts,
  getCachedProducts,
  getCachedProductByBarcode,
  saveOfflineTransaction,
  getPendingTransactions,
  updateTransactionSyncStatus,
  cleanupSyncedTransactions,
  offlineDb,
} from './offline-db'
import type { CachedProduct } from './offline-db'

const mockProduct: CachedProduct = {
  id: 'prod-1',
  name: 'テスト商品',
  barcode: '4901234567890',
  sku: 'SKU-001',
  price: 15000,
  categoryId: 'cat-1',
  taxRateName: '標準税率',
  taxRate: '0.10',
  isReducedTax: false,
  imageUrl: null,
  displayOrder: 0,
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('offline-db', () => {
  beforeEach(() => {
    for (const table of [mockProducts, mockOfflineTransactions]) {
      table.bulkPut.mockClear()
      table.toArray.mockReset().mockResolvedValue([])
      table.first.mockReset().mockResolvedValue(undefined)
      table.add.mockReset().mockResolvedValue(1)
      table.update.mockClear()
      table.bulkDelete.mockClear()
      table.where.mockClear().mockImplementation(() => table)
      table.equals.mockClear().mockImplementation(() => table)
      table.filter.mockClear().mockImplementation(() => table)
    }
  })

  describe('cacheProducts', () => {
    it('bulkPut で商品をキャッシュする', async () => {
      await cacheProducts([mockProduct])
      expect(offlineDb.products.bulkPut).toHaveBeenCalledWith([mockProduct])
    })

    it('空配列でも呼び出せる', async () => {
      await cacheProducts([])
      expect(offlineDb.products.bulkPut).toHaveBeenCalledWith([])
    })
  })

  describe('getCachedProducts', () => {
    it('toArray で全商品を取得する', async () => {
      mockProducts.toArray.mockResolvedValueOnce([mockProduct])
      const products = await getCachedProducts()
      expect(products).toEqual([mockProduct])
    })
  })

  describe('getCachedProductByBarcode', () => {
    it('バーコードで商品を検索する', async () => {
      mockProducts.first.mockResolvedValueOnce(mockProduct)
      const found = await getCachedProductByBarcode('4901234567890')
      expect(found).toEqual(mockProduct)
    })

    it('見つからない場合は undefined を返す', async () => {
      const found = await getCachedProductByBarcode('9999999999999')
      expect(found).toBeUndefined()
    })
  })

  describe('saveOfflineTransaction', () => {
    it('取引を保存して localId を返す', async () => {
      mockOfflineTransactions.add.mockResolvedValueOnce(42)
      const tx = {
        clientId: 'tx-1',
        storeId: 'store-1',
        terminalId: 'terminal-1',
        staffId: 'staff-1',
        items: [],
        payments: [],
        createdAt: new Date().toISOString(),
        syncStatus: 'pending' as const,
      }
      const localId = await saveOfflineTransaction(tx)
      expect(localId).toBe(42)
    })

    it('localId が undefined の場合は 0 を返す', async () => {
      mockOfflineTransactions.add.mockResolvedValueOnce(undefined)
      const tx = {
        clientId: 'tx-2',
        storeId: 'store-1',
        terminalId: 'terminal-1',
        staffId: 'staff-1',
        items: [],
        payments: [],
        createdAt: new Date().toISOString(),
        syncStatus: 'pending' as const,
      }
      const localId = await saveOfflineTransaction(tx)
      expect(localId).toBe(0)
    })
  })

  describe('getPendingTransactions', () => {
    it('pending の取引を取得する', async () => {
      const pendingTx = { localId: 1, clientId: 'tx-1', syncStatus: 'pending' }
      mockOfflineTransactions.toArray.mockResolvedValueOnce([pendingTx])
      const result = await getPendingTransactions()
      expect(result).toEqual([pendingTx])
    })
  })

  describe('updateTransactionSyncStatus', () => {
    it('synced に更新する', async () => {
      await updateTransactionSyncStatus(1, 'synced', 'server-tx-123')
      expect(offlineDb.offlineTransactions.update).toHaveBeenCalledWith(1, {
        syncStatus: 'synced',
        serverTransactionId: 'server-tx-123',
        syncError: undefined,
      })
    })

    it('failed に更新する', async () => {
      await updateTransactionSyncStatus(1, 'failed', undefined, 'Server error')
      expect(offlineDb.offlineTransactions.update).toHaveBeenCalledWith(1, {
        syncStatus: 'failed',
        serverTransactionId: undefined,
        syncError: 'Server error',
      })
    })
  })

  describe('cleanupSyncedTransactions', () => {
    it('古い synced 取引を削除する', async () => {
      const oldTx = { localId: 1, syncStatus: 'synced', createdAt: '2020-01-01T00:00:00Z' }
      mockOfflineTransactions.filter.mockImplementationOnce(() => ({
        toArray: vi.fn().mockResolvedValueOnce([oldTx]),
      }))
      const deleted = await cleanupSyncedTransactions(7)
      expect(deleted).toBe(1)
      expect(offlineDb.offlineTransactions.bulkDelete).toHaveBeenCalledWith([1])
    })

    it('localId が undefined の取引は除外する', async () => {
      const oldTx = { syncStatus: 'synced', createdAt: '2020-01-01T00:00:00Z' }
      mockOfflineTransactions.filter.mockImplementationOnce(() => ({
        toArray: vi.fn().mockResolvedValueOnce([oldTx]),
      }))
      const deleted = await cleanupSyncedTransactions(7)
      expect(deleted).toBe(0)
      expect(offlineDb.offlineTransactions.bulkDelete).toHaveBeenCalledWith([])
    })

    it('デフォルトの日数パラメータで動作する', async () => {
      mockOfflineTransactions.filter.mockImplementationOnce(() => ({
        toArray: vi.fn().mockResolvedValueOnce([]),
      }))
      const deleted = await cleanupSyncedTransactions()
      expect(deleted).toBe(0)
    })
  })
})
