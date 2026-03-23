import { describe, it, expect, beforeEach } from 'vitest'
import { useParkingStore } from './parking-store'
import type { CartItem } from './cart-store'
import type { Product } from '@shared-types/openpos'

const mockProduct: Product = {
  id: '550e8400-e29b-41d4-a716-446655440001',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: 'テスト商品',
  price: 15000,
  displayOrder: 0,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockProduct2: Product = {
  id: '550e8400-e29b-41d4-a716-446655440002',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: 'テスト商品2',
  price: 20000,
  displayOrder: 1,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

function makeItems(...products: Product[]): CartItem[] {
  return products.map((product) => ({ product, quantity: 1 }))
}

describe('parking-store', () => {
  beforeEach(() => {
    useParkingStore.setState({ parkedTransactions: [] })
  })

  describe('parkTransaction', () => {
    it('カートの商品を保留できる', () => {
      const items = makeItems(mockProduct)
      const result = useParkingStore.getState().parkTransaction(items)
      expect(result).toBe(true)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(1)
    })

    it('保留時にデフォルトラベルが付与される', () => {
      const items = makeItems(mockProduct)
      useParkingStore.getState().parkTransaction(items)
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.label).toBe('保留 #1')
    })

    it('カスタムラベルを指定できる', () => {
      const items = makeItems(mockProduct)
      useParkingStore.getState().parkTransaction(items, 'お客様A')
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.label).toBe('お客様A')
    })

    it('空のカートは保留できない', () => {
      const result = useParkingStore.getState().parkTransaction([])
      expect(result).toBe(false)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(0)
    })

    it('上限5件を超えると保留できない', () => {
      const items = makeItems(mockProduct)
      for (let i = 0; i < 5; i++) {
        useParkingStore.getState().parkTransaction(items)
      }
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(5)

      const result = useParkingStore.getState().parkTransaction(items)
      expect(result).toBe(false)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(5)
    })

    it('保留された商品はディープコピーされる', () => {
      const items = makeItems(mockProduct)
      useParkingStore.getState().parkTransaction(items)
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.items).toEqual(items)
      expect(parked.items).not.toBe(items)
    })

    it('保留時にISO形式のタイムスタンプが記録される', () => {
      const items = makeItems(mockProduct)
      useParkingStore.getState().parkTransaction(items)
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.parkedAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/)
    })
  })

  describe('resumeTransaction', () => {
    it('保留中の取引を再開できる', () => {
      const items = makeItems(mockProduct, mockProduct2)
      useParkingStore.getState().parkTransaction(items)
      const parked = useParkingStore.getState().parkedTransactions[0]!

      const resumed = useParkingStore.getState().resumeTransaction(parked.id)
      expect(resumed).toEqual(items)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(0)
    })

    it('存在しないIDを指定するとnullを返す', () => {
      const result = useParkingStore.getState().resumeTransaction('non-existent')
      expect(result).toBeNull()
    })

    it('再開後は保留リストから削除される', () => {
      const items1 = makeItems(mockProduct)
      const items2 = makeItems(mockProduct2)
      useParkingStore.getState().parkTransaction(items1, '取引A')
      useParkingStore.getState().parkTransaction(items2, '取引B')

      const first = useParkingStore.getState().parkedTransactions[0]!
      useParkingStore.getState().resumeTransaction(first.id)

      expect(useParkingStore.getState().parkedTransactions).toHaveLength(1)
      expect(useParkingStore.getState().parkedTransactions[0]!.label).toBe('取引B')
    })
  })

  describe('removeParkedTransaction', () => {
    it('保留中の取引を削除できる', () => {
      const items = makeItems(mockProduct)
      useParkingStore.getState().parkTransaction(items)
      const parked = useParkingStore.getState().parkedTransactions[0]!

      useParkingStore.getState().removeParkedTransaction(parked.id)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(0)
    })

    it('存在しないIDを指定しても例外にならない', () => {
      useParkingStore.getState().removeParkedTransaction('non-existent')
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(0)
    })
  })
})
