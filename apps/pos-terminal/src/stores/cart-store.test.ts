import { describe, it, expect, beforeEach } from 'vitest'
import { useCartStore, getCartSubtotal, getCartItemCount, type CartItem } from './cart-store'
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

describe('cart-store', () => {
  beforeEach(() => {
    useCartStore.setState({ items: [] })
  })

  it('addItem で商品を追加できる', () => {
    useCartStore.getState().addItem(mockProduct)
    const items = useCartStore.getState().items
    expect(items).toHaveLength(1)
    expect(items[0].product.id).toBe(mockProduct.id)
    expect(items[0].quantity).toBe(1)
  })

  it('addItem で同一商品を追加すると数量が増える', () => {
    useCartStore.getState().addItem(mockProduct)
    useCartStore.getState().addItem(mockProduct)
    const items = useCartStore.getState().items
    expect(items).toHaveLength(1)
    expect(items[0].quantity).toBe(2)
  })

  it('removeItem で商品を削除できる', () => {
    useCartStore.getState().addItem(mockProduct)
    useCartStore.getState().addItem(mockProduct2)
    useCartStore.getState().removeItem(mockProduct.id)
    const items = useCartStore.getState().items
    expect(items).toHaveLength(1)
    expect(items[0].product.id).toBe(mockProduct2.id)
  })

  it('updateQuantity で数量を更新できる', () => {
    useCartStore.getState().addItem(mockProduct)
    useCartStore.getState().updateQuantity(mockProduct.id, 5)
    expect(useCartStore.getState().items[0].quantity).toBe(5)
  })

  it('updateQuantity で 0 以下にすると商品が削除される', () => {
    useCartStore.getState().addItem(mockProduct)
    useCartStore.getState().updateQuantity(mockProduct.id, 0)
    expect(useCartStore.getState().items).toHaveLength(0)
  })

  it('clearCart でカートが空になる', () => {
    useCartStore.getState().addItem(mockProduct)
    useCartStore.getState().addItem(mockProduct2)
    useCartStore.getState().clearCart()
    expect(useCartStore.getState().items).toHaveLength(0)
  })
})

describe('getCartSubtotal', () => {
  it('小計を正しく計算する', () => {
    const items: CartItem[] = [
      { product: mockProduct, quantity: 2 },
      { product: mockProduct2, quantity: 1 },
    ]
    expect(getCartSubtotal(items)).toBe(50000)
  })

  it('空カートの小計は 0', () => {
    expect(getCartSubtotal([])).toBe(0)
  })
})

describe('getCartItemCount', () => {
  it('合計数量を正しく計算する', () => {
    const items: CartItem[] = [
      { product: mockProduct, quantity: 2 },
      { product: mockProduct2, quantity: 3 },
    ]
    expect(getCartItemCount(items)).toBe(5)
  })

  it('空カートの数量は 0', () => {
    expect(getCartItemCount([])).toBe(0)
  })
})
