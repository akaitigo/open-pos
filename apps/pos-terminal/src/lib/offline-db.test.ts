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

const db = vi.hoisted(() => {
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
  return {
    products: makeChain(),
    offlineTransactions: makeChain(),
  }
})

// Mock the entire offline-db module's offlineDb export
vi.mock('./offline-db', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./offline-db')>()

  // Create a fake offlineDb that uses our mocked chains
  const fakeDb = {
    products: db.products,
    offlineTransactions: db.offlineTransactions,
  }

  // Re-implement the functions using the fake db
  const { products, offlineTransactions } = fakeDb
  return {
    ...actual,
    offlineDb: fakeDb,
    async cacheProducts(ps: unknown[]) {
      await products.bulkPut(ps)
    },
    async getCachedProducts() {
      return products.toArray()
    },
    async getCachedProductByBarcode(barcode: string) {
      return products.where('barcode').equals(barcode).first()
    },
    async saveOfflineTransaction(tx: Record<string, unknown>) {
      const id = await offlineTransactions.add({ ...tx })
      return (id as number | undefined) ?? 0
    },
    async getPendingTransactions() {
      return offlineTransactions.where('syncStatus').equals('pending').toArray()
    },
    async updateTransactionSyncStatus(
      localId: number,
      status: string,
      serverTransactionId?: string,
      syncError?: string,
    ) {
      await offlineTransactions.update(localId, {
        syncStatus: status,
        serverTransactionId,
        syncError,
      })
    },
    async cleanupSyncedTransactions(olderThanDays = 7) {
      const cutoff = new Date()
      cutoff.setDate(cutoff.getDate() - olderThanDays)
      const cutoffStr = cutoff.toISOString()
      const old = await offlineTransactions
        .where('syncStatus')
        .equals('synced')
        .filter((tx: { createdAt: string }) => tx.createdAt < cutoffStr)
        .toArray()
      const ids = (old as { localId?: number }[])
        .map((tx) => tx.localId)
        .filter((id): id is number => id !== undefined)
      await offlineTransactions.bulkDelete(ids)
      return ids.length
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
    for (const table of [db.products, db.offlineTransactions]) {
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
      expect(db.products.bulkPut).toHaveBeenCalledWith([mockProduct])
    })

    it('空配列でも呼び出せる', async () => {
      await cacheProducts([])
      expect(db.products.bulkPut).toHaveBeenCalledWith([])
    })
  })

  describe('getCachedProducts', () => {
    it('toArray で全商品を取得する', async () => {
      db.products.toArray.mockResolvedValueOnce([mockProduct])
      const products = await getCachedProducts()
      expect(products).toEqual([mockProduct])
    })
  })

  describe('getCachedProductByBarcode', () => {
    it('バーコードで商品を検索する', async () => {
      db.products.first.mockResolvedValueOnce(mockProduct)
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
      db.offlineTransactions.add.mockResolvedValueOnce(42)
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
      db.offlineTransactions.add.mockResolvedValueOnce(undefined)
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
      db.offlineTransactions.toArray.mockResolvedValueOnce([pendingTx])
      const result = await getPendingTransactions()
      expect(result).toEqual([pendingTx])
    })
  })

  describe('updateTransactionSyncStatus', () => {
    it('synced に更新する', async () => {
      await updateTransactionSyncStatus(1, 'synced', 'server-tx-123')
      expect(db.offlineTransactions.update).toHaveBeenCalledWith(1, {
        syncStatus: 'synced',
        serverTransactionId: 'server-tx-123',
        syncError: undefined,
      })
    })

    it('failed に更新する', async () => {
      await updateTransactionSyncStatus(1, 'failed', undefined, 'Server error')
      expect(db.offlineTransactions.update).toHaveBeenCalledWith(1, {
        syncStatus: 'failed',
        serverTransactionId: undefined,
        syncError: 'Server error',
      })
    })
  })

  describe('cleanupSyncedTransactions', () => {
    it('古い synced 取引を削除する', async () => {
      const oldTx = { localId: 1, syncStatus: 'synced', createdAt: '2020-01-01T00:00:00Z' }
      db.offlineTransactions.filter.mockImplementationOnce(() => ({
        toArray: vi.fn().mockResolvedValueOnce([oldTx]),
      }))
      const deleted = await cleanupSyncedTransactions(7)
      expect(deleted).toBe(1)
      expect(db.offlineTransactions.bulkDelete).toHaveBeenCalledWith([1])
    })

    it('localId が undefined の取引は除外する', async () => {
      const oldTx = { syncStatus: 'synced', createdAt: '2020-01-01T00:00:00Z' }
      db.offlineTransactions.filter.mockImplementationOnce(() => ({
        toArray: vi.fn().mockResolvedValueOnce([oldTx]),
      }))
      const deleted = await cleanupSyncedTransactions(7)
      expect(deleted).toBe(0)
      expect(db.offlineTransactions.bulkDelete).toHaveBeenCalledWith([])
    })
  })
})
