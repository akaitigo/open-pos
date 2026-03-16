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

const mockItems: CartItem[] = [
  { product: mockProduct, quantity: 2 },
  { product: mockProduct2, quantity: 1 },
]

describe('parking-store', () => {
  beforeEach(() => {
    useParkingStore.setState({ parkedTransactions: [] })
  })

  describe('parkTransaction', () => {
    it('取引を保留できる', () => {
      const result = useParkingStore.getState().parkTransaction(mockItems)
      expect(result).toBe(true)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(1)
    })

    it('保留した取引のアイテムが正しく保存される', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.items).toHaveLength(2)
      expect(parked.items[0]!.product.id).toBe(mockProduct.id)
      expect(parked.items[0]!.quantity).toBe(2)
    })

    it('ラベルを指定できる', () => {
      useParkingStore.getState().parkTransaction(mockItems, '山田様')
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.label).toBe('山田様')
    })

    it('ラベル未指定時はデフォルトラベルが付与される', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.label).toBe('保留 #1')
    })

    it('空カートは保留できない', () => {
      const result = useParkingStore.getState().parkTransaction([])
      expect(result).toBe(false)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(0)
    })

    it('最大5件まで保留できる', () => {
      for (let i = 0; i < 5; i++) {
        const result = useParkingStore.getState().parkTransaction(mockItems)
        expect(result).toBe(true)
      }
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(5)

      // 6件目は失敗
      const result = useParkingStore.getState().parkTransaction(mockItems)
      expect(result).toBe(false)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(5)
    })

    it('保留時にparkedAtが設定される', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      const parked = useParkingStore.getState().parkedTransactions[0]!
      expect(parked.parkedAt).toBeTruthy()
      // ISO 8601 形式であること
      expect(() => new Date(parked.parkedAt)).not.toThrow()
    })

    it('保留時にユニークなIDが生成される', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      useParkingStore.getState().parkTransaction(mockItems)
      const ids = useParkingStore.getState().parkedTransactions.map((t) => t.id)
      expect(new Set(ids).size).toBe(2)
    })
  })

  describe('resumeTransaction', () => {
    it('保留した取引を再開できる', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      const parked = useParkingStore.getState().parkedTransactions[0]!

      const resumed = useParkingStore.getState().resumeTransaction(parked.id)
      expect(resumed).not.toBeNull()
      expect(resumed).toHaveLength(2)
      expect(resumed![0]!.product.id).toBe(mockProduct.id)
    })

    it('再開した取引は保留リストから削除される', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      const parked = useParkingStore.getState().parkedTransactions[0]!

      useParkingStore.getState().resumeTransaction(parked.id)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(0)
    })

    it('存在しないIDで再開すると null を返す', () => {
      const result = useParkingStore.getState().resumeTransaction('non-existent-id')
      expect(result).toBeNull()
    })

    it('複数の保留から特定の取引のみ再開できる', () => {
      useParkingStore.getState().parkTransaction(mockItems, '取引A')
      useParkingStore.getState().parkTransaction([{ product: mockProduct2, quantity: 3 }], '取引B')

      const parkedA = useParkingStore.getState().parkedTransactions[0]!
      useParkingStore.getState().resumeTransaction(parkedA.id)

      expect(useParkingStore.getState().parkedTransactions).toHaveLength(1)
      expect(useParkingStore.getState().parkedTransactions[0]!.label).toBe('取引B')
    })
  })

  describe('removeParkedTransaction', () => {
    it('保留した取引を削除できる', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      const parked = useParkingStore.getState().parkedTransactions[0]!

      useParkingStore.getState().removeParkedTransaction(parked.id)
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(0)
    })

    it('存在しないIDで削除してもエラーにならない', () => {
      useParkingStore.getState().parkTransaction(mockItems)
      useParkingStore.getState().removeParkedTransaction('non-existent-id')
      expect(useParkingStore.getState().parkedTransactions).toHaveLength(1)
    })

    it('特定の取引のみ削除される', () => {
      useParkingStore.getState().parkTransaction(mockItems, '取引1')
      useParkingStore.getState().parkTransaction(mockItems, '取引2')
      useParkingStore.getState().parkTransaction(mockItems, '取引3')

      const parked2 = useParkingStore.getState().parkedTransactions[1]!
      useParkingStore.getState().removeParkedTransaction(parked2.id)

      const remaining = useParkingStore.getState().parkedTransactions
      expect(remaining).toHaveLength(2)
      expect(remaining.map((t) => t.label)).toEqual(['取引1', '取引3'])
    })
  })
})
