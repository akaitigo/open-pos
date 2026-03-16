import { describe, it, expect, vi, beforeEach } from 'vitest'

// Dexie をモックして IndexedDB 依存を除去
const mockBulkPut = vi.fn().mockResolvedValue(undefined)
const mockToArray = vi.fn().mockResolvedValue([])
const mockFirst = vi.fn().mockResolvedValue(undefined)
const mockFilter = vi.fn().mockReturnValue({
  toArray: vi.fn().mockResolvedValue([]),
})
const mockEquals = vi.fn().mockReturnValue({
  first: mockFirst,
  toArray: mockToArray,
  filter: mockFilter,
})
const mockWhere = vi.fn().mockReturnValue({
  equals: mockEquals,
})
const mockAdd = vi.fn().mockResolvedValue(1)
const mockUpdate = vi.fn().mockResolvedValue(1)
const mockBulkDelete = vi.fn().mockResolvedValue(undefined)

const mockProducts = {
  bulkPut: mockBulkPut,
  toArray: mockToArray,
  where: mockWhere,
}

const mockOfflineTransactions = {
  add: mockAdd,
  where: mockWhere,
  update: mockUpdate,
  bulkDelete: mockBulkDelete,
}

vi.mock('dexie', () => {
  class FakeDexie {
    version() {
      return { stores: vi.fn() }
    }
  }
  return { default: FakeDexie }
})

// dexie モック後にインポート
const mod = await import('./offline-db')
const {
  cacheProducts,
  getCachedProducts,
  getCachedProductByBarcode,
  saveOfflineTransaction,
  getPendingTransactions,
  updateTransactionSyncStatus,
  cleanupSyncedTransactions,
  offlineDb,
} = mod

// offlineDb の products/offlineTransactions を直接モック
Object.defineProperty(offlineDb, 'products', { value: mockProducts, writable: true })
Object.defineProperty(offlineDb, 'offlineTransactions', {
  value: mockOfflineTransactions,
  writable: true,
})

import type { CachedProduct, OfflineTransaction } from './offline-db'

const mockCachedProduct: CachedProduct = {
  id: '550e8400-e29b-41d4-a716-446655440001',
  name: 'テスト商品',
  barcode: '4901234567890',
  sku: 'SKU-001',
  price: 15000,
  categoryId: null,
  taxRateName: '標準税率',
  taxRate: '0.10',
  isReducedTax: false,
  imageUrl: null,
  displayOrder: 0,
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('offline-db', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // リセット後にデフォルト値を再設定
    mockToArray.mockResolvedValue([])
    mockFirst.mockResolvedValue(undefined)
    mockFilter.mockReturnValue({ toArray: vi.fn().mockResolvedValue([]) })
    mockEquals.mockReturnValue({
      first: mockFirst,
      toArray: mockToArray,
      filter: mockFilter,
    })
  })

  describe('cacheProducts', () => {
    it('商品一覧をバルクで保存する', async () => {
      await cacheProducts([mockCachedProduct])
      expect(mockBulkPut).toHaveBeenCalledWith([mockCachedProduct])
    })

    it('空配列でも正常に動作する', async () => {
      await cacheProducts([])
      expect(mockBulkPut).toHaveBeenCalledWith([])
    })
  })

  describe('getCachedProducts', () => {
    it('キャッシュ済み商品を全て取得する', async () => {
      mockToArray.mockResolvedValueOnce([mockCachedProduct])
      const result = await getCachedProducts()
      expect(result).toEqual([mockCachedProduct])
    })

    it('空の場合は空配列を返す', async () => {
      mockToArray.mockResolvedValueOnce([])
      const result = await getCachedProducts()
      expect(result).toEqual([])
    })
  })

  describe('getCachedProductByBarcode', () => {
    it('バーコードで商品を検索する', async () => {
      const localFirst = vi.fn().mockResolvedValue(mockCachedProduct)
      mockEquals.mockReturnValueOnce({ first: localFirst, toArray: vi.fn(), filter: vi.fn() })
      const result = await getCachedProductByBarcode('4901234567890')
      expect(mockWhere).toHaveBeenCalledWith('barcode')
      expect(result).toEqual(mockCachedProduct)
    })

    it('見つからない場合は undefined を返す', async () => {
      const localFirst = vi.fn().mockResolvedValue(undefined)
      mockEquals.mockReturnValueOnce({ first: localFirst, toArray: vi.fn(), filter: vi.fn() })
      const result = await getCachedProductByBarcode('0000000000000')
      expect(result).toBeUndefined()
    })
  })

  describe('saveOfflineTransaction', () => {
    it('オフライン取引を保存してIDを返す', async () => {
      mockAdd.mockResolvedValueOnce(42)
      const tx: Omit<OfflineTransaction, 'localId'> = {
        clientId: 'client-uuid-001',
        storeId: 'store-001',
        terminalId: 'terminal-001',
        staffId: 'staff-001',
        items: [],
        payments: [],
        createdAt: '2026-01-01T12:00:00Z',
        syncStatus: 'pending',
      }
      const id = await saveOfflineTransaction(tx)
      expect(id).toBe(42)
      expect(mockAdd).toHaveBeenCalled()
    })

    it('add が undefined を返した場合は 0 を返す', async () => {
      mockAdd.mockResolvedValueOnce(undefined)
      const tx: Omit<OfflineTransaction, 'localId'> = {
        clientId: 'client-uuid-002',
        storeId: 'store-001',
        terminalId: 'terminal-001',
        staffId: 'staff-001',
        items: [],
        payments: [],
        createdAt: '2026-01-01T12:00:00Z',
        syncStatus: 'pending',
      }
      const id = await saveOfflineTransaction(tx)
      expect(id).toBe(0)
    })
  })

  describe('getPendingTransactions', () => {
    it('未同期の取引を取得する', async () => {
      const pendingTx: OfflineTransaction = {
        localId: 1,
        clientId: 'client-uuid-001',
        storeId: 'store-001',
        terminalId: 'terminal-001',
        staffId: 'staff-001',
        items: [],
        payments: [],
        createdAt: '2026-01-01T12:00:00Z',
        syncStatus: 'pending',
      }
      const localToArray = vi.fn().mockResolvedValue([pendingTx])
      mockEquals.mockReturnValueOnce({
        toArray: localToArray,
        first: vi.fn(),
        filter: vi.fn(),
      })
      const result = await getPendingTransactions()
      expect(result).toEqual([pendingTx])
    })
  })

  describe('updateTransactionSyncStatus', () => {
    it('同期ステータスを synced に更新する', async () => {
      await updateTransactionSyncStatus(1, 'synced', 'server-tx-001')
      expect(mockUpdate).toHaveBeenCalledWith(1, {
        syncStatus: 'synced',
        serverTransactionId: 'server-tx-001',
        syncError: undefined,
      })
    })

    it('同期ステータスを failed に更新する', async () => {
      await updateTransactionSyncStatus(1, 'failed', undefined, 'Network error')
      expect(mockUpdate).toHaveBeenCalledWith(1, {
        syncStatus: 'failed',
        serverTransactionId: undefined,
        syncError: 'Network error',
      })
    })
  })

  describe('cleanupSyncedTransactions', () => {
    it('同期済みの古い取引を削除する', async () => {
      const oldTx: OfflineTransaction = {
        localId: 10,
        clientId: 'client-old',
        storeId: 'store-001',
        terminalId: 'terminal-001',
        staffId: 'staff-001',
        items: [],
        payments: [],
        createdAt: '2020-01-01T00:00:00Z',
        syncStatus: 'synced',
      }
      const localFilter = vi.fn().mockReturnValue({
        toArray: vi.fn().mockResolvedValue([oldTx]),
      })
      mockEquals.mockReturnValueOnce({
        filter: localFilter,
        toArray: vi.fn(),
        first: vi.fn(),
      })
      const count = await cleanupSyncedTransactions(7)
      expect(count).toBe(1)
      expect(mockBulkDelete).toHaveBeenCalledWith([10])
    })
  })
})
